import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  AsrProvider,
  AsrResult,
} from '../interfaces/asr-provider.interface.js';

/**
 * 百度语音识别 Provider
 * 使用百度云语音识别API
 * 文档: https://ai.baidu.com/ai-doc/Speech/1k3h7y3db
 */
@Injectable()
export class BaiduAsrProvider implements AsrProvider {
  readonly name = 'baidu';
  private readonly logger = new Logger(BaiduAsrProvider.name);

  private accessToken: string | null = null;
  private tokenExpireAt = 0;

  private readonly apiKey: string;
  private readonly secretKey: string;

  constructor(private readonly configService: ConfigService) {
    this.apiKey = this.configService.get<string>('BAIDU_ASR_API_KEY', '');
    this.secretKey = this.configService.get<string>('BAIDU_ASR_SECRET_KEY', '');
  }

  /**
   * 获取Access Token (OAuth2 client_credentials)
   * 会缓存到内存，过期前自动刷新
   */
  private async getAccessToken(): Promise<string> {
    const now = Date.now();

    // Token 未过期，直接返回缓存
    if (this.accessToken && now < this.tokenExpireAt) {
      return this.accessToken;
    }

    this.logger.log('刷新百度ASR Access Token...');

    const url = `https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=${this.apiKey}&client_secret=${this.secretKey}`;

    const response = await fetch(url, { method: 'POST' });
    if (!response.ok) {
      throw new Error(`获取百度Token失败: ${response.status}`);
    }

    const data = await response.json();
    if (!data.access_token) {
      throw new Error('百度Token响应格式错误');
    }

    this.accessToken = data.access_token as string;
    // 提前5分钟刷新
    this.tokenExpireAt = now + (data.expires_in - 300) * 1000;

    this.logger.log('百度ASR Token刷新成功');
    return this.accessToken;
  }

  /**
   * 识别语音
   * 使用短语音识别标准版API
   */
  async recognize(
    audioBase64: string,
    format: string = 'pcm',
  ): Promise<AsrResult> {
    const token = await this.getAccessToken();

    // 根据格式确定参数
    const formatConfig = this.getFormatConfig(format);

    // 请求体
    const body = {
      format: formatConfig.format,
      rate: formatConfig.rate,
      channel: 1,
      cuid: 'healthcare-app', // 用户唯一标识
      dev_pid: formatConfig.devPid,
      speech: audioBase64,
      len: Buffer.from(audioBase64, 'base64').length,
    };

    const url = `https://vop.baidu.com/server_api`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error(`百度ASR请求失败: ${response.status}`);
    }

    const data = await response.json();

    if (data.err_no !== 0) {
      throw new Error(`百度ASR错误: ${data.err_no} - ${data.err_msg}`);
    }

    // 解析结果
    const result = data.result;
    const text = result && result.length > 0 ? result[0] : '';

    return {
      text,
      confidence: 0.9, // 百度不返回置信度，默认0.9
      language: 'zh',
      rawResponse: data,
    };
  }

  /**
   * 获取音频格式配置
   */
  private getFormatConfig(format: string): {
    format: string;
    rate: number;
    devPid: number;
  } {
    switch (format.toLowerCase()) {
      case 'pcm':
        return { format: 'pcm', rate: 16000, devPid: 1537 }; // 普通话
      case 'wav':
        return { format: 'wav', rate: 16000, devPid: 1537 };
      case 'amr':
        return { format: 'amr', rate: 8000, devPid: 1537 };
      case 'm4a':
      case 'mp3':
        return { format: format.toLowerCase(), rate: 16000, devPid: 1537 };
      default:
        return { format: 'pcm', rate: 16000, devPid: 1537 };
    }
  }

  /**
   * 检查 Provider 是否可用
   */
  async isAvailable(): Promise<boolean> {
    try {
      if (!this.apiKey || !this.secretKey) {
        return false;
      }
      await this.getAccessToken();
      return true;
    } catch {
      return false;
    }
  }
}
