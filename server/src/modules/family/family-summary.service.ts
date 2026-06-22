import { Injectable, ForbiddenException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import {
  normalizeVisibility,
  selfVisibility,
  type VisibilityConfig,
  type VisibilityLevel,
  type VisibilityMetric,
} from '../../common/dto/visibility.js';

/** 健康摘要状态标签 */
type StatusLabel = 'normal' | 'attention' | 'warning';

interface MemberSummary {
  userId: string;
  name: string;
  avatar: string | null;
  role: string;
  selfRole: string;
  status: StatusLabel;
  visibility: VisibilityConfig;
  metrics: {
    bp: { avgSystolic: number | null; avgDiastolic: number | null; count: number } | null;
    bg: { avgValue: number | null; count: number } | null;
    medication: { adherenceRate: number | null; total: number } | null;
  };
}

/**
 * 家庭看板汇总：成员摘要、默契值统计。
 *
 * 与 FamilyService 拆开是为了保留 FamilyService 的 CRUD 单一职责，
 * 同时方便 family-summary 复用 HealthMetrics/Medication 的查询逻辑。
 */
@Injectable()
export class FamilySummaryService {
  constructor(private readonly prisma: PrismaService) {}

  /** 风险阈值（与 alert 模块保持一致） */
  private static readonly BP_ATTENTION_SYS = 140;
  private static readonly BP_WARNING_SYS = 160;
  private static readonly BG_ATTENTION = 8.0;
  private static readonly BG_WARNING = 11.1;

  /**
   * 校验 viewer 是否家庭成员；返回其在家庭中的 membership 记录。
   */
  private async ensureMembership(familyId: string, viewerId: string) {
    const membership = await this.prisma.familyMember.findUnique({
      where: { familyId_userId: { familyId, userId: viewerId } },
    });
    if (!membership) {
      throw new ForbiddenException('你不是该家庭成员');
    }
    return membership;
  }

  /** 计算近 7 天血压均值 */
  private async last7DaysBp(userId: string) {
    const since = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const records = await this.prisma.bloodPressureRecord.findMany({
      where: { userId, recordedAt: { gte: since } },
      select: { systolic: true, diastolic: true },
    });
    if (records.length === 0) {
      return { avgSystolic: null, avgDiastolic: null, count: 0 };
    }
    const sumSys = records.reduce((s, r) => s + r.systolic, 0);
    const sumDia = records.reduce((s, r) => s + r.diastolic, 0);
    return {
      avgSystolic: Math.round(sumSys / records.length),
      avgDiastolic: Math.round(sumDia / records.length),
      count: records.length,
    };
  }

  /** 计算近 7 天血糖均值 */
  private async last7DaysBg(userId: string) {
    const since = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
    const records = await this.prisma.bloodSugarRecord.findMany({
      where: { userId, recordedAt: { gte: since } },
      select: { value: true },
    });
    if (records.length === 0) return { avgValue: null, count: 0 };
    const sum = records.reduce((s, r) => s + Number(r.value), 0);
    return {
      avgValue: parseFloat((sum / records.length).toFixed(1)),
      count: records.length,
    };
  }

  /** 计算本周服药完成率 */
  private async thisWeekAdherence(userId: string) {
    const since = new Date();
    const day = since.getDay() === 0 ? 6 : since.getDay() - 1;
    since.setDate(since.getDate() - day);
    since.setHours(0, 0, 0, 0);

    const records = await this.prisma.medicationRecord.findMany({
      where: { userId, scheduledTime: { gte: since } },
      select: { status: true },
    });

    if (records.length === 0) return { adherenceRate: null, total: 0 };
    const taken = records.filter((r) => r.status === 'taken').length;
    return {
      adherenceRate: parseFloat((taken / records.length).toFixed(2)),
      total: records.length,
    };
  }

  /** 综合评估状态标签 */
  private evaluateStatus(
    bp: { avgSystolic: number | null },
    bg: { avgValue: number | null },
  ): StatusLabel {
    const sys = bp.avgSystolic;
    const val = bg.avgValue;

    if (
      (sys !== null && sys >= FamilySummaryService.BP_WARNING_SYS) ||
      (val !== null && val >= FamilySummaryService.BG_WARNING)
    ) {
      return 'warning';
    }
    if (
      (sys !== null && sys >= FamilySummaryService.BP_ATTENTION_SYS) ||
      (val !== null && val >= FamilySummaryService.BG_ATTENTION)
    ) {
      return 'attention';
    }
    return 'normal';
  }

  /**
   * 按 visibility 裁剪指标对象，level=none → null；level=summary 不返回 count 之外的细节。
   */
  private gateMetric<T extends Record<string, unknown>>(
    level: VisibilityLevel,
    full: T,
  ): T | null {
    if (level === 'none') return null;
    return full;
  }

  /**
   * B3-1：成员健康摘要列表
   */
  async getMembersSummary(familyId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);

    const members = await this.prisma.familyMember.findMany({
      where: { familyId },
      include: {
        user: {
          select: { id: true, name: true, avatar: true, selfRole: true, diseases: true },
        },
      },
    });

    const summaries: MemberSummary[] = await Promise.all(
      members.map(async (m) => {
        const isSelf = m.userId === viewerId;
        const visibility = isSelf ? selfVisibility() : normalizeVisibility(m.visibility);

        const [bp, bg, medication] = await Promise.all([
          this.last7DaysBp(m.userId),
          this.last7DaysBg(m.userId),
          this.thisWeekAdherence(m.userId),
        ]);

        const status = this.evaluateStatus(bp, bg);

        return {
          userId: m.user.id,
          name: m.nickname ?? m.user.name,
          avatar: m.user.avatar,
          role: m.role,
          selfRole: m.user.selfRole,
          status,
          visibility,
          metrics: {
            bp: this.gateMetric(visibility.bp, bp),
            bg: this.gateMetric(visibility.bg, bg),
            medication: this.gateMetric(visibility.medication, medication),
          },
        };
      }),
    );

    return { familyId, members: summaries };
  }

  /**
   * B3-2：单成员详情
   * metric 决定要查哪一类（bp/bg/medication/diet），days 控制时间窗
   */
  async getMemberDetail(
    familyId: string,
    targetUserId: string,
    viewerId: string,
    metric: VisibilityMetric,
    days = 7,
  ) {
    await this.ensureMembership(familyId, viewerId);

    const target = await this.prisma.familyMember.findUnique({
      where: { familyId_userId: { familyId, userId: targetUserId } },
    });
    if (!target) throw new NotFoundException('该成员不属于该家庭');

    const isSelf = targetUserId === viewerId;
    const visibility = isSelf ? selfVisibility() : normalizeVisibility(target.visibility);
    const level = visibility[metric];

    if (level === 'none') {
      return {
        userId: targetUserId,
        metric,
        level,
        data: [],
        hint: '该成员未授权查看此项数据',
      };
    }

    const since = new Date(Date.now() - days * 24 * 60 * 60 * 1000);

    if (metric === 'bp') {
      const records = await this.prisma.bloodPressureRecord.findMany({
        where: { userId: targetUserId, recordedAt: { gte: since } },
        orderBy: { recordedAt: 'asc' },
        select: { systolic: true, diastolic: true, heartRate: true, recordedAt: true },
      });
      // summary 级别仅返回平均值和次数
      if (level === 'summary') {
        const avg = await this.last7DaysBp(targetUserId);
        return { userId: targetUserId, metric, level, data: avg };
      }
      return { userId: targetUserId, metric, level, data: records };
    }

    if (metric === 'bg') {
      const records = await this.prisma.bloodSugarRecord.findMany({
        where: { userId: targetUserId, recordedAt: { gte: since } },
        orderBy: { recordedAt: 'asc' },
        select: { type: true, value: true, recordedAt: true },
      });
      if (level === 'summary') {
        const avg = await this.last7DaysBg(targetUserId);
        return { userId: targetUserId, metric, level, data: avg };
      }
      return { userId: targetUserId, metric, level, data: records };
    }

    if (metric === 'medication') {
      if (level === 'summary') {
        const adherence = await this.thisWeekAdherence(targetUserId);
        return { userId: targetUserId, metric, level, data: adherence };
      }
      const records = await this.prisma.medicationRecord.findMany({
        where: { userId: targetUserId, scheduledTime: { gte: since } },
        orderBy: { scheduledTime: 'desc' },
        include: { medication: { select: { name: true, specification: true } } },
      });
      return { userId: targetUserId, metric, level, data: records };
    }

    // diet
    if (level === 'summary') {
      const count = await this.prisma.dietRecord.count({
        where: { userId: targetUserId, recordedAt: { gte: since } },
      });
      return { userId: targetUserId, metric, level, data: { count } };
    }
    const records = await this.prisma.dietRecord.findMany({
      where: { userId: targetUserId, recordedAt: { gte: since } },
      orderBy: { recordedAt: 'desc' },
    });
    return { userId: targetUserId, metric, level, data: records };
  }

  /**
   * B3-4：默契值
   * 公式：互相提醒已完成数 × 1 + 共同完成 FamilyGoal × 5
   * 按周聚合，返回当前值 + 近 N 周变化趋势
   */
  async getSynergy(familyId: string, viewerId: string, weeks = 4) {
    await this.ensureMembership(familyId, viewerId);

    // 至少 2 名 owner/member 才有"夫妻默契"概念
    const coreMembers = await this.prisma.familyMember.count({
      where: { familyId, role: { in: ['owner', 'member'] } },
    });
    if (coreMembers < 2) {
      return {
        familyId,
        currentScore: 0,
        weeks: [],
        message: '默契值需至少 2 名核心成员才会计算',
      };
    }

    const buckets: Array<{ start: Date; end: Date; weekIndex: number }> = [];
    for (let i = weeks - 1; i >= 0; i--) {
      const end = new Date();
      end.setHours(23, 59, 59, 999);
      end.setDate(end.getDate() - i * 7);
      const start = new Date(end);
      start.setDate(start.getDate() - 6);
      start.setHours(0, 0, 0, 0);
      buckets.push({ start, end, weekIndex: weeks - 1 - i });
    }

    const data = await Promise.all(
      buckets.map(async ({ start, end, weekIndex }) => {
        const [reminderDone, goalsAchieved] = await Promise.all([
          this.prisma.mutualReminder.count({
            where: {
              familyId,
              status: 'done',
              fulfilledAt: { gte: start, lte: end },
            },
          }),
          this.prisma.familyGoal.count({
            where: {
              familyId,
              achieved: true,
              endDate: { gte: start, lte: end },
            },
          }),
        ]);
        return {
          weekIndex,
          weekStart: start.toISOString().split('T')[0],
          weekEnd: end.toISOString().split('T')[0],
          reminders: reminderDone,
          goals: goalsAchieved,
          score: reminderDone * 1 + goalsAchieved * 5,
        };
      }),
    );

    const currentScore = data[data.length - 1]?.score ?? 0;
    const previousScore = data[data.length - 2]?.score ?? 0;
    const delta = currentScore - previousScore;

    return {
      familyId,
      currentScore,
      delta,
      weeks: data,
    };
  }
}
