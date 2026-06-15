import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class DietService {
  constructor(private readonly prisma: PrismaService) {}

  /** 获取食谱列表（支持按适合人群筛选） */
  async getRecipes(suitableFor?: string) {
    const where: any = {};
    if (suitableFor) {
      where.suitableFor = { array_contains: suitableFor };
    }
    return this.prisma.recipe.findMany({
      where,
      orderBy: { name: 'asc' },
      take: 50,
    });
  }

  /** 获取单个食谱详情 */
  async getRecipe(recipeId: string) {
    const recipe = await this.prisma.recipe.findUnique({
      where: { id: recipeId },
    });
    if (!recipe) throw new NotFoundException('食谱不存在');
    return recipe;
  }

  /** 获取今日推荐菜单（根据病种推荐低盐/低糖食谱） */
  async getDailyMenu(diseases: string[]) {
    const isHypertension = diseases.includes('hypertension');
    const isDiabetes = diseases.includes('diabetes');

    // 获取适合的食谱
    const recipes = await this.prisma.recipe.findMany({
      take: 6,
      orderBy: { calories: 'asc' },
    });

    // 按餐次分组
    return {
      breakfast: recipes.slice(0, 2),
      lunch: recipes.slice(2, 4),
      dinner: recipes.slice(4, 6),
      tips: [
        isHypertension ? '每日食盐不超过5g' : null,
        isDiabetes ? '控制碳水化合物摄入量' : null,
        '多食蔬菜水果，适量运动',
      ].filter(Boolean),
    };
  }

  /** 查询食材替换建议 */
  async getSubstitutions(ingredient: string) {
    // 基于食谱中的 substitutionTips 查询
    const recipes = await this.prisma.recipe.findMany({
      where: {
        substitutionTips: { not: '[]' },
      },
      take: 10,
    });

    // 收集所有替换建议
    const tips: Array<{ original: string; substitute: string; reason: string }> = [];
    for (const recipe of recipes) {
      const tipsData = recipe.substitutionTips as any[];
      if (Array.isArray(tipsData)) {
        for (const tip of tipsData) {
          if (tip && typeof tip === 'object') {
            tips.push({
              original: tip.original ?? '',
              substitute: tip.substitute ?? '',
              reason: tip.reason ?? '',
            });
          }
        }
      }
    }

    // 如果没有预设数据，返回常见替换建议
    if (tips.length === 0) {
      const commonSubstitutions: Record<string, Array<{ original: string; substitute: string; reason: string }>> = {
        盐: [
          { original: '食盐', substitute: '低钠盐', reason: '减少钠摄入，适合高血压患者' },
          { original: '酱油', substitute: '薄盐酱油', reason: '钠含量降低约30%' },
        ],
        糖: [
          { original: '白砂糖', substitute: '木糖醇', reason: '不升血糖，适合糖尿病患者' },
          { original: '蜂蜜', substitute: '罗汉果糖', reason: '天然甜味，零热量' },
        ],
        油: [
          { original: '猪油', substitute: '橄榄油', reason: '富含不饱和脂肪酸' },
          { original: '花生油', substitute: '亚麻籽油', reason: '富含Omega-3' },
        ],
      };

      const key = Object.keys(commonSubstitutions).find((k) =>
        ingredient.includes(k),
      );
      return {
        ingredient,
        substitutions: key ? commonSubstitutions[key] : [],
        message: key ? null : '暂无该食材的替换建议',
      };
    }

    return { ingredient, substitutions: tips };
  }

  /** 记录饮食 */
  async addDietRecord(
    userId: string,
    data: { mealType: string; description?: string; imageUrl?: string },
  ) {
    return this.prisma.dietRecord.create({
      data: {
        id: uuidv4(),
        userId,
        mealType: data.mealType,
        description: data.description ?? null,
        imageUrl: data.imageUrl ?? null,
      },
    });
  }

  /** 获取饮食记录 */
  async getDietRecords(userId: string, limit = 20) {
    return this.prisma.dietRecord.findMany({
      where: { userId },
      orderBy: { recordedAt: 'desc' },
      take: limit,
    });
  }
}
