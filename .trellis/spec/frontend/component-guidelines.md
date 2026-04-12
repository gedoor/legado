# Component Guidelines

> 本项目 Vue 组件的编写约定。

---

## Overview

项目以 Vue SFC 为主（`.vue`），常用组合：

1. `<script setup lang="ts">`
2. Element Plus 组件（`el-*`）
3. `scss` + `scoped` 样式

---

## Component Structure

推荐结构（与现有代码保持一致）：

1. `<template>`
2. `<script setup lang="ts">`：import（包含全局 css/font 引入）+ store + composables + 业务逻辑
3. `<style lang="scss" scoped>`：组件样式（必要时用 `:deep(...)` 处理第三方组件内部）

示例：

1. `modules/web/src/views/BookShelf.vue`
2. `modules/web/src/components/ToolBar.vue`

---

## Props Conventions

1. 需要 props 时，使用 `defineProps<{ ... }>()` 并保持 TypeScript 类型准确。
2. 跨页面/跨模块共享的结构类型优先放 `src/*.d.ts` 并通过 `import type` 引用（如 `Book`, `Source`）。

---

## Styling Patterns

1. 默认使用 `<style lang="scss" scoped>`。
2. 全局样式/字体等以 `import` 的方式在页面入口或页面组件中引入（示例：`BookShelf.vue` 引入 `assets/bookshelf.css` 与字体）。
3. 对 Element Plus 内部 DOM 的样式覆盖使用 `:deep(...)`（见 `BookShelf.vue`）。

---

## Accessibility

1. 图片应提供 `alt` 文本（即使为空也明确）。
2. 交互组件使用 Element Plus 时，优先利用其默认可访问性行为；自定义交互需保证键盘可用性。

---

## Common Mistakes

1. 忘记项目启用了自动导入（AutoImport/Components），导致重复/混乱的 import 风格。
2. 在组件中直接散落 axios 请求，绕过 `src/api/` 封装（不利于统一错误处理）。
3. 一个 `.vue` 文件过大、包含过多职责，难以复用和维护。
