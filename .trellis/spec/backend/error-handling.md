# Error Handling

> 本项目（Android App + 内置 Web 服务）的错误处理约定。

---

## Overview

错误处理主要分两类：

1. App 内部业务异常：通过自定义异常类型、全局 CrashHandler、日志记录来定位问题。
2. 内置 HTTP/WebSocket API：以 `ReturnData` 作为主要的“业务错误返回格式”。

---

## Error Types

1. 自定义异常目录：`app/src/main/java/io/legado/app/exception/`
2. 全局未捕获异常处理：`app/src/main/java/io/legado/app/help/CrashHandler.kt`
3. Throwable 扩展（统一堆栈字符串）：`app/src/main/java/io/legado/app/utils/ThrowableExtensions.kt`

---

## Error Handling Patterns

### Web HTTP API

1. Controller/业务逻辑内部捕获可预期异常，并设置 `ReturnData.setErrorMsg(...)`。
2. 不要把原始异常对象直接返回给前端；如果需要给出原因，返回可读的 `errorMsg`。

### HttpServer 顶层兜底

`HttpServer.serve(...)` 顶层有 try/catch：

1. 记录请求 Start/End/Error（包含 `stackTraceStr`）。
2. 出现异常时返回 `newFixedLengthResponse(e.message)`（历史行为）。

示例：`app/src/main/java/io/legado/app/web/HttpServer.kt`

---

## API Error Responses

内置 Web HTTP API 的“业务返回”以 `ReturnData` 为准（JSON 结构）：

1. 成功：`isSuccess = true`，`errorMsg = ""`，`data` 为结果
2. 失败：`isSuccess = false`，`errorMsg` 为错误信息，`data` 通常为 `null`

示例：`app/src/main/java/io/legado/app/api/ReturnData.kt`

前端会检查该结构（否则提示“后端返回内容格式错误”）：`modules/web/src/api/index.ts`

---

## Common Mistakes

1. 在 Controller 内吞掉异常但不记录日志，导致线上无法定位。
2. 把敏感信息（Cookie/账号/Token/本地路径等）直接拼进 `errorMsg` 返回给前端。
3. 改动 API 返回结构（字段名/语义）但未同步更新 `modules/web` 的拦截器/类型定义。
