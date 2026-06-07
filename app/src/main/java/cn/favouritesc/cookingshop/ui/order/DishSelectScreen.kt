package cn.favouritesc.cookingshop.ui.order

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.StateFlow

interface DishSelectViewModel {
    val availableDishes: StateFlow<List<Dish>>
    val selectedDishes: StateFlow<List<OrderDish>>
    val isLoading: StateFlow<Boolean>
    fun addDish(dish: Dish)
}

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishSelectScreen(
    viewModel: DishSelectViewModel,
    onBackClick: () -> Unit,
    onDishesSelected: () -> Unit
) {
    val availableDishes by viewModel.availableDishes.collectAsState()
    val selectedDishes by viewModel.selectedDishes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedDishes by remember { mutableStateOf(selectedDishes.map { it.dishId }.toSet()) }

    val filteredDishes = if (searchQuery.isBlank()) {
        availableDishes
    } else {
        availableDishes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = { CommonTopBar(title = "选择菜品", onBackClick = onBackClick) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val originalDishIds = selectedDishes.map { it.dishId }.toSet()
                    Log.d("CookingShop", "DishSelect FAB: temp=$tempSelectedDishes, original=$originalDishIds, vm=${viewModel.javaClass.simpleName}")
                    var addedCount = 0
                    tempSelectedDishes.forEach { dishId ->
                        if (dishId !in originalDishIds) {
                            val dish = availableDishes.find { it.id == dishId }
                            if (dish != null) {
                                Log.d("CookingShop", "DishSelect: add dish ${dish.name}(id=$dishId)")
                                viewModel.addDish(dish)
                                addedCount++
                            } else {
                                Log.d("CookingShop", "DishSelect: dishId=$dishId NOT in availableDishes(${availableDishes.size})")
                            }
                        }
                    }
                    Log.d("CookingShop", "DishSelect FAB done, added=$addedCount")
                    onDishesSelected()
                }
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "确认选择")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(LightGray)
        ) {
            // 搜索框 — 白色卡片包裹
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索菜品") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            if (isLoading) {
                LoadingIndicator()
            } else if (filteredDishes.isEmpty()) {
                EmptyState(message = "暂无菜品")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDishes) { dish ->
                        DishSelectItem(
                            dish = dish,
                            isSelected = tempSelectedDishes.contains(dish.id),
                            onToggle = {
                                tempSelectedDishes = if (tempSelectedDishes.contains(dish.id)) {
                                    tempSelectedDishes - dish.id
                                } else {
                                    tempSelectedDishes + dish.id
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DishSelectItem(dish: Dish, isSelected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color.White
        ),
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
                    model = imageUrl, contentDescription = dish.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE))
                )
            } else {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dish.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                dish.recipe?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    }
}
