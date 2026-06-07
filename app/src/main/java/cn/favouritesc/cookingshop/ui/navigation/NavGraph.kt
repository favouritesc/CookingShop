package cn.favouritesc.cookingshop.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import cn.favouritesc.cookingshop.ui.dish.DishDetailScreen
import cn.favouritesc.cookingshop.ui.dish.DishDetailViewModel
import cn.favouritesc.cookingshop.ui.dish.DishEditScreen
import cn.favouritesc.cookingshop.ui.dish.DishEditViewModel
import cn.favouritesc.cookingshop.ui.dish.DishListScreen
import cn.favouritesc.cookingshop.ui.dish.DishListViewModel
import cn.favouritesc.cookingshop.ui.home.HomeScreen
import cn.favouritesc.cookingshop.ui.home.HomeViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientListScreen
import cn.favouritesc.cookingshop.ui.ingredient.IngredientSummaryScreen
import cn.favouritesc.cookingshop.ui.ingredient.IngredientSummaryViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientViewModel
import cn.favouritesc.cookingshop.ui.order.DishSelectScreen
import cn.favouritesc.cookingshop.ui.order.OrderCreateScreen
import cn.favouritesc.cookingshop.ui.order.OrderCreateViewModel
import cn.favouritesc.cookingshop.ui.order.OrderDetailScreen
import cn.favouritesc.cookingshop.ui.order.OrderEditScreen
import cn.favouritesc.cookingshop.ui.order.OrderEditViewModel
import cn.favouritesc.cookingshop.ui.order.OrderHistoryScreen
import cn.favouritesc.cookingshop.ui.order.OrderHistoryViewModel
import cn.favouritesc.cookingshop.ui.order.OrderHomeScreen
import cn.favouritesc.cookingshop.sync.SyncManager
import cn.favouritesc.cookingshop.ui.profile.ProfileScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    dishListViewModel: DishListViewModel,
    dishEditViewModel: DishEditViewModel,
    dishDetailViewModel: DishDetailViewModel,
    orderCreateViewModel: OrderCreateViewModel,
    orderEditViewModel: OrderEditViewModel,
    orderHistoryViewModel: OrderHistoryViewModel,
    ingredientViewModel: IngredientViewModel,
    ingredientSummaryViewModel: IngredientSummaryViewModel,
    syncManager: SyncManager? = null,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // ========== 底部Tab根页面 ==========

        // 首页Tab
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                },
                onCreateOrderClick = {
                    orderCreateViewModel.resetCreateState()
                    navController.navigate(Screen.OrderCreate.route)
                }
            )
        }

        // 菜品库Tab
        composable(Screen.DishLibrary.route) {
            DishListScreen(
                viewModel = dishListViewModel,
                onDishClick = { dishId ->
                    navController.navigate(Screen.DishDetail.createRoute(dishId))
                },
                onAddDishClick = {
                    navController.navigate(Screen.DishEdit.createRoute())
                }
            )
        }

        // 点餐Tab - 使用OrderHomeScreen作为根页面
        composable(Screen.Order.route) {
            OrderHomeScreen(
                viewModel = orderHistoryViewModel,
                onCreateOrderClick = {
                    orderCreateViewModel.resetCreateState()
                    navController.navigate(Screen.OrderCreate.route)
                },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                },
                onHistoryClick = {
                    navController.navigate(Screen.OrderHistory.route)
                }
            )
        }

        // 我的Tab
        composable(Screen.Profile.route) {
            ProfileScreen(
                onDishLibraryClick = {
                    navController.navigate(Screen.DishLibrary.route)
                },
                onIngredientListClick = {
                    navController.navigate(Screen.IngredientList.route)
                },
                onOrderHistoryClick = {
                    navController.navigate(Screen.OrderHistory.route)
                },
                onIngredientSummaryClick = {
                    navController.navigate(Screen.IngredientSummary.route)
                },
                onAboutClick = {
                    // TODO: Show about dialog
                },
                syncManager = syncManager
            )
        }

        // ========== 子页面 ==========

        // 创建点餐
        composable(Screen.OrderCreate.route) {
            OrderCreateScreen(
                viewModel = orderCreateViewModel,
                onBackClick = { navController.popBackStack() },
                onAddDishClick = {
                    navController.navigate("dish_select")
                },
                onOrderCreated = {
                    homeViewModel.loadTodayData()
                    orderHistoryViewModel.loadOrdersIncludingDate(
                        orderCreateViewModel.selectedDate.value
                    )
                    navController.popBackStack()
                }
            )
        }

        // 菜品选择 (创建模式)
        composable("dish_select") {
            DishSelectScreen(
                viewModel = orderCreateViewModel,
                onBackClick = { navController.popBackStack() },
                onDishesSelected = { navController.popBackStack() }
            )
        }

        // 菜品选择 (编辑模式)
        composable("dish_select_edit") {
            Log.d("CookingShop", "NavGraph: dish_select_edit composed, vm=${orderEditViewModel.javaClass.simpleName}, orderDishes.size=${orderEditViewModel.orderDishes.value.size}")
            DishSelectScreen(
                viewModel = orderEditViewModel,
                onBackClick = { navController.popBackStack() },
                onDishesSelected = { navController.popBackStack() }
            )
        }

        // 菜品编辑
        composable(
            route = Screen.DishEdit.route,
            arguments = listOf(navArgument("dishId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val dishId = backStackEntry.arguments?.getLong("dishId") ?: -1L
            DishEditScreen(
                viewModel = dishEditViewModel,
                dishId = dishId,
                onBackClick = { navController.popBackStack() },
                onDishSaved = { dishListViewModel.refreshDishes() }
            )
        }

        // 菜品详情
        composable(
            route = Screen.DishDetail.route,
            arguments = listOf(navArgument("dishId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dishId = backStackEntry.arguments?.getLong("dishId") ?: 0L
            DishDetailScreen(
                viewModel = dishDetailViewModel,
                dishId = dishId,
                onBackClick = { navController.popBackStack() },
                onEditClick = {
                    navController.navigate(Screen.DishEdit.createRoute(dishId))
                }
            )
        }

        // 点餐详情
        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            orderEditViewModel.resetEditState()
            OrderDetailScreen(
                viewModel = orderEditViewModel,
                orderId = orderId,
                onBackClick = {
                    Log.d("CookingShop", "NavGraph: OrderDetail onBackClick, refresh VMs")
                    homeViewModel.loadTodayData()
                    orderHistoryViewModel.loadOrders()
                    navController.popBackStack()
                },
                onEditClick = {
                    navController.navigate(Screen.OrderEdit.createRoute(orderId))
                },
                onDishClick = { dishId ->
                    navController.navigate(Screen.DishDetail.createRoute(dishId))
                }
            )
        }

        // 编辑点餐
        composable(
            route = Screen.OrderEdit.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            Log.d("CookingShop", "NavGraph: OrderEdit composed, orderId=$orderId, orderDishes.size=${orderEditViewModel.orderDishes.value.size}")
            orderEditViewModel.resetEditState()
            OrderEditScreen(
                viewModel = orderEditViewModel,
                orderId = orderId,
                onBackClick = {
                    Log.d("CookingShop", "NavGraph: OrderEdit onBackClick, refresh VMs")
                    homeViewModel.loadTodayData()
                    orderHistoryViewModel.loadOrders()
                    navController.popBackStack()
                },
                onAddDishClick = {
                    Log.d("CookingShop", "NavGraph: OrderEdit onAddDishClick -> navigate to dish_select_edit")
                    navController.navigate("dish_select_edit")
                }
            )
        }

        // 点餐历史
        composable(Screen.OrderHistory.route) {
            OrderHistoryScreen(
                viewModel = orderHistoryViewModel,
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                }
            )
        }

        // 备菜管理
        composable(Screen.IngredientList.route) {
            IngredientListScreen(
                viewModel = ingredientViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 备菜汇总
        composable(Screen.IngredientSummary.route) {
            IngredientSummaryScreen(
                viewModel = ingredientSummaryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
