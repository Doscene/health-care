import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { OcrProvider, OcrResult } from './interfaces/ocr-provider.interface.js';
import { BaiduOcrProvider } from './providers/baidu-ocr.provider.js';
import { TencentOcrProvider } from './providers/tencent-ocr.provider.js';

/** OCR调用日志 */
export interface OcrCallLog {
  provider: string;
  success: boolean;
  confidence: number;
  duration: number;
  error?: string;
  timestamp: Date;
}

/**
 * OCR 门面服务
 * 自动选择主厂商，失败时按优先级切换备选厂商
 */
@Injectable()
export class OcrService {
  private readonly logger = new Logger(OcrService.name);

  /** Provider 列表（按优先级排序） */
  private providers: OcrProvider[] = [];

  /** 主厂商索引 */
  private primaryIndex = 0;

  /** 置信度阈值，低于此值会触发Fallback */
  private readonly confidenceThreshold = 0.6;

  /** 最近的调用日志（内存中保留最近100条） */
  private callLogs: OcrCallLog[] = [];
  private readonly maxLogSize = 100;

  constructor(
    private readonly configService: ConfigService,
    private readonly baiduOcr: BaiduOcrProvider,
    private readonly tencentOcr: TencentOcrProvider,
  ) {
    this.initializeProviders();
  }

  /**
   * 初始化 Provider 列表
   * 根据配置决定主厂商和备选厂商
   */
  private async initializeProviders() {
    const primary = this.configService.get<string>(
      'OCR_PRIMARY_PROVIDER',
      'baidu',
    );

    // 按配置的主厂商排序
    if (primary === 'tencent') {
      this.providers = [this.tencentOcr, this.baiduOcr];
    } else {
      this.providers = [this.baiduOcr, this.tencentOcr];
    }

    this.primaryIndex = 0;

    // 检查各 Provider 可用性
    for (const provider of this.providers) {
      const available = await provider.isAvailable();
      this.logger.log(
        `OCR Provider [${provider.name}] ${available ? '可用' : '不可用'}`,
      );
    }
  }

  /**
   * 识别图片文字
   * 自动选择可用的 Provider，失败时自动 Fallback
   */
  async recognize(imageBase64: string): Promise<OcrResult> {
    if (!imageBase64 || imageBase64.length === 0) {
      throw new BadRequestException('图片数据不能为空');
    }

    // 移除可能的 data:image/xxx;base64, 前缀
    const base64Data = imageBase64.replace(/^data:image\/\w+;base64,/, '');

    let lastError: Error | null = null;

    // 尝试所有 Provider
    for (let i = 0; i < this.providers.length; i++) {
      const providerIndex = (this.primaryIndex + i) % this.providers.length;
      const provider = this.providers[providerIndex];

      const startTime = Date.now();

      try {
        this.logger.log(`尝试使用 [${provider.name}] 进行OCR识别...`);

        const result = await provider.recognize(base64Data);
        const duration = Date.now() - startTime;

        // 检查置信度
        if (result.confidence < this.confidenceThreshold) {
          this.logger.warn(
            `[${provider.name}] 置信度过低: ${result.confidence.toFixed(2)} < ${this.confidenceThreshold}`,
          );

          // 记录日志
          this.addCallLog({
            provider: provider.name,
            success: true,
            confidence: result.confidence,
            duration,
            timestamp: new Date(),
          });

          // 如果不是最后一个 Provider，尝试下一个
          if (i < this.providers.length - 1) {
            this.logger.log('置信度过低，尝试下一个 Provider...');
            continue;
          }
        }

        // 成功
        this.logger.log(
          `[${provider.name}] 识别成功，置信度: ${result.confidence.toFixed(2)}，耗时: ${duration}ms`,
        );

        this.addCallLog({
          provider: provider.name,
          success: true,
          confidence: result.confidence,
          duration,
          timestamp: new Date(),
        });

        // 如果使用的是备选厂商成功，更新主厂商索引
        if (providerIndex !== this.primaryIndex) {
          this.logger.log(`切换主厂商为 [${provider.name}]`);
          this.primaryIndex = providerIndex;
        }

        return result;
      } catch (error) {
        const duration = Date.now() - startTime;
        lastError = error as Error;

        this.logger.error(
          `[${provider.name}] 识别失败: ${lastError.message}，耗时: ${duration}ms`,
        );

        this.addCallLog({
          provider: provider.name,
          success: false,
          confidence: 0,
          duration,
          error: lastError.message,
          timestamp: new Date(),
        });

        // 继续尝试下一个 Provider
        continue;
      }
    }

    // 所有 Provider 都失败
    throw new BadRequestException(
      `OCR识别失败: 所有厂商都不可用。最后错误: ${lastError?.message}`,
    );
  }

  /**
   * 添加调用日志
   */
  private addCallLog(log: OcrCallLog) {
    this.callLogs.push(log);
    if (this.callLogs.length > this.maxLogSize) {
      this.callLogs.shift();
    }
  }

  /**
   * 获取调用统计
   */
  getStats(): {
    totalCalls: number;
    successRate: number;
    providerStats: Record<
      string,
      { calls: number; successRate: number; avgConfidence: number }
    >;
  } {
    const totalCalls = this.callLogs.length;
    const successCalls = this.callLogs.filter((log) => log.success).length;

    // 按 Provider 统计
    const providerStats: Record<
      string,
      { calls: number; success: number; confidenceSum: number }
    > = {};

    for (const log of this.callLogs) {
      if (!providerStats[log.provider]) {
        providerStats[log.provider] = {
          calls: 0,
          success: 0,
          confidenceSum: 0,
        };
      }
      providerStats[log.provider].calls++;
      if (log.success) {
        providerStats[log.provider].success++;
        providerStats[log.provider].confidenceSum += log.confidence;
      }
    }

    const result: Record<
      string,
      { calls: number; successRate: number; avgConfidence: number }
    > = {};
    for (const [provider, stats] of Object.entries(providerStats)) {
      result[provider] = {
        calls: stats.calls,
        successRate: stats.calls > 0 ? stats.success / stats.calls : 0,
        avgConfidence:
          stats.success > 0 ? stats.confidenceSum / stats.success : 0,
      };
    }

    return {
      totalCalls,
      successRate: totalCalls > 0 ? successCalls / totalCalls : 0,
      providerStats: result,
    };
  }

  /**
   * 获取最近的调用日志
   */
  getRecentLogs(limit: number = 20): OcrCallLog[] {
    return this.callLogs.slice(-limit);
  }
}
