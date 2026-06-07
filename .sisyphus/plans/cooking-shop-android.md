# CookingShop Android App — 完整开发计划

## TL;DR
> **Summary**: 基于 Jetpack Compose + Room + MVVM 构建菜品发布与点餐 Android 应用，包含菜品管理、备菜库、标签系统、点餐流程、订单查询和导出功能。
> **Deliverables**: 可运行的 Android 应用 APK，包含全部 CRUD 功能、订单导出（CSV + 图片）、备菜汇总提醒
> **Effort**: Large
> **Parallel**: YES - 5 waves
> **Critical Path**: Task 1 (项目基础) → Task 2 (数据库) → Task 6-10 (核心功能) → Task 11-14 (订单管理) → Task 15-18 (收尾)

## Context

### Original Request
构建一个 Android 菜品发布与点餐应用，分为两大板块：
1. **菜品发布**：上传图片、编辑名称、做法、备菜（预置分类列表）、标签（餐时/类型/烹饪时间/难度/菜系）
2. **菜品点餐**：选择日期时间、从已发布菜品选择、显示备菜和做法、订单列表查询（1/7/30天/自定义日期）

### Interview Summary
| 决策 | 选择 | 理由 |
|------|------|------|
| 数据存储 | Room (本地 SQLite) | 无需服务器，离线可用 |
| UI 框架 | Jetpack Compose | 现代声明式 UI，官方推荐 |
| 架构模式 | MVVM | Compose 标准搭配 |
| 图片存储 | 内部存储 + Room 路径引用 | 简单可靠 |
| 用户系统 | 无 | 单用户本地应用 |
| 测试策略 | Tests-after | 快速开发 |
| 主导航 | 底部导航 (首页/发布/点餐/我的) | 标准移动应用模式 |
| 备菜分类 | 蔬菜、肉类、豆制品、主食、水果 | 用户选择 |
| 菜品标签 | 餐时、类型、烹饪时间、难度、菜系(川菜/湘菜等) | 用户选择 |
| 订单模式 | 独立订单，支持多次增减菜品 | 用户选择 |
| 首页 | 今日概览 | 用户选择 |
| 菜品详情 | 独立详情页 | 用户选择 |
| 图片数量 | 单张/菜品 | 用户选择 |
| 导出格式 | CSV + 图片 | 用户选择 |

### Gap Analysis (自行审查)
| Gap | 类型 | 处理 |
|-----|------|------|
| Room 数据库迁移策略 | Minor | 默认：首版无迁移，version=1 |
| 图片压缩策略 | Minor | 默认：压缩到 1MB 以内再存储 |
| 备菜"库存提醒"含义 | Ambiguous | 默认：点餐时显示所需备菜汇总清单 |
| 日期选择器组件 | Minor | 默认：Material3 DatePicker |
| 底部导航具体页面 | Minor | 首页、菜品库、点餐、我的（4项） |
| 点餐"今日概览"内容 | Ambiguous | 显示今日订单 + 快捷点餐入口 |
| 菜品标签"菜系"子分类 | Minor | 预置：川菜、湘菜、粤菜、鲁菜、苏菜、浙菜、闽菜、徽菜（八大菜系） |

## Work Objectives

### Core Objective
构建一个功能完整的 Android 菜品发布与点餐应用，用户可管理菜品库、创建点餐订单、查看订单历史并导出。

### Deliverables
1. 菜品发布模块（CRUD + 图片 + 标签 + 备菜关联）
2. 备菜管理模块（预置分类 + CRUD）
3. 菜品标签系统（多维度标签）
4. 点餐流程（选菜品 + 选日期时间 + 备菜汇总）
5. 订单管理（今日概览 + 历史查询 + 日期筛选）
6. 导出功能（CSV + 图片）
7. 完整的单元测试和 UI 测试

### Definition of Done
```bash
# 可通过以下命令验证完成度：
./gradlew assembleDebug                    # 构建成功
./gradlew test                             # 单元测试通过
./gradlew connectedAndroidTest             # 仪器测试通过（需模拟器）
# 手动验证：
# 1. 可创建/编辑/删除菜品（含图片上传）
# 2. 可管理备菜分类和标签
# 3. 可创建点餐订单并添加/移除菜品
# 4. 首页显示今日概览
# 5. 可按日期筛选订单
# 6. 可导出订单为 CSV 和图片
```

### Must Have
- 完整的菜品 CRUD（创建、读取、更新、删除）
- 图片上传和内部存储
- 备菜预置分类和管理
- 菜品多标签系统
- 点餐流程（选菜品 + 日期时间）
- 订单列表和日期筛选
- CSV 和图片导出
- 备菜汇总清单

### Must NOT Have (Guardrails)
- 不引入网络请求/Retrofit/OkHttp（纯本地应用）
- 不使用 Dagger/Hilt（手动 DI，保持简单）
- 不实现用户登录/注册
- 不使用 Room AutoMigration（首版手动定义 schema）
- 不在 Composable 中直接访问数据库（通过 ViewModel）
- 不硬编码字符串到 Composable（使用 strings.xml）
- 不跳过错误处理（所有数据库操作需 try-catch）

## Verification Strategy
> ZERO HUMAN INTERVENTION — all verification is agent-executed.

- **Test decision**: Tests-after + JUnit5 (unit) + Espresso (instrumented) + Compose UI Testing
- **QA policy**: Every task has agent-executed scenarios
- **Evidence**: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Foundation — 5 tasks, max parallel):
  ├─ Task 1: 项目基础设置 [build]
  ├─ Task 2: 数据库层设计与实现 [build]
  ├─ Task 3: 通用 UI 组件与主题 [visual-engineering]
  ├─ Task 4: 数据仓库层 [build]
  └─ Task 5: 通用 ViewModel 基类 [build]

Wave 2 (Core Features — 5 tasks, max parallel):
  ├─ Task 6: 备菜管理模块 [build]
  ├─ Task 7: 菜品发布模块 [build]
  ├─ Task 8: 菜品列表与筛选 [build]
  ├─ Task 9: 菜品详情页 [build]
  └─ Task 10: 点餐流程 — 选择菜品与日期 [build]

Wave 3 (Order Management — 4 tasks, max parallel):
  ├─ Task 11: 订单管理与编辑 [build]
  ├─ Task 12: 首页 — 今日概览 [build]
  ├─ Task 13: 订单历史与日期筛选 [build]
  └─ Task 14: 导出功能 (CSV + 图片) [build]

