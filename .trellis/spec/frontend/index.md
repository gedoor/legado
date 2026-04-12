# Frontend Development Guidelines

> 本项目 Web 前端（modules/web，Vue 3）开发指南索引。

---

## Overview

前端位于 `modules/web/`，技术栈为 Vue 3 + TypeScript + Vite，状态管理 Pinia，UI 使用 Element Plus。

前端依赖 App 内置 HTTP/WebSocket API（返回结构为 `isSuccess/errorMsg/data`）。

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | 目录结构与分层 | Filled |
| [Component Guidelines](./component-guidelines.md) | Vue SFC/Element Plus 组件约定 | Filled |
| [Hook Guidelines](./hook-guidelines.md) | composables（useXxx）与数据请求模式 | Filled |
| [State Management](./state-management.md) | Pinia 使用与状态分层 | Filled |
| [Quality Guidelines](./quality-guidelines.md) | lint/type-check/CI 与评审清单 | Filled |
| [Type Safety](./type-safety.md) | TS 类型组织与运行时校验点 | Filled |

---

## Pre-Development Checklist

开始改动 `modules/web` 前，至少先读：

1. `./directory-structure.md`
2. `./hook-guidelines.md`（包含 API 合同与拦截器约束）
3. `./type-safety.md`
4. `./quality-guidelines.md`

---

**Language**: 本仓库已有大量中文文档与注释，本目录文档以中文为主（必要时夹带英文术语）。
