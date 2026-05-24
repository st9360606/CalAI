package com.calai.bitecal.ui.home.ui.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerHost(
    vm: WorkoutViewModel,
    visible: Boolean,
    localeTag: String,
    onCloseFull: () -> Unit,
    onCollapseOnly: () -> Unit,
    @Suppress("UNUSED_PARAMETER") sheetState: SheetState? = null
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (ui.presets.isEmpty() || ui.today == null) {
            vm.init()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        key(localeTag) {
            WorkoutTrackerSheet(
                vm = vm,
                visible = visible,
                localeTag = localeTag,
                onClose = { onCloseFull() },
                onCollapse = { onCollapseOnly() }
            )
        }
    }
}