Wave 4 (Polish & Testing — 4 tasks, max parallel):
  ├─ Task 15: 备菜汇总提醒 [build]
  ├─ Task 16: "我的"页面 [build]
  ├─ Task 17: UI 打磨与错误处理 [visual-engineering]
  └─ Task 18: 单元测试与 UI 测试 [build]

Wave 5 (Final Verification — 4 tasks, parallel):
  ├─ F1: Plan Compliance Audit [oracle]
  ├─ F2: Code Quality Review [unspecified-high]
  ├─ F3: Real Manual QA [unspecified-high]
  └─ F4: Scope Fidelity Check [deep]
```

### Dependency Matrix
| Task | Depends On | Blocks |
|------|------------|--------|
| 1 | — | 2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18 |
| 2 | 1 | 4,6,7,8,9,10,11,12,13,14,15,18 |
| 3 | 1 | 6,7,8,9,10,11,12,13,14,15,16,17 |
| 4 | 2 | 6,7,8,9,10,11,12,13,14,15,18 |
| 5 | 1 | 6,7,8,9,10,11,12,13,14,15 |
| 6 | 2,3,4,5 | 7,10,15 |
| 7 | 2,3,4,5,6 | 8,9,10 |
| 8 | 2,3,4,5 | 10 |
| 9 | 2,3,4,5,7 | 10 |
| 10 | 2,3,4,5,6,7,8 | 11,12 |
| 11 | 2,3,4,5,10 | 12,13,14,15 |
| 12 | 2,3,4,5,11 | 17 |
| 13 | 2,3,4,5,11 | 14,17 |
| 14 | 2,3,4,5,11,13 | 17 |
| 15 | 2,3,4,5,6,11 | 17 |
| 16 | 3 | 17 |
| 17 | All above | 18 |
| 18 | 17 | F1-F4 |

### Agent Dispatch Summary
| Wave | Tasks | Categories |
|------|-------|------------|
| Wave 1 | 5 | build(4), visual-engineering(1) |
| Wave 2 | 5 | build(5) |
| Wave 3 | 4 | build(4) |
| Wave 4 | 4 | build(2), visual-engineering(1), build(1) |
| Wave 5 | 4 | oracle(1), unspecified-high(2), deep(1) |

## TODOs

---

### Task 1: 项目基础设置 — Gradle 依赖与 Compose 配置

**What to do**:
1. 修改 `app/build.gradle.kts`，添加以下依赖：
   - Jetpack Compose BOM (2024.02.00+)
   - Compose UI, Material3, Navigation Compose
   - Room (runtime, ktx, compiler)
   - ViewModel Compose
   - Coil (图片加载)
   - Lifecycle runtime compose
2. 启用 Compose 编译器选项
3. 创建 `libs.versions.toml`（如不存在）统一版本管理
4. 配置 `compileOptions` 和 `kotlinOptions` 支持 Java 11

**Must NOT do**:
- 不添加 Dagger/Hilt
- 不添加 Retrofit/OkHttp
- 不修改 applicationId 或 minSdk

**Recommended Agent Profile**:
- Category: `build` — 理由：Gradle 配置是构建任务
- Skills: [] — 无需额外技能
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: NO | Wave 1 | Blocks: [2,3,4,5] | Blocked By: []

**References**:
- Pattern: `app/build.gradle.kts:1-44` — 现有构建配置，需扩展
- Pattern: `settings.gradle.kts:1-26` — 仓库配置参考
- External: https://developer.android.com/jetpack/compose/bom/bom-mapping — Compose BOM 版本映射

**Acceptance Criteria**:
- [ ] `./gradlew assembleDebug` 构建成功
- [ ] `app/build.gradle.kts` 包含 Compose, Room, Navigation, Coil 依赖
- [ ] Compose 编译器选项已启用

**QA Scenarios**:
```
Scenario: Gradle 构建成功
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew assembleDebug
  Expected: BUILD SUCCESSFUL in Xs
  Evidence: .sisyphus/evidence/task-1-gradle-build.txt

Scenario: 依赖解析成功
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:dependencies --configuration implementation
  Expected: 包含 compose, room, navigation, coil 依赖树
  Evidence: .sisyphus/evidence/task-1-dependencies.txt
```

**Commit**: YES | Message: `chore: add Compose, Room, Navigation, Coil dependencies` | Files: [app/build.gradle.kts, gradle/libs.versions.toml]

---

### Task 2: 数据库层设计与实现 — Room Entities, DAOs, Database

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/data/db/` 目录
2. 定义以下 Room Entity：
   - `Dish` (id, name, imageUrl, recipe, cookingTime, difficulty, createdAt, updatedAt)
   - `IngredientCategory` (id, name, sortOrder)
   - `Ingredient` (id, name, categoryId, isDefault)
   - `DishIngredient` (dishId, ingredientId, quantity) — 关联表
   - `DishTag` (id, name, type: ENUM[MEAL_TIME, TYPE, COOKING_TIME, DIFFICULTY, CUISINE])
   - `DishTagCrossRef` (dishId, tagId) — 多对多关联
   - `Order` (id, date, time, status, note, createdAt)
   - `OrderDish` (orderId, dishId, quantity) — 关联表
3. 创建 DAO 接口：
   - `DishDao` (insert, update, delete, getById, getAll, getByTag, search)
   - `IngredientDao` (CRUD + getByCategory)
   - `OrderDao` (CRUD + getByDateRange, getByDate)
   - `TagDao` (CRUD + getByType)
4. 创建 `AppDatabase` 类，version=1，包含预填充数据（备菜分类 + 默认标签）
5. 使用 `RoomDatabase.Callback` 在首次创建时插入预置数据

**Must NOT do**:
- 不使用 AutoMigration
- 不在 Entity 中使用 @Embedded 嵌套复杂对象
- 不在 DAO 中使用 @Transaction（除非必要）

