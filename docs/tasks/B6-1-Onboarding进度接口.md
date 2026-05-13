# B6-1 Onboarding进度接口

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | B6-1 |
| 所属阶段 | Phase 6 |
| 负责人 | 后端 |
| 预估工期 | 0.5天 |
| 关联PRD | 12第3章; 12第11章 |
| 前置依赖 | B1-1 |

## 任务描述

实现GET/PUT /api/onboarding/:userId。OnboardingProgress: profile/family/medication/firstRecord/goal各步骤boolean。首次登录自动创建(全部false)。完成每步后前端更新对应字段。

## 输入

12_PRD 3, 11

## 产出物

Onboarding接口就绪

## 验收标准

新增用户自动创建进度记录; 步骤更新保存

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
