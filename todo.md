# CookingShop — 项目状态 & 待办

## 项目概述

基于 Kotlin + Jetpack Compose 的 Android 点餐管理应用（`cn.favouritesc.cookingshop`）。

- **单 Activity**: `MainActivity` 承载所有 Compose UI
- **数据库**: `SQLiteOpenHelper` 直接操作 SQLite（WAL 模式），**禁止引入 Room**
- **DI**: `AppContainer` 手动注入，位于 `ui/common/`
- **构建**: JDK 21, AGP 9.2.1, minSdk/targetSdk 36，`gradlew assembleDebug`
- **图片**: Coil `AsyncImage`，`imageUrl` 为本地文件路径

## 关键路径速查

| 页面 | 文件 |
|------|------|
| 首页 Tab | `ui/home/HomeScreen.kt` + `HomeViewModel.kt` |
| 菜品库 Tab | `ui/dish/DishListScreen.kt` + `DishListViewModel.kt` |
| 点餐 Tab | `ui/order/OrderHomeScreen.kt` + `OrderHistoryViewModel.kt` |
| 我的 Tab | `ui/profile/ProfileScreen.kt` |
| 创建点餐 | `ui/order/OrderCreateScreen.kt` + `OrderCreateViewModel.kt` |
| 编辑点餐 | `ui/order/OrderEditScreen.kt` + `OrderEditViewModel.kt` |
| 点餐详情 | `ui/order/OrderDetailScreen.kt` (共用 OrderEditViewModel) |
| 菜品详情 | `ui/dish/DishDetailScreen.kt` + `DishDetailViewModel.kt` |
| 编辑菜品 | `ui/dish/DishEditScreen.kt` + `DishEditViewModel.kt` |
| 添加菜品(选择) | `ui/order/DishSelectScreen.kt` + `DishSelectViewModel` 接口 |
| 点餐历史 | `ui/order/OrderHistoryScreen.kt` |
| 备菜管理 | `ui/ingredient/IngredientListScreen.kt` + `IngredientViewModel.kt` |
| 备菜汇总 | `ui/ingredient/IngredientSummaryScreen.kt` |
| 导航 | `ui/navigation/NavGraph.kt` + `Screen.kt` + `BottomNavBar.kt` |
| 基础设施 | `ui/common/AppContainer.kt` + `BaseViewModel.kt` + `ViewModelFactory.kt` |
| 数据库 | `data/db/DatabaseHelper.kt` (v2, WAL) |
| 数据模型 | `data/db/Dish.kt`, `Order.kt`, `Ingredient.kt`, `OrderDish.kt`, `DishTag.kt` |
| Repository | `data/repository/{Dish,Order,Ingredient,Tag}Repository.kt` |
| 同步模块 | `sync/` (新) |

## 公共组件 (ui/components/)

| 组件 | 文件 | 用途 |
|------|------|------|
| `DishThumbnail` | `DishThumbnail.kt` | 菜品缩略图（有图→AsyncImage，无图→彩色首字底） |
| `IngredientIcon` | `IngredientIcons.kt` | 食材图标（200+ 映射，10 分类，支持自定义存储 icon） |
| `IngredientSummaryGrid` | `IngredientSummaryGrid.kt` | 备菜汇总彩色标签网格 |
| `StaggeredItem` | `StaggeredItem.kt` | 交错入场动画（80ms 间隔 fadeIn+slideIn） |
| `ShimmerEffect` | `ShimmerEffect.kt` | 骨架屏加载效果 |
| `AnimatedQuantity` | `AnimatedQuantity.kt` | 数字翻滚动效（±按钮） |
| `CommonTopBar` | `CommonTopBar.kt` | 通用橙色顶栏 |
| `ImagePicker` | `ImagePicker.kt` | 图片选择器（Coil） |
| 其他 | `ConfirmDialog`, `DatePickerButton`, `TimePickerButton`, `EmptyState`, `LoadingIndicator`, `TagChip` | |

## 设计系统

- 页面背景: `#F5F5F5` (LightGray)
- 卡片: 白色, 10dp 圆角, 1dp 阴影
- 主色调: `#FF8F00` (Primary)
- 状态标签: 未完成=橙底橙字, 已完成=绿底绿字
- 交错动画: Home/OrderHome/DishList 的 LazyColumn/Grid 使用 StaggeredItem
- 生命周期刷新: HomeScreen/OrderHomeScreen/OrderHistoryScreen 的 ON_RESUME 带 skipFirstResume
- 导航刷新: NavGraph 的 onBackClick/onOrderCreated 处显式调用 loadTodayData/loadOrders

