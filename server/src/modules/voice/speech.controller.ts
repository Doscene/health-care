import {
  Controller,
  Post,
  Get,
  Body,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { SpeechService } from './speech.service.js';
import { VoiceParserService } from './voice-parser.service.js';
import {
  CurrentUser,
  type UserPayload,
} from '../../common/decorators/current-user.decorator.js';

@ApiTags('语音识别')
@ApiBearerAuth()
@Controller('speech')
export class SpeechController {
  constructor(
    private readonly speechService: SpeechService,
    private readonly voiceParserService: VoiceParserService,
  ) {}

  @Post('recognize')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '语音识别' })
  async recognize(
    @CurrentUser() user: UserPayload,
    @Body() body: { audioBase64: string; format?: string },
  ) {
    const result = await this.speechService.recognize(
      body.audioBase64,
      body.format,
    );

    return {
      code: 0,
      data: result,
      message: '识别成功',
    };
  }

  @Post('parse')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '解析语音文本为结构化数据' })
  async parseText(
    @CurrentUser() user: UserPayload,
    @Body() body: { text: string },
  ) {
    const result = this.voiceParserService.parse(body.text);

    return {
      code: 0,
      data: result,
      message: result.parsed ? '解析成功' : '解析失败，请手动修正',
    };
  }

  @Post('recognize-and-parse')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: '语音识别并解析为结构化数据' })
  async recognizeAndParse(
    @CurrentUser() user: UserPayload,
    @Body() body: { audioBase64: string; format?: string },
  ) {
    // 1. 语音识别
    const recognizeResult = await this.speechService.recognize(
      body.audioBase64,
      body.format,
    );

    // 2. 文本解析
    const parseResult = this.voiceParserService.parse(recognizeResult.text);

    return {
      code: 0,
      data: {
        recognize: recognizeResult,
        parse: parseResult,
      },
      message: parseResult.parsed
        ? '识别并解析成功'
        : '识别成功，但解析失败，请手动修正',
    };
  }

  @Get('providers')
  @ApiOperation({ summary: '获取可用的语音识别厂商' })
  async getProviders(@CurrentUser() user: UserPayload) {
    const providers = await this.speechService.getAvailableProviders();

    return {
      code: 0,
      data: providers,
      message: 'ok',
    };
  }
}
