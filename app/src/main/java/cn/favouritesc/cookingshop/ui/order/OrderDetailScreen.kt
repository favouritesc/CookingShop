package cn.favouritesc.cookingshop.ui.order

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.OrderDish
import cn.favouritesc.cookingshop.data.db.OrderStatus
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.ConfirmDialog
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientSummaryGrid
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.theme.Primary
import coil.compose.AsyncImage

private val LightGray = Color(0xFFF5F5F5)
private val PendingColor = Color(0xFFFF8F00)
private val CompletedColor = Color(0xFF4CAF50)
private val PendingBgColor = Color(0xFFFFF3E0)
private val CompletedBgColor = Color(0xFFE8F5E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderEditViewModel,
    orderId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDishClick: (Long) -> Unit
) {
    val order by viewModel.order.collectAsState()
    val orderDishes by viewModel.orderDishes.collectAsState()
    val dishDetails by viewModel.dishDetails.collectAsState()
    val ingredientSummary by viewModel.ingredientSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val deletedSuccessfully by viewModel.deletedSuccessfully.collectAsState()
    val completedSuccessfully by viewModel.completedSuccessfully.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCompleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        Log.d("CookingShop", "OrderDetailScreen: LaunchedEffect loadOrder, orderId=$orderId")
        viewModel.loadOrder(orderId)
    }

    LaunchedEffect(deletedSuccessfully) {
        if (deletedSuccessfully) {
            Log.d("CookingShop", "OrderDetailScreen: deletedSuccessfully=true, onBackClick")
            onBackClick()
        }
    }

    LaunchedEffect(completedSuccessfully) {
        if (completedSuccessfully) {
            Log.d("CookingShop", "OrderDetailScreen: completedSuccessfully=true, reload order")
            viewModel.loadOrder(orderId)
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "点餐详情",
                onBackClick = onBackClick,
                actions = listOf(
                    Icons.Default.Edit to { onEditClick(orderId) }
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else if (order == null) {
            EmptyState(message = "订单不存在", modifier = Modifier.padding(paddingValues))
        } else {
            val isCompleted = order!!.statusEnum == OrderStatus.COMPLETED
            val statusText = if (isCompleted) "已完成" else "未完成"
            val statusColor = if (isCompleted) CompletedColor else PendingColor
            val statusBgColor = if (isCompleted) CompletedBgColor else PendingBgColor

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(LightGray),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 订单信息卡片
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "订单信息",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusBgColor)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column {
                                    Text(text = "日期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = order!!.date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                                Column {
                                    Text(text = "时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = order!!.time, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // 菜品列表标题
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已选菜品",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${orderDishes.size} 道",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 菜品列表
                if (orderDishes.isEmpty()) {
                    item { EmptyState(message = "暂无菜品") }
                } else {
                    items(orderDishes) { orderDish ->
                        val dish = dishDetails.find { it.id == orderDish.dishId }
                        if (dish != null) {
                            DetailDishCard(
                                dish = dish,
                                quantity = orderDish.quantity,
                                onClick = { onDishClick(dish.id) }
                            )
                        }
                    }
                }

                // 备菜汇总
                if (ingredientSummary.isNotEmpty()) {
                    item {
                        IngredientSummaryGrid(ingredientSummary = ingredientSummary)
                    }
                }

                // 操作按钮
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!isCompleted) {
                                    OutlinedButton(
                                        onClick = { onEditClick(orderId) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                        Text("编辑订单")
                                    }
                                    Button(
                                        onClick = { showCompleteConfirm = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("确认完成")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                                Text("删除订单")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除订单",
            message = "确定要删除这个订单吗？此操作不可撤销。",
            onConfirm = {
                Log.d("CookingShop", "OrderDetailScreen: delete confirmed")
                viewModel.deleteOrder()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showCompleteConfirm) {
        ConfirmDialog(
            title = "确认完成订单",
            message = "确定要将此订单标记为已完成吗？完成后将无法编辑。",
            onConfirm = {
                Log.d("CookingShop", "OrderDetailScreen: complete confirmed")
                viewModel.completeOrder()
                showCompleteConfirm = false
            },
            onDismiss = { showCompleteConfirm = false }
        )
    }
}

@Composable
private fun DetailDishCard(
    dish: Dish,
    quantity: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 菜品缩略图
            val imageUrl = dish.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = dish.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dish.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                dish.recipe?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(text = "x$quantity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
    }
}
