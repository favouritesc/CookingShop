package cn.favouritesc.cookingshop.ui.ingredient

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.favouritesc.cookingshop.data.db.Ingredient
import cn.favouritesc.cookingshop.data.db.IngredientCategory
import cn.favouritesc.cookingshop.ui.components.IconCategory
import cn.favouritesc.cookingshop.ui.components.allIconCategories
import cn.favouritesc.cookingshop.ui.components.ingredientIcon
import cn.favouritesc.cookingshop.ui.theme.Primary

private val LightGray = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientEditDialog(
    ingredient: Ingredient?,
    categories: List<IngredientCategory>,
    onSave: (name: String, categoryId: Long, icon: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(ingredient?.name ?: "") }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == ingredient?.categoryId } ?: categories.firstOrNull())
    }
    var expanded by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableStateOf(ingredient?.icon) }
    val iconCategories = remember { allIconCategories() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (ingredient != null) "编辑备菜" else "添加备菜",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("备菜名称") },
                    placeholder = { Text("例如：白菜") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 分类
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { selectedCategory = category; expanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // 图标选择区域
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "选择图标", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    val autoIcon = ingredientIcon(name)
                    Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape)
                            .background(if (selectedIcon == null) Primary.copy(alpha = 0.15f) else Color(0xFFF0F0F0))
                            .border(width = if (selectedIcon == null) 2.dp else 0.dp, color = Primary, shape = CircleShape)
                            .clickable { selectedIcon = null },
                        contentAlignment = Alignment.Center
                    ) { Text(text = autoIcon, fontSize = 14.sp, textAlign = TextAlign.Center) }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "自动", style = MaterialTheme.typography.labelSmall, color = if (selectedIcon == null) Primary else Color(0xFF9E9E9E))
                }
                Spacer(modifier = Modifier.height(6.dp))

                // 分类图标网格
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGray)
                ) {
                    LazyColumn(
                        modifier = Modifier.height(220.dp).padding(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(iconCategories) { category ->
                            Column {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF757575),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    category.icons.forEach { icon ->
                                        val isSelected = selectedIcon == icon
                                        Box(
                                            modifier = Modifier.size(34.dp).clip(CircleShape)
                                                .background(if (isSelected) Primary.copy(alpha = 0.15f) else Color.White)
                                                .border(width = if (isSelected) 2.dp else 0.5.dp, color = if (isSelected) Primary else Color(0xFFE0E0E0), shape = CircleShape)
                                                .clickable { selectedIcon = if (isSelected) null else icon },
                                            contentAlignment = Alignment.Center
                                        ) { Text(text = icon, fontSize = 16.sp, textAlign = TextAlign.Center) }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 按钮
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = { if (name.isNotBlank() && selectedCategory != null) onSave(name, selectedCategory!!.id, selectedIcon) },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() && selectedCategory != null
                    ) { Text("保存") }
                }
            }
        }
    }
}
