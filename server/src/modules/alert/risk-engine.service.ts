import { Injectable, Logger } from '@nestjs/common';
import { PrismaService } from '../../prisma/prisma.service.js';
import { v4 as uuidv4 } from 'uuid';

type RiskLevel = 'green' | 'yellow' | 'orange' | 'red';
type TriggerType =
  | 'bp_high'
  | 'bp_critical'
  | 'bg_high'
  | 'bg_low'
  | 'bg_critical'
  | 'missed_dose'
  | 'symptom_combo';

interface RiskResult {
  level: RiskLevel;
  triggerType: TriggerType;
  triggerValue: string;
  message: string;
}

interface BpInput {
  systolic: number;
  diastolic: number;
}

interface BgInput {
  type: string;
  value: number;
}

/**
 * 四级风险判断引擎
 *
 * 绿色 🟢 → 正常，无需任何动作
 * 黄色 🟡 → 单次异常，推送健康科普 + 建议复测
 * 橙色 🟠 → 持续异常（连续3天），通知紧急联系人 + 问询卡片
 * 红色 🔴 → 危险值，全屏弹窗 + 通知全部联系人 + 一键呼救
 */
@Injectable()
export class RiskEngine {
  private readonly logger = new Logger(RiskEngine.name);

  /** 血压风险阈值（单位 mmHg） */
  private static readonly BP_THRESHOLDS = {
    /** 绿色：收缩压 < 140 且 舒张压 < 90 */
    green: { systolicMax: 139, diastolicMax: 89 },
    /** 黄色：收缩压 140-159 或 舒张压 90-99 */
    yellow: {
      systolicMin: 140,
      systolicMax: 159,
      diastolicMin: 90,
      diastolicMax: 99,
    },
    /** 橙色：连续3天 收缩压 > 150 */
    orange: { systolicPersistent: 150 },
    /** 红色：收缩压 ≥ 180 或 舒张压 ≥ 110 */
    red: { systolicMin: 180, diastolicMin: 110 },
  };

  /** 血糖风险阈值（单位 mmol/L） */
  private static readonly BG_THRESHOLDS = {
    /** 绿色：空腹 3.9-7.0，餐后 < 10.0 */
    green: { fastingMin: 3.9, fastingMax: 7.0, postMealMax: 10.0 },
    /** 黄色：空腹 7.0-8.0 或 餐后 10.0-13.0 */
    yellow: {
      fastingMin: 7.0,
      fastingMax: 8.0,
      postMealMin: 10.0,
      postMealMax: 13.0,
    },
    /** 橙色：连续3天 空腹 > 8.0 */
    orange: { fastingPersistent: 8.0 },
    /** 红色：空腹 ≥ 13.9 或 ≤ 2.8 */
    red: { fastingCriticalHigh: 13.9, fastingCriticalLow: 2.8 },
  };

  constructor(private readonly prisma: PrismaService) {}

  /** 评估血压风险 */
  evaluateBpRisk(bp: BpInput): RiskResult {
    const { systolic, diastolic } = bp;

    // 红色：收缩压 ≥ 180 或 舒张压 ≥ 110
    if (
      systolic >= RiskEngine.BP_THRESHOLDS.red.systolicMin ||
      diastolic >= RiskEngine.BP_THRESHOLDS.red.diastolicMin
    ) {
      return {
        level: 'red',
        triggerType: 'bp_critical',
        triggerValue: `${systolic}/${diastolic}`,
        message: `血压危险值 ${systolic}/${diastolic}！请立即休息并联系医生`,
      };
    }

    // 黄色：收缩压 140-159 或 舒张压 90-99
    const yb = RiskEngine.BP_THRESHOLDS.yellow;
    if (
      (systolic >= yb.systolicMin && systolic <= yb.systolicMax) ||
      (diastolic >= yb.diastolicMin && diastolic <= yb.diastolicMax)
    ) {
      return {
        level: 'yellow',
        triggerType: 'bp_high',
        triggerValue: `${systolic}/${diastolic}`,
        message: '血压偏高，建议休息后复测',
      };
    }

    // 绿色
    return {
      level: 'green',
      triggerType: 'bp_high',
      triggerValue: `${systolic}/${diastolic}`,
      message: '血压正常',
    };
  }

