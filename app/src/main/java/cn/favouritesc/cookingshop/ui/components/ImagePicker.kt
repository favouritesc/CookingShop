package cn.favouritesc.cookingshop.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ImagePicker(
    imageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "选择图片"
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "菜品图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(8.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
