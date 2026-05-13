# P0-8 CICD配置

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | P0-8 |
| 所属阶段 | Phase 0 |
| 负责人 | 后端 |
| 预估工期 | 0.5天 |
| 关联PRD | 无 |
| 前置依赖 | P0-1, P0-2 |

## 任务描述

配置GitHub Actions：Android端执行gradle build编译检查；后端执行npm run lint + npm run build + prisma generate。提交到develop分支自动触发构建。

## 输入

无

## 产出物

CI构建通过

## 验收标准

git push → CI自动构建通过

## 技术要点

见开发计划文档第13.1节。
