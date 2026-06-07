# ui/order - 订单模块

## 概述
包含订单创建、编辑、历史记录和详情展示的所有 UI 逻辑。

## 关键文件
- **`OrderCreateViewModel.kt`**: 处理新订单的创建逻辑，包括日期选择和菜品汇总。
- **`OrderEditViewModel.kt`**: 处理现有订单的修改和删除。
- **`OrderHistoryViewModel.kt`**: 加载历史订单列表。
- **`OrderHomeScreen.kt`**: 订单功能的入口 Tab 页。

## 复杂度
这是项目中最复杂的 UI 模块，涉及多个 ViewModel 之间的数据同步（如菜品选择与原料汇总）。

## 代理指令
- 修改订单逻辑时，请确保 `OrderCreateViewModel` 和 `OrderEditViewModel` 的状态管理保持一致。
- 新增 UI 组件时，请优先复用 `ui/components` 中的通用组件。
