package com.calai.app.ui.home.ui.workout

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.data.workout.api.WorkoutSessionDto
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    vm: WorkoutViewModel,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val today = ui.today
    val total = today?.totalKcalToday ?: 0
    val list = today?.sessions ?: emptyList()

    val surface = Color.White
    val onSurface = Color(0xFF111114)
    val onSurfaceSecondary = Color(0xFF6B7280) // 次要文字灰
    val divider = Color(0xFFE5E7EB)

    Scaffold(
        containerColor = surface,
        topBar = {
            // 置中標題 + 輕微下移 6dp（icon 與標題一起）
            CenterAlignedTopAppBar(
                colors = centerAlignedTopAppBarColors(
                    containerColor = surface,
                    navigationIconContentColor = onSurface,
                    titleContentColor = onSurface,
                    actionIconContentColor = onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Activity History",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Total today: $total kcal",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = onSurface
                )
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list) { s: WorkoutSessionDto ->
                    HistoryRow(
                        session = s,
                        nameColor = onSurface,
                        metaColor = onSurfaceSecondary
                    )
                    Divider(
                        color = divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    session: WorkoutSessionDto,
    nameColor: Color,
    metaColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 左側綠色圓（placeholder）
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color(0xFF84CC16),
            contentColor = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor
                )
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "\u23F0 ${session.minutes} min",
                    style = MaterialTheme.typography.bodyMedium.copy(color = metaColor)
                )
                Text(
                    text = "\uD83D\uDD25 ${session.kcal} kcal",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDC2626)) // 紅
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 右邊時間（由後端給 "h:mm am/pm"）
        Text(
            text = session.timeLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = metaColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
