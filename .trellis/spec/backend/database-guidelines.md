# Database Guidelines

> 本项目的数据库（Room/SQLite）约定与模式。

---

## Overview

本项目使用 **Room (SQLite)** 作为本地数据库。

关键位置：

1. 数据库入口：`app/src/main/java/io/legado/app/data/AppDatabase.kt`
2. 迁移：`app/src/main/java/io/legado/app/data/DatabaseMigrations.kt`
3. DAO：`app/src/main/java/io/legado/app/data/dao/`
4. Entities：`app/src/main/java/io/legado/app/data/entities/`

---

## Query Patterns

1. DAO 使用 `@Dao` + `@Query` 为主，复杂 SQL 用多行字符串（`""" ... """`）表达。
2. 需要“订阅式更新”的查询，优先返回 `kotlinx.coroutines.flow.Flow`。
3. 多步骤写操作用 `@Transaction` 包裹（确保一致性）。

示例：

1. `Flow` 查询 + 二次 map/filter：`app/src/main/java/io/legado/app/data/dao/BookDao.kt`
2. 复杂 SQL 多行字符串：`app/src/main/java/io/legado/app/data/dao/BookDao.kt`

---

## Migrations

本项目的迁移来源有两类：

1. `@AutoMigration`：在 `AppDatabase.kt` 的 `@Database(autoMigrations = [...])` 中声明。
2. 手写 `Migration(x, y)`：集中在 `DatabaseMigrations.kt`，并在 `AppDatabase.kt` builder 中通过 `.addMigrations(*DatabaseMigrations.migrations)` 注入。

修改 schema 时的常见步骤：

1. 修改/新增 entity/dao。
2. 更新 `AppDatabase.kt` 的 `@Database(version = ...)`。
3. 视变更情况添加 `AutoMigration` 或在 `DatabaseMigrations.kt` 新增 `Migration` 并加入 `migrations` 数组。
4. 如果需要验证迁移链，可参考（或补全）迁移测试：`app/src/androidTest/java/io/legado/app/MigrationTest.kt`。

注意：`AppDatabase.kt` 当前 builder 启用了 `.allowMainThreadQueries()`（历史原因）。新增代码仍应避免引入高耗时查询阻塞主线程。

---

## Naming Conventions

1. 表名以 `@Entity(tableName = "...")` 为准，例如：`Book` 使用 `tableName = "books"`（`app/src/main/java/io/legado/app/data/entities/Book.kt`）。
2. 索引常见写法：`@Entity(indices = [Index(value = ["name", "author"], unique = true)])`（同上）。
3. 字段默认值常用 `@ColumnInfo(defaultValue = "...")`（同上）。

---

## Common Mistakes

1. 修改 entity 但忘记同步更新 `AppDatabase` 的 `version`。
2. 新增字段/表但忘记补迁移（或 AutoMigration 声明），导致升级崩溃。
3. 在主线程执行重查询/批量操作（即使当前允许主线程查询，也要避免卡顿）。
4. SQL 字符串拼接缺少 `encode/escape` 或参数化（DAO 优先用 `:param` 绑定）。
