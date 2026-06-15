# Phase 0 基础搭建完成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 Phase 0 剩余 7 个任务，打通开发-构建-部署全链路

**Architecture:** 后端：Prisma Schema → MySQL → Redis/MinIO Docker → JWT 认证 → CI/CD；Android：Retrofit 网络层 → 极光推送

**Tech Stack:** NestJS 11.x, Prisma 7.x, MySQL 8.0, Redis 7, MinIO, Retrofit 2.9, JPush 5.1.0, GitHub Actions

---

### Task 1: MySQL 表结构创建 (P0-3)

**Covers:** P0-3

**Files:**
- Modify: `server/prisma/schema.prisma` (已有完整 Schema)
- Create: `server/.env` (DATABASE_URL 配置)

- [ ] **Step 1: 确认 Prisma Schema 完整性**

检查 `server/prisma/schema.prisma` 包含所有核心表：User, Family, FamilyMember, Medication, MedicationRecord, BloodPressureRecord, BloodSugarRecord, RiskAlert, EmergencyContact, Appointment, FamilyGoal, Recipe, DietRecord, KnowledgeArticle, CommunityGroup, UserAchievement

- [ ] **Step 2: 配置 .env 文件**

```bash
# server/.env
DATABASE_URL="mysql://root:password@localhost:3306/healthcare"
```

- [ ] **Step 3: 创建数据库并执行迁移**

```bash
cd server
npx prisma generate
npx prisma db push
```

- [ ] **Step 4: 验证数据库表**

```bash
npx prisma studio
```

确认所有表已创建

- [ ] **Step 5: Commit**

```bash
git add server/prisma/schema.prisma server/.env
git commit -m "feat: complete Prisma schema and initialize MySQL tables"
```

---

### Task 2: Redis + MinIO Docker-Compose 配置 (P0-4)

**Covers:** P0-4

**Files:**
- Create: `docker-compose.yml`
- Create: `server/.env` (添加 REDIS_URL, MINIO 配置)

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
# docker-compose.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: healthcare-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: healthcare
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: healthcare-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:latest
    container_name: healthcare-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql_data:
  redis_data:
  minio_data:
```

- [ ] **Step 2: 更新 server/.env**

```bash
# 追加到 server/.env
REDIS_URL="redis://localhost:6379"
MINIO_ENDPOINT="localhost"
MINIO_PORT=9000
MINIO_ACCESS_KEY="minioadmin"
MINIO_SECRET_KEY="minioadmin"
```

- [ ] **Step 3: 启动服务并验证**

```bash
docker-compose up -d
docker ps
```

确认 3 个服务均 healthy

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml server/.env
git commit -m "feat: add Docker Compose for MySQL, Redis, MinIO"
```

---

### Task 3: Retrofit 网络层封装 (P0-5)

**Covers:** P0-5

**Files:**
- Create: `android/app/src/main/java/com/healthcare/family/data/remote/interceptor/AuthInterceptor.kt`
- Create: `android/app/src/main/java/com/healthcare/family/data/remote/api/HealthCareApi.kt`
- Modify: `android/app/src/main/java/com/healthcare/family/di/NetworkModule.kt`
- Create: `android/app/src/main/java/com/healthcare/family/data/remote/dto/ApiResponse.kt`

- [ ] **Step 1: 创建 AuthInterceptor**

```kotlin
// android/app/src/main/java/com/healthcare/family/data/remote/interceptor/AuthInterceptor.kt
package com.healthcare.family.data.remote.interceptor

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = getToken()
        
        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        return chain.proceed(authenticatedRequest)
    }
    
    private fun getToken(): String? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("access_token", null)
    }
}
```

- [ ] **Step 2: 创建 ApiResponse DTO**

```kotlin
// android/app/src/main/java/com/healthcare/family/data/remote/dto/ApiResponse.kt
package com.healthcare.family.data.remote.dto

data class ApiResponse<T>(
    val code: Int,
    val data: T,
    val message: String
)
```

- [ ] **Step 3: 创建 HealthCareApi 接口**

