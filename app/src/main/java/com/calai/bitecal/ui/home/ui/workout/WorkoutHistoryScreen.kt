package com.calai.bitecal.ui.home.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.bitecal.data.workout.api.WorkoutSessionDto
import com.calai.bitecal.ui.common.CalaiCenteredTopBar
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel

@Composable
fun WorkoutHistoryScreen(
    vm: WorkoutViewModel,
    onBack: () -> Unit,
    currentTab: HomeTab = HomeTab.Workout,
    onOpenTab: (HomeTab) -> Unit = {}
) {

    LaunchedEffect(Unit) {
        vm.init()
        vm.refreshToday()
    }

    val ui = vm.ui.collectAsStateWithLifecycle().value
    val today = ui.today
    val total = today?.totalKcalToday ?: 0
    val list = today?.sessions ?: emptyList()

    LaunchedEffect(ui.saving) {
        if (ui.saving) onBack()
    }

    val surface = Color(0xFFF5F5F5)
    val onSurface = Color(0xFF111114)
    val onSurfaceSecondary = Color(0xFF6B7280)
    val divider = Color(0xFFE5E7EB)

    Scaffold(
        containerColor = surface,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
            ) {
                CalaiCenteredTopBar(
                    title = "Workout History",
                    onBack = onBack,
                    backBackgroundColor = surface,
                    action = {
                        FilledIconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .offset(x = (-8).dp, y = 2.dp)
                                .size(32.dp)
                                .clip(CircleShape),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF111114),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "close",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            MainBottomBar(
                current = currentTab,
                onOpenTab = onOpenTab
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(surface)
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
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = divider
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
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color(0xFFB5B5B5),
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
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFDC2626))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = session.timeLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = metaColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
