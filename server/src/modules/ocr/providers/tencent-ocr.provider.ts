import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHmac, createHash } from 'crypto';
import { OcrProvider, OcrResult, OcrLine } from '../interfaces/ocr-provider.interface.js';

/**
 * 腾讯云OCR Provider
 * 使用腾讯云通用印刷体识别API
 * 文档: https://cloud.tencent.com/document/product/866/33526
 */
@Injectable()
export class TencentOcrProvider implements OcrProvider {
  readonly name = 'tencent';
  private readonly logger = new Logger(TencentOcrProvider.name);

  private readonly secretId: string;
  private readonly secretKey: string;
  private readonly region: string;

  constructor(private readonly configService: ConfigService) {
    this.secretId = this.configService.get<string>('TENCENT_OCR_SECRET_ID', '');
    this.secretKey = this.configService.get<string>('TENCENT_OCR_SECRET_KEY', '');
    this.region = this.configService.get<string>('TENCENT_OCR_REGION', 'ap-guangzhou');
  }

  /**
   * 生成腾讯云API签名
   * 使用 TC3-HMAC-SHA256 签名方法
   */
  private generateSignature(payload: string, timestamp: number): {
    authorization: string;
    timestamp: number;
  } {
    const date = new Date(timestamp * 1000).toISOString().split('T')[0];
    const service = 'ocr';
    const action = 'GeneralBasicOCR';
    const version = '2018-11-19';

    // 1. 拼接规范请求串
    const httpRequestMethod = 'POST';
    const canonicalUri = '/';
    const canonicalQueryString = '';
    const canonicalHeaders = `content-type:application/json\nhost:ocr.tencentcloudapi.com\nx-tc-action:${action.toLowerCase()}\n`;
    const signedHeaders = 'content-type;host;x-tc-action';
    const hashedRequestPayload = createHash('sha256').update(payload).digest('hex');
    const canonicalRequest = `${httpRequestMethod}\n${canonicalUri}\n${canonicalQueryString}\n${canonicalHeaders}\n${signedHeaders}\n${hashedRequestPayload}`;

    // 2. 拼接待签名字符串
    const algorithm = 'TC3-HMAC-SHA256';
    const credentialScope = `${date}/${service}/tc3_request`;
    const hashedCanonicalRequest = createHash('sha256').update(canonicalRequest).digest('hex');
    const stringToSign = `${algorithm}\n${timestamp}\n${credentialScope}\n${hashedCanonicalRequest}`;

    // 3. 计算签名
    const secretDate = createHmac('sha256', `TC3${this.secretKey}`).update(date).digest();
    const secretService = createHmac('sha256', secretDate).update(service).digest();
    const secretSigning = createHmac('sha256', secretService).update('tc3_request').digest();
    const signature = createHmac('sha256', secretSigning).update(stringToSign).digest('hex');

    // 4. 拼接 Authorization
    const authorization = `${algorithm} Credential=${this.secretId}/${credentialScope}, SignedHeaders=${signedHeaders}, Signature=${signature}`;

    return { authorization, timestamp };
  }

  /**
   * 识别图片文字
   * 使用通用印刷体识别
   */
  async recognize(imageBase64: string): Promise<OcrResult> {
    const timestamp = Math.floor(Date.now() / 1000);
    const action = 'GeneralBasicOCR';
    const version = '2018-11-19';

    // 请求体
    const payload = JSON.stringify({
      ImageBase64: imageBase64,
      LanguageType: 'zh', // 中文
    });

    const { authorization } = this.generateSignature(payload, timestamp);

    const url = `https://ocr.tencentcloudapi.com`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Host': 'ocr.tencentcloudapi.com',
        'X-TC-Action': action,
        'X-TC-Version': version,
        'X-TC-Timestamp': timestamp.toString(),
        'X-TC-Region': this.region,
        'Authorization': authorization,
      },
      body: payload,
    });

    if (!response.ok) {
      throw new Error(`腾讯OCR请求失败: ${response.status}`);
    }

    const data = await response.json();

    if (data.Response?.Error) {
      throw new Error(`腾讯OCR错误: ${data.Response.Error.Code} - ${data.Response.Error.Message}`);
    }

    // 解析结果
    const textDetections = data.Response?.TextDetections || [];
    const lines: OcrLine[] = textDetections.map((item: any) => ({
      text: item.DetectedText,
      confidence: item.Confidence / 100, // 腾讯返回的是0-100，转换为0-1
      position: item.ItemPolygon
        ? {
            left: item.ItemPolygon.X,
            top: item.ItemPolygon.Y,
            width: item.ItemPolygon.Width,
            height: item.ItemPolygon.Height,
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
      rawResponse: data.Response,
    };
  }

  /**
   * 检查 Provider 是否可用
   */
  async isAvailable(): Promise<boolean> {
    try {
      if (!this.secretId || !this.secretKey) {
        return false;
      }
      // 尝试发送一个空请求测试连接
      const timestamp = Math.floor(Date.now() / 1000);
      const payload = JSON.stringify({});
      this.generateSignature(payload, timestamp);
      return true;
    } catch {
      return false;
    }
  }
}
