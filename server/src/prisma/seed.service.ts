import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { PrismaService } from './prisma.service.js';

/**
 * 种子数据服务：在应用启动时插入急救指南数据（如果不存在）
 */
@Injectable()
export class SeedService implements OnModuleInit {
  private readonly logger = new Logger(SeedService.name);

  constructor(private readonly prisma: PrismaService) {}

  async onModuleInit() {
    await this.seedEmergencyGuides();
    await this.seedMedicationConflicts();
  }

  private async seedEmergencyGuides() {
    const count = await this.prisma.firstAidGuide.count();
    if (count > 0) return;

    const guides = [
      {
        type: 'chest_pain',
        title: '胸痛怎么办？',
        content:
          '如果突发胸痛，请立即停止活动，保持镇静。如有硝酸甘油，舌下含服1片。5分钟后不缓解可再含1片，最多3片。同时拨打120。',
        steps: [
          '立即停止活动，坐下或半卧位休息',
          '解开衣领和腰带，保持呼吸通畅',
          '舌下含服硝酸甘油（如有）',
          '5分钟后不缓解再含1片，最多3片',
          '拨打120，清晰说明地址和症状',
          '等待救护车期间不要走动',
        ],
      },
      {
        type: 'hypoglycemia',
        title: '低血糖怎么办？',
        content:
          '低血糖（血糖 ≤ 3.9 mmol/L）时立即摄入15-20克快速吸收的糖分，如半杯果汁、3-4颗硬糖或1杯牛奶。15分钟后复测血糖。',
        steps: [
          '立即摄入15-20克糖分：果汁半杯、硬糖3-4颗、蜂蜜1勺',
          '15分钟后复测血糖',
          '如仍低于3.9，再摄入15克糖分',
          '如恢复正常，吃一小餐（如面包片）',
          '如意识不清，不要喂食，立即拨打120',
        ],
      },
      {
        type: 'call_120',
        title: '打120时说什么？',
        content:
          '拨打120时要清晰、简洁地说明以下信息，并等对方挂断后再挂电话。',
        steps: [
          '说清楚地址：省市区街道小区楼号单元门牌号',
          '说清患者情况：年龄、性别、主要症状',
          '说清联系方式：保持电话畅通',
          '如有条件，派人到小区门口引导救护车',
          '准备好患者身份证、医保卡、常用药清单',
          '等对方挂断后再挂电话',
        ],
      },
      {
        type: 'nitroglycerin',
        title: '硝酸甘油怎么舌下含服？',
        content:
          '硝酸甘油片应在舌下含化，不要吞服。舌下黏膜吸收快，1-3分钟起效。含服时应取坐位，避免因血压下降而晕倒。',
        steps: [
          '取坐位或半卧位',
          '将1片硝酸甘油放在舌下',
          '让药片自然溶解，不要咀嚼或吞咽',
          '1-3分钟起效，药效持续30分钟',
          '5分钟后不缓解可再含1片，最多3片',
          '含完3片仍不缓解，立即拨打120',
        ],
      },
    ];

    for (const guide of guides) {
      await this.prisma.firstAidGuide.create({
        data: {
          ...guide,
          id: undefined as any,
          order: guides.indexOf(guide),
        },
      });
    }

    this.logger.log(`Seeded ${guides.length} emergency guides`);
  }

  private async seedMedicationConflicts() {
    const count = await this.prisma.medicationConflict.count();
    if (count > 0) return;

    const conflicts = [
      {
        drugA: '硝苯地平',
        drugB: '西柚汁',
        severity: 'high',
        description:
          '西柚汁可抑制硝苯地平代谢，导致血药浓度升高，增加低血压和心悸风险',
      },
      {
        drugA: '阿司匹林',
        drugB: '布洛芬',
        severity: 'high',
        description:
          '两者联用增加胃肠道出血风险，且布洛芬可能降低阿司匹林的心血管保护作用',
      },
      {
        drugA: '卡托普利',
        drugB: '钾补充剂',
        severity: 'high',
        description: '普利类降压药可升高血钾，与钾补充剂联用可能导致高钾血症',
      },
      {
        drugA: '二甲双胍',
        drugB: '含碘造影剂',
        severity: 'high',
        description:
          '二甲双胍与含碘造影剂联用可能增加乳酸性酸中毒风险，检查前后应暂停用药',
      },
      {
        drugA: '华法林',
        drugB: '阿司匹林',
        severity: 'high',
        description: '两者联用显著增加出血风险，需严密监测凝血功能',
      },
      {
        drugA: '氨氯地平',
        drugB: '葡萄柚汁',
        severity: 'medium',
        description: '葡萄柚汁可能增加氨氯地平的血药浓度，增强降压效果',
      },
      {
        drugA: '硝苯地平',
        drugB: '利福平',
        severity: 'medium',
        description: '利福平可诱导CYP3A4酶加速硝苯地平代谢，降低降压疗效',
      },
      {
        drugA: '胰岛素',
        drugB: '氢化可的松',
        severity: 'medium',
        description: '糖皮质激素可升高血糖，与胰岛素同时使用需调整胰岛素剂量',
      },
      {
        drugA: '氢氯噻嗪',
        drugB: '地高辛',
        severity: 'medium',
        description: '噻嗪类利尿剂可能引起低钾血症，增加地高辛中毒风险',
      },
      {
        drugA: '普萘洛尔',
        drugB: '胰岛素',
        severity: 'medium',
        description:
          'β受体阻滞剂可能掩盖低血糖症状（如心悸、震颤），延迟低血糖识别',
      },
    ];

    for (const c of conflicts) {
      await this.prisma.medicationConflict.create({ data: c });
    }

    this.logger.log(`Seeded ${conflicts.length} medication conflicts`);
  }
}
