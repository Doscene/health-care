import { Module } from '@nestjs/common';
import { MedicationController } from './medication.controller.js';
import { MedicationService } from './medication.service.js';
import { MedicationReminderScheduler } from './medication-reminder.scheduler.js';
import { PrismaModule } from '../../prisma/prisma.module.js';
import { NotificationModule } from '../notification/notification.module.js';

@Module({
  imports: [PrismaModule, NotificationModule],
  controllers: [MedicationController],
  providers: [MedicationService, MedicationReminderScheduler],
  exports: [MedicationService],
})
export class MedicationModule {}
