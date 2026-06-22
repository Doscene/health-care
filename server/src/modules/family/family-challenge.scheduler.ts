import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service.js';
import { FamilyChallengeService } from './family-challenge.service.js';

/**
 * 家庭挑战每日打点定时任务（B3-6）。
 *
 * 每日 0:30 跑批：扫描所有 status=active 挑战，对昨天逐参与者评估 → 写 FamilyChallengeProgress。
 * 0:30 而非 0:00 是为了让昨日数据完全落库，避免边界条件。
 */
@Injectable()
export class FamilyChallengeScheduler {
  private readonly logger = new Logger(FamilyChallengeScheduler.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly challengeService: FamilyChallengeService,
  ) {}

  @Cron('30 0 * * *')
  async dailyEvaluate() {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    yesterday.setHours(0, 0, 0, 0);

    try {
      const active = await this.prisma.familyChallenge.findMany({
        where: {
          status: 'active',
          startDate: { lte: yesterday },
        },
        select: { id: true },
      });

      this.logger.log(`扫描 ${active.length} 个进行中挑战，评估日期 ${yesterday.toISOString()}`);

      for (const c of active) {
        try {
          await this.challengeService.evaluateDay(c.id, yesterday);
        } catch (e) {
          this.logger.error(`挑战 ${c.id} 评估失败`, e);
        }
      }
    } catch (e) {
      this.logger.error('FamilyChallengeScheduler 跑批异常', e);
    }
  }
}