```kotlin
// android/app/src/main/java/com/healthcare/family/data/remote/api/HealthCareApi.kt
package com.healthcare.family.data.remote.api

import com.healthcare.family.data.remote.dto.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface HealthCareApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
    
    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): ApiResponse<Unit>
}

data class LoginRequest(
    val phone: String,
    val code: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class SendCodeRequest(
    val phone: String
)
```

- [ ] **Step 4: 更新 NetworkModule**

```kotlin
// android/app/src/main/java/com/healthcare/family/di/NetworkModule.kt
package com.healthcare.family.di

import com.healthcare.family.data.remote.api.HealthCareApi
import com.healthcare.family.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "http://10.0.2.2:3000/api/"
    
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHealthCareApi(retrofit: Retrofit): HealthCareApi {
        return retrofit.create(HealthCareApi::class.java)
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd android
./gradlew assembleDebug
```

- [ ] **Step 6: Commit**

```bash
git add android/app/src/main/java/com/healthcare/family/data/
git add android/app/src/main/java/com/healthcare/family/di/NetworkModule.kt
git commit -m "feat: implement Retrofit network layer with AuthInterceptor"
```

---

### Task 4: JWT 认证模块 (P0-6)

**Covers:** P0-6

**Files:**
- Create: `server/src/modules/auth/auth.module.ts` (更新)
- Create: `server/src/modules/auth/auth.controller.ts`
- Create: `server/src/modules/auth/auth.service.ts`
- Create: `server/src/modules/auth/strategies/jwt.strategy.ts`
- Create: `server/src/modules/auth/dto/auth.dto.ts`
- Create: `server/src/common/guards/jwt-auth.guard.ts`
- Create: `server/src/common/decorators/current-user.decorator.ts`
- Modify: `server/src/app.module.ts`

- [ ] **Step 1: 创建 PrismaService**

```typescript
// server/src/prisma/prisma.service.ts
import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { PrismaClient } from '@prisma/client';

@Injectable()
export class PrismaService extends PrismaClient implements OnModuleInit, OnModuleDestroy {
    async onModuleInit() {
        await this.$connect();
    }

    async onModuleDestroy() {
        await this.$disconnect();
    }
}
```

- [ ] **Step 2: 创建 PrismaModule**

```typescript
// server/src/prisma/prisma.module.ts
import { Global, Module } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Global()
@Module({
    providers: [PrismaService],
    exports: [PrismaService],
})
export class PrismaModule {}
```

- [ ] **Step 3: 创建 Auth DTO**

```typescript
// server/src/modules/auth/dto/auth.dto.ts
import { IsString, IsPhoneNumber } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class SendCodeDto {
    @ApiProperty({ example: '13800138000' })
    @IsPhoneNumber('CN')
    phone: string;
}

export class LoginDto {
    @ApiProperty({ example: '13800138000' })
    @IsPhoneNumber('CN')
    phone: string;

    @ApiProperty({ example: '123456' })
    @IsString()
    code: string;
}

export class AuthResponseDto {
    @ApiProperty()
    accessToken: string;

    @ApiProperty()
    refreshToken: string;
}
```

- [ ] **Step 4: 创建 JWT Strategy**

```typescript
// server/src/modules/auth/strategies/jwt.strategy.ts
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
    constructor() {
        super({
            jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
            ignoreExpiration: false,
            secretOrKey: process.env.JWT_SECRET || 'healthcare-jwt-secret-2024',
        });
    }

    async validate(payload: any) {
        if (!payload.sub) {
            throw new UnauthorizedException();
        }
        return { id: payload.sub, phone: payload.phone };
    }
}
```

- [ ] **Step 5: 创建 CurrentUser Decorator**

```typescript
// server/src/common/decorators/current-user.decorator.ts
import { createParamDecorator, ExecutionContext } from '@nestjs/common';

export const CurrentUser = createParamDecorator(
    (data: unknown, ctx: ExecutionContext) => {
        const request = ctx.switchToHttp().getRequest();
        return request.user;
    },
);
```

- [ ] **Step 6: 创建 JwtAuthGuard**

```typescript
// server/src/common/guards/jwt-auth.guard.ts
import { Injectable, ExecutionContext } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { Reflector } from '@nestjs/core';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
    constructor(private reflector: Reflector) {
        super();
    }

    canActivate(context: ExecutionContext) {
        const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
            context.getHandler(),
            context.getClass(),
        ]);
        if (isPublic) {
            return true;
        }
        return super.canActivate(context);
    }
}
```

