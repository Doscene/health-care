# B2-1 百度OCR Provider实现

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | B2-1 |
| 所属阶段 | Phase 2 |
| 负责人 | 后端 |
| 预估工期 | 1.5天 |
| 关联PRD | 架构设计第4章 |
| 前置依赖 | P0-6 |

## 任务描述

实现BaiduOcrProvider (implements OcrProvider接口)。集成百度OCR通用文字识别API。实现getAccessToken(OAuth2 client_credentials->缓存到Redis，过期前自动刷新)。recognize方法返回OcrResult。单元测试覆盖。

## 输入

架构设计 4.3

## 产出物

百度OCR Provider就绪

## 验收标准

拍药盒照片返回药品名+规格文字

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
