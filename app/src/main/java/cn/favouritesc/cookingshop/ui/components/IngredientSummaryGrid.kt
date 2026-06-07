package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.data.db.Ingredient

// 食材网格卡片颜色池
private val IngredientColors = listOf(
    Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFE3F2FD),
    Color(0xFFFCE4EC), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
    Color(0xFFFFF8E1), Color(0xFFEFEBE9)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientSummaryGrid(
    ingredientSummary: Map<Ingredient, String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "备菜汇总",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ingredientSummary.entries.forEachIndexed { index, (ingredient, quantity) ->
                    val colorIndex = index % IngredientColors.size
                    val bgColor = IngredientColors[colorIndex]

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IngredientIcon(name = ingredient.name, size = 26, storedIcon = ingredient.icon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = quantity,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