- [ ] **Step 7: 创建 Public Decorator**

```typescript
// server/src/common/decorators/public.decorator.ts
import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
```

- [ ] **Step 8: 创建 AuthService**

```typescript
// server/src/modules/auth/auth.service.ts
import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../../prisma/prisma.service';
import { v4 as uuidv4 } from 'uuid';

@Injectable()
export class AuthService {
    private verificationCodes = new Map<string, { code: string; expiresAt: number }>();

    constructor(
        private prisma: PrismaService,
        private jwtService: JwtService,
    ) {}

    async sendCode(phone: string): Promise<void> {
        const code = Math.random().toString().substring(2, 8);
        const expiresAt = Date.now() + 5 * 60 * 1000;
        
        this.verificationCodes.set(phone, { code, expiresAt });
        
        console.log(`[Dev] Verification code for ${phone}: ${code}`);
    }

    async login(phone: string, code: string) {
        const stored = this.verificationCodes.get(phone);
        
        if (!stored || stored.code !== code) {
            throw new UnauthorizedException('验证码错误');
        }
        
        if (Date.now() > stored.expiresAt) {
            throw new UnauthorizedException('验证码已过期');
        }
        
        this.verificationCodes.delete(phone);
        
        let user = await this.prisma.user.findUnique({ where: { phone } });
        
        if (!user) {
            user = await this.prisma.user.create({
                data: {
                    id: uuidv4(),
                    phone,
                    name: `用户${phone.slice(-4)}`,
                    selfRole: 'young_patient',
                    diseases: [],
                },
            });
        }
        
        const payload = { sub: user.id, phone: user.phone };
        
        return {
            accessToken: this.jwtService.sign(payload),
            refreshToken: this.jwtService.sign(payload, { expiresIn: '30d' }),
        };
    }
}
```

- [ ] **Step 9: 创建 AuthController**

```typescript
// server/src/modules/auth/auth.controller.ts
import { Controller, Post, Body, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { SendCodeDto, LoginDto, AuthResponseDto } from './dto/auth.dto';
import { Public } from '../../common/decorators/public.decorator';

@ApiTags('auth')
@Controller('auth')
export class AuthController {
    constructor(private readonly authService: AuthService) {}

    @Post('send-code')
    @Public()
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: '发送验证码' })
    async sendCode(@Body() dto: SendCodeDto) {
        await this.authService.sendCode(dto.phone);
        return { code: 0, data: null, message: '验证码已发送' };
    }

    @Post('login')
    @Public()
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: '登录/注册' })
    @ApiResponse({ type: AuthResponseDto })
    async login(@Body() dto: LoginDto) {
        const result = await this.authService.login(dto.phone, dto.code);
        return { code: 0, data: result, message: '登录成功' };
    }
}
```

- [ ] **Step 10: 更新 AuthModule**

```typescript
// server/src/modules/auth/auth.module.ts
import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { JwtStrategy } from './strategies/jwt.strategy';

@Module({
    imports: [
        PassportModule,
        JwtModule.register({
            secret: process.env.JWT_SECRET || 'healthcare-jwt-secret-2024',
            signOptions: { expiresIn: '7d' },
        }),
    ],
    controllers: [AuthController],
    providers: [AuthService, JwtStrategy],
    exports: [AuthService, JwtModule],
})
export class AuthModule {}
```

- [ ] **Step 11: 更新 AppModule**

```typescript
// server/src/app.module.ts
import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './modules/auth/auth.module';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';

@Module({
    imports: [
        PrismaModule,
        AuthModule,
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
```

- [ ] **Step 12: 编译验证**

```bash
cd server
npm run build
```

- [ ] **Step 13: Commit**

```bash
git add server/src/prisma/ server/src/modules/auth/ server/src/common/ server/src/app.module.ts
git commit -m "feat: implement JWT authentication with login/send-code endpoints"
```

---

### Task 5: 极光推送 SDK 集成 (P0-7)

**Covers:** P0-7

