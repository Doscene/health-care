import { Module } from '@nestjs/common';
import { SpeechController } from './speech.controller.js';
import { SpeechService } from './speech.service.js';
import { VoiceParserService } from './voice-parser.service.js';
import { BaiduAsrProvider } from './providers/baidu-asr.provider.js';

@Module({
  controllers: [SpeechController],
  providers: [SpeechService, VoiceParserService, BaiduAsrProvider],
  exports: [SpeechService, VoiceParserService],
})
export class VoiceModule {}
