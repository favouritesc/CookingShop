package cn.favouritesc.cookingshop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val selectedIcon: ImageVector? = null
) {
    // 主要页面 (底部Tab) — 选中/未选中不同图标
    object Home : Screen("home", "首页", Icons.Outlined.Home, Icons.Filled.Home)
    object DishLibrary : Screen("dish_library", "菜品库", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook)
    object Order : Screen("order", "点餐", Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart)
    object Profile : Screen("profile", "我的", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)

    // 菜品相关
    object DishEdit : Screen("dish_edit/{dishId}", "编辑菜品") {
        fun createRoute(dishId: Long = -1L) = "dish_edit/$dishId"
    }
    object DishDetail : Screen("dish_detail/{dishId}", "菜品详情") {
        fun createRoute(dishId: Long) = "dish_detail/$dishId"
    }

    // 订单相关
    object OrderCreate : Screen("order_create", "创建点餐")
    object OrderEdit : Screen("order_edit/{orderId}", "编辑点餐") {
        fun createRoute(orderId: Long) = "order_edit/$orderId"
    }
    object OrderDetail : Screen("order_detail/{orderId}", "点餐详情") {
        fun createRoute(orderId: Long) = "order_detail/$orderId"
    }
    object OrderHistory : Screen("order_history", "点餐历史")

    // 备菜相关
    object IngredientList : Screen("ingredient_list", "备菜管理")
    object IngredientSummary : Screen("ingredient_summary", "备菜汇总")

    // 底部导航项
    companion object {
        val bottomNavItems = listOf(Home, DishLibrary, Order, Profile)
    }
}
