# Directory Structure

> 本项目 Web 前端（Vue 3）代码组织方式。

---

## Overview

前端位于 `modules/web/`，技术栈为 **Vue 3 + TypeScript + Vite**，UI 组件为 **Element Plus**。

该前端通过 App 内置的 HTTP/WebSocket 服务交互（见 `app/src/main/java/io/legado/app/web/`）。

---

## Directory Layout

前端主要目录：`modules/web/src/`

```
modules/web/
├── src/
│   ├── api/         # axios + API 封装（HTTP/WebSocket）
│   ├── assets/      # 静态资源、全局样式、字体等
│   ├── components/  # 可复用组件
│   ├── hooks/       # composables（useXxx）
│   ├── router/      # vue-router
│   ├── store/       # Pinia stores
│   ├── utils/       # 工具方法
│   ├── views/       # 主要页面/视图
│   ├── pages/       # 多入口页面（bookshelf/source）
│   ├── *.d.ts       # 跨模块共享类型（与 App 数据结构对齐）
│   └── main.ts      # App 入口（按具体页面可能不同）
└── scripts/
    └── sync.js      # build 后同步产物到 app assets（CI 也会执行）
```

---

## Module Organization

1. **页面/路由**：优先在 `src/views/` + `src/router/` 中组织。
2. **通用组件**：放 `src/components/`。
3. **API 封装**：统一通过 `src/api/` 暴露（避免在组件里散落 axios 调用）。
4. **全局状态**：Pinia store 放在 `src/store/`，并由 `src/store/index.ts` 聚合导出。

---

## Naming Conventions

1. `.vue` 组件/页面文件名通常为 PascalCase：`BookShelf.vue`。
2. Pinia store 文件通常以 `*Store.ts` 命名：`bookStore.ts`。
3. composable 以 `useXxx` 命名：`useLoading`（`src/hooks/loading.ts`）。
4. 共享类型集中在 `src/*.d.ts`（如 `book.d.ts`, `source.d.ts`）。

---

## Examples

1. 页面示例：`modules/web/src/views/BookShelf.vue`
2. 组件示例：`modules/web/src/components/ToolBar.vue`
3. composable 示例：`modules/web/src/hooks/loading.ts`
4. API 封装：`modules/web/src/api/api.ts` + `modules/web/src/api/index.ts`
5. Pinia store：`modules/web/src/store/bookStore.ts`
