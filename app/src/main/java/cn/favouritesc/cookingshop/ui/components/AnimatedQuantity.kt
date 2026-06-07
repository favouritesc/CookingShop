package cn.favouritesc.cookingshop.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

/**
 * 带翻滚动效的数字显示。
 * 数字变化时从上方滑入、向下方滑出。
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedQuantity(
    quantity: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = quantity,
        transitionSpec = {
            if (targetState > initialState) {
                // 增加：新数字从下方滑入
                (slideInVertically(tween(200)) { it } + fadeIn(tween(200)))
                    .togetherWith(slideOutVertically(tween(200)) { -it } + fadeOut(tween(200)))
            } else {
                // 减少：新数字从上方滑入
                (slideInVertically(tween(200)) { -it } + fadeIn(tween(200)))
                    .togetherWith(slideOutVertically(tween(200)) { it } + fadeOut(tween(200)))
            }
        },
        modifier = modifier
    ) { targetQty ->
        Text(
            text = targetQty.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
