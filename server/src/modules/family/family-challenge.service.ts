import {
  Injectable,
  BadRequestException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

const CHALLENGE_TYPES = [
  'weekend_walk',
  'bp_stable',
  'full_attendance',
  'custom',
] as const;
type ChallengeType = (typeof CHALLENGE_TYPES)[number];

interface ChallengeTemplate {
  type: ChallengeType;
  title: string;
  description: string;
  durationDays: number;
  ruleConfig: Record<string, unknown>;
}

/**
 * 预设挑战模板，前端可走 GET /api/family/:id/challenges/templates 拉到。
 * 真正创建仍需调用 POST /api/family/:id/challenges 并指定 participantIds。
 */
const TEMPLATES: ChallengeTemplate[] = [
  {
    type: 'weekend_walk',
    title: '周末健走挑战',
    description: '本周末每位成员步行 ≥ 6000 步',
    durationDays: 2,
    ruleConfig: { target: 6000, metric: 'steps', evaluateOn: 'weekend' },
  },
  {
    type: 'bp_stable',
    title: '血压稳定周',
    description: '连续 7 天每人血压 ≤ 140/90',
    durationDays: 7,
    ruleConfig: { systolicMax: 140, diastolicMax: 90, metric: 'bp_daily_avg' },
  },
  {
    type: 'full_attendance',
    title: '全家全勤打卡',
    description: '一周内全员每天至少录 1 次血压或血糖',
    durationDays: 7,
    ruleConfig: { metric: 'measure_count_per_day', threshold: 1 },
  },
];

interface CreateChallengeDto {
  type: string;
  title?: string;
  description?: string;
  participantIds: string[];
  startDate: string;
  endDate?: string;
  ruleConfig?: Record<string, unknown>;
}

/**
 * 家庭挑战（B3-6）。
 *
 * - 通过模板（type）快速创建，custom 允许完全自定义 ruleConfig
 * - 进度由 FamilyChallengeScheduler 每日 0:30 跑批写 FamilyChallengeProgress
 * - 完成判定：所有参与者在挑战完整周期内每天 met=true，则 challenge.status=completed
 */
@Injectable()
export class FamilyChallengeService {
  constructor(private readonly prisma: PrismaService) {}

  private async ensureMembership(familyId: string, userId: string) {
    const m = await this.prisma.familyMember.findUnique({
      where: { familyId_userId: { familyId, userId } },
    });
    if (!m) throw new ForbiddenException('你不是该家庭成员');
    return m;
  }

  /** 模板列表 */
  listTemplates() {
    return TEMPLATES;
  }

  /** 创建挑战 */
  async create(familyId: string, operatorId: string, dto: CreateChallengeDto) {
    await this.ensureMembership(familyId, operatorId);

    if (!CHALLENGE_TYPES.includes(dto.type as ChallengeType)) {
      throw new BadRequestException(
        `挑战类型无效，允许值：${CHALLENGE_TYPES.join(', ')}`,
      );
    }
    if (!Array.isArray(dto.participantIds) || dto.participantIds.length === 0) {
      throw new BadRequestException('至少指定一名参与者');
    }

    const template = TEMPLATES.find((t) => t.type === dto.type);
    const start = new Date(dto.startDate);
    if (Number.isNaN(start.getTime())) {
      throw new BadRequestException('startDate 无效');
    }
    let end: Date;
    if (dto.endDate) {
      end = new Date(dto.endDate);
      if (Number.isNaN(end.getTime())) {
        throw new BadRequestException('endDate 无效');
      }
    } else if (template) {
      end = new Date(start);
      end.setDate(end.getDate() + template.durationDays - 1);
    } else {
      throw new BadRequestException('custom 挑战需指定 endDate');
    }

    if (end <= start) {
      throw new BadRequestException('结束时间需晚于开始时间');
    }

    // 校验参与者
    const members = await this.prisma.familyMember.findMany({
      where: { familyId, userId: { in: dto.participantIds } },
      select: { userId: true },
    });
    if (members.length !== dto.participantIds.length) {
      throw new BadRequestException('部分参与者不属于该家庭');
    }

    return this.prisma.familyChallenge.create({
      data: {
        id: uuidv4(),
        familyId,
        type: dto.type,
        title: dto.title ?? template?.title ?? '家庭挑战',
        description: dto.description ?? template?.description ?? null,
        ruleConfig: dto.ruleConfig ?? template?.ruleConfig ?? {},
        participantIds: dto.participantIds,
        startDate: start,
        endDate: end,
        status: 'active',
      },
    });
  }

  /** 列表 */
  async list(familyId: string, viewerId: string, status?: string) {
    await this.ensureMembership(familyId, viewerId);
    const where: { familyId: string; status?: string } = { familyId };
    if (status) where.status = status;
    return this.prisma.familyChallenge.findMany({
      where,
      orderBy: [{ status: 'asc' }, { endDate: 'desc' }],
    });
  }

  /** 进行中挑战 */
  async listActive(familyId: string, viewerId: string) {
    return this.list(familyId, viewerId, 'active');
  }

  /** 加入挑战（参与者后期补加） */
  async join(familyId: string, challengeId: string, userId: string) {
    await this.ensureMembership(familyId, userId);
    const challenge = await this.prisma.familyChallenge.findFirst({
      where: { id: challengeId, familyId },
    });
    if (!challenge) throw new NotFoundException('挑战不存在');
    if (challenge.status !== 'active') {
      throw new BadRequestException('挑战已结束，无法加入');
    }
    const ids = new Set([...(challenge.participantIds as string[]), userId]);
    return this.prisma.familyChallenge.update({
      where: { id: challengeId },
      data: { participantIds: Array.from(ids) },
    });
  }

  /** 详情（含每日进度） */
  async detail(familyId: string, challengeId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);
    const challenge = await this.prisma.familyChallenge.findFirst({
      where: { id: challengeId, familyId },
      include: {
        progresses: { orderBy: { date: 'asc' } },
      },
    });
    if (!challenge) throw new NotFoundException('挑战不存在');
    return challenge;
  }

  /** 删除（已结束的不允许删，避免历史记录丢失） */
  async remove(familyId: string, challengeId: string, viewerId: string) {
    await this.ensureMembership(familyId, viewerId);
    const challenge = await this.prisma.familyChallenge.findFirst({
      where: { id: challengeId, familyId },
    });
    if (!challenge) throw new NotFoundException('挑战不存在');
    if (challenge.status !== 'active') {
      throw new BadRequestException('已结束/失败的挑战不可删除');
    }
    await this.prisma.familyChallengeProgress.deleteMany({
      where: { challengeId },
    });
    await this.prisma.familyChallenge.delete({ where: { id: challengeId } });
    return { deleted: true };
  }

  /**
   * 给定挑战的某一天，对所有参与者计算 met 并写入 progress 表。
   * 由 FamilyChallengeScheduler 每日调用；导出供测试时手动触发。
   */
  async evaluateDay(challengeId: string, date: Date) {
    const challenge = await this.prisma.familyChallenge.findUnique({
      where: { id: challengeId },
    });
    if (!challenge) return;
    if (challenge.status !== 'active') return;

    const dayStart = new Date(date);
    dayStart.setHours(0, 0, 0, 0);
    const dayEnd = new Date(dayStart);
    dayEnd.setDate(dayEnd.getDate() + 1);

    const participants = challenge.participantIds as string[];
    const config = (challenge.ruleConfig as Record<string, unknown>) ?? {};

    for (const userId of participants) {
      let value = 0;
      let met = false;

      if (challenge.type === 'bp_stable') {
        const records = await this.prisma.bloodPressureRecord.findMany({
          where: { userId, recordedAt: { gte: dayStart, lt: dayEnd } },
          select: { systolic: true, diastolic: true },
        });
        if (records.length > 0) {
          const avgSys =
            records.reduce((s, r) => s + r.systolic, 0) / records.length;
          const avgDia =
            records.reduce((s, r) => s + r.diastolic, 0) / records.length;
          value = Math.round(avgSys);
          met =
            avgSys <= Number(config['systolicMax'] ?? 140) &&
            avgDia <= Number(config['diastolicMax'] ?? 90);
        }
      } else if (challenge.type === 'full_attendance') {
        const [bp, bg] = await Promise.all([
          this.prisma.bloodPressureRecord.count({
            where: { userId, recordedAt: { gte: dayStart, lt: dayEnd } },
          }),
          this.prisma.bloodSugarRecord.count({
            where: { userId, recordedAt: { gte: dayStart, lt: dayEnd } },
          }),
        ]);
        value = bp + bg;
        met = value >= Number(config['threshold'] ?? 1);
      } else if (challenge.type === 'weekend_walk') {
        // 步数数据 MVP 不接，这里用饮食/记录次数占位（前端可走 custom 自定义）
        value = 0;
        met = false;
      } else {
        // custom 由前端 PATCH 进度，scheduler 不动
        continue;
      }

      const dateOnly = dayStart;
      await this.prisma.familyChallengeProgress.upsert({
        where: {
          challengeId_userId_date: {
            challengeId,
            userId,
            date: dateOnly,
          },
        },
        update: { value, met },
        create: {
          id: uuidv4(),
          challengeId,
          userId,
          date: dateOnly,
          value,
          met,
        },
      });
    }

    // 评估整体完成
    await this.evaluateCompletion(challengeId);
  }

  /** 判定挑战是否结束 */
  private async evaluateCompletion(challengeId: string) {
    const challenge = await this.prisma.familyChallenge.findUnique({
      where: { id: challengeId },
      include: { progresses: true },
    });
    if (!challenge || challenge.status !== 'active') return;

    const now = new Date();
    if (now < challenge.endDate) return;

    const participants = challenge.participantIds as string[];
    const totalDays =
      Math.ceil(
        (challenge.endDate.getTime() - challenge.startDate.getTime()) /
          (24 * 60 * 60 * 1000),
      ) + 1;
    const expectedRows = participants.length * totalDays;

    const metRows = challenge.progresses.filter((p) => p.met).length;
    const allMet = metRows === expectedRows;

    await this.prisma.familyChallenge.update({
      where: { id: challengeId },
      data: { status: allMet ? 'completed' : 'failed' },
    });

    // TODO Phase 6：派发徽章 + 推送庆祝
  }
}
