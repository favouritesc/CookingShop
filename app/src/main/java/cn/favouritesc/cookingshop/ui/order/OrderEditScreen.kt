package cn.favouritesc.cookingshop.ui.order

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import cn.favouritesc.cookingshop.ui.components.AnimatedQuantity
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.ConfirmDialog
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientSummaryGrid
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import coil.compose.AsyncImage

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditScreen(
    viewModel: OrderEditViewModel,
    orderId: Long,
    onBackClick: () -> Unit,
    onAddDishClick: () -> Unit
) {
    val order by viewModel.order.collectAsState()
    val orderDishes by viewModel.orderDishes.collectAsState()
    val dishDetails by viewModel.dishDetails.collectAsState()
    val ingredientSummary by viewModel.ingredientSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val updatedSuccessfully by viewModel.updatedSuccessfully.collectAsState()
    val deletedSuccessfully by viewModel.deletedSuccessfully.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        Log.d("CookingShop", "OrderEditScreen: LaunchedEffect loadOrder, orderId=$orderId")
        viewModel.loadOrder(orderId)
    }

    LaunchedEffect(updatedSuccessfully) {
        if (updatedSuccessfully) {
            Log.d("CookingShop", "OrderEditScreen: updatedSuccessfully=true -> onBackClick")
            onBackClick()
        }
    }

    LaunchedEffect(deletedSuccessfully) {
        if (deletedSuccessfully) {
            Log.d("CookingShop", "OrderEditScreen: deletedSuccessfully=true -> onBackClick")
            onBackClick()
        }
    }

    Scaffold(
        topBar = { CommonTopBar(title = "编辑点餐", onBackClick = onBackClick) }
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else if (order == null) {
            EmptyState(message = "订单不存在", modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(LightGray)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 订单信息
                    item {
                        val dateText = try {
                            val parts = order!!.date.split("-")
                            if (parts.size == 3) "${parts[1].toInt()}月${parts[2].toInt()}日"
                            else order!!.date
                        } catch (_: Exception) { order!!.date }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "订单信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Column {
                                        Text(text = "日期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = dateText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    }
                                    Column {
                                        Text(text = "时间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = order!!.time, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // 已选菜品标题
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
                                Text(text = "已选菜品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "${orderDishes.size} 道", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                EditDishCard(
                                    dish = dish,
                                    quantity = orderDish.quantity,
                                    onQuantityChange = { viewModel.updateDishQuantity(orderDish.dishId, it) },
                                    onRemove = { viewModel.removeDish(orderDish.dishId) }
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
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // 底部按钮区
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { Log.d("CookingShop", "OrderEditScreen: onAddDishClick"); onAddDishClick() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Text("添加菜品")
                            }
                            Button(
                                onClick = { Log.d("CookingShop", "OrderEditScreen: updateOrder, dishes.size=${orderDishes.size}"); viewModel.updateOrder() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("保存修改")
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
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除订单",
            message = "确定要删除这个订单吗？此操作不可撤销。",
            onConfirm = { Log.d("CookingShop", "OrderEditScreen: delete confirmed"); viewModel.deleteOrder(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun EditDishCard(
    dish: Dish,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = dish.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl, contentDescription = dish.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE))
                )
            } else {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = dish.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { onQuantityChange(quantity - 1) }, modifier = Modifier.size(32.dp)) { Text("-", style = MaterialTheme.typography.titleMedium) }
                AnimatedQuantity(quantity = quantity)
                IconButton(onClick = { onQuantityChange(quantity + 1) }, modifier = Modifier.size(32.dp)) { Text("+", style = MaterialTheme.typography.titleMedium) }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "移除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
