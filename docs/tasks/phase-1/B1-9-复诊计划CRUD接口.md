# B1-9 复诊计划CRUD接口

## 基本信息

| 维度 | 内容 |
|------|------|
| 任务编号 | B1-9 |
| 所属阶段 | Phase 1 |
| 负责人 | 后端 |
| 预估工期 | 1天 |
| 关联PRD | 11第5章 |
| 前置依赖 | B1-1 |

## 任务描述

实现Appointment的CRUD接口：GET /api/appointments（按时间排序，含upcoming和history）、POST/PUT/DELETE。字段：department/hospital/date/notes/remindBefore。支持按userId查询。

## 输入

11_PRD 5

## 产出物

复诊计划接口就绪

## 验收标准

增删改查正常；列表按时间排序

## 技术要点

见关联PRD文档和技术架构设计中的对应章节。