## 已知模式 & 注意事项

1. **ViewModel 成功标志必须重置**: `_createdSuccessfully`, `_deletedSuccessfully`, `_updatedSuccessfully` 等 flag 在 NavGraph 入口 composable 中调用 `resetXxxState()` 重置
2. **DishSelectScreen 路由分离**: 创建用 `dish_select` (OrderCreateViewModel)，编辑用 `dish_select_edit` (OrderEditViewModel)
3. **编辑模式 addDish 增量持久化**: OrderEditViewModel 的 addDish/removeDish/updateDishQuantity 直接调用 repository 单条 insert/delete/update，不用 updateOrderWithDishes（避免竞态）
4. **烹饪时间双来源**: cookingTime Int + COOKING_TIME 标签已去重，编辑页标签区 filter 跳过 COOKING_TIME 和 DIFFICULTY
5. **默认时间显示**: 烹饪时间为 15/30/45/60/90/120 → "≤Nmin"，自定义 → "Nmin"
6. **Ingredient.icon 字段**: Ingredient 模型有可空 icon，用户可在编辑弹窗选择、存储，优先于自动匹配

---

## ✅ 局域网同步 — 已完成（会话 #1）

### 实现内容

**UI 层** (`ProfileScreen.kt`):
- ✅ "局域网同步" 菜单项（`Icons.Default.Sync`），点击弹出角色选择
- ✅ 主机模式面板：显示本机 IP:端口 + 绿色指示器 + 「停止主机」按钮
- ✅ 客户端面板：三色状态指示器（绿=已连接 / 橙=连接中 / 红=已断开）+ 「断开连接」按钮
- ✅ 角色选择弹窗：「开启主机」/「加入其他设备」
- ✅ IP 输入弹窗：支持 `ip:port` 格式（默认端口 8765）

**数据层** (`sync/` 包核心改造):
- ✅ `GET /full` 端点：返回全量数据快照（dishes+ingredients+orders+relations）
- ✅ 消息体含完整实体：DISH_SAVED/ORDER_SAVED 携带完整 JSON + 关联数据
- ✅ 客户端自动导入：SyncManager 内部 `syncConsumerJob` 消费 syncEvents → upsert 到本地 DB
- ✅ `dataVersion` StateFlow：UI 层轻量触发 Home/OrderHistory 刷新
- ✅ `getLocalIpAddress()`：遍历 NetworkInterface 获取本机 IPv4
- ✅ JSON 序列化/反序列化：12 个转换函数覆盖所有实体类型

**导航层**:
- ✅ NavGraph 传入 `syncManager` → ProfileScreen
- ✅ MainScreen `dataVersion.collect` 触发 one-shot ViewModel 刷新

### 当前同步架构

```
主机 end:
  Repo.save() → sync?.invoke(id)
    → SyncManager.onDishSaved(id)
      → scope.launch { 查 DB 完整实体 → 序列化 JSON → server.append(msg) }

  SyncServer 端点:
    /hello     → {cursor: N}              握手
    /full      → 全量数据快照 (JSON)       首次同步
    /poll?since=N → {cursor, changes}     增量拉取

客户端 end:
  SyncClient.connect() → scope.launch { /hello → /full → import → startPolling }
  /poll 收到消息 → onMessage → syncEvents.tryEmit → syncConsumerJob.collect
    → handleSyncEvent(msg)
      → 解析 JSON → Repo.insert/update/delete (upsert 语义)
      → emitDataChange() → Flow 自动刷新 UI (DishList/Ingredients)
      → dataVersion++ → MainScreen LaunchedEffect 触发 Home/OrderHistory 刷新
```

### 已知待办

- [ ] 数据隔离：客户端离开房间时恢复原始数据（方案：`sync_session_id` 列标记）
- [ ] 主机掉线检测：连续 poll 失败 N 次 → 自动 DISCONNECTED
- [ ] 局域网自动发现：Android NsdManager (mDNS)
- [ ] Changelog 持久化：当前纯内存，进程被杀丢失
- [ ] 游标过期检测：主机重启后客户端 cursor 失效
- [ ] 新客户端无存量数据：需首次调用 `/full`（已实现端点，SyncClient 连接后应自动调用）
