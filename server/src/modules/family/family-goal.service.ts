import {
  Injectable,
  BadRequestException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

const GOAL_TYPES = [
  'bp_control',
  'measure_checkin',
  'exercise',
  'custom',
] as const;
type GoalType = (typeof GOAL_TYPES)[number];

interface CreateGoalDto {
  type: string;
  title: string;
  targetValue: number;
  unit: string;
  participantIds: string[];
  startDate: string;
  endDate: string;
}

interface UpdateGoalDto {
  title?: string;
  targetValue?: number;
  currentValue?: number;
  endDate?: string;
}

/**
 * 家庭目标 CRUD（B3-5）。
 *
 * 进度计算策略：
 * - bp_control      → 参与者近 N 天「日均收缩压 ≤ targetValue」的天数 / 总天数 × targetValue
 * - measure_checkin → 参与者总记录次数（BP+BG）
 * - exercise        → 暂用饮食拍照记录数占位（MVP）
 * - custom          → 通过 update 接口手动设置 currentValue
 *
 * recompute 失败不应阻塞 CRUD，因此外层用 try/catch 兜底。
 */
@Injectable()
export class FamilyGoalService {
  constructor(private readonly prisma: PrismaService) {}

  /** 校验调用方为家庭成员 */
  private async ensureMembership(familyId: string, userId: string) {
    const m = await this.prisma.familyMember.findUnique({
      where: { familyId_userId: { familyId, userId } },
    });
    if (!m) throw new ForbiddenException('你不是该家庭成员');
    return m;
  }

  /** 创建目标 */
  async create(familyId: string, operatorId: string, dto: CreateGoalDto) {
    await this.ensureMembership(familyId, operatorId);

    if (!GOAL_TYPES.includes(dto.type as GoalType)) {
      throw new BadRequestException(
        `目标类型无效，允许值：${GOAL_TYPES.join(', ')}`,
      );
    }
    if (!dto.title || dto.title.trim().length === 0) {
      throw new BadRequestException('目标标题不能为空');
    }
    if (!Array.isArray(dto.participantIds) || dto.participantIds.length === 0) {
      throw new BadRequestException('至少指定一名参与者');
    }
    if (dto.targetValue <= 0) {
      throw new BadRequestException('目标值需大于 0');
    }
    const start = new Date(dto.startDate);
    const end = new Date(dto.endDate);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      throw new BadRequestException('日期格式无效');
    }
    if (end <= start) {
      throw new BadRequestException('结束时间需晚于开始时间');
    }

    // 校验参与者都在家庭中
    const members = await this.prisma.familyMember.findMany({
      where: { familyId, userId: { in: dto.participantIds } },
      select: { userId: true },
    });
    if (members.length !== dto.participantIds.length) {
      throw new BadRequestException('部分参与者不属于该家庭');
    }

    const goal = await this.prisma.familyGoal.create({
      data: {
        id: uuidv4(),
        familyId,
        type: dto.type,
        title: dto.title.trim(),
        targetValue: dto.targetValue,
        currentValue: 0,
        unit: dto.unit ?? '',
        participantIds: dto.participantIds,
        startDate: start,
        endDate: end,
        achieved: false,
      },
    });

    await this.recomputeProgress(goal.id);

    return this.prisma.familyGoal.findUnique({ where: { id: goal.id } });
  }

  /** 列表 */
  async list(familyId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);
    return this.prisma.familyGoal.findMany({
      where: { familyId },
      orderBy: [{ achieved: 'asc' }, { endDate: 'asc' }],
    });
  }

  /** 详情 */
  async get(familyId: string, goalId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);
    const goal = await this.prisma.familyGoal.findFirst({
      where: { id: goalId, familyId },
    });
    if (!goal) throw new NotFoundException('目标不存在');
    return goal;
  }

  /** 更新（自定义目标可手动改 currentValue） */
  async update(
    familyId: string,
    goalId: string,
    viewerId: string,
    dto: UpdateGoalDto,
  ) {
    await this.ensureMembership(familyId, viewerId);

    const existing = await this.prisma.familyGoal.findFirst({
      where: { id: goalId, familyId },
    });
    if (!existing) throw new NotFoundException('目标不存在');

    const data: Record<string, unknown> = {};
    if (dto.title !== undefined) data.title = dto.title.trim();
    if (dto.targetValue !== undefined) {
      if (dto.targetValue <= 0) throw new BadRequestException('目标值需大于 0');
      data.targetValue = dto.targetValue;
    }
    if (dto.endDate !== undefined) {
      const end = new Date(dto.endDate);
      if (Number.isNaN(end.getTime()))
        throw new BadRequestException('日期无效');
      data.endDate = end;
    }
    if (dto.currentValue !== undefined && existing.type === 'custom') {
      data.currentValue = dto.currentValue;
      data.achieved = dto.currentValue >= Number(existing.targetValue);
    }

    await this.prisma.familyGoal.update({ where: { id: goalId }, data });

    if (existing.type !== 'custom') {
      await this.recomputeProgress(goalId);
    }

    return this.prisma.familyGoal.findUnique({ where: { id: goalId } });
  }

  /** 删除 */
  async remove(familyId: string, goalId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);
    const existing = await this.prisma.familyGoal.findFirst({
      where: { id: goalId, familyId },
    });
    if (!existing) throw new NotFoundException('目标不存在');
    await this.prisma.familyGoal.delete({ where: { id: goalId } });
    return { deleted: true };
  }

  /**
   * 重新计算目标进度。
   *
   * 在 Phase 6 接入定时任务后会被周期触发；目前依赖：
   * - 目标创建/更新时由本服务主动调用
   * - HealthMetrics/Medication 写入指标后由 controller 显式触发（暂未接，下轮联调时补）
   */
  async recomputeProgress(goalId: string) {
    const goal = await this.prisma.familyGoal.findUnique({
      where: { id: goalId },
    });
    if (!goal) return;

    const participantIds = (goal.participantIds as string[]) ?? [];
    if (participantIds.length === 0) return;

    const target = Number(goal.targetValue);
    let current = 0;

    if (goal.type === 'bp_control') {
      // 计算开始日到结束日（不超过今天）每天均值 ≤ target 的"达标天数"
      const start = new Date(goal.startDate);
      start.setHours(0, 0, 0, 0);
      const end = new Date(goal.endDate);
      end.setHours(23, 59, 59, 999);
      const today = new Date();
      const cap = today < end ? today : end;

      const records = await this.prisma.bloodPressureRecord.findMany({
        where: {
          userId: { in: participantIds },
          recordedAt: { gte: start, lte: cap },
        },
        select: { systolic: true, recordedAt: true, userId: true },
      });

      // 按 userId+date 聚合
      const byKey = new Map<string, number[]>();
      for (const r of records) {
        const day = r.recordedAt.toISOString().split('T')[0];
        const key = `${r.userId}|${day}`;
        if (!byKey.has(key)) byKey.set(key, []);
        byKey.get(key)!.push(r.systolic);
      }

      let achievedDays = 0;
      for (const sysList of byKey.values()) {
        const avg = sysList.reduce((a, b) => a + b, 0) / sysList.length;
        if (avg <= target) achievedDays += 1;
      }
      // currentValue 用"达标天数"语义（前端展示进度条 = current/totalDays）
      const totalDays = Math.max(
        1,
        Math.ceil((cap.getTime() - start.getTime()) / (24 * 60 * 60 * 1000)),
      );
      const dailyTotal = totalDays * participantIds.length;
      // 归一化到 targetValue 量纲：current = achievedDays / dailyTotal * target
      current = parseFloat(((achievedDays / dailyTotal) * target).toFixed(2));
    } else if (goal.type === 'measure_checkin') {
      const since = new Date(goal.startDate);
      const [bp, bg] = await Promise.all([
        this.prisma.bloodPressureRecord.count({
          where: { userId: { in: participantIds }, recordedAt: { gte: since } },
        }),
        this.prisma.bloodSugarRecord.count({
          where: { userId: { in: participantIds }, recordedAt: { gte: since } },
        }),
      ]);
      current = bp + bg;
    } else if (goal.type === 'exercise') {
      // MVP 占位：饮食拍照次数（含运动后餐拍）作为次数估算
      const since = new Date(goal.startDate);
      current = await this.prisma.dietRecord.count({
        where: { userId: { in: participantIds }, recordedAt: { gte: since } },
      });
    } else {
      // custom：保留现值
      current = Number(goal.currentValue);
    }

    const achieved = current >= target;
    await this.prisma.familyGoal.update({
      where: { id: goalId },
      data: { currentValue: current, achieved },
    });
  }
}
