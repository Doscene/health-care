import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
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
import { RecordModule } from './modules/record/record.module.js';
import { MedicationModule } from './modules/medication/medication.module.js';
import { AlertModule } from './modules/alert/alert.module.js';
import { DietModule } from './modules/diet/diet.module.js';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard.js';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    PrismaModule,
    AuthModule,
    UserModule,
    FamilyModule,
    AppointmentModule,
    NotificationModule,
    HomeModule,
    RecordModule,
    MedicationModule,
    AlertModule,
    DietModule,
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
