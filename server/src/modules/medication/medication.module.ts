import { Module } from '@nestjs/common';
import { MedicationController } from './medication.controller.js';
import { MedicationService } from './medication.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [MedicationController],
  providers: [MedicationService],
  exports: [MedicationService],
})
export class MedicationModule {}
