package cn.favouritesc.cookingshop.ui.dish

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.favouritesc.cookingshop.data.db.Dish
import cn.favouritesc.cookingshop.data.db.DishTag
import cn.favouritesc.cookingshop.data.db.TagType
import cn.favouritesc.cookingshop.ui.components.ConfirmDialog
import cn.favouritesc.cookingshop.ui.components.DishThumbnail
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.ShimmerList
import cn.favouritesc.cookingshop.ui.components.StaggeredItem
import cn.favouritesc.cookingshop.ui.theme.Primary

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DishListScreen(viewModel: DishListViewModel, onDishClick: (Long) -> Unit, onAddDishClick: () -> Unit) {
    val filteredDishes by viewModel.filteredDishes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf<Dish?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val hasActiveFilters = selectedTags.isNotEmpty() || searchQuery.isNotBlank()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) viewModel.refreshDishes() }
        lifecycleOwner.lifecycle.addObserver(obs); onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("菜品库") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = Color.White, actionIconContentColor = Color.White),
                actions = { IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Default.Tune, contentDescription = "筛选", tint = if (hasActiveFilters) Color.White.copy(alpha = 0.7f) else Color.White) } })
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddDishClick) { Icon(Icons.Default.Add, "添加菜品") } }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(LightGray)) {
            // 搜索栏
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                OutlinedTextField(searchQuery, { viewModel.updateSearchQuery(it) }, placeholder = { Text("搜索菜品", color = Color(0xFFBDBDBD)) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9E9E9E)) }, trailingIcon = { if (searchQuery.isNotBlank()) IconButton({ viewModel.updateSearchQuery("") }) { Icon(Icons.Default.Close, "清除") } }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent))
            }
            // 筛选标签
            AnimatedVisibility(selectedTags.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedTags.forEach { tag -> FilterChip(true, { viewModel.toggleTag(tag) }, { Text(tag.name, fontSize = 12.sp) }, trailingIcon = { Icon(Icons.Default.Close, contentDescription = "移除", modifier = Modifier.size(14.dp)) }, modifier = Modifier.height(28.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary)) }
                }
                Spacer(Modifier.height(4.dp))
            }
            // 内容
            if (isLoading) ShimmerList(Modifier.padding(12.dp))
            else if (filteredDishes.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { EmptyState("暂无菜品") }
            else LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredDishes) { index, dish -> StaggeredItem(index, 60) { DishCard(dish, { onDishClick(dish.id) }, { showDeleteConfirm = dish }, { onDishClick(dish.id) }) } }
                item { Spacer(Modifier.height(64.dp)) }
            }
        }
    }
    if (showFilterSheet) FilterSheet(availableTags, selectedTags, { viewModel.toggleTag(it) }, { viewModel.clearFilters(); showFilterSheet = false }) { showFilterSheet = false }
    showDeleteConfirm?.let { dish -> ConfirmDialog("删除菜品", "确定要删除 ${dish.name} 吗？", { viewModel.deleteDish(dish); showDeleteConfirm = null }, { showDeleteConfirm = null }) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(availableTags: Map<TagType, List<DishTag>>, selectedTags: List<DishTag>, onTagToggle: (DishTag) -> Unit, onClearAll: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("筛选条件", style = MaterialTheme.typography.titleLarge); TextButton(onClearAll) { Text("清除全部", color = MaterialTheme.colorScheme.error) } }
            Spacer(Modifier.height(12.dp))
            availableTags.filter { it.key != TagType.COOKING_TIME && it.key != TagType.DIFFICULTY }.forEach { (type, tags) ->
                Text(when (type) { TagType.MEAL_TIME -> "餐时"; TagType.TYPE -> "类型"; TagType.CUISINE -> "菜系"; else -> type.name }, style = MaterialTheme.typography.titleSmall, color = Primary, modifier = Modifier.padding(vertical = 6.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { tag -> FilterChip(selectedTags.any { it.id == tag.id }, { onTagToggle(tag) }, { Text(tag.name) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Primary.copy(alpha = 0.12f), selectedLabelColor = Primary)) } }
            }
            Spacer(Modifier.height(16.dp)); Button(onDismiss, Modifier.fillMaxWidth()) { Text("确认") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DishCard(dish: Dish, onClick: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { showMenu = true }), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(130.dp)) {
                DishThumbnail(dish.name, dish.imageUrl, Modifier.fillMaxSize(), 10, 32)
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) { IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.MoreVert, "更多", tint = Color.White, modifier = Modifier.size(18.dp)) } }
            }
            Column(Modifier.padding(10.dp)) {
                Text(dish.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dish.cookingTime?.takeIf { it > 0 }?.let { time ->
                        val preset = listOf(15, 30, 45, 60, 90, 120)
                        val label = if (time in preset) "⏱ ≤${time}min" else "⏱ ${time}min"
                        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF757575), fontSize = 10.sp)
                    }
                    dish.difficulty?.takeIf { it.isNotBlank() }?.let { Text("📊 $it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF757575), fontSize = 10.sp) }
                }
            }
        }
    }
    // 自定义圆角菜单
    if (showMenu) {
        Popup(onDismissRequest = { showMenu = false }, properties = PopupProperties(focusable = true)) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(Modifier.width(120.dp)) {
                    Row(Modifier.fillMaxWidth().clickable { showMenu = false; onEdit() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF616161), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("编辑", style = MaterialTheme.typography.bodyMedium)
                    }
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Color(0xFFEEEEEE)))
                    Row(Modifier.fillMaxWidth().clickable { showMenu = false; onDelete() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("删除", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
