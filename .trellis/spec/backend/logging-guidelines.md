# Logging Guidelines

> 本项目的日志记录方式与约定。

---

## Overview

项目主要使用两套日志入口：

1. `LogUtils`：基于 `java.util.logging.Logger`，可写入文件（受 `AppConfig.recordLog` 控制）。
2. `AppLog`：面向业务的“最近日志”缓存（最多 100 条），DEBUG 下也会走 Android `Log.e`。

关键实现：

1. `app/src/main/java/io/legado/app/utils/LogUtils.kt`
2. `app/src/main/java/io/legado/app/constant/AppLog.kt`

---

## Log Levels

当前 `LogUtils` 的常用方法语义：

1. `LogUtils.d(...)` 实际写入 `Level.INFO`
2. `LogUtils.e(...)` 实际写入 `Level.WARNING`

（注意：这里的 `d/e` 命名偏历史习惯，实际 Level 以实现为准。）

---

## Structured Logging

文件日志格式由 `LogUtils` formatter 控制，输出类似：

1. `yy-MM-dd HH:mm:ss.SSS: <message>`

日志文件位置：外部缓存目录下的 `logs/`（见 `LogUtils.createFileHandler(...)`）。

---

## What to Log

1. 内置 Web 服务请求的 start/end/error（便于排查 Web 端问题）：`app/src/main/java/io/legado/app/web/HttpServer.kt`
2. 数据迁移/数据库初始化等关键路径的异常（影响启动/升级）。
3. 影响用户体验的关键状态变化（必要时用 `AppLog.put(..., toast = true)` 提示）。

---

## What NOT to Log

1. Cookie、账号密码、token、鉴权 header（项目有 `Cookie` 表，避免把其内容落日志）。
2. 用户隐私数据或可识别信息（PII）。
3. 大体量内容（章节正文、超大 JSON 等），避免撑爆日志文件与性能问题。
