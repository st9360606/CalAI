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
 */
@Composable
fun MainBottomBar(
    current: HomeTab,
    onOpenTab: (HomeTab) -> Unit
) {
    val barSurface = Color(0xFFF5F5F5)
    val selected = Color(0xFF111114)     // ✅ 選中：全黑
    val unselected = Color(0xFF9CA3AF)   // 未選中：灰

    Column(
        modifier = Modifier
            .background(barSurface)
            .navigationBarsPadding()
    ) {
        NavigationBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            containerColor = barSurface,
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

            @Composable
            fun NavItem(
                tab: HomeTab,
                label: String,
                icon: @Composable (tint: Color) -> Unit
            ) {
                val isSelected = current == tab
                val tint = if (isSelected) selected else unselected

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onOpenTab(tab) },
                    alwaysShowLabel = true,
                    label = { Text(text = label, color = tint) },     // ✅ 強制 label 顏色
                    icon = { icon(tint) },                            // ✅ 強制 icon tint
                    colors = itemColors()
                )
            }

            NavItem(tab = HomeTab.Home, label = "Home") { tint ->
                Icon(Icons.Filled.Home, contentDescription = null, tint = tint)
            }
            NavItem(tab = HomeTab.Progress, label = "Progress") { tint ->
                Icon(Icons.Filled.BarChart, contentDescription = null, tint = tint)
            }
            NavItem(tab = HomeTab.Weight, label = "Weight") { tint ->
                Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = tint)
            }
            NavItem(tab = HomeTab.Fasting, label = "Fasting") { tint ->
                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = tint)
            }
            NavItem(tab = HomeTab.Workout, label = "Workout") { tint ->
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = tint)
            }
        }
    }
}
