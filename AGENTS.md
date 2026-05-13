# AGENTS.md

## 项目概况

家庭慢病健康管理应用（高血压+糖尿病），纯 Android 原生 App + NestJS 后端。当前处于 **Phase 0**（第1周），目标：脚手架 + 全链路就绪。

## 目录与边界

```
android/    → Android App (Kotlin, Compose, Hilt, Gradle 8.x, minSdk 26, targetSdk 34)
server/     → NestJS 后端 (TypeScript, Prisma + MySQL, Swagger)
docs/       → 技术架构设计.md + 开发计划.md + tasks/*.md（158个任务文件）
prd/        → 12篇产品需求文档
prototype/  → 原型文件
```

Android 和 server 是两个**独立项目**，各自有独立的构建和依赖。

## 关键命令

### Server（工作目录：`server/`）

```bash
cd server && npm run start:dev    # 开发启动，端口 3000，Swagger: http://localhost:3000/api/docs
cd server && npm run build        # 编译到 dist/
cd server && npm run lint         # ESLint（flat config: eslint.config.mjs）
cd server && npm run format       # Prettier（singleQuote, trailingComma: all）
cd server && npm run test         # 单元测试（Jest）
cd server && npm run test:e2e     # E2E 测试
```

### Prisma（工作目录：`server/`）

```bash
cd server && npx prisma generate  # schema 修改后重新生成 client
cd server && npx prisma db push   # 同步 schema 到数据库（Phase 0 无 migrate）
cd server && npx prisma studio    # 数据浏览
```

### Android（工作目录：项目根目录）

```bash
./gradlew assembleDebug           # 编译 Debug APK
./gradlew test                    # 运行测试
```

Android 项目通过 Android Studio 打开 `android/` 目录开发。

## 开发环境依赖

- **Server**: Node.js + MySQL 8.0+（本地需有 `healthcare` 数据库）+ Redis + MinIO
- **Android**: JDK 17, Android SDK（compileSdk 34）
- Server `.env` 文件在 `server/.env`，含开发用凭据（JWT_SECRET 等已硬编码默认值）

## API 约定

- 统一响应格式：`{ code: number, data: T, message: string }`
- Swagger 文档路径：`/api/docs`
- Server 全局前缀：`/api`

## 项目注意事项

- **TypeScript 模块系统**：server 使用 `module: nodenext`，相对路径 imports 必须带 `.js` 扩展名（NestJS 标准）
- **ESLint 配置**：flat config 格式 `eslint.config.mjs`，不是 `.eslintrc`
- **modules 骨架**：12个模块目录已创建但**未在 AppModule 中导入**（注释写"Phase 1 will be imported"）
- **版本差异**：`package.json` 中 NestJS 实际是 11.x，Prisma 实际是 7.x（开发计划文档写 10.x / 5.x，以代码为准）
- **根 settings.gradle** 有拼写问题 `include ':helath'`，不要随意修复
- **没有 CI/CD**、**没有 Docker**（P0-4、P0-8 尚未完成）

## 任务管理

- 主跟踪表：`docs/开发计划.md`（含 Phase 0~6 全量任务 + 里程碑检查表）
- 单任务详情：`docs/tasks/P0-N-xxx.md`
- 任务编号：`P0-N` = Phase 0, `A1-N` = Android Phase 1, `B1-N` = 后端 Phase 1, `T1-N` = 测试
- P0-1（Android 初始化）和 P0-2（NestJS 初始化）**已完成**，当前待开始：P0-3~P0-9
