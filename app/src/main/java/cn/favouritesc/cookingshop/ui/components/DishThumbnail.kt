package cn.favouritesc.cookingshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// 15 种柔和色彩，用于无图片时的首字底色
private val PlaceholderColors = listOf(
    Color(0xFFFFCDD2), Color(0xFFF8BBD0), Color(0xFFE1BEE7),
    Color(0xFFD1C4E9), Color(0xFFC5CAE9), Color(0xFFBBDEFB),
    Color(0xFFB3E5FC), Color(0xFFB2EBF2), Color(0xFFB2DFDB),
    Color(0xFFC8E6C9), Color(0xFFDCEDC8), Color(0xFFF0F4C3),
    Color(0xFFFFF9C4), Color(0xFFFFECB3), Color(0xFFFFE0B2),
)

@Composable
fun DishThumbnail(
    dishName: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8 /* dp */,
    fontSize: Int = 16 /* sp */
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = dishName,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp))
        )
    } else {
        // 根据菜名 hashCode 选择底色 + 显示首字
        val colorIndex = kotlin.math.abs(dishName.hashCode()) % PlaceholderColors.size
        val bgColor = PlaceholderColors[colorIndex]
        val firstChar = if (dishName.isNotEmpty()) dishName.take(1) else "?"

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstChar,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}
