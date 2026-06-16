import { Injectable, Logger } from '@nestjs/common';

/** 解析后的健康数据 */
export interface ParsedHealthData {
  /** 数据类型 */
  type: 'blood_pressure' | 'blood_sugar' | 'unknown';
  /** 原始文本 */
  rawText: string;
  /** 解析结果 */
  data: BloodPressureData | BloodSugarData | null;
  /** 是否解析成功 */
  parsed: boolean;
}

/** 血压数据 */
export interface BloodPressureData {
  systolic: number;
  diastolic: number;
  heartRate?: number;
}

/** 血糖数据 */
export interface BloodSugarData {
  type: 'fasting' | 'before_meal' | 'after_meal' | 'after_meal_2h' | 'random' | 'bedtime';
  value: number;
}

/**
 * 语音文本解析服务
 * 将语音识别文本解析为结构化数据
 * 支持血压、血糖等健康指标的语音录入
 */
@Injectable()
export class VoiceParserService {
  private readonly logger = new Logger(VoiceParserService.name);

  /**
   * 解析语音文本
   * @param text 语音识别出的文本
   * @returns 解析后的结构化数据
   */
  parse(text: string): ParsedHealthData {
    if (!text || text.trim().length === 0) {
      return {
        type: 'unknown',
        rawText: text,
        data: null,
        parsed: false,
      };
    }

    // 清理文本
    const cleanedText = this.cleanText(text);

    // 尝试解析血压
    const bpResult = this.parseBloodPressure(cleanedText);
    if (bpResult) {
      return {
        type: 'blood_pressure',
        rawText: text,
        data: bpResult,
        parsed: true,
      };
    }

    // 尝试解析血糖
    const bgResult = this.parseBloodSugar(cleanedText);
    if (bgResult) {
      return {
        type: 'blood_sugar',
        rawText: text,
        data: bgResult,
        parsed: true,
      };
    }

    // 解析失败
    return {
      type: 'unknown',
      rawText: text,
      data: null,
      parsed: false,
    };
  }

  /**
   * 清理文本
   * 移除多余的空格、标点符号等
   */
  private cleanText(text: string): string {
    return text
      .replace(/[，。！？、]/g, ' ') // 中文标点替换为空格
      .replace(/[,.!?]/g, ' ') // 英文标点替换为空格
      .replace(/\s+/g, ' ') // 多个空格合并
      .trim();
  }

  /**
   * 解析血压数据
   * 支持格式:
   * - "血压135 85"
   * - "收缩压135舒张压85"
   * - "高压135低压85"
   * - "135 85"
   * - "135/85"
   * - "135 85 心率72"
   */
  private parseBloodPressure(text: string): BloodPressureData | null {
    // 模式1: 血压/收缩压/高压 + 数字 + 舒张压/低压 + 数字
    const pattern1 = /(?:血压|收缩压|高压)\s*(\d{2,3})\s*(?:舒张压|低压)?\s*(\d{2,3})/i;
    const match1 = text.match(pattern1);
    if (match1) {
      const systolic = parseInt(match1[1]);
      const diastolic = parseInt(match1[2]);
      if (this.isValidBloodPressure(systolic, diastolic)) {
        const heartRate = this.extractHeartRate(text);
        return { systolic, diastolic, heartRate };
      }
    }

    // 模式2: 数字/数字 (如 135/85)
    const pattern2 = /(\d{2,3})\s*[/]\s*(\d{2,3})/;
    const match2 = text.match(pattern2);
    if (match2) {
      const systolic = parseInt(match2[1]);
      const diastolic = parseInt(match2[2]);
      if (this.isValidBloodPressure(systolic, diastolic)) {
        const heartRate = this.extractHeartRate(text);
        return { systolic, diastolic, heartRate };
      }
    }

    // 模式3: 两个连续的3位数 (如 135 85)
    const pattern3 = /(\d{2,3})\s+(\d{2,3})/;
    const match3 = text.match(pattern3);
    if (match3) {
      const systolic = parseInt(match3[1]);
      const diastolic = parseInt(match3[2]);
      if (this.isValidBloodPressure(systolic, diastolic)) {
        const heartRate = this.extractHeartRate(text);
        return { systolic, diastolic, heartRate };
      }
    }

    return null;
  }

  /**
   * 提取心率
   */
  private extractHeartRate(text: string): number | undefined {
    const pattern = /(?:心率|脉搏)\s*(\d{2,3})/;
    const match = text.match(pattern);
    if (match) {
      const heartRate = parseInt(match[1]);
      if (heartRate >= 30 && heartRate <= 250) {
        return heartRate;
      }
    }
    return undefined;
  }

  /**
   * 校验血压值是否有效
   */
  private isValidBloodPressure(systolic: number, diastolic: number): boolean {
    return (
      systolic > diastolic &&
      systolic >= 60 &&
      systolic <= 300 &&
      diastolic >= 30 &&
      diastolic <= 200
    );
  }

  /**
   * 解析血糖数据
   * 支持格式:
   * - "空腹血糖6.8"
   * - "餐后血糖9.0"
   * - "餐后两小时9.0"
   * - "随机血糖7.5"
   * - "睡前血糖6.2"
   * - "血糖6.8"
   */
  private parseBloodSugar(text: string): BloodSugarData | null {
    // 血糖类型映射
    const typeMap: Record<string, BloodSugarData['type']> = {
      '空腹': 'fasting',
      '餐前': 'before_meal',
      '餐后两小时': 'after_meal_2h',
      '餐后2小时': 'after_meal_2h',
      '餐后': 'after_meal',
      '随机': 'random',
      '睡前': 'bedtime',
    };

    // 模式1: 类型 + 血糖 + 数字
    for (const [keyword, type] of Object.entries(typeMap)) {
      const pattern = new RegExp(`${keyword}\\s*(?:血糖)?\\s*(\\d+\\.?\\d*)`, 'i');
      const match = text.match(pattern);
      if (match) {
        const value = parseFloat(match[1]);
        if (this.isValidBloodSugar(value)) {
          return { type, value };
        }
      }
    }

    // 模式2: 血糖 + 类型 + 数字
    for (const [keyword, type] of Object.entries(typeMap)) {
      const pattern = new RegExp(`血糖\\s*${keyword}\\s*(\\d+\\.?\\d*)`, 'i');
      const match = text.match(pattern);
      if (match) {
        const value = parseFloat(match[1]);
        if (this.isValidBloodSugar(value)) {
          return { type, value };
        }
      }
    }

    // 模式3: 血糖 + 数字 (默认为随机血糖)
    const pattern3 = /血糖\s*(\d+\.?\d*)/i;
    const match3 = text.match(pattern3);
    if (match3) {
      const value = parseFloat(match3[1]);
      if (this.isValidBloodSugar(value)) {
        return { type: 'random', value };
      }
    }

    // 模式4: 纯数字 + 血糖
    const pattern4 = /(\d+\.?\d*)\s*血糖/i;
    const match4 = text.match(pattern4);
    if (match4) {
      const value = parseFloat(match4[1]);
      if (this.isValidBloodSugar(value)) {
        return { type: 'random', value };
      }
    }

    return null;
  }

  /**
   * 校验血糖值是否有效
   */
  private isValidBloodSugar(value: number): boolean {
    return value >= 1.0 && value <= 35.0;
  }
}
