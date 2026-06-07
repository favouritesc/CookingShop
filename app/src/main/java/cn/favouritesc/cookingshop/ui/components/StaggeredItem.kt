package cn.favouritesc.cookingshop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 交错入场动画包装器。
 * 每个 item 以 80ms 的间隔逐个从下方 40dp 滑入 + 淡入。
 *
 * @param index 列表中的位置（从 0 开始）
 * @param staggerDelayMs 每个 item 之间的延迟（ms）
 */
@Composable
fun StaggeredItem(
    index: Int,
    staggerDelayMs: Int = 80,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = index * staggerDelayMs))
                + slideInVertically(
                    animationSpec = tween(durationMillis = 400, delayMillis = index * staggerDelayMs),
                    initialOffsetY = { it / 4 } // 从下方 1/4 高度滑入
                )
    ) {
        content()
    }
}
