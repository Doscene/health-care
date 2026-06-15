import { Module } from '@nestjs/common';
import { AlertController } from './alert.controller.js';
import { AlertService } from './alert.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [AlertController],
  providers: [AlertService],
  exports: [AlertService],
})
export class AlertModule {}
