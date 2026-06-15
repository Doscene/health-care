import { Module } from '@nestjs/common';
import { RecordController } from './record.controller.js';
import { RecordService } from './record.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';
import { AlertModule } from '../alert/alert.module.js';

@Module({
  imports: [PrismaModule, AlertModule],
  controllers: [RecordController],
  providers: [RecordService],
  exports: [RecordService],
})
export class RecordModule {}
