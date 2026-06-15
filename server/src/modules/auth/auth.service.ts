import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import type { JwtSignOptions } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { PrismaService } from '../../prisma/prisma.service.js';
import { TokenResponseDto } from './dto/auth.dto.js';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  private generateCode(): string {
    return Math.floor(100000 + Math.random() * 900000).toString();
  }

  async sendCode(phone: string): Promise<{ message: string }> {
    const code = this.generateCode();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

    await this.prisma.verificationCode.upsert({
      where: { phone },
      update: { code, expiresAt, attempts: 0 },
      create: { phone, code, expiresAt },
    });

    // 仅开发环境输出验证码到日志
    if (this.configService.get('NODE_ENV') !== 'production') {
      this.logger.debug(`[SMS] Phone: ${phone}, Code: ${code}`);
    }

    return { message: '验证码已发送' };
  }

  async login(phone: string, code: string): Promise<TokenResponseDto> {
    const record = await this.prisma.verificationCode.findUnique({
      where: { phone },
    });

    if (!record) {
      throw new UnauthorizedException('请先获取验证码');
    }

    if (record.expiresAt < new Date()) {
      throw new UnauthorizedException('验证码已过期，请重新获取');
    }

    if (record.attempts >= 5) {
      throw new UnauthorizedException('验证码尝试次数过多，请重新获取');
    }

    if (record.code !== code) {
      await this.prisma.verificationCode.update({
        where: { phone },
        data: { attempts: { increment: 1 } },
      });
      throw new UnauthorizedException('验证码错误');
    }

    let user = await this.prisma.user.findUnique({ where: { phone } });

    if (!user) {
      user = await this.prisma.user.create({
        data: {
          id: uuidv4(),
          phone,
          name: `用户${phone.slice(-4)}`,
          selfRole: 'patient',
          diseases: [],
        },
      });
    }

    await this.prisma.verificationCode.delete({ where: { phone } });

    return this.generateTokens(user.id, user.phone);
  }

  generateTokens(userId: string, phone: string): TokenResponseDto {
    const payload = { sub: userId, phone };

    const accessExpiresIn: JwtSignOptions['expiresIn'] = this.configService.get(
      'JWT_ACCESS_EXPIRES',
      '7d',
    );
    const refreshExpiresIn: JwtSignOptions['expiresIn'] =
      this.configService.get('JWT_REFRESH_EXPIRES', '30d');

    const accessToken = this.jwtService.sign(payload, {
      expiresIn: accessExpiresIn,
    });

    const refreshToken = this.jwtService.sign(payload, {
      expiresIn: refreshExpiresIn,
    });

    return { accessToken, refreshToken };
  }

  async refreshToken(refreshToken: string): Promise<TokenResponseDto> {
    try {
      const payload = this.jwtService.verify<{ sub: string; phone: string }>(
        refreshToken,
      );
      const user = await this.prisma.user.findUnique({
        where: { id: payload.sub },
      });
      if (!user) {
        throw new UnauthorizedException('用户不存在');
      }
      return this.generateTokens(user.id, user.phone);
    } catch {
      throw new UnauthorizedException('Refresh Token 无效或已过期');
    }
  }
}
