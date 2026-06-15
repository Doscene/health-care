import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service.js';
import { RiskEngine } from '../alert/risk-engine.service.js';

/**
 * 风险扫描定时任务
 * 每分钟扫描最近录入的 BP/BG 数据，调用风险引擎评估并生成告警
 */
@Injectable()
export class RiskScanScheduler {
  private readonly logger = new Logger(RiskScanScheduler.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly riskEngine: RiskEngine,
  ) {}

  @Cron('*/5 * * * *') // 每5分钟执行一次
  async scanRisk() {
    try {
      // 扫描最近1小时内有活跃用药记录的用户
      const recentRecords = await this.prisma.medicationRecord.findMany({
        where: {
          status: 'missed',
          scheduledTime: {
            gte: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
          },
        },
        select: { userId: true },
        distinct: ['userId'],
      });

      for (const { userId } of recentRecords) {
        // 检查连续漏服
        const missedCount = await this.prisma.medicationRecord.count({
          where: {
            userId,
            status: 'missed',
            scheduledTime: {
              gte: new Date(Date.now() - 48 * 60 * 60 * 1000),
            },
          },
        });

        if (missedCount >= 2) {
          // 检查是否已有活跃告警
          const existingAlert = await this.prisma.riskAlert.findFirst({
            where: {
              userId,
              triggerType: 'missed_dose',
              status: 'active',
              createdAt: {
                gte: new Date(Date.now() - 24 * 60 * 60 * 1000),
              },
            },
          });

          if (!existingAlert) {
            const alertId = await this.riskEngine.createAlert(userId, {
              level: 'yellow',
              triggerType: 'missed_dose',
              triggerValue: `漏服${missedCount}次`,
              message: `您连续${missedCount}次未按时服药，请留意`,
            });
            if (alertId) {
              await this.riskEngine.createInquiry(userId, alertId, 'missed_dose');
            }
          }
        }
      }

      // 扫描最近录入的血压/血糖数据，检查持续异常
      const usersWithBp = await this.prisma.bloodPressureRecord.findMany({
        where: {
          recordedAt: {
            gte: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
          },
        },
        select: { userId: true },
        distinct: ['userId'],
      });

      for (const { userId } of usersWithBp) {
        const persistentBp = await this.riskEngine.evaluateBpPersistent(userId);
        if (persistentBp && persistentBp.level !== 'green') {
          await this.riskEngine.createAlert(userId, persistentBp);
        }
      }

      const usersWithBg = await this.prisma.bloodSugarRecord.findMany({
        where: {
          type: 'fasting',
          recordedAt: {
            gte: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
          },
        },
        select: { userId: true },
        distinct: ['userId'],
      });

      for (const { userId } of usersWithBg) {
        const persistentBg = await this.riskEngine.evaluateBgPersistent(userId);
        if (persistentBg && persistentBg.level !== 'green') {
          await this.riskEngine.createAlert(userId, persistentBg);
        }
      }
    } catch (error) {
      this.logger.error('Risk scan failed', error);
    }
  }
}
