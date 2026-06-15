import { Module } from '@nestjs/common';
import { AlertController } from './alert.controller.js';
import { AlertService } from './alert.service.js';
import { RiskEngine } from './risk-engine.service.js';
import { RiskScanScheduler } from './risk-scan.scheduler.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [AlertController],
  providers: [AlertService, RiskEngine, RiskScanScheduler],
  exports: [AlertService, RiskEngine],
})
export class AlertModule {}