  /** 评估血糖风险 */
  evaluateBgRisk(bg: BgInput): RiskResult {
    const isFasting = bg.type === 'fasting' || bg.type === 'before_meal';

    // 红色：空腹 ≥ 13.9 或 ≤ 2.8；任何低血糖症状
    if (
      bg.value >= RiskEngine.BG_THRESHOLDS.red.fastingCriticalHigh ||
      bg.value <= RiskEngine.BG_THRESHOLDS.red.fastingCriticalLow
    ) {
      return {
        level: 'red',
        triggerType:
          bg.value <= RiskEngine.BG_THRESHOLDS.red.fastingCriticalLow
            ? 'bg_low'
            : 'bg_critical',
        triggerValue: `${bg.type}:${bg.value}`,
        message:
          bg.value <= RiskEngine.BG_THRESHOLDS.red.fastingCriticalLow
            ? '低血糖危险值！请立即摄入糖分'
            : '血糖危险值！请立即采取措施',
      };
    }

    // 黄色：空腹 7.0-8.0 或 餐后 10.0-13.0
    const yb = RiskEngine.BG_THRESHOLDS.yellow;
    if (
      (isFasting && bg.value >= yb.fastingMin && bg.value <= yb.fastingMax) ||
      (!isFasting && bg.value >= yb.postMealMin && bg.value <= yb.postMealMax)
    ) {
      return {
        level: 'yellow',
        triggerType: 'bg_high',
        triggerValue: `${bg.type}:${bg.value}`,
        message: '血糖偏高，建议记录饮食',
      };
    }

    // 绿色
    return {
      level: 'green',
      triggerType: 'bg_high',
      triggerValue: `${bg.type}:${bg.value}`,
      message: '血糖正常',
    };
  }

  /** 评估连续异常——检查最近3天的 BP 数据判断是否升级为橙色 */
  async evaluateBpPersistent(userId: string): Promise<RiskResult | null> {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);

    const recentRecords = await this.prisma.bloodPressureRecord.findMany({
      where: {
        userId,
        recordedAt: { gte: threeDaysAgo },
      },
      orderBy: { recordedAt: 'desc' },
      take: 10,
    });

    if (recentRecords.length < 3) return null;

    // 按天分组
    const dayRecords = new Map<string, typeof recentRecords>();
    for (const r of recentRecords) {
      const day = r.recordedAt.toISOString().split('T')[0];
      if (!dayRecords.has(day)) dayRecords.set(day, []);
      dayRecords.get(day)!.push(r);
    }

    // 检查是否连续3天收缩压 > 150
    const days = Array.from(dayRecords.keys()).sort().reverse();
    if (days.length < 3) return null;

    let consecutiveHigh = 0;
    const orangeThreshold = RiskEngine.BP_THRESHOLDS.orange.systolicPersistent;

