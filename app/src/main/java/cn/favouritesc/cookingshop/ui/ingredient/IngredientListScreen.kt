package cn.favouritesc.cookingshop.ui.ingredient

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.ConfirmDialog
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.IngredientIcon
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.components.StaggeredItem

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientListScreen(
    viewModel: IngredientViewModel,
    onBackClick: () -> Unit
) {
    val ingredientsByCategory by viewModel.ingredientsByCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val editingIngredient by viewModel.editingIngredient.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = { CommonTopBar(title = "备菜管理", onBackClick = onBackClick) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showEditDialog() }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "添加备菜")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator()
        } else if (ingredientsByCategory.isEmpty()) {
            EmptyState(message = "暂无备菜", modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(LightGray),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var globalIndex = 0
                ingredientsByCategory.forEach { (category, ingredients) ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${ingredients.size} 种",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ingredients.forEach { ingredient ->
                                        IngredientChip(
                                            ingredient = ingredient,
                                            onEdit = { viewModel.showEditDialog(ingredient) },
                                            onDelete = { viewModel.deleteIngredient(ingredient) }
                                        )
                                    }
                                }
                            }
                        }
                        globalIndex++
                    }
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }
    }

    if (showEditDialog) {
        IngredientEditDialog(
            ingredient = editingIngredient,
            categories = categories,
            onSave = { name, categoryId, icon -> viewModel.saveIngredient(name, categoryId, icon) },
            onDismiss = { viewModel.hideEditDialog() }
        )
    }
}

@Composable
private fun IngredientChip(
    ingredient: Ingredient,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F8F8))
            .clickable { if (!ingredient.isDefault) onEdit() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IngredientIcon(name = ingredient.name, size = 28, storedIcon = ingredient.icon)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!ingredient.isDefault) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除备菜",
            message = "确定要删除 ${ingredient.name} 吗？",
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
