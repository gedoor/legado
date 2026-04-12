# Type Safety

> 本项目 Web 前端的类型安全约定（TypeScript）。

---

## Overview

本项目使用 TypeScript，并通过 `vue-tsc` 做类型检查。

关键脚本：`modules/web/package.json` 中 `type-check: "vue-tsc --build --force"`。

---

## Type Organization

1. 与 App 数据结构对齐的共享类型集中在 `modules/web/src/*.d.ts`：
   - `modules/web/src/book.d.ts`
   - `modules/web/src/source.d.ts`
   - `modules/web/src/web.d.ts`
2. API 响应泛型：`LeagdoApiResponse<T>`（`modules/web/src/api/api.ts`）。

---

## Validation

本项目的运行时校验主要集中在两处：

1. axios 响应拦截器检查 API 返回形状（`isSuccess/errorMsg`，成功时要求有 `data`）：`modules/web/src/api/index.ts`
2. 表单输入校验使用工具函数（例如后端地址校验 `validatorHttpUrl`）：`modules/web/src/utils/utils`（调用点见 `BookShelf.vue`）

---

## Common Patterns

1. 使用泛型定义 API 响应：`ajax.get<LeagdoApiResponse<Book[]>>(...)`（见 `api.ts`）。
2. 使用 `Pick`/交叉类型组合业务结构：`BookProgress`（见 `book.d.ts`）。
3. `import type` 引入类型，避免引入运行时代码。

---

## Forbidden Patterns

1. 无理由使用 `any` 或大范围 `as unknown as ...`。
2. 随意增加 `@ts-expect-error` 来“压过”类型问题（已有少量历史用法，新增需给出明确原因）。
3. 改动 API 返回结构/字段名但不更新 `LeagdoApiResponse`、拦截器与 `*.d.ts`。
