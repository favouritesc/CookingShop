package cn.favouritesc.cookingshop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cn.favouritesc.cookingshop.ui.common.AppContainer
import cn.favouritesc.cookingshop.ui.common.AppViewModelFactory
import cn.favouritesc.cookingshop.ui.dish.DishDetailViewModel
import cn.favouritesc.cookingshop.ui.dish.DishEditViewModel
import cn.favouritesc.cookingshop.ui.dish.DishListViewModel
import cn.favouritesc.cookingshop.ui.home.HomeViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientSummaryViewModel
import cn.favouritesc.cookingshop.ui.ingredient.IngredientViewModel
import cn.favouritesc.cookingshop.ui.navigation.BottomNavBar
import cn.favouritesc.cookingshop.ui.navigation.NavGraph
import cn.favouritesc.cookingshop.ui.order.OrderCreateViewModel
import cn.favouritesc.cookingshop.ui.order.OrderEditViewModel
import cn.favouritesc.cookingshop.ui.order.OrderHistoryViewModel
import cn.favouritesc.cookingshop.ui.theme.CookingShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as CookingShopApplication).appContainer
        appContainer.initSync()

        setContent {
            CookingShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(appContainer = appContainer)
                }
            }
        }
    }
}

@Composable
fun MainScreen(appContainer: AppContainer) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val factory = remember { AppViewModelFactory(appContainer) }

    // ViewModels with Factory
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val dishListViewModel: DishListViewModel = viewModel(factory = factory)
    val dishEditViewModel: DishEditViewModel = viewModel(factory = factory)
    val dishDetailViewModel: DishDetailViewModel = viewModel(factory = factory)
    val orderCreateViewModel: OrderCreateViewModel = viewModel(factory = factory)
    val orderEditViewModel: OrderEditViewModel = viewModel(factory = factory)
    val orderHistoryViewModel: OrderHistoryViewModel = viewModel(factory = factory)
    val ingredientViewModel: IngredientViewModel = viewModel(factory = factory)
    val ingredientSummaryViewModel: IngredientSummaryViewModel = viewModel(factory = factory)

    // Error handling
    val homeError by homeViewModel.error.collectAsState()
    val dishListError by dishListViewModel.error.collectAsState()
    val dishEditError by dishEditViewModel.error.collectAsState()
    val dishDetailError by dishDetailViewModel.error.collectAsState()
    val orderCreateError by orderCreateViewModel.error.collectAsState()
    val orderEditError by orderEditViewModel.error.collectAsState()
    val orderHistoryError by orderHistoryViewModel.error.collectAsState()
    val ingredientError by ingredientViewModel.error.collectAsState()
    val ingredientSummaryError by ingredientSummaryViewModel.error.collectAsState()

    val error = homeError ?: dishListError ?: dishEditError ?: dishDetailError ?: 
                orderCreateError ?: orderEditError ?: orderHistoryError ?: 
                ingredientError ?: ingredientSummaryError

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // 客户端同步数据写入后触发一次 UI 刷新（Home/OrderHistory 使用 one-shot 查询，需手动触发）
    val syncManager = appContainer.syncManager
    LaunchedEffect(syncManager) {
        syncManager?.dataVersion?.collect { version ->
            if (version > 0) {
                homeViewModel.loadTodayData()
                orderHistoryViewModel.loadOrders()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        bottomBar = {
            BottomNavBar(navController = navController)
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            homeViewModel = homeViewModel,
            dishListViewModel = dishListViewModel,
            dishEditViewModel = dishEditViewModel,
            dishDetailViewModel = dishDetailViewModel,
            orderCreateViewModel = orderCreateViewModel,
            orderEditViewModel = orderEditViewModel,
            orderHistoryViewModel = orderHistoryViewModel,
            ingredientViewModel = ingredientViewModel,
            ingredientSummaryViewModel = ingredientSummaryViewModel,
            syncManager = appContainer.syncManager,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        )
    }
}
