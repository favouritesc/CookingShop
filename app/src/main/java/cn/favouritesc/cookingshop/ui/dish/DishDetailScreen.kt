package cn.favouritesc.cookingshop.ui.dish

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.DishThumbnail
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientIcon
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.theme.Primary

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishDetailScreen(viewModel: DishDetailViewModel, dishId: Long, onBackClick: () -> Unit, onEditClick: (Long) -> Unit) {
    val dish by viewModel.dish.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val dishIngredients by viewModel.dishIngredients.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(dishId) { viewModel.loadDish(dishId) }

    Scaffold(topBar = { CommonTopBar(title = dish?.name ?: "菜品详情", onBackClick = onBackClick, actions = listOf(Icons.Default.Edit to { onEditClick(dishId) })) }) { paddingValues ->
        if (isLoading) LoadingIndicator(Modifier.padding(paddingValues))
        else if (dish == null) EmptyState("菜品不存在", Modifier.padding(paddingValues))
        else LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(LightGray),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 图片卡片
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    DishThumbnail(dish!!.name, dish!!.imageUrl, Modifier.fillMaxWidth().height(220.dp), 12, 36)
                }
            }
            // 基本信息卡片
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(dish!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            dish!!.cookingTime?.takeIf { it > 0 }?.let { time ->
                                val preset = listOf(15, 30, 45, 60, 90, 120)
                                val label = if (time in preset) "⏱ ≤${time}分钟" else "⏱ ${time}分钟"
                                Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF757575))
                            }
                            dish!!.difficulty?.takeIf { it.isNotBlank() }?.let { Text("📊 $it", style = MaterialTheme.typography.bodySmall, color = Color(0xFF757575)) }
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tags.forEach { tag -> Text("#${tag.name}", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
            // 做法卡片
            if (!dish!!.recipe.isNullOrBlank()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("做法", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                            Spacer(Modifier.height(6.dp))
                            Text(dish!!.recipe!!, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF424242))
                        }
                    }
                }
            }
            // 备菜清单
            if (ingredients.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("备菜清单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                            Spacer(Modifier.height(6.dp))
                            ingredients.forEach { ingredient ->
                                val di = dishIngredients.find { it.ingredientId == ingredient.id }
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IngredientIcon(ingredient.name, Modifier.size(24.dp), 24, ingredient.icon)
                                    Spacer(Modifier.width(8.dp))
                                    Text(ingredient.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(di?.quantity ?: "适量", style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
