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
import cn.favouritesc.cookingshop.ui.components.DatePickerButton
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientSummaryGrid
import cn.favouritesc.cookingshop.ui.components.TimePickerButton
import coil.compose.AsyncImage

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCreateScreen(
    viewModel: OrderCreateViewModel,
    onBackClick: () -> Unit,
    onAddDishClick: () -> Unit,
    onOrderCreated: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val selectedDishes by viewModel.selectedDishes.collectAsState()
    val selectedDishDetails by viewModel.selectedDishDetails.collectAsState()
    val ingredientSummary by viewModel.ingredientSummary.collectAsState()
    val createdSuccessfully by viewModel.createdSuccessfully.collectAsState()

    LaunchedEffect(createdSuccessfully) {
        if (createdSuccessfully) {
            Log.d("CookingShop", "OrderCreateScreen: createdSuccessfully=true -> onOrderCreated")
            onOrderCreated()
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(title = "创建点餐", onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(LightGray)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 点餐信息卡片（日期+时间）
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "点餐信息",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                DatePickerButton(
                                    selectedDate = selectedDate,
                                    onDateSelected = { viewModel.updateDate(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                TimePickerButton(
                                    selectedTime = selectedTime,
                                    onTimeSelected = { viewModel.updateTime(it) },
                                    modifier = Modifier.weight(1f)
                                )
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
                            Text(
                                text = "已选菜品",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedDishes.size} 道",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 菜品列表
                if (selectedDishes.isEmpty()) {
                    item { EmptyState(message = "暂未选择菜品") }
                } else {
                    items(selectedDishes) { orderDish ->
                        val dish = selectedDishDetails.find { it.id == orderDish.dishId }
                        if (dish != null) {
                            CreateDishCard(
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Log.d("CookingShop", "OrderCreateScreen: onAddDishClick")
                            onAddDishClick()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Text("添加菜品")
                    }
                    Button(
                        onClick = {
                            Log.d("CookingShop", "OrderCreateScreen: createOrder, dishes.size=${selectedDishes.size}")
                            viewModel.createOrder()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedDishes.isNotEmpty()
                    ) {
                        Text("发布点餐")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDishCard(
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
            // 缩略图
            val imageUrl = dish.imageUrl
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = dish.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            // 菜名
            Text(
                text = dish.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // 数量控件
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) { Text("-", style = MaterialTheme.typography.titleMedium) }
                AnimatedQuantity(quantity = quantity)
                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "移除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