**Recommended Agent Profile**:
- Category: `build` — 理由：数据层实现
- Skills: [] — 标准 Room 实现
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES (与 Task 3 并行) | Wave 1 | Blocks: [4,6,7,8,9,10,11,12,13,14,15] | Blocked By: [1]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/` — 包路径
- External: https://developer.android.com/training/data-storage/room — Room 官方文档
- External: https://developer.android.com/training/data-storage/room/relationships — Room 关系定义

**Acceptance Criteria**:
- [ ] 所有 Entity 类编译通过，无 LSP 错误
- [ ] 所有 DAO 接口定义完整，包含所需查询方法
- [ ] AppDatabase 类包含预填充回调
- [ ] 预置数据包含：5 个备菜分类 + 默认标签（餐时、类型、烹饪时间、难度、菜系）

**QA Scenarios**:
```
Scenario: Entity 编译验证
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: 编译成功，无 Entity 相关错误
  Evidence: .sisyphus/evidence/task-2-entity-compile.txt

Scenario: 预置数据验证
  Tool: Bash
  Steps: grep -r "INSERT" app/src/main/java/cn/favouritesc/cookingshop/data/db/
  Expected: 包含备菜分类和标签的 INSERT 语句
  Evidence: .sisyphus/evidence/task-2-prefill-data.txt
```

**Commit**: YES | Message: `feat(data): add Room entities, DAOs, and database with prefilled data` | Files: [app/src/main/java/cn/favouritesc/cookingshop/data/db/*.kt]

---

### Task 3: 通用 UI 组件与主题 — Compose Theme, Navigation, Shared Components

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/ui/theme/` 目录
2. 实现 Material3 主题：
   - `Theme.kt` — 颜色方案（浅色/深色）
   - `Color.kt` — 定义应用颜色（食物相关暖色调）
   - `Type.kt` — 字体样式
3. 创建 `ui/navigation/` 目录：
   - `Screen.kt` — 定义屏幕密封类（Home, DishPublish, DishLibrary, Order, Profile）
   - `NavGraph.kt` — 导航图配置
   - `BottomNavBar.kt` — 底部导航栏（4项：首页、菜品库、点餐、我的）
4. 创建 `ui/components/` 目录：
   - `CommonTopBar.kt` — 通用顶部栏
   - `LoadingIndicator.kt` — 加载指示器
   - `EmptyState.kt` — 空状态组件
   - `ConfirmDialog.kt` — 确认对话框
   - `DatePickerButton.kt` — 日期选择按钮（Material3 DatePicker）
   - `TimePickerButton.kt` — 时间选择按钮
   - `ImagePicker.kt` — 图片选择组件（相机/相册）
   - `TagChip.kt` — 标签 Chip 组件

**Must NOT do**:
- 不硬编码颜色值到 Composable（使用主题颜色）
- 不在 Composable 中直接处理业务逻辑
- 不跳过深色主题支持

**Recommended Agent Profile**:
- Category: `visual-engineering` — 理由：UI 组件和主题设计
- Skills: [`frontend-ui-ux`] — 需要 UI 设计能力
- Omitted: [`playwright`] — 无需浏览器测试

**Parallelization**: Can Parallel: YES (与 Task 2 并行) | Wave 1 | Blocks: [6,7,8,9,10,11,12,13,14,15,16,17] | Blocked By: [1]

**References**:
- Pattern: `app/src/main/res/values/themes.xml` — 现有主题参考
- Pattern: `app/src/main/res/values/colors.xml` — 现有颜色参考
- External: https://developer.android.com/jetpack/compose/designsystems/material3 — Material3 指南

**Acceptance Criteria**:
- [ ] Theme 文件创建，支持浅色/深色模式
- [ ] 底部导航栏 4 项可正常切换
- [ ] 所有通用组件编译通过
- [ ] DatePickerButton 和 TimePickerButton 可正常弹出选择器

**QA Scenarios**:
```
Scenario: 主题编译验证
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: 主题和组件文件编译成功
  Evidence: .sisyphus/evidence/task-3-theme-compile.txt

Scenario: 导航结构验证
  Tool: Bash
  Steps: grep -r "NavHost\|NavGraph\|BottomNavigation" app/src/main/java/
  Expected: 包含 NavHost 和底部导航定义
  Evidence: .sisyphus/evidence/task-3-navigation.txt
```

**Commit**: YES | Message: `feat(ui): add theme, navigation, and shared Compose components` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/**/*.kt]

---

### Task 4: 数据仓库层 — Repository Pattern 实现

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/data/repository/` 目录
2. 实现以下 Repository：
   - `DishRepository` — 菜品 CRUD、按标签查询、搜索
   - `IngredientRepository` — 备菜 CRUD、按分类查询
   - `OrderRepository` — 订单 CRUD、按日期范围查询、添加/移除订单菜品
   - `TagRepository` — 标签 CRUD、按类型查询
3. 每个 Repository 注入对应的 DAO
4. 使用 Kotlin Flow 返回数据（`Flow<List<T>>`）
5. 所有写操作使用 `withContext(Dispatchers.IO)`

**Must NOT do**:
- 不在 Repository 中使用 LiveData（使用 Flow）
- 不在 Repository 中处理 UI 逻辑
- 不跳过错误处理

**Recommended Agent Profile**:
- Category: `build` — 理由：数据层架构
- Skills: [] — 标准 Repository 模式
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES (与 Task 3 并行) | Wave 1 | Blocks: [6,7,8,9,10,11,12,13,14,15] | Blocked By: [2]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — DAO 接口定义
- External: https://developer.android.com/topic/architecture/data-layer — Android 数据层架构

**Acceptance Criteria**:
- [ ] 4 个 Repository 类创建，注入对应 DAO
- [ ] 所有查询方法返回 `Flow<List<T>>`
- [ ] 所有写操作在 `Dispatchers.IO` 上执行
- [ ] 编译通过，无 LSP 错误

**QA Scenarios**:
```
Scenario: Repository 编译验证
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: Repository 类编译成功
  Evidence: .sisyphus/evidence/task-4-repo-compile.txt

Scenario: Flow 返回类型验证
  Tool: Bash
  Steps: grep -r "Flow<" app/src/main/java/cn/favouritesc/cookingshop/data/repository/
  Expected: 所有查询方法返回 Flow 类型
  Evidence: .sisyphus/evidence/task-4-flow-return.txt
```

**Commit**: YES | Message: `feat(data): add repository layer with Flow-based data access` | Files: [app/src/main/java/cn/favouritesc/cookingshop/data/repository/*.kt]

---

### Task 5: 通用 ViewModel 基类与依赖注入

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/ui/common/` 目录
2. 实现 `BaseViewModel` 抽象类：
   - 包含 `viewModelScope` 协程管理
   - 通用错误处理 `_error: MutableStateFlow<String?>`
   - 通用加载状态 `_isLoading: MutableStateFlow<Boolean>`
