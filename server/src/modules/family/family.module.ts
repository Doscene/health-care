import { Module } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { NotificationModule } from '../notification/notification.module.js';
import { FamilyController } from './family.controller.js';
import { FamilyService } from './family.service.js';
import { FamilySummaryService } from './family-summary.service.js';
import { MutualReminderService } from './mutual-reminder.service.js';
import { FamilyGoalService } from './family-goal.service.js';
import { FamilyChallengeService } from './family-challenge.service.js';
import { FamilyChallengeScheduler } from './family-challenge.scheduler.js';

@Module({
  imports: [NotificationModule],
  controllers: [FamilyController],
  providers: [
    PrismaService,
    FamilyService,
    FamilySummaryService,
    MutualReminderService,
    FamilyGoalService,
    FamilyChallengeService,
    FamilyChallengeScheduler,
  ],
  exports: [
    FamilyService,
    FamilySummaryService,
    MutualReminderService,
    FamilyGoalService,
    FamilyChallengeService,
  ],
})
export class FamilyModule {}
