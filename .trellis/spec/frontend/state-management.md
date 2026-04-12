# State Management

> 本项目 Web 前端的状态管理约定。

---

## Overview

本项目使用 **Pinia** 作为全局状态管理方案。

位置：`modules/web/src/store/`，入口：`modules/web/src/store/index.ts`。

---

## State Categories

1. **组件本地状态**：只服务于当前组件的 UI 交互，优先放在组件内（`ref/computed`）。
2. **全局状态（Pinia）**：跨组件共享、需要缓存、或需要集中管理的状态（例如书架、目录、连接状态）。
3. **“服务端状态”**：主要通过 `API` 拉取并存入 store 的内存缓存；当前没有使用专门的 server-state 库（如 Vue Query）。

---

## When to Use Global State

1. 被多个页面/组件使用（例如连接状态 `connectionStore`）。
2. 需要做内存级缓存以减少请求（例如 `bookStore` 的 `shelf/catalog`）。
3. 有集中更新逻辑，组件只负责展示（例如保存阅读进度、加载配置）。

示例：

1. `modules/web/src/store/bookStore.ts`
2. `modules/web/src/store/connectionStore.ts`

---

## Server State

当前模式偏“store 内缓存 + 条件拉取”：

1. `bookStore.loadBookShelf()` 在已有缓存时优先返回缓存，否则调用 `API.getBookShelf()` 拉取。
2. `bookStore.loadWebCatalog()` 按 `bookUrl` 与 `catalog` 状态决定是否复用缓存。

---

## Common Mistakes

1. 直接对 `toRaw(store.xxx)` 得到的对象做变更，导致响应式更新丢失。
2. 把只属于单个页面的临时 UI 状态放进全局 store，造成耦合。
3. 对 Web API 的成功/失败处理不一致（应遵循 `isSuccess/errorMsg` 合同）。
