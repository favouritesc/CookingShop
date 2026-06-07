package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagChip(
    text: String,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = { onSelected(!selected) },
        label = { Text(text = text) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun AssistTagChip(
    text: String,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = { Text(text = text) },
        modifier = modifier,
        trailingIcon = if (onRemove != null) {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}
