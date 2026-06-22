import { Injectable, Logger } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { PrismaService } from '../../prisma/prisma.service.js';
import { ReportService } from './report.service.js';
import { JPushService } from '../notification/push/jpush.service.js';

@Injectable()
export class ReportScheduler {
  private readonly logger = new Logger(ReportScheduler.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly reportService: ReportService,
    private readonly jpushService: JPushService,
  ) {}

  @Cron('0 0 * * 1')
  async generateWeeklyReports() {
    this.logger.log('开始自动生成周报...');

    const now = new Date();
    const lastWeekStart = new Date(now);
    lastWeekStart.setDate(now.getDate() - 7);
    const weekStartStr = lastWeekStart.toISOString().split('T')[0];

    const families = await this.prisma.family.findMany({
      include: {
        members: {
          include: {
            user: {
              select: { id: true, name: true },
            },
          },
        },
      },
    });

    let successCount = 0;
    let failCount = 0;

    for (const family of families) {
      try {
        const report = await this.reportService.generateWeeklyReport(
          family.id,
          weekStartStr,
        );

        const childMembers = family.members.filter(
          (m) => m.role === 'caregiver' || m.role === 'viewer',
        );

        for (const child of childMembers) {
          await this.jpushService.pushToUser(child.userId, {
            notificationTitle: '爸妈上周健康简报已生成',
            notificationContent: `${family.name}的上周健康简报已准备好，点击查看`,
            extras: {
              type: 'weekly_report',
              familyId: family.id,
              weekStart: weekStartStr,
              reportId: report.id,
            },
          });
        }

        successCount++;
        this.logger.log(`家庭 ${family.name} 周报生成成功`);
      } catch (error) {
        failCount++;
        this.logger.error(
          `家庭 ${family.name} 周报生成失败: ${error instanceof Error ? error.message : String(error)}`,
        );
      }
    }

    this.logger.log(
      `周报生成完成: 成功 ${successCount}, 失败 ${failCount}, 共 ${families.length}`,
    );
  }
}