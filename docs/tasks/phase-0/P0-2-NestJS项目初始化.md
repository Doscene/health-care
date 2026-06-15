# P0-2 NestJS项目初始化

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | P0-2 |
| 所属阶段 | Phase 0 |
| 负责人 | 后端 |
| 预估工期 | 1天 |
| 关联PRD | 技术架构设计.md |
| 前置依赖 | 无 |

## 任务描述

创建NestJS 10.x项目骨架：安装@nestjs/core、@nestjs/swagger。按架构设计创建Module目录骨架（auth/user/family/medication/record/report/diet/alert/community/achievement/notification/ocr）。配置Prisma 5.x + MySQL provider。

## 输入

技术架构设计.md 第三章

## 产出物

Swagger可访问，模块骨架就绪

## 验收标准

npm run start成功，http://localhost:3000/api/docs 可访问Swagger

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
