package com.calai.app.ui.home.ui.personal.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.ui.home.ui.personal.details.model.AutoGenEvent
import com.calai.app.ui.home.ui.personal.details.model.AutoGenerateGoalsCalcViewModel

@Composable
fun AutoGenerateGoalsCalcScreen(
    onDone: (String) -> Unit,
    onFailToast: (String) -> Unit,
    vm: AutoGenerateGoalsCalcViewModel = hiltViewModel()
) {
    // 進頁就做 commit（只會做一次）
    LaunchedEffect(Unit) {
        vm.startCommitOnce()
    }

    // 監聽結果
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is AutoGenEvent.Success -> onDone(ev.message)
                is AutoGenEvent.Error -> onFailToast(ev.message)
            }
        }
    }

    // UI：loading
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