**Files:**
- Create: `android/app/src/main/java/com/healthcare/family/worker/PushWorker.kt`
- Modify: `android/app/src/main/java/com/healthcare/family/HealthCareApp.kt`
- Create: `server/src/modules/notification/notification.module.ts`
- Create: `server/src/modules/notification/push/jpush.service.ts`

- [ ] **Step 1: 更新 HealthCareApp 初始化 JPush**

```kotlin
// android/app/src/main/java/com/healthcare/family/HealthCareApp.kt
package com.healthcare.family

import android.app.Application
import cn.jiguang.api.JCoreInterface
import cn.jiguang.api.JLifecycleInterface
import cn.jpush.android.api.JPushInterface
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HealthCareApp : Application(), JLifecycleInterface {
    
    override fun onCreate() {
        super.onCreate()
        initJPush()
    }
    
    private fun initJPush() {
        JPushInterface.setDebugMode(true)
        JPushInterface.init(this)
    }
    
    override fun onLifecycleEvent(activity: android.app.Activity?, event: String?) {
        // Handle lifecycle events if needed
    }
}
```

- [ ] **Step 2: 创建 PushWorker**

```kotlin
// android/app/src/main/java/com/healthcare/family/worker/PushWorker.kt
package com.healthcare.family.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.jpush.android.api.JPushInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    override suspend fun doWork(): Result {
        val registrationId = JPushInterface.getRegistrationID(applicationContext)
        // Send registrationId to server
        return Result.success()
    }
}
```

- [ ] **Step 3: 创建 JPush Service (后端)**

```typescript
// server/src/modules/notification/push/jpush.service.ts
import { Injectable } from '@nestjs/common';
import * as https from 'https';

@Injectable()
export class JPushService {
    private readonly appKey = process.env.JPUSH_APP_KEY || '';
    private readonly masterSecret = process.env.JPUSH_MASTER_SECRET || '';

    async sendPush(registrationId: string, title: string, content: string) {
        const payload = {
            platform: 'all',
            audience: { registration_id: [registrationId] },
            notification: {
                android: { alert: content, title },
                ios: { alert: content, sound: 'default', badge: 1 },
            },
        };

        return this.sendRequest(payload);
    }

    private sendRequest(payload: any): Promise<any> {
        return new Promise((resolve, reject) => {
            const auth = Buffer.from(`${this.appKey}:${this.masterSecret}`).toString('base64');
            
            const options = {
                hostname: 'api.jpush.cn',
                path: '/v3/push',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Basic ${auth}`,
                },
            };

            const req = https.request(options, (res) => {
                let data = '';
                res.on('data', (chunk) => { data += chunk; });
                res.on('end', () => resolve(JSON.parse(data)));
            });

            req.on('error', reject);
            req.write(JSON.stringify(payload));
            req.end();
        });
    }
}
```

- [ ] **Step 4: 创建 NotificationModule**

```typescript
// server/src/modules/notification/notification.module.ts
import { Module } from '@nestjs/common';
import { JPushService } from './push/jpush.service';

@Module({
    providers: [JPushService],
    exports: [JPushService],
})
export class NotificationModule {}
```

- [ ] **Step 5: 更新 AppModule 导入 NotificationModule**

```typescript
// server/src/app.module.ts (追加 NotificationModule)
imports: [
    PrismaModule,
    AuthModule,
    NotificationModule,
],
```

- [ ] **Step 6: 编译验证**

```bash
cd android && ./gradlew assembleDebug
cd server && npm run build
```

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/java/com/healthcare/family/HealthCareApp.kt
git add android/app/src/main/java/com/healthcare/family/worker/
git add server/src/modules/notification/
git commit -m "feat: integrate JPush SDK for push notifications"
```

---

### Task 6: CI/CD 配置 (P0-8)

**Covers:** P0-8

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: 创建 GitHub Actions 配置**

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop]

jobs:
  server:
    name: Server Build
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: server/package-lock.json
      
      - name: Install dependencies
        working-directory: server
        run: npm ci
      
      - name: Lint
        working-directory: server
        run: npm run lint
      
      - name: Generate Prisma Client
        working-directory: server
        run: npx prisma generate
      
      - name: Build
        working-directory: server
        run: npm run build
      
      - name: Test
        working-directory: server
        run: npm run test

  android:
    name: Android Build
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Build Debug APK
        run: ./gradlew assembleDebug
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow for server and Android"
```

---

### Task 7: PRD 走查与测试用例大纲 (P0-9)

**Covers:** P0-9

**Files:**
- Create: `docs/testing/test-plan.md`

- [ ] **Step 1: 创建测试计划文档**

```markdown
# 测试计划 - 家庭慢病健康管理应用

