# Quality Guidelines

> 本项目 Web 前端（modules/web）的质量约定。

---

## Overview

前端质量体系以“类型检查 + lint/format + CI 构建” 为主。

---

## Forbidden Patterns

1. 绕过 `src/api/` 直接在组件中散落网络请求，导致错误处理不一致。
2. 提交未通过 `vue-tsc` 的类型错误。
3. 破坏与 App 侧 API 的合同（`isSuccess/errorMsg/data`）。

---

## Required Patterns

1. 构建前必须通过类型检查：`pnpm type-check`（`modules/web/package.json`）。
2. 代码风格遵循 ESLint/Prettier 配置：
   - ESLint：`modules/web/eslint.config.mjs`
   - Prettier：`modules/web/.prettierrc.json`
3. 生产构建使用 `pnpm build`（会自动同步产物到 `app/src/main/assets/web/vue/`）：`modules/web/scripts/sync.js`。

---

## Testing Requirements

当前仓库未看到成体系的前端单元测试目录（`tsconfig` 也排除了 `src/**/__tests__/*`）。

因此对前端改动的最低要求通常为：

1. `pnpm type-check`
2. `pnpm lint:fix`（或至少 `eslint` 不报错）
3. 手动验证关键路径（页面可用、与后端联调 OK、移动端样式不崩）

---

## Code Review Checklist

1. 是否依旧遵循 API 合同与拦截器行为（见 `modules/web/src/api/index.ts`）。
2. 是否引入明显性能问题（大列表渲染、过多 watchEffect 等）。
3. 是否在移动端仍可用（`BookShelf.vue` 有 media query，可作为参考）。
4. 是否保持一致的 import/自动导入风格（项目启用了 AutoImport/Components）。
