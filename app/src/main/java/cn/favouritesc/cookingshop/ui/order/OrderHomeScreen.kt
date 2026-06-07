package cn.favouritesc.cookingshop.ui.order

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.db.OrderStatus
import cn.favouritesc.cookingshop.ui.components.DishThumbnail
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.components.ShimmerList
import cn.favouritesc.cookingshop.ui.components.StaggeredItem
import cn.favouritesc.cookingshop.ui.theme.Primary
import coil.compose.AsyncImage

private val LightGray = Color(0xFFF5F5F5)
private val PendingColor = Color(0xFFFF8F00)
private val CompletedColor = Color(0xFF4CAF50)
private val PendingBgColor = Color(0xFFFFF3E0)
private val CompletedBgColor = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHomeScreen(
    viewModel: OrderHistoryViewModel,
    onCreateOrderClick: () -> Unit,
    onOrderClick: (Long) -> Unit,
    onHistoryClick: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val orderDishes by viewModel.orderDishes.collectAsState()
    val dishDetails by viewModel.dishDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 当页面恢复可见时刷新数据（跳过首次，init 已加载）
    var skipFirstResume by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipFirstResume) {
                    Log.d("CookingShop", "OrderHomeScreen: skip first ON_RESUME (init already loaded)")
                    skipFirstResume = false
                } else {
                    Log.d("CookingShop", "OrderHomeScreen: ON_RESUME -> loadOrders")
                    viewModel.loadOrders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("点餐") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "历史记录",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateOrderClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新建点餐")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ShimmerList()
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(message = "暂无点餐记录")
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
                // 标题区域 —— 白色卡片化
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
                            Text(
                                text = "近期点餐",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "共 ${orders.size} 单",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 订单列表
                itemsIndexed(orders) { index, order ->
                    StaggeredItem(index = index) {
                        OrderCard(
                        order = order,
                        orderDishes = orderDishes[order.id] ?: emptyList(),
                        dishDetails = dishDetails,
                        onClick = { onOrderClick(order.id) }
                    )
                    }
                }

                // 底部留白给 FAB
                item { Spacer(modifier = Modifier.height(56.dp)) }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    orderDishes: List<OrderDish>,
    dishDetails: Map<Long, Dish>,
    onClick: () -> Unit
) {
    // 解析日期: "2026-06-05" → "6月5日"
    val dateText = try {
        val parts = order.date.split("-")
        if (parts.size == 3) "${parts[1].toInt()}月${parts[2].toInt()}日"
        else order.date
    } catch (_: Exception) { order.date }

    // 解析状态
    val isCompleted = order.statusEnum == OrderStatus.COMPLETED
    val statusText = if (isCompleted) "已完成" else "未完成"
    val statusColor = if (isCompleted) CompletedColor else PendingColor
    val statusBgColor = if (isCompleted) CompletedBgColor else PendingBgColor

    // 获取有图片的菜品列表(最多取前3个)
    val dishesWithImages = orderDishes
        .mapNotNull { od -> dishDetails[od.dishId] }
        .filter { !it.imageUrl.isNullOrBlank() }
    val dishCount = orderDishes.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // ===== 顶部行: 日期 | 时间 | 状态 | 菜数 =====
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
                    // 状态标签
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    // 菜数
                    Text(
                        text = "${dishCount}道菜",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ===== 菜品展示区 =====
            when {
                // >3 个菜品: 显示3张图 + 共XXX件
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
                            if (index < showDishes.size - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
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
                // 2-3 个菜品: 横向排列图片，下方排列菜名
                dishCount in 2..3 -> {
                    val showDishes = orderDishes
                        .mapNotNull { od -> dishDetails[od.dishId] }
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
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                // 1 个菜品: 图片 + 右侧菜名
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
                                // 显示菜品数量（如果多于1份）
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
