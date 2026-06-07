package cn.favouritesc.cookingshop.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShimmerBase = Color(0xFFE0E0E0)
private val ShimmerHighlight = Color(0xFFF5F5F5)

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    return Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
fun ShimmerOrderCard(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 模拟订单卡片骨架
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(brush)
        )
    }
}

@Composable
fun ShimmerList(modifier: Modifier = Modifier, count: Int = 5) {
    Column(modifier = modifier) {
        repeat(count) {
            ShimmerOrderCard()
        }
    }
}