## 一、测试范围

### Phase 0 测试
- [ ] 服务器启动与健康检查
- [ ] Prisma 数据库连接
- [ ] JWT 认证流程（登录/注册/验证码）
- [ ] Redis/MinIO 服务可用性

### Phase 1 测试（预告）
- [ ] 用户注册登录流程
- [ ] 家庭圈创建/加入
- [ ] 三角色首页展示

## 二、测试用例大纲

### 认证模块
| 用例ID | 描述 | 预期结果 |
|--------|------|----------|
| AUTH-001 | 发送验证码 | 返回成功，验证码发送 |
| AUTH-002 | 使用正确验证码登录 | 返回 accessToken 和 refreshToken |
| AUTH-003 | 使用错误验证码登录 | 返回 401 错误 |
| AUTH-004 | 使用过期验证码登录 | 返回 401 错误 |
| AUTH-005 | 首次登录自动注册 | 创建新用户记录 |

### 数据库模块
| 用例ID | 描述 | 预期结果 |
|--------|------|----------|
| DB-001 | 数据库连接 | 连接成功 |
| DB-002 | 所有表创建 | 16 个核心表存在 |
| DB-003 | 数据类型正确 | JSON 字段正常存储 |

### 基础设施
| 用例ID | 描述 | 预期结果 |
|--------|------|----------|
| INFRA-001 | MySQL 服务健康 | docker ps 显示 healthy |
| INFRA-002 | Redis 服务健康 | docker ps 显示 healthy |
| INFRA-003 | MinIO 服务健康 | docker ps 显示 healthy |
| INFRA-004 | CI 构建通过 | GitHub Actions 绿灯 |

## 三、测试环境

- 本地开发环境：Docker Compose
- CI 环境：GitHub Actions
- 测试工具：Jest (后端), JUnit (Android)

## 四、验收标准

- [ ] 所有 Phase 0 任务完成
- [ ] 服务器可正常启动
- [ ] 数据库表结构完整
- [ ] 认证接口可调通
- [ ] CI 构建通过
```

- [ ] **Step 2: Commit**

```bash
git add docs/testing/test-plan.md
git commit -m "docs: add test plan for Phase 0"
```

---

### Task 8: 完整验证

**Files:** 无（运行验证命令）

- [ ] **Step 1: 启动 Docker 服务**

```bash
docker-compose up -d
docker ps
```

预期：3 个服务均 healthy

- [ ] **Step 2: 验证数据库**

```bash
cd server
npx prisma db push
npx prisma studio
```

预期：所有表已创建

- [ ] **Step 3: 启动服务器**

```bash
cd server
npm run start:dev
```

预期：服务器在 3000 端口启动

- [ ] **Step 4: 测试认证接口**

```bash
# 发送验证码
curl -X POST http://localhost:3000/api/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000"}'

# 登录（使用控制台输出的验证码）
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"123456"}'
```

预期：返回 accessToken 和 refreshToken

- [ ] **Step 5: 编译 Android**

```bash
cd android
./gradlew assembleDebug
```

预期：编译成功

- [ ] **Step 6: 运行后端测试**

```bash
cd server
npm run test
```

预期：测试通过

- [ ] **Step 7: 运行 Lint 检查**

```bash
cd server
npm run lint
```

预期：无错误

---

## Self-Review

1. **Spec coverage:** P0-3 到 P0-9 全部覆盖
2. **Placeholder scan:** 无 TBD/TODO 占位符
3. **Type consistency:** API 接口、DTO 类型一致
4. **Prerequisites:** P0-3 依赖 P0-2 ✅，P0-6 依赖 P0-3 ✅，P0-7 依赖 P0-6 ✅

## Execution Handoff

Plan saved. This plan contains 8 tasks with clear dependencies. Recommended execution: **Inline** (tightly coupled, sequential tasks).
