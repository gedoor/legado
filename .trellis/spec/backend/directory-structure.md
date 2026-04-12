# Directory Structure

> 本项目“后端”代码在仓库中的组织方式。

---

## Overview

本仓库没有传统的独立后端服务。

这里的 **backend** 主要指：Android App 侧的业务逻辑 + 本地数据层（Room/SQLite）+ 内置的 HTTP/WebSocket 服务（为 `modules/web` 提供 API）。

---

## Directory Layout

后端核心代码位于：`app/src/main/java/io/legado/app/`

```
app/src/main/java/io/legado/app/
├── api/          # 给 Web/外部使用的 API（ReturnData、controller 等）
├── base/         # 基类
├── constant/     # 常量、全局配置
├── data/         # Room 数据库相关（dao/entities/database/migrations）
├── exception/    # 自定义异常类型
├── help/         # 帮助/辅助逻辑（配置、工具封装等）
├── lib/          # 第三方库封装/适配
├── model/        # 解析/模型层
├── receiver/     # 广播接收
├── service/      # Android Service（包含 WebService 等）
├── ui/           # UI 界面相关
├── utils/        # 工具方法（Log、Gson、扩展等）
└── web/          # 内置 Web 服务（HttpServer / WebSocketServer 等）
```

该结构在仓库内也有简要说明：`app/src/main/java/io/legado/app/README.md`。

---

## Module Organization

### 新增/修改 Web HTTP API

1. 在 `app/src/main/java/io/legado/app/api/controller/` 增加或修改 Controller 方法。
2. 在 `app/src/main/java/io/legado/app/web/HttpServer.kt` 的 `when (uri)` 中挂载路由。
3. Controller 返回 `io.legado.app.api.ReturnData`，前端通过 `isSuccess/errorMsg/data` 判断。

### 新增/修改 Web WebSocket API

1. 在 `app/src/main/java/io/legado/app/web/socket/` 下实现 WebSocket（按具体功能拆分）。
2. 在 `app/src/main/java/io/legado/app/web/WebSocketServer.kt` 的 `when (handshake.uri)` 中挂载路由。

### 新增/修改数据库功能

1. 实体类：`app/src/main/java/io/legado/app/data/entities/`。
2. DAO：`app/src/main/java/io/legado/app/data/dao/`。
3. 数据库入口：`app/src/main/java/io/legado/app/data/AppDatabase.kt`。
4. 迁移：`app/src/main/java/io/legado/app/data/DatabaseMigrations.kt`。

---

## Naming Conventions

1. Kotlin/Java 包名全小写：`io.legado.app.<module>`。
2. Kotlin 文件名通常为 PascalCase：`AppDatabase.kt`, `HttpServer.kt`。
3. Room 表名以实体 `@Entity(tableName=...)` 为准，例如 `books`（见 `Book.kt`）。

---

## Examples

1. 总览目录说明：`app/src/main/java/io/legado/app/README.md`
2. data 目录说明：`app/src/main/java/io/legado/app/data/README.md`
3. web 服务说明：`app/src/main/java/io/legado/app/web/ReadMe.md`
4. HTTP 路由分发：`app/src/main/java/io/legado/app/web/HttpServer.kt`
5. WebSocket 路由分发：`app/src/main/java/io/legado/app/web/WebSocketServer.kt`
