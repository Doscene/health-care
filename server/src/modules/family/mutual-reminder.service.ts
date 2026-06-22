import {
  Injectable,
  BadRequestException,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { JPushService } from '../notification/push/jpush.service.js';
import { v4 as uuidv4 } from 'uuid';

const ALLOWED_TYPES = ['bp', 'bg', 'medication'] as const;
type ReminderType = (typeof ALLOWED_TYPES)[number];

const TYPE_LABEL: Record<ReminderType, string> = {
  bp: '量血压',
  bg: '测血糖',
  medication: '吃药',
};

interface CreateReminderDto {
  toUserId: string;
  type: string;
  message?: string;
}

/**
 * 家庭成员互相提醒（B3-3）。
 *
 * 流程：A 点提醒 → 写 MutualReminder → 推送 B → B 完成动作 → 调 fulfill 把 status 置 done。
 * 默契值统计（B3-4）依赖 status=done 的记录条数。
 */
@Injectable()
export class MutualReminderService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jpush: JPushService,
  ) {}

  /** 校验提醒人和被提醒人都在同一家庭 */
  private async ensureBothInFamily(
    familyId: string,
    fromUserId: string,
    toUserId: string,
  ) {
    if (fromUserId === toUserId) {
      throw new BadRequestException('不能给自己发提醒');
    }
    const members = await this.prisma.familyMember.findMany({
      where: {
        familyId,
        userId: { in: [fromUserId, toUserId] },
      },
      select: { userId: true },
    });
    const ids = new Set(members.map((m) => m.userId));
    if (!ids.has(fromUserId)) throw new ForbiddenException('你不是该家庭成员');
    if (!ids.has(toUserId)) throw new BadRequestException('对方不属于该家庭');
  }

  /** 发起一条提醒 */
  async send(familyId: string, fromUserId: string, dto: CreateReminderDto) {
    if (!ALLOWED_TYPES.includes(dto.type as ReminderType)) {
      throw new BadRequestException(
        `提醒类型无效，允许值：${ALLOWED_TYPES.join(', ')}`,
      );
    }

    await this.ensureBothInFamily(familyId, fromUserId, dto.toUserId);

    const reminder = await this.prisma.mutualReminder.create({
      data: {
        id: uuidv4(),
        familyId,
        fromUserId,
        toUserId: dto.toUserId,
        type: dto.type,
        message: dto.message ?? null,
        status: 'pending',
      },
    });

    // 异步推送，不阻塞主流程；JPushService 内部已对未配置凭据做了兜底
    const fromUser = await this.prisma.user.findUnique({
      where: { id: fromUserId },
      select: { name: true },
    });
    const label = TYPE_LABEL[dto.type as ReminderType];
    void this.jpush.pushToUser(dto.toUserId, {
      notificationTitle: '家人提醒',
      notificationContent:
        dto.message ?? `${fromUser?.name ?? '家人'}提醒您该${label}了`,
      extras: {
        reminderId: reminder.id,
        type: dto.type,
        familyId,
      },
    });

    return reminder;
  }

  /** 收件箱 / 发件箱 */
  async list(
    familyId: string,
    userId: string,
    box: 'inbox' | 'sent' = 'inbox',
    limit = 30,
  ) {
    const where =
      box === 'sent'
        ? { familyId, fromUserId: userId }
        : { familyId, toUserId: userId };

    return this.prisma.mutualReminder.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: {
        fromUser: { select: { id: true, name: true, avatar: true } },
        toUser: { select: { id: true, name: true, avatar: true } },
      },
    });
  }

  /** 被提醒人完成动作后回调 */
  async fulfill(reminderId: string, userId: string) {
    const reminder = await this.prisma.mutualReminder.findUnique({
      where: { id: reminderId },
    });
    if (!reminder) throw new NotFoundException('提醒不存在');
    if (reminder.toUserId !== userId) {
      throw new ForbiddenException('只有被提醒人可以确认完成');
    }
    if (reminder.status !== 'pending') {
      // 幂等：已 done 直接返回当前
      return reminder;
    }

    return this.prisma.mutualReminder.update({
      where: { id: reminderId },
      data: { status: 'done', fulfilledAt: new Date() },
    });
  }
}
