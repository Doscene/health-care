# P0-5 Retrofit网络层封装

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | P0-5 |
| 所属阶段 | Phase 0 |
| 负责人 | Android |
| 预估工期 | 0.5天 |
| 关联PRD | 技术架构设计.md 2.2 |
| 前置依赖 | P0-1 |

## 任务描述

封装Retrofit + OkHttp网络层：配置baseUrl、GsonConverterFactory、LoggingInterceptor。实现AuthInterceptor自动添加JWT Authorization头。创建NetworkModule（Hilt）提供单例Retrofit实例。

## 输入

技术架构设计.md 2.2 核心依赖

## 产出物

API调用链路通

## 验收标准

调用测试接口返回200

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
