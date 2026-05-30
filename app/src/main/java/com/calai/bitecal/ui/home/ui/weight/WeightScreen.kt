package com.calai.bitecal.ui.home.ui.weight

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.weight.api.WeightItemDto
import com.calai.bitecal.ui.home.components.toast.DeleteFailedTopToast
import com.calai.bitecal.ui.home.components.toast.DeleteSuccessTopToast
import com.calai.bitecal.ui.home.components.toast.ErrorTopToast
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.home.ui.weight.components.FilterTabs
import com.calai.bitecal.ui.home.ui.weight.components.HistoryRow
import com.calai.bitecal.ui.home.ui.weight.components.SegmentedButtons
import com.calai.bitecal.ui.home.ui.weight.components.WeightChartCard
import com.calai.bitecal.ui.home.ui.weight.components.WeightComponents
import com.calai.bitecal.ui.home.ui.weight.model.WeightViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    vm: WeightViewModel,
    onLogClick: () -> Unit,
    onEditGoalWeight: () -> Unit,
    onBack: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val error = ui.error
    val deleteToastType = ui.deleteToastType
    val deleteToastTick = ui.deleteToastTick

    val historySorted = remember(ui.history7) {
        ui.history7.sortedByDescending { dto ->
            runCatching { LocalDate.parse(dto.logDate) }.getOrNull() ?: LocalDate.MIN
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFF5F5F5),
            topBar = {
                BiteCalTopBar(
                    title = stringResource(R.string.weight_title),
                    onBack = onBack
                )
            },
            bottomBar = {
                BottomLogWeightBar(
                    onLogClick = onLogClick
                )
            }
        ) { inner ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 6.dp,
                        end = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.weight_overview_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )

                            SegmentedButtons(
                                selected = ui.unit,
                                onSelect = { vm.setUnit(it) },
                                width = 108.dp,
                                height = 36.dp,
                                pillExtraWidth = 6.dp,
                                labelPadding = 6.dp
                            )
                        }
                    }

                    item {
                        WeightComponents(ui = ui)
                    }

                    item {
                        FilterTabs(
                            selected = ui.range,
                            onSelect = { vm.setRange(it) }
                        )
                    }

                    item {
                        WeightChartCard(
                            ui = ui,
                            onEditGoalWeight = onEditGoalWeight
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.weight_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = historySorted,
                        key = { _, item -> item.logDate }
                    ) { index, item ->
                        val previousFromHistory = historySorted.getOrNull(index + 1)
                        val previous = previousFromHistory
                            ?: buildProfileWeightFallbackPrevious(
                                ui = ui,
                                current = item
                            )

                        SwipeToDeleteHistoryRow(
                            item = item,
                            unit = ui.unit,
                            previous = previous,
                            deleting = ui.deletingLogDates.contains(item.logDate),
                            onDeleteClick = {
                                vm.deleteHistory(item.logDate)
                            }
                        )
                    }
                }
            }
        }

        when {
            error != null -> {
                ErrorTopToast(
                    message = error,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            deleteToastType == WeightViewModel.DeleteToastType.SUCCESS -> {
                DeleteSuccessTopToast(
                    message = stringResource(R.string.weight_delete_success),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            deleteToastType == WeightViewModel.DeleteToastType.FAILED -> {
                DeleteFailedTopToast(
                    message = stringResource(R.string.weight_delete_failed),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            delay(2_000)
            vm.clearError()
        }
    }

    LaunchedEffect(deleteToastTick, deleteToastType) {
        if (deleteToastType != null) {
            delay(2_000)
            vm.clearDeleteToast()
        }
    }

    // ✅ 防止 toast 還沒自動消失就離開 WeightScreen，回來後又顯示舊 toast。
    DisposableEffect(Unit) {
        onDispose {
            vm.clearError()
            vm.clearDeleteToast()
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SwipeToDeleteHistoryRow(
    item: WeightItemDto,
    unit: UserProfileStore.WeightUnit,
    previous: WeightItemDto?,
    deleting: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val scope = rememberCoroutineScope()
        val actionWidth = maxWidth * 0.25f
        val density = LocalDensity.current
        val actionWidthPx = with(density) { actionWidth.toPx() }
        val openThresholdPx = actionWidthPx * 0.42f
        val flingThresholdPx = with(density) { 380.dp.toPx() }

        val offsetX = remember(item.logDate) { Animatable(0f) }
        val isOpened = offsetX.value < -1f

        val dragState = rememberDraggableState { delta ->
            scope.launch {
                val next = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                offsetX.snapTo(next)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFFE46A6A))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
                    .fillMaxHeight()
                    .padding(end = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = rememberClickWithHaptic {
                        scope.launch { offsetX.snapTo(0f) }
                        onDeleteClick()
                    },
                    enabled = isOpened && !deleting,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("weight_history_delete_button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trash),
                        contentDescription = stringResource(
                            R.string.weight_history_delete_content_description
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            HistoryRow(
                item = item,
                unit = unit,
                previous = previous,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = offsetX.value
                    }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        enabled = !deleting,
                        onDragStarted = {
                            scope.launch { offsetX.stop() }
                        },
                        onDragStopped = { velocity ->
                            val target = when {
                                velocity <= -flingThresholdPx -> -actionWidthPx
                                velocity >= flingThresholdPx -> 0f
                                offsetX.value <= -openThresholdPx -> -actionWidthPx
                                else -> 0f
                            }

                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(
                                        durationMillis = 180,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                    .biteCalClickable(enabled = isOpened) {
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(
                                    durationMillis = 160,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    }
            )
        }
    }
}

private fun buildProfileWeightFallbackPrevious(
    ui: WeightViewModel.UiState,
    current: WeightItemDto
): WeightItemDto? {
    val profileWeightKg = ui.profileWeightKg ?: return null
    val profileWeightLbs = ui.profileWeightLbs
    val currentWeightLbs = current.weightLbs

    val isSameKg = abs(current.weightKg - profileWeightKg) < 0.05
    val isSameLbs = currentWeightLbs != null &&
        profileWeightLbs != null &&
        abs(currentWeightLbs - profileWeightLbs) < 0.05

    if (isSameKg || isSameLbs) return null

    return WeightItemDto(
        logDate = current.logDate,
        weightKg = profileWeightKg,
        weightLbs = profileWeightLbs,
        photoUrl = null
    )
}

@Composable
private fun BottomLogWeightBar(
    onLogClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = rememberClickWithHaptic(onClick = onLogClick),
            modifier = Modifier
                .width(158.dp)
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF111114),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.height(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.weight_log_weight),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
