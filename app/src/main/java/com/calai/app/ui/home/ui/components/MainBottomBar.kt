package com.calai.app.ui.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
 * ✅ 共用 BottomBar：Home / Personal(Settings) 都用同一個
 * - 你原本 BottomBar 在 HomeScreen 裡是 private，Personal 用不到，抽出來就能共用
 * - 為了更接近你圖：Personal 這格顯示成 Settings（齒輪）
 */
@Composable
fun MainBottomBar(
    current: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    val barSurface = Color(0xFFF5F5F5)
    val selected = Color(0xFF111114)
    val unselected = Color(0xFF9CA3AF)

    Column(modifier = Modifier.background(barSurface)) {
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
                icon = { Icon(Icons.Filled.Home, null) },
                colors = itemColors()
            )

            NavigationBarItem(
                selected = current == HomeTab.Progress,
                onClick = { onOpenTab(HomeTab.Progress) },
                label = { Text("Progress") },
                icon = { Icon(Icons.Filled.BarChart, null) },
                colors = itemColors()
            )

            NavigationBarItem(
                selected = current == HomeTab.Workout,
                onClick = { onOpenTab(HomeTab.Workout) },
                label = { Text("Workout") },
                icon = { Icon(Icons.Filled.Edit, null) },
                colors = itemColors()
            )

            NavigationBarItem(
                selected = current == HomeTab.Fasting,
                onClick = { onOpenTab(HomeTab.Fasting) },
                label = { Text("Fasting") },
                icon = { Icon(Icons.Filled.AccessTime, null) },
                colors = itemColors()
            )

            // ★ Personal 改成更貼近你圖：Settings（齒輪）
            NavigationBarItem(
                selected = current == HomeTab.Personal,
                onClick = { onOpenTab(HomeTab.Personal) },
                label = { Text("Personal") },
                icon = { Icon(Icons.Filled.Person, null) },
                colors = itemColors()
            )
        }
    }
}
