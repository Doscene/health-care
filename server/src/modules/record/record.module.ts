import { Module } from '@nestjs/common';
import { RecordController } from './record.controller.js';
import { RecordService } from './record.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [RecordController],
  providers: [RecordService],
  exports: [RecordService],
})
export class RecordModule {}
