import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ScheduleModule } from '@nestjs/schedule';
import { APP_GUARD } from '@nestjs/core';
import { AppController } from './app.controller.js';
import { AppService } from './app.service.js';
import { PrismaModule } from './prisma/prisma.module.js';
import { AuthModule } from './modules/auth/auth.module.js';
import { UserModule } from './modules/user/user.module.js';
import { FamilyModule } from './modules/family/family.module.js';
import { AppointmentModule } from './modules/appointment/appointment.module.js';
import { NotificationModule } from './modules/notification/notification.module.js';
import { HomeModule } from './modules/home/home.module.js';
import { MedicationModule } from './modules/medication/medication.module.js';
import { AlertModule } from './modules/alert/alert.module.js';
import { DietModule } from './modules/diet/diet.module.js';
import { HealthMetricsModule } from './modules/health-metrics/health-metrics.module.js';
import { OcrModule } from './modules/ocr/ocr.module.js';
import { VoiceModule } from './modules/voice/voice.module.js';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard.js';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    ScheduleModule.forRoot(),
    PrismaModule,
    AuthModule,
    UserModule,
    FamilyModule,
    AppointmentModule,
    NotificationModule,
    HomeModule,
    MedicationModule,
    AlertModule,
    DietModule,
    HealthMetricsModule,
    OcrModule,
    VoiceModule,
  ],
  controllers: [AppController],
  providers: [
    AppService,
    {
      provide: APP_GUARD,
      useClass: JwtAuthGuard,
    },
  ],
})
export class AppModule {}
