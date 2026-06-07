package cn.favouritesc.cookingshop.ui.order

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.Order
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.db.OrderStatus
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.DatePickerButton
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.theme.Primary
import coil.compose.AsyncImage

private val LightGray = Color(0xFFF5F5F5)
private val PendingColor = Color(0xFFFF8F00)
private val CompletedColor = Color(0xFF4CAF50)
private val PendingBgColor = Color(0xFFFFF3E0)
private val CompletedBgColor = Color(0xFFE8F5E9)
private val TimelineDot = Primary
private val TimelineLine = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    onBackClick: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val dateRange by viewModel.dateRange.collectAsState()
    val customStartDate by viewModel.customStartDate.collectAsState()
    val customEndDate by viewModel.customEndDate.collectAsState()
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
                    Log.d("CookingShop", "OrderHistory: skip first ON_RESUME")
                    skipFirstResume = false
                } else {
                    Log.d("CookingShop", "OrderHistory: ON_RESUME -> loadOrders")
                    viewModel.loadOrders()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 按日期分组
    val groupedOrders = orders.groupBy { it.date }

    Scaffold(
        topBar = { CommonTopBar(title = "点餐历史", onBackClick = onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(LightGray)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 日期范围选择
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "日期范围", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = dateRange == DateRange.ONE_DAY, onClick = { viewModel.updateDateRange(DateRange.ONE_DAY) }, label = { Text("最近1天") })
                                FilterChip(selected = dateRange == DateRange.SEVEN_DAYS, onClick = { viewModel.updateDateRange(DateRange.SEVEN_DAYS) }, label = { Text("最近7天") })
                                FilterChip(selected = dateRange == DateRange.THIRTY_DAYS, onClick = { viewModel.updateDateRange(DateRange.THIRTY_DAYS) }, label = { Text("最近30天") })
                                FilterChip(selected = dateRange == DateRange.CUSTOM, onClick = { viewModel.updateDateRange(DateRange.CUSTOM) }, label = { Text("自定义") })
                            }
                            if (dateRange == DateRange.CUSTOM) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DatePickerButton(selectedDate = customStartDate, onDateSelected = { viewModel.updateCustomStartDate(it) }, modifier = Modifier.weight(1f), label = "开始日期")
                                    DatePickerButton(selectedDate = customEndDate, onDateSelected = { viewModel.updateCustomEndDate(it) }, modifier = Modifier.weight(1f), label = "结束日期")
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item { LoadingIndicator() }
                } else if (orders.isEmpty()) {
                    item { EmptyState(message = "暂无点餐记录") }
                } else {
                    // 时间线
                    groupedOrders.entries.forEachIndexed { groupIdx, (date, dateOrders) ->
                        val dateText = try {
                            val parts = date.split("-")
                            if (parts.size == 3) "${parts[1].toInt()}月${parts[2].toInt()}日"
                            else date
                        } catch (_: Exception) { date }

                        // 日期标题
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 时间线圆点
                                Canvas(modifier = Modifier.size(14.dp)) {
                                    drawCircle(color = TimelineDot, radius = 7.dp.toPx(), center = Offset(7.dp.toPx(), 7.dp.toPx()))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "共 ${dateOrders.size} 单",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 日期下的订单列表
                        itemsIndexed(dateOrders) { orderIdx, order ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                    // 时间线竖线区域
                                    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.TopCenter) {
                                        // 竖线（非最后一个订单才画）
                                        if (orderIdx < dateOrders.size - 1 || groupIdx < groupedOrders.size - 1) {
                                            Canvas(modifier = Modifier.fillMaxSize().padding(start = 6.dp)) {
                                                drawLine(
                                                    color = TimelineLine,
                                                    start = Offset(0f, 0f),
                                                    end = Offset(0f, size.height),
                                                    strokeWidth = 2.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // 订单卡片
                                    TimelineOrderCard(
                                        order = order,
                                        orderDishes = orderDishes[order.id] ?: emptyList(),
                                        dishDetails = dishDetails,
                                        onClick = { onOrderClick(order.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TimelineOrderCard(
    order: Order,
    orderDishes: List<OrderDish>,
    dishDetails: Map<Long, Dish>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = order.statusEnum == OrderStatus.COMPLETED
    val statusText = if (isCompleted) "已完成" else "未完成"
    val statusColor = if (isCompleted) CompletedColor else PendingColor
    val statusBgColor = if (isCompleted) CompletedBgColor else PendingBgColor
    val dishCount = orderDishes.size

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = order.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusText, style = MaterialTheme.typography.labelSmall, color = statusColor,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(statusBgColor).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(text = "${dishCount}道菜", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (dishCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    dishCount > 3 -> {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val showDishes = orderDishes.mapNotNull { od -> dishDetails[od.dishId] }.take(3)
                            showDishes.forEachIndexed { index, dish ->
                                TimelineDishThumb(dish = dish, modifier = Modifier.size(48.dp))
                                if (index < showDishes.size - 1) Spacer(modifier = Modifier.width(6.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "共${dishCount}件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                    dishCount == 1 -> {
                        val dish = dishDetails[orderDishes.first().dishId]
                        if (dish != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TimelineDishThumb(dish = dish, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = dish.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                val qty = orderDishes.first().quantity
                                if (qty > 1) Text(text = " x$qty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = orderDishes.mapNotNull { od -> dishDetails[od.dishId]?.name }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDishThumb(dish: Dish, modifier: Modifier = Modifier) {
    val imageUrl = dish.imageUrl
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(model = imageUrl, contentDescription = dish.name, contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEEEEEE)))
    } else {
        Box(modifier = modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(18.dp))
        }
    }
}
