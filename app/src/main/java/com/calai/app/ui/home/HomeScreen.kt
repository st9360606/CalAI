package com.calai.app.ui.home

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.calai.app.R
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.ui.home.components.DayItem
import com.calai.app.ui.home.components.DayPillCalendar
import com.calai.app.ui.home.components.DonutProgress
import com.calai.app.ui.home.components.MealCard
import com.calai.app.ui.home.model.HomeViewModel
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenAlarm: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenTab: (HomeTab) -> Unit
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCamera,
                containerColor = Color(0xFF111114),
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "cam") }
        },
        bottomBar = {
            BottomBar(
                current = HomeTab.Home,
                onOpenTab = onOpenTab
            )
        }
    ) { inner ->
        val s = ui.summary ?: return@Scaffold

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ===== Top bar: avatar + bell
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(s.avatarUrl)
                IconButton(onClick = onOpenAlarm) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "alarm")
                }
            }

            // ===== Calendar (一週)
            val week = remember {
                val today = LocalDate.now()
                (-3..3).map { DayItem(today.plusDays(it.toLong())) }
            }
            var selected by remember { mutableStateOf(LocalDate.now()) }
            DayPillCalendar(
                days = week,
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // ===== Second block: Pager-like（左右滑）
            TwoPageCards(
                summary = s,
                onAddWater = { vm.onAddWater(it) }
            )

            Spacer(Modifier.height(12.dp))

            // ===== Third block: 三個小卡
            TripleStats(summary = s, onAddWater = { vm.onAddWater(it) })

            // ===== Fourth block: 最近上傳
            if (s.recentMeals.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.recently_uploaded),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                for (m in s.recentMeals) {
                    MealCard(m)
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable private fun Avatar(url: Uri?) {
    AsyncImage(
        model = url,
        contentDescription = "avatar",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TwoPageCards(
    summary: HomeSummary,
    onAddWater: (Int) -> Unit
) {
    // 這裡用 Row + horizontalScroll 簡單實作左右滑
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Page 1: TDEE + Macro
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            BigTdeeCard(summary)
            Spacer(Modifier.height(12.dp))
            MacroRow(summary)
        }
        // Page 2: 飲水/體重差/斷食 + 運動日記
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .fillMaxWidth()
        ) {
            WaterGoalCard(summary, onAddWater)
            Spacer(Modifier.height(12.dp))
            ExerciseDiaryCard(summary)
        }
    }
}

@Composable private fun BigTdeeCard(s: HomeSummary) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${s.tdee}", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold))
                Text("Calories left", style = MaterialTheme.typography.bodyMedium)
            }
            DonutProgress(
                progress = 0f, // 若未做「已吃」整合，先顯示 0 進度圈
                modifier = Modifier.size(90.dp)
            )
        }
    }
}

@Composable private fun StatSmallCard(
    title: String,
    value: String,
    icon: @Composable (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun MacroRow(s: HomeSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatSmallCard("Protein left", "${s.proteinG}g")
        StatSmallCard("Carbs left", "${s.carbsG}g")
        StatSmallCard("Fats left", "${s.fatG}g")
    }
}

@Composable
private fun WaterGoalCard(s: HomeSummary, onAddWater: (Int) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Water / Weight / Fasting", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // ---- 水分 ----
                Column(Modifier.weight(1f)) {
                    Text(
                        "${s.waterTodayMl}/${s.waterGoalMl} ml",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row {
                        AssistChip("+250 ml") { onAddWater(250) }
                        Spacer(Modifier.width(8.dp))
                        AssistChip("+500 ml") { onAddWater(500) }
                    }
                }

                // ---- 體重差（Δ = target - current；lbs 整數、kg 小數一位）----
                Column(Modifier.weight(1f)) {
                    val deltaSigned = -s.weightDiffSigned  // repository 是 current - target，這裡取反得到 target - current
                    val unit = s.weightDiffUnit
                    val valueText =
                        if (unit == "lbs") {
                            val v = deltaSigned.roundToInt() // lbs 取整數
                            "$v $unit"
                        } else {
                            String.format(Locale.getDefault(), "%.1f %s", deltaSigned, unit) // kg 一位小數
                        }

                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("Δ weight", style = MaterialTheme.typography.bodySmall)
                }

                // ---- 斷食方案 ----
                Column(Modifier.weight(1f)) {
                    Text(
                        s.fastingPlan ?: "—",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("Fasting plan", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun AssistChip(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable private fun ExerciseDiaryCard(s: HomeSummary) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Workout diary", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (s.todayActivity.exerciseMinutes / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${s.todayActivity.activeKcal.toInt()} kcal • ${s.todayActivity.exerciseMinutes} min",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable private fun TripleStats(summary: HomeSummary, onAddWater: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallCircleWithLabel(
            title = "Steps",
            value = "${summary.todayActivity.steps}",
        )
        SmallCircleWithLabel(
            title = "Water today",
            value = "${summary.waterTodayMl} ml",
            trailing = { AssistChip("+250") { onAddWater(250) } }
        )
        SmallCircleWithLabel(
            title = "Workout",
            value = "${summary.todayActivity.activeKcal.toInt()} kcal / ${summary.todayActivity.exerciseMinutes} m"
        )
    }
}

@Composable private fun SmallCircleWithLabel(
    title: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(title, style = MaterialTheme.typography.bodySmall)
            if (trailing != null) {
                Spacer(Modifier.height(6.dp))
                trailing()
            }
        }
    }
}

enum class HomeTab { Home, Progress, Note, Fasting, Personal }

@Composable private fun BottomBar(
    current: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(selected = current==HomeTab.Home,     onClick = { onOpenTab(HomeTab.Home) },     label = { Text("Home") },     icon = { Icon(Icons.Filled.Home, null) })
        NavigationBarItem(selected = current==HomeTab.Progress, onClick = { onOpenTab(HomeTab.Progress) }, label = { Text("Progress") }, icon = { Icon(Icons.Filled.BarChart, null) })
        NavigationBarItem(selected = current==HomeTab.Note,     onClick = { onOpenTab(HomeTab.Note) },     label = { Text("Note") },     icon = { Icon(Icons.Filled.Edit, null) })
        NavigationBarItem(selected = current==HomeTab.Fasting,  onClick = { onOpenTab(HomeTab.Fasting) },  label = { Text("Fasting") },  icon = { Icon(Icons.Filled.AccessTime, null) })
        NavigationBarItem(selected = current==HomeTab.Personal, onClick = { onOpenTab(HomeTab.Personal) }, label = { Text("Personal") }, icon = { Icon(Icons.Filled.Person, null) })
    }
}
