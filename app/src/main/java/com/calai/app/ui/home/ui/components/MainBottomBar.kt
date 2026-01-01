package com.calai.app.ui.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calai.app.ui.home.HomeTab

/**
 * 共用 BottomBar：Home / Progress / Weight / Fasting / Workout
 * Personal 入口保留給右上角 user button（不放在 bottom bar）
 */
@Composable
fun MainBottomBar(
    current: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    val barSurface = Color(0xFFF5F5F5)
    val selected = Color(0xFF111114)
    val unselected = Color(0xFF9CA3AF)

    Column(
        modifier = Modifier
            .background(barSurface)
            // 🔑 關鍵：確保整條導覽列在系統導航列上方，不會被遮住
            .navigationBarsPadding()
    ) {
        NavigationBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            containerColor = barSurface,
            contentColor = selected,
            tonalElevation = 0.dp
        ) {
            @Composable
            fun itemColors() = NavigationBarItemDefaults.colors(
                selectedIconColor = selected,
                selectedTextColor = selected,
                unselectedIconColor = unselected,
                unselectedTextColor = unselected,
                indicatorColor = Color.Transparent
            )

            NavigationBarItem(
                selected = current == HomeTab.Home,
                onClick = { onOpenTab(HomeTab.Home) },
                label = { Text("Home") },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = current == HomeTab.Progress,
                onClick = { onOpenTab(HomeTab.Progress) },
                label = { Text("Progress") },
                icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = current == HomeTab.Weight,
                onClick = { onOpenTab(HomeTab.Weight) },
                label = { Text("Weight") },
                icon = { Icon(Icons.Filled.MonitorWeight, contentDescription = null) },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = current == HomeTab.Fasting,
                onClick = { onOpenTab(HomeTab.Fasting) },
                label = { Text("Fasting") },
                icon = { Icon(Icons.Filled.AccessTime, contentDescription = null) },
                colors = itemColors()
            )
            NavigationBarItem(
                selected = current == HomeTab.Workout,
                onClick = { onOpenTab(HomeTab.Workout) },
                label = { Text("Workout") },
                icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) },
                colors = itemColors()
            )
        }
    }
}
