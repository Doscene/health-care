import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import {
  OcrProvider,
  OcrResult,
  OcrLine,
} from '../interfaces/ocr-provider.interface.js';

/**
 * 百度OCR Provider
 * 使用百度云通用文字识别API
 * 文档: https://ai.baidu.com/ai-doc/OCR/1k3h7y3db
 */
@Injectable()
export class BaiduOcrProvider implements OcrProvider {
  readonly name = 'baidu';
  private readonly logger = new Logger(BaiduOcrProvider.name);

  private accessToken: string | null = null;
  private tokenExpireAt = 0;

  private readonly apiKey: string;
  private readonly secretKey: string;

  constructor(private readonly configService: ConfigService) {
    this.apiKey = this.configService.get<string>('BAIDU_OCR_API_KEY', '');
    this.secretKey = this.configService.get<string>('BAIDU_OCR_SECRET_KEY', '');
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

    this.logger.log('刷新百度OCR Access Token...');

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

    this.logger.log('百度OCR Token刷新成功');
    return this.accessToken;
  }

  /**
   * 识别图片文字
   * 使用通用文字识别（高精度版）
   */
  async recognize(imageBase64: string): Promise<OcrResult> {
    const token = await this.getAccessToken();

    // 使用高精度版API
    const url = `https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=${token}`;

    const body = new URLSearchParams();
    body.append('image', imageBase64);
    body.append('language_type', 'CHN_ENG'); // 中英文混合
    body.append('detect_direction', 'true'); // 检测朝向

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: body.toString(),
    });

    if (!response.ok) {
      throw new Error(`百度OCR请求失败: ${response.status}`);
    }

    const data = await response.json();

    if (data.error_code) {
      throw new Error(`百度OCR错误: ${data.error_code} - ${data.error_msg}`);
    }

    // 解析结果
    const lines: OcrLine[] = (data.words_result || []).map((item: any) => ({
      text: item.words,
      confidence: item.probability?.average || 0.9,
      position: item.location
        ? {
            left: item.location.left,
            top: item.location.top,
            width: item.location.width,
            height: item.location.height,
          }
        : undefined,
    }));

    // 计算整体置信度
    const avgConfidence =
      lines.length > 0
        ? lines.reduce((sum, line) => sum + line.confidence, 0) / lines.length
        : 0;

    return {
      lines,
      confidence: avgConfidence,
      rawResponse: data,
    };
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
