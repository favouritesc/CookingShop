package cn.favouritesc.cookingshop.ui.dish

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.favouritesc.cookingshop.data.db.DishIngredient
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.TagType
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.ImagePicker
import cn.favouritesc.cookingshop.ui.components.IngredientIcon
import cn.favouritesc.cookingshop.ui.theme.Primary

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishEditScreen(viewModel: DishEditViewModel, dishId: Long, onBackClick: () -> Unit, onDishSaved: () -> Unit = {}) {
    val ctx = LocalContext.current
    val dishName by viewModel.dishName.collectAsState()
    val recipe by viewModel.recipe.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val selectedIngredients by viewModel.selectedIngredients.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val availableIngredients by viewModel.availableIngredients.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val savedSuccessfully by viewModel.savedSuccessfully.collectAsState()
    val cookingTime by viewModel.cookingTime.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()

    var customCookingTime by remember { mutableStateOf("") }
    var showCustomTime by remember { mutableStateOf(cookingTime > 0 && cookingTime !in listOf(15, 30, 45, 60, 90, 120)) }
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var editQty by remember { mutableStateOf("") }

    LaunchedEffect(dishId) { if (dishId != -1L) viewModel.loadDish(dishId) }
    LaunchedEffect(savedSuccessfully) { if (savedSuccessfully) { viewModel.resetSavedState(); onDishSaved(); onBackClick() } }
    LaunchedEffect(Unit) { if (dishId == -1L) viewModel.resetForm() }

    Scaffold(topBar = { CommonTopBar(title = if (dishId == -1L) "添加菜品" else "编辑菜品", onBackClick = onBackClick, actions = listOf(Icons.Default.Save to { viewModel.saveDish(ctx) })) }) { paddingValues ->
        LazyColumn(Modifier.fillMaxSize().padding(paddingValues).background(LightGray), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 图片
            item { Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) { Box(Modifier.padding(12.dp)) { ImagePicker(imageUri, { viewModel.updateImageUri(it) }) } } }

            // 基本信息
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("基本信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(dishName, { viewModel.updateDishName(it) }, label = { Text("菜品名称") }, placeholder = { Text("例如：宫保鸡丁") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(recipe, { viewModel.updateRecipe(it) }, label = { Text("做法") }, placeholder = { Text("描述这道菜的做法...") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8, shape = RoundedCornerShape(10.dp))
                    }
                }
            }

            // 烹饪时间
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("烹饪时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(15, 30, 45, 60, 90, 120).forEach { min -> FilterChip(cookingTime == min, { viewModel.updateCookingTime(min); showCustomTime = false }, { Text("${min}分钟内") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary)) }
                            FilterChip(showCustomTime, { showCustomTime = true; viewModel.updateCookingTime(0) }, { Text("自定义") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary))
                        }
                        if (showCustomTime) {
                            Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(customCookingTime, { customCookingTime = it; it.toIntOrNull()?.let { m -> viewModel.updateCookingTime(m) } }, label = { Text("自定义分钟") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                                Spacer(Modifier.width(8.dp)); Text("分钟", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF757575))
                            }
                        }
                    }
                }
            }

            // 难度
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("难度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("简单", "中等", "困难").forEach { d -> FilterChip(difficulty == d, { viewModel.updateDifficulty(d) }, { Text(d) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary)) } }
                    }
                }
            }

            // 标签选择
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                        Spacer(Modifier.height(6.dp))
                        availableTags.filter { it.key != TagType.COOKING_TIME && it.key != TagType.DIFFICULTY }.forEach { (type, tags) ->
                            Text(when (type) { TagType.MEAL_TIME -> "餐时"; TagType.TYPE -> "类型"; TagType.CUISINE -> "菜系"; else -> type.name }, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E), modifier = Modifier.padding(vertical = 4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { tag -> FilterChip(selectedTags.any { it.id == tag.id }, { viewModel.toggleTag(tag) }, { Text(tag.name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary)) } }
                        }
                    }
                }
            }

            // 备菜清单（紧凑版，提取为子组件减少重组开销）
            item(key = "ingredients") {
                IngredientSelector(availableIngredients, selectedIngredients, { viewModel.addIngredient(it, "适量") }, { ing -> editingIngredient = ing; editQty = selectedIngredients.find { s -> s.ingredientId == ing.id }?.quantity ?: "" }, { viewModel.removeIngredient(it.id) })
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // 数量编辑弹窗
    editingIngredient?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { editingIngredient = null },
            title = { Text("编辑数量 — ${ingredient.name}") },
            text = { OutlinedTextField(editQty, { editQty = it }, label = { Text("数量") }, placeholder = { Text("例如：200g 或 2个") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp)) },
            confirmButton = { TextButton({ viewModel.addIngredient(ingredient, editQty.ifBlank { "适量" }); editingIngredient = null }) { Text("确定", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ viewModel.removeIngredient(ingredient.id); editingIngredient = null }) { Text("移除", color = MaterialTheme.colorScheme.error) } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientSelector(
    ingredients: List<Ingredient>,
    selected: List<DishIngredient>,
    onSelect: (Ingredient) -> Unit,
    onEdit: (Ingredient) -> Unit,
    onRemove: (Ingredient) -> Unit
) {
    val selectedMap = remember(selected) { selected.associateBy { it.ingredientId } }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("备菜清单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(Modifier.height(4.dp)); Text("点击食材切换选择，已选食材点击可编辑数量", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ingredients.forEach { ingredient ->
                    key(ingredient.id) {
                        val sel = selectedMap[ingredient.id]; val isSelected = sel != null
                        FilterChip(isSelected, { if (isSelected) onEdit(ingredient) else onSelect(ingredient) }, label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IngredientIcon(ingredient.name, Modifier.size(18.dp), 18, ingredient.icon); Spacer(Modifier.width(4.dp))
                                Text(ingredient.name, fontSize = 12.sp)
                                if (isSelected) { Spacer(Modifier.width(2.dp)); Text("·${sel!!.quantity}", fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold) }
                            }
                        }, trailingIcon = if (isSelected) {{ Icon(Icons.Default.Close, "移除", Modifier.size(14.dp), tint = Color(0xFF9E9E9E)) }} else null,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary))
                    }
                }
            }
        }
    }
}
