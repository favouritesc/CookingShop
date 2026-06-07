package cn.favouritesc.cookingshop.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import cn.favouritesc.cookingshop.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: List<Pair<ImageVector, () -> Unit>> = emptyList()
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        },
        actions = {
            actions.forEach { (icon, action) ->
                IconButton(onClick = action) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}
