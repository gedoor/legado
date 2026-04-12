# Hook Guidelines

> 本项目的 hooks（Vue composables）约定。

---

## Overview

本项目的“hook”指 Vue 3 的 composable（通常命名为 `useXxx`），位于：`modules/web/src/hooks/`。

---

## Custom Hook Patterns

1. 命名：`useXxx`。
2. 入参可接受 `MaybeRef`（便于传入 `ref` 或普通值）。
3. 返回值通常为响应式状态 + 操作方法（对象形式）。
4. 清理逻辑写在 `onUnmounted`，避免组件卸载后遗留状态或副作用。

示例：`modules/web/src/hooks/loading.ts`（封装 `ElLoading.service`，并通过 `watch` 驱动显示/关闭）。

---

## Data Fetching

数据请求的统一约定：

1. axios 实例：`modules/web/src/api/axios.ts`（baseURL 来自 `VITE_API` / localStorage / `location.origin`）。
2. API 封装：`modules/web/src/api/api.ts`（HTTP + WebSocket）。
3. 全局响应校验/错误提示：`modules/web/src/api/index.ts`（response interceptor 校验 `isSuccess/errorMsg`）。

组件通常直接调用 `API.xxx()`（来自 `@api` alias），而不是自行创建 axios。

---

## Naming Conventions

1. composable 文件名通常与函数同名或语义一致：`loading.ts` -> `useLoading`。
2. 返回对象字段名尽量稳定，便于在多个组件复用（如 `showLoading/closeLoading/loadingWrapper`）。

---

## Common Mistakes

1. watch/事件监听没有在卸载时清理。
2. 对 `ref`/`MaybeRef` 忘记 `unref`，导致传参类型不一致。
3. 在 composable 内直接使用全局 UI 提示（如 `ElMessage`）导致复用困难（除非这是明确的 UI hook）。