    for (let i = 0; i < Math.min(days.length, 5); i++) {
      const dailyRecords = dayRecords.get(days[i])!;
      const maxSystolic = Math.max(...dailyRecords.map((r) => r.systolic));
      if (maxSystolic > orangeThreshold) {
        consecutiveHigh++;
        if (consecutiveHigh >= 3) {
          return {
            level: 'orange',
            triggerType: 'bp_high',
            triggerValue: `连续${consecutiveHigh}天收缩压>${orangeThreshold}`,
            message: '近3天血压偏高，建议联系家庭医生',
          };
        }
      } else {
        consecutiveHigh = 0;
      }
    }
    return null;
  }

  /** 评估连续血糖异常——检查最近3天的 BG 数据 */
  async evaluateBgPersistent(userId: string): Promise<RiskResult | null> {
    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000);

    const recentRecords = await this.prisma.bloodSugarRecord.findMany({
      where: {
        userId,
        type: 'fasting',
        recordedAt: { gte: threeDaysAgo },
      },
      orderBy: { recordedAt: 'desc' },
      take: 10,
    });

    if (recentRecords.length < 3) return null;

    const days = new Set(
      recentRecords.map((r) => r.recordedAt.toISOString().split('T')[0]),
    );
    if (days.size < 3) return null;

    const persistentThreshold =
      RiskEngine.BG_THRESHOLDS.orange.fastingPersistent;
    const highCount = recentRecords.filter(
      (r) => Number(r.value) > persistentThreshold,
    ).length;

    if (highCount >= 3) {
      return {
        level: 'orange',
        triggerType: 'bg_high',
        triggerValue: `连续${highCount}天空腹血糖>${persistentThreshold}`,
        message: '血糖持续偏高，建议复诊调整方案',
      };
    }

    return null;
  }

  /** 创建风险告警记录 */
  async createAlert(
    userId: string,
    result: RiskResult,
  ): Promise<string | null> {
    if (result.level === 'green') return null;

    const contacts = await this.prisma.emergencyContact.findMany({
      where: { userId },
      orderBy: { priority: 'asc' },
    });

    const notifiedContacts: string[] = [];
    if (result.level === 'red') {
      // 红色：通知全部紧急联系人
      notifiedContacts.push(...contacts.map((c) => c.id));
    } else if (result.level === 'orange') {
      // 橙色：通知第一紧急联系人
      const first = contacts[0];
      if (first) notifiedContacts.push(first.id);
    }
    // 黄色：仅患者本人（不通知联系人）

    // 检查是否已有活跃的同类告警，避免重复创建
    const existing = await this.prisma.riskAlert.findFirst({
      where: {
        userId,
        triggerType: result.triggerType,
        status: 'active',
        createdAt: { gte: new Date(Date.now() - 6 * 60 * 60 * 1000) },
      },
    });
    if (existing) return null;

    const alert = await this.prisma.riskAlert.create({
      data: {
        id: uuidv4(),
        userId,
        level: result.level,
        triggerType: result.triggerType,
        triggerValue: result.triggerValue,
        status: 'active',
        notifiedContacts,
      },
    });

    this.logger.log(
      `[Risk] ${result.level.toUpperCase()} alert for user ${userId}: ${result.message}`,
    );

    return alert.id;
  }

  /** 橙色级别：创建问询卡片 */
  async createInquiry(
    userId: string,
    alertId: string,
    triggerType: TriggerType,
  ): Promise<void> {
    let question: string;
    let options: string[];

    if (triggerType === 'bp_high' || triggerType === 'bp_critical') {
      question = '血压最近波动有点大 📈\n最近血压偏高，可能和什么有关？';
      options = [
        '睡眠不好',
        '吃咸了',
        '忘记吃药',
        '最近压力大',
        '不知道 / 其他',
      ];
    } else if (triggerType === 'bg_high' || triggerType === 'bg_critical') {
      question = '血糖最近有点高 📈\n最近血糖偏高，可能和什么有关？';
      options = [
        '饮食问题',
        '忘记吃药',
        '缺少运动',
        '最近压力大',
        '不知道 / 其他',
      ];
    } else if (triggerType === 'bg_low') {
      question = '血糖偏低！请确认身体状况';
      options = ['已进食糖分', '感觉还好', '头晕不适', '需要帮助'];
    } else if (triggerType === 'missed_dose') {
      question = '您连续2次没按时吃药了，是遇到什么情况吗？';
      options = ['忘了', '身体不舒服', '药吃完了', '自己减量了', '其他'];
    } else {
      question = '身体状况有变化，请确认';
      options = ['还好', '不舒服', '需要帮助'];
    }

    await this.prisma.riskInquiry.create({
      data: {
        id: uuidv4(),
        alertId,
        question,
        options,
      },
    });

    this.logger.log(`[Inquiry] Created for alert ${alertId}`);
  }
}
