# P0-4 Redis+MinIO环境配置

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | P0-4 |
| 所属阶段 | Phase 0 |
| 负责人 | 后端 |
| 预估工期 | 0.5天 |
| 关联PRD | 技术架构设计.md 第六章 |
| 前置依赖 | 无 |

## 任务描述

编写docker-compose.yml，包含MySQL 8.0、Redis 7、MinIO三个服务。配置端口映射、数据卷持久化、健康检查。配置NestJS环境变量（DATABASE_URL/REDIS_URL/MINIO_ENDPOINT）。

## 输入

技术架构设计.md 6 部署架构

## 产出物

docker-compose up -d 一键启动本地环境

## 验收标准

docker ps显示3个服务均healthy

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
