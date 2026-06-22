import { Module } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { DietController } from './diet.controller.js';
import { DietService } from './diet.service.js';
import { RecipeRecommendationService } from './recipe-recommendation.service.js';

@Module({
  controllers: [DietController],
  providers: [PrismaService, DietService, RecipeRecommendationService],
  exports: [DietService, RecipeRecommendationService],
})
export class DietModule {}
