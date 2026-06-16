import { Module } from '@nestjs/common';
import { OcrController } from './ocr.controller.js';
import { OcrService } from './ocr.service.js';
import { BaiduOcrProvider } from './providers/baidu-ocr.provider.js';
import { TencentOcrProvider } from './providers/tencent-ocr.provider.js';

@Module({
  controllers: [OcrController],
  providers: [OcrService, BaiduOcrProvider, TencentOcrProvider],
  exports: [OcrService],
})
export class OcrModule {}
