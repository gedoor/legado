# Backend Development Guidelines

> 本项目“后端”（Android App + 内置 Web 服务）开发指南索引。

---

## Overview

本仓库没有独立的后端服务。

这里的 backend 主要指：

1. Android App 侧业务代码（Kotlin 为主）
2. Room/SQLite 本地数据库
3. 内置 HTTP/WebSocket 服务（为 `modules/web` 提供 API）

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 包/目录布局与模块划分 | Filled |
| [Database Guidelines](./database-guidelines.md) | Room、迁移、DAO/Entity 约定 | Filled |
| [Error Handling](./error-handling.md) | 业务错误与 API 错误返回模式 | Filled |
| [Quality Guidelines](./quality-guidelines.md) | 质量约束、测试、评审清单 | Filled |
| [Logging Guidelines](./logging-guidelines.md) | 日志工具、级别与敏感信息约束 | Filled |

---

## Pre-Development Checklist

开始改动 Android App / 内置 Web API 前，至少先读：

1. `./directory-structure.md`
2. `./error-handling.md`
3. `./logging-guidelines.md`
4. `./database-guidelines.md`（若涉及 data 层）

---

**Language**: 本仓库已有大量中文文档与注释，本目录文档以中文为主（必要时夹带英文术语）。
