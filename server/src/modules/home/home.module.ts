import { Module } from '@nestjs/common';
import { HomeController } from './home.controller.js';
import { HomeService } from './home.service.js';
import { PrismaModule } from '../../prisma/prisma.module.js';

@Module({
  imports: [PrismaModule],
  controllers: [HomeController],
  providers: [HomeService],
  exports: [HomeService],
})
export class HomeModule {}
