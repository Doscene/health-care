import { Module } from '@nestjs/common';
import { HealthMetricsController } from './health-metrics.controller.js';
import { HealthMetricsService } from './health-metrics.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';
import { AlertModule } from '../alert/alert.module.js';

@Module({
  imports: [PrismaModule, AlertModule],
  controllers: [HealthMetricsController],
  providers: [HealthMetricsService],
  exports: [HealthMetricsService],
})
export class HealthMetricsModule {}