3. 创建 `AppContainer` 类（手动 DI）：
   - 持有 Database 实例
   - 提供所有 Repository 实例
   - 在 Application 类中初始化
4. 创建 `CookingShopApplication` 类继承 `Application`：
   - 初始化 `AppContainer`
5. 更新 `AndroidManifest.xml` 注册 Application 类

**Must NOT do**:
- 不使用 Hilt/Dagger
- 不使用 Koin
- 不在 ViewModel 中直接引用 Application Context（除非必要）

**Recommended Agent Profile**:
- Category: `build` — 理由：架构基础设施
- Skills: [] — 标准 MVVM 实现
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES (与 Task 2,3 并行) | Wave 1 | Blocks: [6,7,8,9,10,11,12,13,14,15] | Blocked By: [1]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/` — 包路径
- Pattern: `app/src/main/AndroidManifest.xml` — Application 注册
- External: https://developer.android.com/topic/architecture/viewmodel — ViewModel 指南

**Acceptance Criteria**:
- [ ] BaseViewModel 包含 error 和 loading 状态
- [ ] AppContainer 提供所有 Repository
- [ ] CookingShopApplication 注册到 AndroidManifest
- [ ] 编译通过，无 LSP 错误

**QA Scenarios**:
```
Scenario: Application 注册验证
  Tool: Bash
  Steps: grep -r "android:name" app/src/main/AndroidManifest.xml
  Expected: 包含 CookingShopApplication 类名
  Evidence: .sisyphus/evidence/task-5-app-manifest.txt

Scenario: ViewModel 编译验证
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: BaseViewModel 和 AppContainer 编译成功
  Evidence: .sisyphus/evidence/task-5-vm-compile.txt
```

**Commit**: YES | Message: `feat(arch): add BaseViewModel, AppContainer, and Application class` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/common/*.kt, app/src/main/java/cn/favouritesc/cookingshop/CookingShopApplication.kt, app/src/main/AndroidManifest.xml]

---

### Task 6: 备菜管理模块 — Ingredient CRUD 与分类

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/ui/ingredient/` 目录
2. 实现 `IngredientViewModel`：
   - `ingredientsByCategory: StateFlow<Map<String, List<Ingredient>>>`
   - `addIngredient(name, categoryId)`
   - `updateIngredient(ingredient)`
   - `deleteIngredient(ingredient)`
3. 实现 `IngredientListScreen`：
   - 按分类分组显示备菜列表
   - 每个分类可折叠/展开
   - 添加按钮（FloatingActionButton）
   - 长按删除，点击编辑
4. 实现 `IngredientEditDialog`：
   - 名称输入
   - 分类选择（下拉菜单）
   - 保存/取消按钮
5. 注册到导航图

**Must NOT do**:
- 不删除预置的默认备菜（isDefault=true 的记录）
- 不在 ViewModel 中直接操作 UI

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [7,10,15] | Blocked By: [2,3,4,5]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Ingredient Entity
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/` — 通用组件
- External: https://developer.android.com/jetpack/compose/lists — LazyColumn 使用

**Acceptance Criteria**:
- [ ] 备菜列表按分类分组显示
- [ ] 可添加、编辑、删除自定义备菜
- [ ] 预置备菜不可删除
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 备菜列表显示
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: IngredientScreen 编译成功
  Evidence: .sisyphus/evidence/task-6-ingredient-compile.txt

Scenario: 预置数据保护
  Tool: Bash
  Steps: grep -r "isDefault" app/src/main/java/cn/favouritesc/cookingshop/
  Expected: 删除逻辑检查 isDefault 标志
  Evidence: .sisyphus/evidence/task-6-default-protection.txt
```

