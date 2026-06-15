import { Module } from '@nestjs/common';
import { DietController } from './diet.controller.js';
import { DietService } from './diet.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [DietController],
  providers: [DietService],
  exports: [DietService],
})
export class DietModule {}
