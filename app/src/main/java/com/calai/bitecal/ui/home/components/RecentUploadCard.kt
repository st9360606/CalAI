package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.calai.bitecal.ui.home.model.HomeRecentUploadUi

@Composable
fun RecentUploadCard(
    item: HomeRecentUploadUi,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = CardStyles.Border,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecentUploadThumb(
                previewUri = item.previewUri,
                modifier = Modifier
                    .width(108.dp)
                    .height(100.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
            ) {
                when (item) {
                    is HomeRecentUploadUi.Pending -> PendingContent(item)
                    is HomeRecentUploadUi.Success -> SuccessContent(item)
                }
            }
        }
    }
}

@Composable
private fun RecentUploadThumb(
    previewUri: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (!previewUri.isNullOrBlank()) {
            AsyncImage(
                model = previewUri,
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F2F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "☕", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun PendingContent(item: HomeRecentUploadUi.Pending) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "正在分析食物...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(Modifier.height(10.dp))

        SkeletonBar(widthFraction = 0.52f)
        Spacer(Modifier.height(8.dp))
        SkeletonBar(widthFraction = 0.36f)
        Spacer(Modifier.height(8.dp))
        SkeletonBar(widthFraction = 0.42f)
    }
}

@Composable
private fun SuccessContent(item: HomeRecentUploadUi.Success) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = item.timeText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "🔥 ${item.kcal} 卡路里",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🍗 ${item.proteinG}g", style = MaterialTheme.typography.bodySmall)
            Text("🌾 ${item.carbsG}g", style = MaterialTheme.typography.bodySmall)
            Text("🥑 ${item.fatG}g", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE3E6EC))
    )
}
