package com.calai.app.ui.home

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calai.app.R
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.ui.home.components.CalendarStrip
import com.calai.app.ui.home.components.CaloriesCardModern
import com.calai.app.ui.home.components.DonutProgress
import com.calai.app.ui.home.components.MacroRowModern
import com.calai.app.ui.home.components.MealCard
import com.calai.app.ui.home.components.PagerDots
import com.calai.app.ui.home.components.StepsWorkoutRowModern
import com.calai.app.ui.home.model.HomeViewModel
import java.time.LocalDate
import java.util.Locale
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
                Avatar(s.avatarUrl, size = 48.dp, startPadding = 6.dp)  // 放大＋往右一些
                IconButton(onClick = onOpenAlarm) {
                    Icon(
                        painter = painterResource(R.drawable.home_notification), // ← 換成你的 drawable
                        contentDescription = "alarm",
                        modifier = Modifier.size(28.dp)                     // ← 再大一點
                    )
                }
            }

            val today = remember { LocalDate.now() }
            val pastDays = 20
            val futureDays = 1   // 若不想顯示未來任何一天，改成 0
            val days = remember(today) { (-pastDays..futureDays).map { today.plusDays(it.toLong()) } }
            var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
            CalendarStrip(
                days = days,
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                selectedBgCorner = 16.dp   // ← 圓角更圓（原 8.dp）
            )

            // ===== Second block: Pager-like（左右滑）
            TwoPagePager(
                summary = s,
                onAddWater = { vm.onAddWater(it) }
            )

            Spacer(Modifier.height(12.dp))

            StepsWorkoutRowModern(summary = s)

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

@Composable
private fun Avatar(
    url: Uri?,
    size: Dp = 40.dp,
    startPadding: Dp = 0.dp
) {
    val modifier = Modifier
        .padding(start = startPadding) // ← 新增：整體往右一點
        .size(size)                    // ← 新增：支援放大
        .clip(CircleShape)

    if (url == null) {
        Image(
            painter = painterResource(R.drawable.profile),
            contentDescription = "avatar_default",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val ctx = LocalContext.current
        val request = remember(url) {
            ImageRequest.Builder(ctx)
                .data(url)
                .crossfade(false)
                .allowHardware(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = "avatar",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.profile)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwoPagePager(
    summary: HomeSummary,
    onAddWater: (Int) -> Unit
) {
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> {
                    Column {
                        // ↓↓↓ 這裡換成「精簡高度」參數 ↓↓↓
                        CaloriesCardModern(
                            caloriesLeft = summary.tdee,
                            progress = 0f,
                            contentPaddingV = 12.dp, // 原 18.dp → 12.dp
                            ringSize = 76.dp,        // 原 84.dp → 76.dp
                            ringStroke = 9.dp        // 原 12.dp → 9.dp
                        )
                        Spacer(Modifier.height(10.dp))   // 原 12.dp → 10.dp
                        MacroRowModern(summary)          // 保持不動（你的 Macro 高度維持原設定）
                    }
                }
                1 -> {
                    Column {
                        WaterGoalCard(summary, onAddWater)
                        Spacer(Modifier.height(12.dp))
                        ExerciseDiaryCard(summary)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PagerDots(count = pageCount, current = pagerState.currentPage)
        }
    }
}

@Composable
private fun BigTdeeCard(s: HomeSummary) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${s.tdee}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text("Calories left", style = MaterialTheme.typography.bodyMedium)
            }
            DonutProgress(
                progress = 0f, // 若未做「已吃」整合，先顯示 0 進度圈
                modifier = Modifier.size(90.dp)
            )
        }
    }
}

@Composable
private fun StatSmallCard(
    title: String,
    value: String,
    icon: @Composable (() -> Unit)? = null
) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MacroRow(s: HomeSummary) {
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
                    val deltaSigned =
                        -s.weightDiffSigned  // repository 是 current - target，這裡取反得到 target - current
                    val unit = s.weightDiffUnit
                    val valueText =
                        if (unit == "lbs") {
                            val v = deltaSigned.roundToInt() // lbs 取整數
                            "$v $unit"
                        } else {
                            String.format(
                                Locale.getDefault(),
                                "%.1f %s",
                                deltaSigned,
                                unit
                            ) // kg 一位小數
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

@Composable
private fun AssistChip(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun ExerciseDiaryCard(s: HomeSummary) {
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

enum class HomeTab { Home, Progress, Note, Fasting, Personal }

@Composable
private fun BottomBar(
    current: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == HomeTab.Home,
            onClick = { onOpenTab(HomeTab.Home) },
            label = { Text("Home") },
            icon = { Icon(Icons.Filled.Home, null) })
        NavigationBarItem(
            selected = current == HomeTab.Progress,
            onClick = { onOpenTab(HomeTab.Progress) },
            label = { Text("Progress") },
            icon = { Icon(Icons.Filled.BarChart, null) })
        NavigationBarItem(
            selected = current == HomeTab.Note,
            onClick = { onOpenTab(HomeTab.Note) },
            label = { Text("Note") },
            icon = { Icon(Icons.Filled.Edit, null) })
        NavigationBarItem(
            selected = current == HomeTab.Fasting,
            onClick = { onOpenTab(HomeTab.Fasting) },
            label = { Text("Fasting") },
            icon = { Icon(Icons.Filled.AccessTime, null) })
        NavigationBarItem(
            selected = current == HomeTab.Personal,
            onClick = { onOpenTab(HomeTab.Personal) },
            label = { Text("Personal") },
            icon = { Icon(Icons.Filled.Person, null) })
    }
}
