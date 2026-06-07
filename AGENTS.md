# AGENTS.md - CookingShop

**Generated:** 2026-05-27
**Mode:** Update

## 项目概览
基于 **Kotlin** 和 **Jetpack Compose** 的单模块 Android 应用。

## 核心架构
- **单 Activity**: `MainActivity` 承载所有 Compose UI。
- **手动 DI**: 通过 `AppContainer` 注入 `Repository` 和 `DatabaseHelper`。
- **原生数据库**: 使用 `SQLiteOpenHelper` 直接操作 SQLite。**禁止引入 Room**。
- **版本目录**: 依赖由 `gradle/libs.versions.toml` 统一管理。
- **无独立 DI 包**: `AppContainer` 位于 `ui/common`，而非标准的 `di` 包。

## 关键目录
| 目录 | 职责 | 备注 |
|---|---|---|
| `app/src/main/java/.../data/db` | 数据库层 | 见 `data/db/AGENTS.md` |
| `app/src/main/java/.../ui/order` | 订单模块 | 见 `ui/order/AGENTS.md` |
| `app/src/main/java/.../ui/common` | 基础设施 | `AppContainer`, `BaseViewModel` |
| `app/src/main/java/.../ui/navigation` | 路由定义 | `NavGraph.kt`, `Screen.kt` |

## 构建环境 (关键)
- **JDK**: 必须使用 **JDK 21** (由 Gradle Toolchain 强制指定)。
- **AGP**: 9.2.1 (Alpha版)，需要最新的构建工具支持。
- **SDK**: `minSdk = 36`, `targetSdk = 36`。

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 运行单元测试
./gradlew test
```

## 代理指令
1. **数据库**: 仅修改 `DatabaseHelper.kt` 和 `data/db` 下的模型类。不要尝试使用 Room。
2. **UI**: 遵循 Jetpack Compose 的声明式写法，使用 `AppViewModelFactory` 获取 ViewModel。
3. **依赖**: 所有新依赖必须先添加到 `gradle/libs.versions.toml`。
