import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class DietService {
  constructor(private readonly prisma: PrismaService) {}

  /** 获取食谱列表（支持按适合人群筛选） */
  async getRecipes(suitableFor?: string, limit = 50) {
    const where: any = {};
    if (suitableFor) {
      where.suitableFor = { array_contains: suitableFor };
    }
    return this.prisma.recipe.findMany({
      where,
      orderBy: { name: 'asc' },
      take: limit,
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

  /** 获取用户自定义今日菜单 */
  async getCustomMenu(userId: string) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const menus = await this.prisma.dailyMenu.findMany({
      where: {
        userId,
        date: { gte: today, lt: tomorrow },
      },
      include: { user: false },
    });

    // 获取关联的食谱
    const recipeIds = menus.map((m) => m.recipeId).filter(Boolean);
    const recipes = recipeIds.length > 0
      ? await this.prisma.recipe.findMany({ where: { id: { in: recipeIds } } })
      : [];

    return {
      date: today,
      meals: menus.map((m) => ({
        ...m,
        recipe: recipes.find((r) => r.id === m.recipeId) || null,
      })),
    };
  }

  /** 保存自定义今日菜单 */
  async saveCustomMenu(userId: string, recipeIds: string[]) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    // 删除今天的旧菜单
    await this.prisma.dailyMenu.deleteMany({
      where: { userId, date: { gte: today, lt: tomorrow } },
    });

    // 创建新菜单
    const mealTypes = ['breakfast', 'lunch', 'dinner'];
    const created = [];
    for (let i = 0; i < recipeIds.length; i++) {
      const menu = await this.prisma.dailyMenu.create({
        data: {
          id: uuidv4(),
          userId,
          date: today,
          mealType: mealTypes[Math.min(i, 2)],
          recipeId: recipeIds[i],
          servings: 1,
        },
      });
      created.push(menu);
    }

    return created;
  }

  /** 查询食材替换建议 */
  async getSubstitutions(ingredient: string) {
    // 1) 优先查 SubstitutionRule 规则表（B3-8）
    if (ingredient && ingredient.trim().length > 0) {
      const rules = await this.prisma.substitutionRule.findMany({
        where: { originalFood: { contains: ingredient.trim() } },
        orderBy: { priority: 'desc' },
        take: 20,
      });

      if (rules.length > 0) {
        return {
          ingredient,
          source: 'rule',
          substitutions: rules.map((r) => ({
            original: r.originalFood,
            substitute: r.substituteFood,
            reason: r.reason,
            suitableFor: r.suitableFor,
            nutritionDelta: r.nutritionDelta,
          })),
        };
      }
    }

    // 2) Fallback：从食谱 substitutionTips 收集
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
        source: 'fallback',
        substitutions: key ? commonSubstitutions[key] : [],
        message: key ? null : '暂无该食材的替换建议',
      };
    }

    return { ingredient, source: 'recipe-tips', substitutions: tips };
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

  /** 获取饮食记录（支持按日期过滤） */
  async getDietRecords(userId: string, options: { limit?: number; date?: string } = {}) {
    const { limit = 20, date } = options;

    const where: { userId: string; recordedAt?: { gte: Date; lt: Date } } = {
      userId,
    };

    if (date) {
      const start = new Date(date);
      if (Number.isNaN(start.getTime())) {
        return [];
      }
      start.setHours(0, 0, 0, 0);
      const end = new Date(start);
      end.setDate(end.getDate() + 1);
      where.recordedAt = { gte: start, lt: end };
    }

    return this.prisma.dietRecord.findMany({
      where,
      orderBy: { recordedAt: 'desc' },
      take: limit,
    });
  }

  /** 删除饮食记录 */
  async deleteDietRecord(recordId: string, userId: string) {
    const record = await this.prisma.dietRecord.findUnique({
      where: { id: recordId },
    });

    if (!record) throw new NotFoundException('记录不存在');
    if (record.userId !== userId) throw new ForbiddenException('无权删除');

    await this.prisma.dietRecord.delete({ where: { id: recordId } });
    return { deleted: true };
  }

  /** 生成买菜清单（基于今日菜单） */
  async generateShoppingList(userId: string) {
    // 获取用户今日菜单
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const menus = await this.prisma.dailyMenu.findMany({
      where: { userId, date: { gte: today, lt: tomorrow } },
    });

    if (menus.length === 0) {
      return { items: [], message: '请先设置今日菜单' };
    }

    // 获取关联的食谱
    const recipeIds = menus.map((m) => m.recipeId);
    const recipes = await this.prisma.recipe.findMany({
      where: { id: { in: recipeIds } },
    });

    // 收集所有食材
    const ingredientMap = new Map<string, { amount: number; unit: string }>();
    for (const recipe of recipes) {
      const ingredients = recipe.ingredients as any[];
      if (Array.isArray(ingredients)) {
        for (const ing of ingredients) {
          const key = ing.name;
          if (ingredientMap.has(key)) {
            const existing = ingredientMap.get(key)!;
            existing.amount += ing.amount || 0;
          } else {
            ingredientMap.set(key, {
              amount: ing.amount || 0,
              unit: ing.unit || '份',
            });
          }
        }
      }
    }

    // 转换为数组
    const items = Array.from(ingredientMap.entries()).map(([name, info]) => ({
      ingredient: name,
      amount: info.amount,
      unit: info.unit,
      checked: false,
    }));

    return { items, date: today };
  }

  /** 获取已保存的买菜清单 */
  async getSavedShoppingList(userId: string) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const list = await this.prisma.shoppingList.findFirst({
      where: {
        userId,
        date: { gte: today, lt: tomorrow },
      },
      include: { items: true },
      orderBy: { createdAt: 'desc' },
    });

    return list || { items: [] };
  }

  /** 保存买菜清单 */
  async saveShoppingList(
    userId: string,
    items: Array<{ ingredient: string; amount: string; unit: string }>,
  ) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    // 删除今天的旧清单
    const oldLists = await this.prisma.shoppingList.findMany({
      where: { userId, date: { gte: today, lt: tomorrow } },
      select: { id: true },
    });
    if (oldLists.length > 0) {
      await this.prisma.shoppingListItem.deleteMany({
        where: { listId: { in: oldLists.map((l) => l.id) } },
      });
      await this.prisma.shoppingList.deleteMany({
        where: { id: { in: oldLists.map((l) => l.id) } },
      });
    }

    // 创建新清单
    const list = await this.prisma.shoppingList.create({
      data: {
        id: uuidv4(),
        userId,
        date: today,
        name: `买菜清单 ${today.toLocaleDateString('zh-CN')}`,
        items: {
          create: items.map((item) => ({
            id: uuidv4(),
            category: '食材',
            name: item.ingredient,
            quantity: parseFloat(item.amount) || 1,
            unit: item.unit,
            checked: false,
          })),
        },
      },
      include: { items: true },
    });

    return list;
  }

  /** 勾选买菜清单项目 */
  async checkShoppingItem(listId: string, itemIndex: number, userId: string) {
    const list = await this.prisma.shoppingList.findUnique({
      where: { id: listId },
      include: { items: true },
    });

    if (!list) throw new NotFoundException('清单不存在');
    if (list.userId !== userId) throw new ForbiddenException('无权操作');

    const items = list.items.sort((a, b) => a.id.localeCompare(b.id));
    if (itemIndex >= 0 && itemIndex < items.length) {
      const item = items[itemIndex];
      await this.prisma.shoppingListItem.update({
        where: { id: item.id },
        data: { checked: !item.checked },
      });
    }

    return this.prisma.shoppingList.findUnique({
      where: { id: listId },
      include: { items: true },
    });
  }
}
