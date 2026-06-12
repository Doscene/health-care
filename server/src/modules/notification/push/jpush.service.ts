import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as https from 'https';
import * as http from 'http';

export interface PushOptions {
  registrationIds?: string[];
  alias?: string[];
  tags?: string[];
  notificationTitle?: string;
  notificationContent: string;
  extras?: Record<string, string>;
  platform?: ('android' | 'ios')[];
}

export interface PushResult {
  success: boolean;
  sendno?: number;
  msgId?: string;
  error?: string;
}

@Injectable()
export class JPushService {
  private readonly logger = new Logger(JPushService.name);
  private readonly appKey: string;
  private readonly masterSecret: string;
  private readonly apiHost = 'https://api.jpush.cn';

  constructor(private configService: ConfigService) {
    this.appKey = this.configService.get<string>('JPUSH_APP_KEY') || '';
    this.masterSecret =
      this.configService.get<string>('JPUSH_MASTER_SECRET') || '';
  }

  async push(options: PushOptions): Promise<PushResult> {
    if (!this.appKey || !this.masterSecret) {
      this.logger.warn('JPush credentials not configured');
      return { success: false, error: 'JPush credentials not configured' };
    }

    const platform = options.platform || ['android'];
    const notification: Record<string, unknown> = {
      title: options.notificationTitle || '健康管理',
      content: options.notificationContent,
    };

    if (options.extras) {
      notification.extras = options.extras;
    }

    const body: Record<string, unknown> = {
      platform: platform,
      notification,
    };

    if (options.registrationIds?.length) {
      body.audience = { registration_id: options.registrationIds };
    } else if (options.alias?.length) {
      body.audience = { alias: options.alias };
    } else if (options.tags?.length) {
      body.audience = { tag: options.tags };
    } else {
      return { success: false, error: 'No audience specified' };
    }

    try {
      const result = await this.request('/v3/push', 'POST', body);
      return { success: true, ...result };
    } catch (error) {
      this.logger.error('Push failed', error);
      return { success: false, error: String(error) };
    }
  }

  async pushToUser(
    userId: string,
    options: Omit<PushOptions, 'alias'>,
  ): Promise<PushResult> {
    return this.push({ ...options, alias: [userId] });
  }

  private request(
    path: string,
    method: string,
    body: unknown,
  ): Promise<Record<string, unknown>> {
    return new Promise((resolve, reject) => {
      const auth = Buffer.from(`${this.appKey}:${this.masterSecret}`).toString(
        'base64',
      );
      const url = new URL(path, this.apiHost);
      const isHttps = url.protocol === 'https:';
      const transport = isHttps ? https : http;

      const options = {
        hostname: url.hostname,
        port: url.port || (isHttps ? 443 : 80),
        path: url.pathname,
        method,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Basic ${auth}`,
        },
      };

      const req = transport.request(options, (res) => {
        let data = '';
        res.on('data', (chunk: Buffer) => (data += chunk.toString()));
        res.on('end', () => {
          try {
            const parsed: Record<string, unknown> = JSON.parse(data) as Record<
              string,
              unknown
            >;
            if (
              res.statusCode &&
              res.statusCode >= 200 &&
              res.statusCode < 300
            ) {
              resolve(parsed);
            } else {
              const errorObj = parsed['error'] as
                | Record<string, unknown>
                | undefined;
              const message = (errorObj?.['message'] as string) || data;
              reject(new Error(message));
            }
          } catch {
            reject(new Error(`Invalid response: ${data}`));
          }
        });
      });

      req.setTimeout(10000, () => {
        req.destroy(new Error('Request timeout'));
        reject(new Error('JPush API request timeout'));
      });

      req.on('error', reject);
      req.write(JSON.stringify(body));
      req.end();
    });
  }
}