**Commit**: YES | Message: `feat(ingredient): add ingredient management with category grouping` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/ingredient/*.kt]

---

### Task 7: 菜品发布模块 — Dish CRUD 与图片上传

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/ui/dish/` 目录
2. 实现 `DishEditViewModel`：
   - `dishName: MutableStateFlow<String>`
   - `recipe: MutableStateFlow<String>`
   - `imageUri: MutableStateFlow<Uri?>`
   - `selectedIngredients: MutableStateFlow<List<DishIngredient>>`
   - `selectedTags: MutableStateFlow<List<DishTag>>`
   - `saveDish()` — 保存到数据库
   - `loadDish(dishId)` — 编辑时加载
3. 实现 `DishEditScreen`：
   - 顶部：图片选择区域（点击选择相册/拍照）
   - 菜品名称输入框
   - 做法文本框（多行）
   - 备菜选择区域（从已备菜列表选择，输入数量）
   - 标签选择区域（Chip 多选，按类型分组）
   - 保存按钮
4. 实现图片处理：
   - 从相册/相机获取图片 URI
   - 压缩图片到 1MB 以内
   - 保存到应用内部存储
   - 保存路径到数据库
5. 注册到导航图（新建/编辑共用同一 Screen，通过参数区分）

**Must NOT do**:
- 不在 Composable 中直接访问文件系统
- 不跳过图片压缩
- 不存储原始大图

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [8,9,10] | Blocked By: [2,3,4,5,6]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Dish Entity
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/ImagePicker.kt` — 图片选择组件
- External: https://developer.android.com/training/data-storage/app-specific — 应用内部存储

**Acceptance Criteria**:
- [ ] 可创建新菜品（含图片、名称、做法、备菜、标签）
- [ ] 可编辑现有菜品
- [ ] 图片压缩后存储，路径保存到数据库
- [ ] 标签按类型分组显示
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 菜品编辑页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: DishEditScreen 编译成功
  Evidence: .sisyphus/evidence/task-7-dish-edit-compile.txt

Scenario: 图片压缩逻辑验证
  Tool: Bash
  Steps: grep -r "compress\|Bitmap\|quality" app/src/main/java/
  Expected: 包含图片压缩逻辑
  Evidence: .sisyphus/evidence/task-7-image-compress.txt
```

**Commit**: YES | Message: `feat(dish): add dish publishing with image upload and tag selection` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/dish/*.kt]

---

### Task 8: 菜品列表与筛选 — Dish Library Screen

**What to do**:
1. 实现 `DishListViewModel`：
   - `dishes: StateFlow<List<Dish>>`
   - `searchQuery: MutableStateFlow<String>`
   - `selectedTags: MutableStateFlow<List<DishTag>>`
   - `filteredDishes: StateFlow<List<Dish>>` — 组合筛选
   - `deleteDish(dish)`
2. 实现 `DishListScreen`：
   - 顶部搜索栏
   - 标签筛选横向滚动条（按类型分组）
   - 菜品网格列表（2列 Grid）
   - 每个菜品卡片：图片、名称、标签预览
   - FAB 按钮：新建菜品
   - 点击进入详情页，长按删除
3. 实现搜索逻辑：按名称模糊匹配
4. 实现标签筛选逻辑：多标签取交集

**Must NOT do**:
- 不使用 Paging 库（数据量小，无需分页）
- 不在列表中直接加载原图（使用 Coil 缩略图）

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [10] | Blocked By: [2,3,4,5]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Dish Entity
- External: https://developer.android.com/jetpack/compose/lists — LazyGrid 使用
- External: https://coil-kt.github.io/coil/compose/ — Coil 图片加载

**Acceptance Criteria**:
- [ ] 菜品以 2 列网格显示
- [ ] 搜索栏可按名称筛选
- [ ] 标签可多选筛选
- [ ] 点击进入详情页
- [ ] 长按弹出删除确认

**QA Scenarios**:
```
Scenario: 菜品列表编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: DishListScreen 编译成功
  Evidence: .sisyphus/evidence/task-8-dish-list-compile.txt

Scenario: 筛选逻辑验证
  Tool: Bash
  Steps: grep -r "filter\|search\|selectedTags" app/src/main/java/cn/favouritesc/cookingshop/ui/dish/
  Expected: 包含筛选逻辑实现
  Evidence: .sisyphus/evidence/task-8-filter-logic.txt
```

**Commit**: YES | Message: `feat(dish): add dish library with search and tag filtering` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/dish/DishListScreen.kt, DishListViewModel.kt]

---

### Task 9: 菜品详情页 — Dish Detail Screen

**What to do**:
1. 实现 `DishDetailViewModel`：
   - `dish: StateFlow<Dish?>`
   - `ingredients: StateFlow<List<DishIngredient>>`
   - `tags: StateFlow<List<DishTag>>`
   - `loadDish(dishId)` — 加载菜品详情
2. 实现 `DishDetailScreen`：
   - 顶部大图（全宽）
   - 菜品名称
   - 标签展示（Chip 组）
   - 做法区域（如有则显示，否则显示"尚未发布做法"）
   - 备菜清单（食材 + 数量）
   - 底部操作栏：编辑按钮
3. 注册到导航图（接收 dishId 参数）

**Must NOT do**:
- 不在详情页直接编辑（跳转到编辑页）
- 不跳过"尚未发布做法"的空状态处理

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [10] | Blocked By: [2,3,4,5,7]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/` — 通用组件
- External: https://developer.android.com/jetpack/compose/components/scaffold — Scaffold 布局

**Acceptance Criteria**:
- [ ] 详情页显示大图、名称、标签、做法、备菜
- [ ] 无做法时显示"尚未发布做法"
- [ ] 编辑按钮跳转到编辑页
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 详情页编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: DishDetailScreen 编译成功
  Evidence: .sisyphus/evidence/task-9-detail-compile.txt

Scenario: 空做法处理
  Tool: Bash
  Steps: grep -r "尚未发布做法\|recipe.*null\|recipe.*empty" app/src/main/java/
  Expected: 包含空做法的处理逻辑
  Evidence: .sisyphus/evidence/task-9-empty-recipe.txt
```

**Commit**: YES | Message: `feat(dish): add dish detail screen with recipe and ingredients display` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/dish/DishDetailScreen.kt, DishDetailViewModel.kt]

---

### Task 10: 点餐流程 — 选择菜品与日期时间

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/ui/order/` 目录
2. 实现 `OrderCreateViewModel`：
   - `selectedDate: MutableStateFlow<LocalDate>`
   - `selectedTime: MutableStateFlow<LocalTime>`
   - `availableDishes: StateFlow<List<Dish>>` — 已发布菜品
   - `selectedDishes: MutableStateFlow<List<OrderDish>>` — 已选菜品
   - `addDish(dish)`
   - `removeDish(dishId)`
   - `updateDishQuantity(dishId, quantity)`
   - `createOrder()` — 创建订单
3. 实现 `OrderCreateScreen`：
   - 日期选择（Material3 DatePicker）
   - 时间选择（Material3 TimePicker）
   - 已选菜品列表（可调整数量、移除）
   - 备菜汇总预览（实时计算所需备菜）
   - "添加菜品"按钮 → 跳转菜品选择页
   - "发布点餐"按钮
4. 实现 `DishSelectScreen`：
   - 菜品列表（复选框多选）
   - 搜索和筛选
   - 确认选择按钮
5. 注册到导航图

**Must NOT do**:
- 不允许选择未来超过 7 天的日期（合理范围）
- 不允许不选择任何菜品就创建订单

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 2 | Blocks: [11,12] | Blocked By: [2,3,4,5,6,7,8]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Order Entity
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/DatePickerButton.kt` — 日期选择组件
- External: https://developer.android.com/jetpack/compose/components/datepickers — Material3 DatePicker

**Acceptance Criteria**:
- [ ] 可选择日期和时间
- [ ] 可从已发布菜品中选择并添加
- [ ] 可调整菜品数量和移除
- [ ] 实时显示备菜汇总
- [ ] 点击"发布点餐"创建订单
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 点餐页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: OrderCreateScreen 编译成功
  Evidence: .sisyphus/evidence/task-10-order-compile.txt

Scenario: 日期验证逻辑
  Tool: Bash
  Steps: grep -r "LocalDate\|isAfter\|isBefore" app/src/main/java/cn/favouritesc/cookingshop/ui/order/
  Expected: 包含日期范围验证
  Evidence: .sisyphus/evidence/task-10-date-validation.txt
```

**Commit**: YES | Message: `feat(order): add order creation with dish selection and date/time picker` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/order/*.kt]

---

### Task 11: 订单管理与编辑 — Order Edit & Management

**What to do**:
1. 实现 `OrderEditViewModel`：
   - `order: StateFlow<Order?>`
   - `orderDishes: StateFlow<List<OrderDish>>`
   - `loadOrder(orderId)`
   - `addDish(dish)`
   - `removeDish(dishId)`
   - `updateQuantity(dishId, quantity)`
   - `updateOrder()` — 保存修改
   - `deleteOrder()`
2. 实现 `OrderEditScreen`：
   - 显示订单日期和时间
   - 已选菜品列表（可增减、调整数量）
   - 备菜汇总
   - 保存/删除按钮
3. 实现 `OrderDetailScreen`：
   - 只读模式显示订单详情
   - 菜品列表、备菜汇总
   - 编辑/删除按钮
4. 注册到导航图

**Must NOT do**:
- 不允许修改订单日期（只读）
- 不跳过删除确认对话框

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [12,13,14,15] | Blocked By: [2,3,4,5,10]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Order Entity
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/ConfirmDialog.kt` — 确认对话框

**Acceptance Criteria**:
- [ ] 可查看订单详情
- [ ] 可编辑订单（增减菜品、调整数量）
- [ ] 可删除订单（需确认）
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 订单编辑页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: OrderEditScreen 编译成功
  Evidence: .sisyphus/evidence/task-11-order-edit-compile.txt

Scenario: 删除确认逻辑
  Tool: Bash
  Steps: grep -r "ConfirmDialog\|deleteOrder" app/src/main/java/cn/favouritesc/cookingshop/ui/order/
  Expected: 删除操作前弹出确认对话框
  Evidence: .sisyphus/evidence/task-11-delete-confirm.txt
```

**Commit**: YES | Message: `feat(order): add order editing and management` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/order/OrderEditScreen.kt, OrderDetailScreen.kt, OrderEditViewModel.kt]

---

### Task 12: 首页 — 今日概览 Home Screen

**What to do**:
1. 实现 `HomeViewModel`：
   - `todayOrders: StateFlow<List<Order>>` — 今日订单
   - `todayDishesSummary: StateFlow<Map<Dish, Int>>` — 今日菜品汇总
   - `ingredientSummary: StateFlow<Map<Ingredient, String>>` — 今日备菜汇总
   - `loadTodayData()` — 加载今日数据
2. 实现 `HomeScreen`：
   - 顶部：今日日期 + 快捷点餐按钮
   - 今日订单卡片列表（显示订单时间、菜品数量）
   - 今日备菜汇总区域（显示需要哪些备菜及数量）
   - 空状态：无订单时显示"今天还没有点餐"
3. 点击订单卡片进入订单详情
4. 注册到导航图（首页默认页面）

**Must NOT do**:
- 不显示非今日订单（首页只显示今日）
- 不跳过空状态处理

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [17] | Blocked By: [2,3,4,5,11]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/EmptyState.kt` — 空状态组件
- External: https://developer.android.com/jetpack/compose/components/card — Card 组件

**Acceptance Criteria**:
- [ ] 首页显示今日订单列表
- [ ] 显示今日备菜汇总
- [ ] 有快捷点餐入口
- [ ] 无订单时显示空状态
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 首页编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: HomeScreen 编译成功
  Evidence: .sisyphus/evidence/task-12-home-compile.txt

Scenario: 空状态处理
  Tool: Bash
  Steps: grep -r "EmptyState\|还没有点餐" app/src/main/java/cn/favouritesc/cookingshop/ui/home/
  Expected: 包含空状态显示逻辑
  Evidence: .sisyphus/evidence/task-12-empty-state.txt
```

**Commit**: YES | Message: `feat(home): add home screen with today's overview and ingredient summary` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/home/*.kt]

---

### Task 13: 订单历史与日期筛选 — Order History Screen

**What to do**:
1. 实现 `OrderHistoryViewModel`：
   - `orders: StateFlow<List<Order>>`
   - `dateRange: MutableStateFlow<DateRange>` — 1天/7天/30天/自定义
   - `customStartDate: MutableStateFlow<LocalDate?>`
   - `customEndDate: MutableStateFlow<LocalDate?>`
   - `loadOrders()` — 按日期范围加载
2. 实现 `OrderHistoryScreen`：
   - 顶部筛选栏：1天 | 7天 | 30天 | 自定义
   - 自定义日期：开始日期 + 结束日期选择器
   - 订单列表（按日期分组，最近的在前）
   - 每个订单卡片：日期、时间、菜品数量、菜品预览
3. 点击订单进入详情
4. 注册到导航图（"我的"页面入口）

**Must NOT do**:
- 不使用 Paging 库
- 不跳过日期范围为空的处理

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [14,17] | Blocked By: [2,3,4,5,11]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/OrderDao.kt` — 按日期查询
- External: https://developer.android.com/jetpack/compose/components/chips — FilterChip 组件

**Acceptance Criteria**:
- [ ] 可按 1天/7天/30天 筛选订单
- [ ] 可自定义日期范围筛选
- [ ] 订单按日期分组显示
- [ ] 点击进入订单详情
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 订单历史页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: OrderHistoryScreen 编译成功
  Evidence: .sisyphus/evidence/task-13-history-compile.txt

Scenario: 日期筛选逻辑
  Tool: Bash
  Steps: grep -r "DateRange\|getByDateRange" app/src/main/java/
  Expected: 包含日期范围查询逻辑
  Evidence: .sisyphus/evidence/task-13-date-filter.txt
```

**Commit**: YES | Message: `feat(order): add order history with date range filtering` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/order/OrderHistoryScreen.kt, OrderHistoryViewModel.kt]

---

### Task 14: 导出功能 — CSV 和图片导出

**What to do**:
1. 创建 `app/src/main/java/cn/favouritesc/cookingshop/data/export/` 目录
2. 实现 `ExportManager`：
   - `exportToCsv(order, context): Uri` — 导出订单为 CSV
   - `exportToImage(order, context): Uri` — 导出订单为图片
3. CSV 导出逻辑：
   - 列：日期、时间、菜品名称、数量、备菜清单
   - 使用 `OutputStreamWriter` 写入
   - 保存到应用内部存储，返回 URI
4. 图片导出逻辑：
   - 使用 Compose 截图 API (`graphicsLayer`)
   - 设计订单卡片布局
   - 渲染为 Bitmap 并保存为 PNG
5. 在订单详情页添加"导出"按钮（弹出选择：CSV/图片）
6. 导出后显示分享 Intent（可选）

**Must NOT do**:
- 不使用第三方截图库
- 不跳过文件权限处理
- 不导出未保存的订单

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Android 文件操作
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 3 | Blocks: [17] | Blocked By: [2,3,4,5,11,13]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Order Entity
- External: https://developer.android.com/training/data-storage/app-specific — 应用内部存储
- External: https://developer.android.com/develop/ui/compose/graphics/graphicslayer — Compose 截图

**Acceptance Criteria**:
- [ ] 可导出订单为 CSV 文件
- [ ] 可导出订单为图片（PNG）
- [ ] 导出后可分享
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 导出功能编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: ExportManager 编译成功
  Evidence: .sisyphus/evidence/task-14-export-compile.txt

Scenario: CSV 格式验证
  Tool: Bash
  Steps: grep -r "OutputStreamWriter\|\.csv" app/src/main/java/
  Expected: 包含 CSV 写入逻辑
  Evidence: .sisyphus/evidence/task-14-csv-format.txt
```

**Commit**: YES | Message: `feat(export): add order export to CSV and image` | Files: [app/src/main/java/cn/favouritesc/cookingshop/data/export/*.kt]

---

### Task 15: 备菜汇总提醒 — Ingredient Summary

**What to do**:
1. 实现 `IngredientSummaryViewModel`：
   - `dateRange: MutableStateFlow<DateRange>`
   - `ingredientSummary: StateFlow<Map<IngredientCategory, List<IngredientSummaryItem>>>`
   - `loadSummary(dateRange)` — 汇总指定日期范围内的备菜需求
2. 实现 `IngredientSummaryScreen`：
   - 日期范围选择（1天/7天/30天）
   - 按分类分组显示备菜需求
   - 每项显示：备菜名称、所需数量、来源菜品列表
   - 高亮显示"库存不足"的备菜（如果备菜数量 > 预设阈值）
3. 在首页和订单详情页添加"备菜汇总"入口
4. 注册到导航图

**Must NOT do**:
- 不实现真正的库存管理（只是汇总显示）
- 不跳过空汇总的处理

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose + ViewModel
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [17] | Blocked By: [2,3,4,5,6,11]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/data/db/` — Ingredient, Order Entity
- External: https://developer.android.com/jetpack/compose/components/card — Card 组件

**Acceptance Criteria**:
- [ ] 可按日期范围汇总备菜需求
- [ ] 按分类分组显示
- [ ] 显示每项备菜的来源菜品
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 备菜汇总页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: IngredientSummaryScreen 编译成功
  Evidence: .sisyphus/evidence/task-15-summary-compile.txt

Scenario: 汇总逻辑验证
  Tool: Bash
  Steps: grep -r "ingredientSummary\|loadSummary" app/src/main/java/
  Expected: 包含汇总计算逻辑
  Evidence: .sisyphus/evidence/task-15-summary-logic.txt
```

**Commit**: YES | Message: `feat(ingredient): add ingredient summary with date range aggregation` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/ingredient/IngredientSummaryScreen.kt, IngredientSummaryViewModel.kt]

---

### Task 16: "我的"页面 — Profile Screen

**What to do**:
1. 实现 `ProfileScreen`：
   - 用户头像（默认占位图）
   - 菜品数量统计
   - 订单数量统计
   - 功能入口：
     - 菜品管理（跳转菜品库）
     - 备菜管理（跳转备菜列表）
     - 订单历史（跳转订单历史）
     - 备菜汇总（跳转汇总页）
     - 关于（显示版本信息）
2. 注册到导航图

**Must NOT do**:
- 不实现用户登录/注册
- 不显示个人信息（无用户系统）

**Recommended Agent Profile**:
- Category: `build` — 理由：功能模块实现
- Skills: [] — 标准 Compose
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [17] | Blocked By: [3]

**References**:
- Pattern: `app/src/main/java/cn/favouritesc/cookingshop/ui/components/` — 通用组件
- External: https://developer.android.com/jetpack/compose/components/card — Card 组件

**Acceptance Criteria**:
- [ ] 显示菜品和订单统计
- [ ] 功能入口可正常跳转
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 我的页面编译
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew :app:compileDebugKotlin
  Expected: ProfileScreen 编译成功
  Evidence: .sisyphus/evidence/task-16-profile-compile.txt
```

**Commit**: YES | Message: `feat(profile): add profile screen with stats and navigation entries` | Files: [app/src/main/java/cn/favouritesc/cookingshop/ui/profile/*.kt]

---

### Task 17: UI 打磨与错误处理 — Polish & Error Handling

**What to do**:
1. 全局错误处理：
   - ViewModel 错误状态显示到 Snackbar
   - 数据库操作 try-catch 包装
   - 图片加载失败显示占位图
2. 加载状态：
   - 所有列表页面添加 LoadingIndicator
   - 骨架屏（可选）
3. 空状态完善：
   - 所有列表页面添加 EmptyState
   - 友好提示文案
4. 动画优化：
   - 页面切换动画
   - 列表项添加/删除动画
   - FAB 显示/隐藏动画
5. 响应式布局：
   - 支持横屏（可选）
   - 小屏幕适配
6. 字符串资源：
   - 所有硬编码字符串移到 `strings.xml`
   - 支持中文

**Must NOT do**:
- 不引入复杂的动画库
- 不实现多语言支持（仅中文）
- 不跳过错误状态处理

**Recommended Agent Profile**:
- Category: `visual-engineering` — 理由：UI 优化和打磨
- Skills: [`frontend-ui-ux`] — 需要 UI 设计能力
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [18] | Blocked By: [All above]

**References**:
- Pattern: `app/src/main/res/values/strings.xml` — 字符串资源
- External: https://developer.android.com/jetpack/compose/animation — Compose 动画

**Acceptance Criteria**:
- [ ] 所有列表页面有 Loading 和 Empty 状态
- [ ] 错误信息显示到 Snackbar
- [ ] 所有字符串在 strings.xml 中
- [ ] 页面切换有动画
- [ ] 编译通过

**QA Scenarios**:
```
Scenario: 错误处理验证
  Tool: Bash
  Steps: grep -r "Snackbar\|error\|Loading" app/src/main/java/
  Expected: 包含错误处理和加载状态逻辑
  Evidence: .sisyphus/evidence/task-17-error-handling.txt

Scenario: 字符串资源验证
  Tool: Bash
  Steps: grep -r "hardcoded\|\"[^\"]*\"" app/src/main/java/ --include="*.kt" | head -20
  Expected: 无硬编码中文字符串（应在 strings.xml 中）
  Evidence: .sisyphus/evidence/task-17-strings.txt
```

**Commit**: YES | Message: `ui: polish error handling, loading states, and animations` | Files: [多个文件]

---

### Task 18: 单元测试与 UI 测试 — Tests

**What to do**:
1. 单元测试（`app/src/test/`）：
   - Repository 测试（Mock DAO）
   - ViewModel 测试（Mock Repository）
   - 工具类测试（图片压缩、CSV 生成）
2. UI 测试（`app/src/androidTest/`）：
   - 菜品 CRUD 流程
   - 点餐流程
   - 订单查询流程
   - 导出流程
3. 测试数据：
   - 创建测试用的 Entity 数据
   - 使用内存数据库（Room.inMemoryDatabaseBuilder）

**Must NOT do**:
- 不使用真实数据库（使用内存数据库）
- 不跳过边界条件测试
- 不测试第三方库（Coil, Room）

**Recommended Agent Profile**:
- Category: `build` — 理由：测试实现
- Skills: [] — 标准 JUnit + Espresso
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 4 | Blocks: [F1-F4] | Blocked By: [17]

**References**:
- Pattern: `app/src/test/java/` — 现有测试目录
- Pattern: `app/src/androidTest/java/` — 现有仪器测试目录
- External: https://developer.android.com/training/testing — Android 测试指南

**Acceptance Criteria**:
- [ ] 单元测试覆盖 Repository 和 ViewModel
- [ ] UI 测试覆盖主要流程
- [ ] `./gradlew test` 通过
- [ ] `./gradlew connectedAndroidTest` 通过（需模拟器）

**QA Scenarios**:
```
Scenario: 单元测试执行
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew test
  Expected: 所有测试通过
  Evidence: .sisyphus/evidence/task-18-unit-test.txt

Scenario: 测试覆盖率
  Tool: Bash
  Steps: grep -r "@Test" app/src/test/ | wc -l
  Expected: 至少 20 个测试方法
  Evidence: .sisyphus/evidence/task-18-test-count.txt
```

**Commit**: YES | Message: `test: add unit tests for repositories and view models` | Files: [app/src/test/**/*.kt, app/src/androidTest/**/*.kt]

