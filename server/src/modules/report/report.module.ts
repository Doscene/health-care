import { Module } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { ReportController } from './report.controller.js';
import { ReportService } from './report.service.js';
import { ReportScheduler } from './report.scheduler.js';
import { FamilyModule } from '../family/family.module.js';
import { MedicationModule } from '../medication/medication.module.js';
import { NotificationModule } from '../notification/notification.module.js';

@Module({
  imports: [FamilyModule, MedicationModule, NotificationModule],
  controllers: [ReportController],
  providers: [PrismaService, ReportService, ReportScheduler],
  exports: [ReportService],
})
export class ReportModule {}
