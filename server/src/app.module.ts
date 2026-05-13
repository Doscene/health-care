import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';

@Module({
  imports: [
    // Phase 1 modules will be imported here
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