## Final Verification Wave

---

### F1. Plan Compliance Audit — oracle

**What to do**:
- 对照计划检查所有任务是否完成
- 验证所有 Acceptance Criteria 是否满足
- 检查 Must Have 功能是否全部实现
- 检查 Must NOT Have 是否被违反

**Recommended Agent Profile**:
- Category: `oracle` — 理由：需要高 IQ 推理能力
- Skills: [] — 标准审计
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 5 | Blocks: [] | Blocked By: [18]

**QA Scenarios**:
```
Scenario: 计划合规性检查
  Tool: Bash
  Steps: 对照 .sisyphus/plans/cooking-shop-android.md 检查所有任务
  Expected: 所有任务完成，所有 Acceptance Criteria 满足
  Evidence: .sisyphus/evidence/F1-plan-compliance.txt
```

---

### F2. Code Quality Review — unspecified-high

**What to do**:
- 代码风格检查（ktlint）
- 架构分层检查（UI → ViewModel → Repository → DAO）
- 命名规范检查
- 注释和文档检查
- 无硬编码字符串检查
- 无内存泄漏检查（ViewModel 生命周期）

**Recommended Agent Profile**:
- Category: `unspecified-high` — 理由：需要代码审查能力
- Skills: [] — 标准代码审查
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 5 | Blocks: [] | Blocked By: [18]

