/**
 * Phase 3 数据 seed：食材替换规则（B3-8）。
 *
 * 运行方式（开发环境）：
 *   cd server && npx tsx prisma/seed-phase3.ts
 *
 * 已存在同名规则会被跳过（按 originalFood + substituteFood 联合去重）。
 */
import { PrismaClient } from '@prisma/client';
import { v4 as uuidv4 } from 'uuid';

const prisma = new PrismaClient();

interface RuleSeed {
  originalFood: string;
  substituteFood: string;
  reason: string;
  suitableFor: string[];
  nutritionDelta?: Record<string, string | number>;
  priority?: number;
}

const RULES: RuleSeed[] = [
  {
    originalFood: '红烧肉',
    substituteFood: '蒟蒻烧肉（魔芋+少量瘦肉）',
    reason: '魔芋热量近 0、含丰富膳食纤维，可减脂控糖',
    suitableFor: ['hypertension', 'diabetes'],
    nutritionDelta: { calories: '-60%', sodium: '-50%' },
    priority: 9,
  },
  {
    originalFood: '白米饭',
    substituteFood: '杂粮饭（糙米+燕麦+小米）',
    reason: 'GI 显著降低，餐后血糖更平稳',
    suitableFor: ['diabetes'],
    nutritionDelta: { gi: '70 → 55' },
    priority: 9,
  },
  {
    originalFood: '猪油',
    substituteFood: '橄榄油',
    reason: '富含单不饱和脂肪酸，降低心血管风险',
    suitableFor: ['hypertension'],
    nutritionDelta: { saturatedFat: '-70%' },
    priority: 8,
  },
  {
    originalFood: '酱油',
    substituteFood: '薄盐酱油 / 香菇粉',
    reason: '减少钠摄入，适合控盐人群',
    suitableFor: ['hypertension'],
    nutritionDelta: { sodium: '-30%' },
    priority: 8,
  },
  {
    originalFood: '白砂糖',
    substituteFood: '木糖醇',
    reason: '不显著升血糖，适合糖尿病人群',
    suitableFor: ['diabetes'],
    nutritionDelta: { gi: '0' },
    priority: 9,
  },
  {
    originalFood: '蜂蜜',
    substituteFood: '罗汉果糖',
    reason: '天然甜味、零热量、不升糖',
    suitableFor: ['diabetes'],
    priority: 7,
  },
  {
    originalFood: '咸菜',
    substituteFood: '凉拌时蔬',
    reason: '高钠食品替换为新鲜蔬菜，降低血压负担',
    suitableFor: ['hypertension'],
    nutritionDelta: { sodium: '-90%' },
    priority: 7,
  },
  {
    originalFood: '面包',
    substituteFood: '全麦面包',
    reason: 'GI 更低，膳食纤维含量更高',
    suitableFor: ['diabetes'],
    priority: 6,
  },
  {
    originalFood: '香肠',
    substituteFood: '鸡胸肉 / 鱼肉',
    reason: '加工肉钠和饱和脂肪过高，瘦肉/白肉更健康',
    suitableFor: ['hypertension', 'diabetes'],
    priority: 8,
  },
  {
    originalFood: '油炸食品',
    substituteFood: '空气炸 / 烤箱烤制',
    reason: '减少油脂摄入，降低心血管风险',
    suitableFor: ['hypertension', 'diabetes'],
    priority: 7,
  },
  {
    originalFood: '碳酸饮料',
    substituteFood: '柠檬水 / 淡茶',
    reason: '高糖饮料 → 零糖天然饮品',
    suitableFor: ['diabetes'],
    priority: 9,
  },
  {
    originalFood: '土豆',
    substituteFood: '红薯（适量）',
    reason: '红薯 GI 略低，膳食纤维更多',
    suitableFor: ['diabetes'],
    priority: 5,
  },
];

async function main() {
  let inserted = 0;
  let skipped = 0;
  for (const rule of RULES) {
    const exists = await prisma.substitutionRule.findFirst({
      where: {
        originalFood: rule.originalFood,
        substituteFood: rule.substituteFood,
      },
    });
    if (exists) {
      skipped += 1;
      continue;
    }
    await prisma.substitutionRule.create({
      data: {
        id: uuidv4(),
        originalFood: rule.originalFood,
        substituteFood: rule.substituteFood,
        reason: rule.reason,
        suitableFor: rule.suitableFor,
        nutritionDelta: rule.nutritionDelta ?? undefined,
        priority: rule.priority ?? 0,
      },
    });
    inserted += 1;
  }
  console.log(`[seed-phase3] inserted=${inserted}, skipped=${skipped}`);
}

main()
  .catch((e) => {
    console.error('[seed-phase3] failed', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
