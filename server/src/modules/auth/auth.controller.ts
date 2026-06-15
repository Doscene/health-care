import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { AuthService } from './auth.service.js';
import { SendCodeDto, LoginDto, TokenResponseDto } from './dto/auth.dto.js';
import { Public } from '../../common/decorators/public.decorator.js';

@ApiTags('认证')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('send-code')
  @Public()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '发送短信验证码' })
  @ApiResponse({ status: 200, description: '验证码已发送' })
  async sendCode(@Body() dto: SendCodeDto) {
    const result = await this.authService.sendCode(dto.phone);
    return {
      code: 0,
      data: result,
      message: 'ok',
    };
  }

  @Post('login')
  @Public()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '手机号验证码登录' })
  @ApiResponse({
    status: 200,
    description: '登录成功',
    type: TokenResponseDto,
  })
  async login(@Body() dto: LoginDto) {
    const result = await this.authService.login(dto.phone, dto.code);
    return {
      code: 0,
      data: result,
      message: 'ok',
    };
  }

  @Post('refresh')
  @Public()
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '刷新 Token' })
  @ApiResponse({
    status: 200,
    description: '刷新成功',
    type: TokenResponseDto,
  })
  async refresh(@Body('refreshToken') refreshToken: string) {
    const result = await this.authService.refreshToken(refreshToken);
    return {
      code: 0,
      data: result,
      message: 'ok',
    };
  }
}