**QA Scenarios**:
```
Scenario: 代码质量检查
  Tool: Bash
  Steps: cd E:\Project\CookingShop && ./gradlew ktlintCheck (if configured)
  Expected: 无代码风格问题
  Evidence: .sisyphus/evidence/F2-code-quality.txt
```

---

### F3. Real Manual QA — unspecified-high

**What to do**:
- 安装 APK 到模拟器/真机
- 测试完整流程：
  1. 创建菜品（含图片上传）
  2. 查看菜品列表和筛选
  3. 创建点餐订单
  4. 查看今日概览
  5. 查看订单历史
  6. 导出订单
  7. 查看备菜汇总
- 记录所有发现的问题

**Recommended Agent Profile**:
- Category: `unspecified-high` — 理由：需要 QA 能力
- Skills: [`playwright`] — 可能需要浏览器测试（如果有 Web 组件）
- Omitted: [] — 无

**Parallelization**: Can Parallel: YES | Wave 5 | Blocks: [] | Blocked By: [18]

**QA Scenarios**:
```
Scenario: 手动 QA 流程
  Tool: Bash
  Steps: 安装 APK 并执行完整流程
  Expected: 所有功能正常工作，无崩溃
  Evidence: .sisyphus/evidence/F3-manual-qa.txt
```

