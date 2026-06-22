import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';

interface RecommendQuery {
  diseases?: string[];
  servings?: number;
  mealType?: string;
  keyword?: string;
  limit?: number;
}

/**
 * 双病食谱推荐（B3-7）。
 *
 * 筛选规则：
 * - 高血压（hypertension）→ sodium < 120 mg/100g
 * - 糖尿病（diabetes）   → glycemicIndex 属于 ['low', 'medium-low']
 * - 多病种取交集（且关系）
 * - 同时 suitableFor Json 必须包含传入的 diseases
 *
 * servings 切换：库内默认按食谱内置份数，调用端传 servings 时按比例缩放 ingredients 数量。
 */
@Injectable()
export class RecipeRecommendationService {
  constructor(private readonly prisma: PrismaService) {}

  /** 低 GI 集合：库内 glycemicIndex 字段允许的取值 */
  private static readonly LOW_GI_VALUES = ['low', 'medium-low'];

  async recommend(query: RecommendQuery) {
    const {
      diseases = [],
      servings,
      mealType,
      keyword,
      limit = 30,
    } = query;

    const where: Record<string, unknown> = {};

    if (diseases.includes('hypertension')) {
      // sodium 低于 120 mg/100g
      (where as { sodium?: object }).sodium = { lt: 120 };
    }
    if (diseases.includes('diabetes')) {
      where.glycemicIndex = {
        in: RecipeRecommendationService.LOW_GI_VALUES,
      };
    }
    if (keyword && keyword.trim().length > 0) {
      where.name = { contains: keyword.trim() };
    }

    let recipes = await this.prisma.recipe.findMany({
      where,
      orderBy: { calories: 'asc' },
      take: limit,
    });

    // suitableFor 是 Json，前置 SQL 过滤难复用，此处内存二次筛
    if (diseases.length > 0) {
      recipes = recipes.filter((r) => {
        const tags = Array.isArray(r.suitableFor) ? r.suitableFor : [];
        return diseases.every((d) => tags.includes(d));
      });
    }

    // mealType 过滤：当前 Recipe 表无 mealType 字段，约定通过 suitableFor 标签或 keyword 匹配
    // MVP 实现：把 mealType 加进 keyword 模糊匹配条件
    if (mealType && !keyword) {
      const mealKeywords: Record<string, string[]> = {
        breakfast: ['粥', '面包', '蛋', '豆浆'],
        lunch: ['饭', '面', '汤'],
        dinner: ['饭', '炒', '蒸'],
      };
      const keys = mealKeywords[mealType] ?? [];
      if (keys.length > 0) {
        recipes = recipes.filter((r) => keys.some((k) => r.name.includes(k)));
      }
    }

    // servings 缩放（仅在返回数据时计算，不影响原始数据）
    const result = servings && servings > 0
      ? recipes.map((r) => this.scaleRecipe(r, servings))
      : recipes;

    return {
      total: result.length,
      diseases,
      servings: servings ?? null,
      list: result,
    };
  }

  /** 获取推荐食谱（简化接口，供 Controller 调用） */
  async getRecommendedRecipes(diseases: string[], people: number) {
    return this.recommend({
      diseases,
      servings: people,
      limit: 20,
    });
  }

  /** 按目标人数缩放 ingredients 中的 quantity（返回新对象） */
  private scaleRecipe(recipe: any, targetServings: number) {
    if (!Array.isArray(recipe.ingredients) || recipe.servings <= 0) return recipe;
    const ratio = targetServings / recipe.servings;
    const scaled = recipe.ingredients.map((it: any) => {
      if (it && typeof it === 'object') {
        const q = it.quantity;
        if (typeof q === 'number') {
          return { ...it, quantity: parseFloat((q * ratio).toFixed(2)) };
        }
      }
      return it;
    });
    return { ...recipe, servings: targetServings, ingredients: scaled };
  }
}
