package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.favouritesc.cookingshop.ui.theme.Primary

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (isVisible) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Primary
            )
        }
    }
}
