package cn.favouritesc.cookingshop.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.db.OrderStatus
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.DishThumbnail
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientSummaryGrid
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.components.ShimmerList
import cn.favouritesc.cookingshop.ui.components.StaggeredItem
import cn.favouritesc.cookingshop.ui.theme.Primary
import java.time.format.DateTimeFormatter

private val LightGray = Color(0xFFF5F5F5)
private val PendingColor = Color(0xFFFF8F00)
private val CompletedColor = Color(0xFF4CAF50)
private val PendingBgColor = Color(0xFFFFF3E0)
private val CompletedBgColor = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOrderClick: (Long) -> Unit,
    onCreateOrderClick: () -> Unit
) {
    val todayOrders by viewModel.todayOrders.collectAsState()
    val todayOrderDishes by viewModel.todayOrderDishes.collectAsState()
    val todayDishDetails by viewModel.todayDishDetails.collectAsState()
    val ingredientSummary by viewModel.ingredientSummary.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 当页面恢复可见时刷新数据（跳过首次，init 已加载）
    var skipFirstResume by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipFirstResume) {
                    Log.d("CookingShop", "HomeScreen: skip first ON_RESUME (init already loaded)")
                    skipFirstResume = false
                } else {
                    Log.d("CookingShop", "HomeScreen: ON_RESUME -> loadTodayData")
                    viewModel.loadTodayData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CommonTopBar(title = "首页")
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateOrderClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "创建点餐")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                ShimmerList()
            }
        } else if (todayOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(message = "今天还没有点餐")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(LightGray),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 标题区域
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "今日点餐",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = todayDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "共 ${todayOrders.size} 单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 订单列表
                itemsIndexed(todayOrders) { index, order ->
                    StaggeredItem(index = index) {
                        HomeOrderCard(
                            order = order,
                            orderDishes = todayOrderDishes[order.id] ?: emptyList(),
                            dishDetails = todayDishDetails,
                            onClick = { onOrderClick(order.id) }
                        )
                    }
                }

                // 备菜汇总
                if (ingredientSummary.isNotEmpty()) {
                    item {
                        IngredientSummaryGrid(ingredientSummary = ingredientSummary)
                    }
                }

                // 底部留白给 FAB
                item { Spacer(modifier = Modifier.height(56.dp)) }
            }
        }
    }
}

@Composable
private fun HomeOrderCard(
    order: Order,
    orderDishes: List<OrderDish>,
    dishDetails: Map<Long, Dish>,
    onClick: () -> Unit
) {
    val dateText = try {
        val parts = order.date.split("-")
        if (parts.size == 3) "${parts[1].toInt()}月${parts[2].toInt()}日"
        else order.date
    } catch (_: Exception) { order.date }

    val isCompleted = order.statusEnum == OrderStatus.COMPLETED
    val statusText = if (isCompleted) "已完成" else "未完成"
    val statusColor = if (isCompleted) CompletedColor else PendingColor
    val statusBgColor = if (isCompleted) CompletedBgColor else PendingBgColor

    val dishCount = orderDishes.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = order.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "${dishCount}道菜",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 菜品展示
            when {
                dishCount > 3 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val showDishes = orderDishes
                            .mapNotNull { od -> dishDetails[od.dishId] }
                            .take(3)
                        showDishes.forEachIndexed { index, dish ->
                            DishThumbnail(dishName = dish.name, imageUrl = dish.imageUrl, modifier = Modifier.size(64.dp))
                            if (index < showDishes.size - 1) Spacer(modifier = Modifier.width(8.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "共${dishCount}件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                dishCount in 2..3 -> {
                    val showDishes = orderDishes.mapNotNull { od -> dishDetails[od.dishId] }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        showDishes.forEach { dish ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                DishThumbnail(dishName = dish.name, imageUrl = dish.imageUrl, modifier = Modifier.fillMaxWidth().height(80.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dish.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                dishCount == 1 -> {
                    val dish = dishDetails[orderDishes.first().dishId]
                    if (dish != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DishThumbnail(dishName = dish.name, imageUrl = dish.imageUrl, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = dish.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val qty = orderDishes.first().quantity
                                if (qty > 1) {
                                    Text(
                                        text = "x$qty",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
