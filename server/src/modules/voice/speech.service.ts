import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AsrProvider, AsrResult } from './interfaces/asr-provider.interface.js';
import { BaiduAsrProvider } from './providers/baidu-asr.provider.js';

/**
 * 语音识别门面服务
 * 支持主备切换（MVP先实现百度ASR）
 */
@Injectable()
export class SpeechService {
  private readonly logger = new Logger(SpeechService.name);

  /** Provider 列表（按优先级排序） */
  private providers: AsrProvider[] = [];

  /** 主厂商索引 */
  private primaryIndex = 0;

  constructor(
    private readonly configService: ConfigService,
    private readonly baiduAsr: BaiduAsrProvider,
  ) {
    this.initializeProviders();
  }

  /**
   * 初始化 Provider 列表
   */
  private async initializeProviders() {
    // MVP 只有百度 ASR
    this.providers = [this.baiduAsr];
    this.primaryIndex = 0;

    // 检查各 Provider 可用性
    for (const provider of this.providers) {
      const available = await provider.isAvailable();
      this.logger.log(`ASR Provider [${provider.name}] ${available ? '可用' : '不可用'}`);
    }
  }

  /**
   * 识别语音
   * 自动选择可用的 Provider
   */
  async recognize(audioBase64: string, format: string = 'pcm'): Promise<AsrResult> {
    if (!audioBase64 || audioBase64.length === 0) {
      throw new BadRequestException('音频数据不能为空');
    }

    let lastError: Error | null = null;

    // 尝试所有 Provider
    for (let i = 0; i < this.providers.length; i++) {
      const providerIndex = (this.primaryIndex + i) % this.providers.length;
      const provider = this.providers[providerIndex];

      try {
        this.logger.log(`尝试使用 [${provider.name}] 进行语音识别...`);

        const result = await provider.recognize(audioBase64, format);

        this.logger.log(`[${provider.name}] 识别成功: "${result.text}"`);

        return result;
      } catch (error) {
        lastError = error as Error;
        this.logger.error(`[${provider.name}] 识别失败: ${lastError.message}`);
        continue;
      }
    }

    // 所有 Provider 都失败
    throw new BadRequestException(`语音识别失败: 所有厂商都不可用。最后错误: ${lastError?.message}`);
  }

  /**
   * 获取可用的 Provider 列表
   */
  async getAvailableProviders(): Promise<string[]> {
    const available: string[] = [];
    for (const provider of this.providers) {
      if (await provider.isAvailable()) {
        available.push(provider.name);
      }
    }
    return available;
  }
}
