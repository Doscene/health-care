# P0-6 JWT认证模块

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | P0-6 |
| 所属阶段 | Phase 0 |
| 负责人 | 后端 |
| 预估工期 | 1天 |
| 关联PRD | 01_用户认证与家庭圈管理需求.md |
| 前置依赖 | P0-2; P0-3 |

## 任务描述

实现JWT认证：passport-jwt策略、/api/auth/login接口（手机号+验证码，首次自动注册）、/api/auth/send-code接口（短信验证码）、Access Token(7天)+Refresh Token(30天)双Token机制。

## 输入

01_PRD.md 3.1-3.3 注册/登录

## 产出物

Postman可调通登录接口并返回JWT

## 验收标准

/api/auth/login 返回 {accessToken, refreshToken}

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
