package cn.favouritesc.cookingshop.ui.ingredient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.ui.components.CommonTopBar
import cn.favouritesc.cookingshop.ui.components.EmptyState
import cn.favouritesc.cookingshop.ui.components.LoadingIndicator
import cn.favouritesc.cookingshop.ui.order.DateRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IngredientSummaryScreen(
    viewModel: IngredientSummaryViewModel,
    onBackClick: () -> Unit
) {
    val dateRange by viewModel.dateRange.collectAsState()
    val ingredientSummary by viewModel.ingredientSummary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "备菜汇总",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 日期范围选择
            Text(
                text = "选择日期范围",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = dateRange == DateRange.ONE_DAY,
                    onClick = { viewModel.updateDateRange(DateRange.ONE_DAY) },
                    label = { Text("最近1天") }
                )
                FilterChip(
                    selected = dateRange == DateRange.SEVEN_DAYS,
                    onClick = { viewModel.updateDateRange(DateRange.SEVEN_DAYS) },
                    label = { Text("最近7天") }
                )
                FilterChip(
                    selected = dateRange == DateRange.THIRTY_DAYS,
                    onClick = { viewModel.updateDateRange(DateRange.THIRTY_DAYS) },
                    label = { Text("最近30天") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LoadingIndicator()
            } else if (ingredientSummary.isEmpty()) {
                EmptyState(message = "暂无备菜需求")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ingredientSummary.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(items) { item ->
                            IngredientSummaryItemCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientSummaryItemCard(item: IngredientSummaryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.ingredient.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.totalQuantity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "来源: ${item.sourceDishes.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
