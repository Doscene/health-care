import { Module } from '@nestjs/common';
import { JPushService } from './push/jpush.service.js';

@Module({
  providers: [JPushService],
  exports: [JPushService],
})
export class NotificationModule {}
