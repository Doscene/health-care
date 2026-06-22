import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service.js';
import { JPushService } from '../notification/push/jpush.service.js';

/**
 * 用药提醒定时任务
 * 每分钟扫描活跃用药计划，匹配当前时间与 remindTimes，推送提醒
 */
@Injectable()
export class MedicationReminderScheduler {
  private readonly logger = new Logger(MedicationReminderScheduler.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly jpush: JPushService,
  ) {}

  @Cron('* * * * *')
  async scanMedicationReminders() {
    const now = new Date();
    const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

    try {
      // 查找活跃的用药计划，remindTimes 包含当前时间
      const medications = await this.prisma.medication.findMany({
        where: {
          status: 'active',
          startDate: { lte: now },
          OR: [{ endDate: null }, { endDate: { gte: now } }],
        },
        include: {
          user: { select: { id: true, name: true } },
        },
      });

      let reminded = 0;
      for (const med of medications) {
        const remindTimes = med.remindTimes as string[];
        if (!Array.isArray(remindTimes) || !remindTimes.includes(currentTime)) {
          continue;
        }

        // 检查是否已创建过该时间点的记录（避免重复）
        const existing = await this.prisma.medicationRecord.findFirst({
          where: {
            medicationId: med.id,
            scheduledTime: {
              gte: new Date(now.getFullYear(), now.getMonth(), now.getDate()),
              lt: new Date(
                now.getFullYear(),
                now.getMonth(),
                now.getDate() + 1,
              ),
            },
          },
          orderBy: { scheduledTime: 'desc' },
        });

        if (existing) continue;

        // 创建待确认的服药记录
        await this.prisma.medicationRecord.create({
          data: {
            medicationId: med.id,
            userId: med.userId,
            scheduledTime: new Date(
              now.getFullYear(),
              now.getMonth(),
              now.getDate(),
              now.getHours(),
              now.getMinutes(),
            ),
            status: 'pending',
          },
        });

        // 推送提醒
        const medName = med.name;
        const dosage = `${med.dosagePerTime}${med.specification || '片'}`;
        await this.jpush.pushToUser(med.userId, {
          notificationTitle: '💊 服药提醒',
          notificationContent: `${med.user.name}，该吃${medName}了（${dosage}）`,
          extras: {
            type: 'medication_reminder',
            medicationId: med.id,
          },
        });

        reminded++;
      }

      if (reminded > 0) {
        this.logger.log(
          `Sent ${reminded} medication reminders at ${currentTime}`,
        );
      }
    } catch (error) {
      this.logger.error('Medication reminder scan failed', error);
    }
  }
}
