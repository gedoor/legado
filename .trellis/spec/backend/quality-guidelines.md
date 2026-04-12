# Quality Guidelines

> 本项目“后端”（Android App + 内置 Web 服务）代码质量约定。

---

## Overview

本项目核心约束：

1. Android 侧代码以 Kotlin 为主，使用 `kotlin.code.style=official`（见 `gradle.properties`）。
2. Web 端依赖 App 内置的 HTTP/WebSocket API，改动需要关注跨层兼容性（返回结构/字段名/语义）。

---

## Forbidden Patterns

1. **破坏 API 合同**：修改 `ReturnData` 字段名、让成功时缺失 `data` 等（Web 端会拦截并报错）。
2. **吞异常不记录**：catch 后既不 `LogUtils/AppLog` 也不返回可理解错误。
3. **在主线程做重 IO/重查询**：尽管数据库 builder 允许主线程查询，但新增代码应避免卡顿。
4. **日志泄露敏感信息**：Cookie、账号、token、可识别隐私信息等。

---

## Required Patterns

1. Web HTTP API 返回 `ReturnData`（`isSuccess/errorMsg/data`），并保持与 Web 端一致。
2. 数据库 schema 变更必须伴随 `AppDatabase` 版本更新与迁移（AutoMigration 或 `DatabaseMigrations`）。
3. 关键错误路径至少记录一次可定位的日志（优先 `LogUtils`，必要时 `AppLog`）。

---

## Testing Requirements

1. 影响构建/发布的改动：至少能通过 CI 的构建流程（参考 `.github/workflows/test.yml`）。
2. Room schema 变更：建议补充/更新迁移验证（参考 `app/src/androidTest/java/io/legado/app/MigrationTest.kt`）。
3. 涉及 Web API：建议用 `modules/web` 进行手动联调（至少覆盖一次成功与失败场景）。

---

## Code Review Checklist

1. API 返回结构是否仍满足 `modules/web/src/api/index.ts` 的检查逻辑。
2. 数据库变更是否同步：entity/dao/version/migration（以及是否会影响旧数据升级）。
3. 是否引入明显的性能回退（serve 路径阻塞、序列化大对象等）。
4. 日志是否足够定位问题且不包含敏感信息。
