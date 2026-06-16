import {
  Controller,
  Post,
  Get,
  Body,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { OcrService } from './ocr.service.js';
import { CurrentUser, type UserPayload } from '../../common/decorators/current-user.decorator.js';

@ApiTags('OCR识别')
@ApiBearerAuth()
@Controller('ocr')
export class OcrController {
  constructor(private readonly ocrService: OcrService) {}

  @Post('recognize')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '图片文字识别' })
  async recognize(
    @CurrentUser() user: UserPayload,
    @Body() body: { imageBase64: string },
  ) {
    return {
      code: 0,
      data: await this.ocrService.recognize(body.imageBase64),
      message: '识别成功',
    };
  }

  @Get('stats')
  @ApiOperation({ summary: '获取OCR调用统计' })
  async getStats(@CurrentUser() user: UserPayload) {
    return {
      code: 0,
      data: this.ocrService.getStats(),
      message: 'ok',
    };
  }

  @Get('logs')
  @ApiOperation({ summary: '获取最近OCR调用日志' })
  async getLogs(
    @CurrentUser() user: UserPayload,
  ) {
    return {
      code: 0,
      data: this.ocrService.getRecentLogs(20),
      message: 'ok',
    };
  }
}