---

### F4. Scope Fidelity Check — deep

**What to do**:
- 检查实现是否超出计划范围（Scope Creep）
- 检查是否有遗漏的功能
- 检查技术栈是否与计划一致（Room, Compose, MVVM）
- 检查是否有不必要的依赖添加
- 确认所有 Must NOT Have 未被违反

**Recommended Agent Profile**:
- Category: `deep` — 理由：需要深度分析能力
- Skills: [] — 标准范围检查
- Omitted: [`playwright`] — 无需浏览器

**Parallelization**: Can Parallel: YES | Wave 5 | Blocks: [] | Blocked By: [18]

**QA Scenarios**:
```
Scenario: 范围保真度检查
  Tool: Bash
  Steps: 对照计划检查实现范围
  Expected: 无 Scope Creep，无遗漏功能
  Evidence: .sisyphus/evidence/F4-scope-fidelity.txt
```

## Commit Strategy
- 每个 Task 完成后立即提交
- Commit Message 格式：`type(scope): description`
- 提交前检查 `git status` 和 `git diff`
- 不提交临时文件或构建产物

## Success Criteria
1. `./gradlew assembleDebug` 构建成功
2. `./gradlew test` 单元测试通过
3. 所有功能可用：
   - 菜品 CRUD（含图片上传）
   - 备菜管理
   - 菜品标签
   - 点餐流程
   - 订单管理
   - 今日概览
   - 订单历史查询
   - 导出功能（CSV + 图片）
   - 备菜汇总
4. 无崩溃，无严重 UI 问题
5. 所有字符串中文，无硬编码
