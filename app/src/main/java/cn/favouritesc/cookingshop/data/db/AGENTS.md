# data/db - 数据库层

## 概述
使用原生 `SQLiteOpenHelper` 实现数据持久化，不依赖 Room。

## 关键文件
- **`DatabaseHelper.kt`**: 数据库创建、升级、CRUD 操作的核心类（约 800+ 行）。这是一个**巨型类**，包含了所有表的操作逻辑。
- **模型类**: `Dish`, `Ingredient`, `Order` 等，定义表结构和数据对象。

## 约定
- **SQL 语句**: 直接在 `DatabaseHelper` 中编写原始 SQL。
- **ID 策略**: 所有表使用 `INTEGER PRIMARY KEY AUTOINCREMENT`。
- **外键**: 启用了 `FOREIGN KEY` 约束并设置了 `ON DELETE CASCADE`。
- **时间戳**: 使用 `INTEGER` 存储 Unix 时间戳。

## 代理指令
- **禁止使用 Room**：这是项目的明确架构决策。
- **巨型类警告**：`DatabaseHelper.kt` 是一个"上帝类"。修改时请格外小心，避免引入副作用。
- 修改表结构时，必须同步更新 `onCreate` 中的建表语句和 `onUpgrade` 中的迁移逻辑。
- 新增字段时，需更新相关的 `ContentValues` 转换逻辑。
