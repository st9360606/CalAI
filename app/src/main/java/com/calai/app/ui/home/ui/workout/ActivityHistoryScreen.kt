package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.WorkoutSessionDto

/**
 * ActivityHistoryScreen
 *
 * 目標是 4.jpg：
 * - Top bar: ← Activity History
 * - "Total today: 510 kcal"
 * - 每筆 session: 左綠圓icon + name + "30 min / 141 kcal" + 右邊時間
 *
 * 注意：這個畫面不一定需要 bottom sheet，通常是 full screen page。
 * 你可以把這個畫面掛到 Routes.WORKOUT_HISTORY 之類的 Nav route。
 */
@Composable
fun ActivityHistoryScreen(
    vm: WorkoutViewModel,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = ui.today
    val total = today?.totalKcalToday ?: 0
    val list = today?.sessions ?: emptyList()

    Surface(
        color = Color(0xFF111114),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Activity History",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .padding(start = 4.dp)
                )
            }

            Text(
                text = "Total today: $total kcal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE5E7EB)
                )
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(list) { s: WorkoutSessionDto ->
                    HistoryRow(session = s)
                    HorizontalDivider(
                        color = Color(0xFF374151),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: WorkoutSessionDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 綠色圓icon (目前先用第一個字母 placeholder)
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color(0xFF65A30D),
            contentColor = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\u23F0 ${session.minutes} min", // ⏰
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFE5E7EB)
                    )
                )
                Text(
                    text = "\uD83D\uDD25 ${session.kcal} kcal", // 🔥
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFF87171) // 紅
                    )
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 右邊時間 (e.g. "12:05 PM")
        Text(
            text = session.timeLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFE5E7EB),
                fontWeight = FontWeight.Medium
            )
        )
    }
}
