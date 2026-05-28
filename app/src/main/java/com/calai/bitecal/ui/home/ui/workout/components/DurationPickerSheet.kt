package com.calai.bitecal.ui.home.ui.workout.components

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.i18n.LanguageManager
import com.calai.bitecal.i18n.ProvideComposeLocale
import com.calai.bitecal.ui.common.haptic.HapticWheelTickEffect
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerSheet(
    @StringRes presetNameResId: Int?,
    fallbackPresetName: String,
    localeTag: String,
    onSaveMinutes: (Int) -> Unit,
    onCancel: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden
        }
    )
) {
    val sheetHeight = 546.dp

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }

    val rowItemHeight = 44.dp
    val wheelAreaHeight = 260.dp



    val titleText = presetNameResId?.let { localizedStringResource(localeTag, it) }
        ?: fallbackPresetName
    val subtitleText = localizedStringResource(localeTag, R.string.workout_duration_sheet_subtitle)
    val hourText = localizedStringResource(localeTag, R.string.workout_duration_hour_short)
    val minuteText = localizedStringResource(localeTag, R.string.workout_duration_minute_short)
    val saveText = localizedStringResource(localeTag, R.string.workout_duration_save)
    val cancelText = localizedStringResource(localeTag, R.string.workout_duration_cancel)

    val saveClick = rememberClickWithHaptic {
        val safeMinutes = if (hours == 24) 0 else minutes
        val total = hours * 60 + safeMinutes

        if (total > 0 && total <= 24 * 60) {
            onSaveMinutes(total)
        }
    }
    val cancelClick = rememberClickWithHaptic(onClick = onCancel)

    key(localeTag, titleText, subtitleText, hourText, minuteText, saveText, cancelText) {
        ProvideComposeLocale(localeTag) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { onCancel() },
                dragHandle = null,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = Color(0xFFF5F5F5),
                tonalElevation = 0.dp,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 170.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .width(42.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFD1D1D6))
                        )
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF111114),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(wheelAreaHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            DurationSelectionBandBehind(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .height(rowItemHeight)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DurationWheelColumn(
                                    values = (0..24).map { "%02d".format(it) },
                                    startIndex = hours.coerceIn(0, 24),
                                    columnWidth = 60.dp,
                                    onSnapped = { index -> hours = index },
                                    infinite = true
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = hourText,
                                    color = Color(0xFF6B7280),
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(24.dp))
                                DurationWheelColumn(
                                    values = (0..59).map { "%02d".format(it) },
                                    startIndex = minutes.coerceIn(0, 59),
                                    columnWidth = 60.dp,
                                    onSnapped = { index -> minutes = index },
                                    infinite = true
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = minuteText,
                                    color = Color(0xFF6B7280),
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.height(42.dp))
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = saveClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = saveText,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = cancelClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFE1E4EA),
                                contentColor = Color(0xFF111114)
                            )
                        ) {
                            Text(
                                text = cancelText,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationWheelColumn(
    values: List<String>,
    startIndex: Int,
    columnWidth: Dp,
    onSnapped: (index: Int) -> Unit,
    infinite: Boolean,
    selectedFontSize: TextUnit = 22.sp,
    unselectedFontSize: TextUnit = 21.sp,
    selectedFontWeight: FontWeight = FontWeight.Bold,
    unselectedFontWeight: FontWeight = FontWeight.Normal
) {
    val visibleCount = 5
    val itemHeight = 44.dp

    val total: Int
    val initialIndex: Int
    val normalize: (Int) -> Int

    if (infinite) {
        val loop = 1000
        total = values.size * loop
        val base = (loop / 2) * values.size
        initialIndex = (base + startIndex).coerceIn(0, total - 1)
        normalize = { index -> ((index % values.size) + values.size) % values.size }
    } else {
        total = values.size
        initialIndex = startIndex.coerceIn(0, total - 1)
        normalize = { index -> index.coerceIn(0, total - 1) }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )
    val flingBehavior = rememberSnapFlingBehavior(
        lazyListState = listState
    )

    val centerListIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                return@derivedStateOf initialIndex
            }

            val viewportCenter =
                (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }?.index ?: initialIndex
        }
    }

    HapticWheelTickEffect(
        tickKey = normalize(centerListIndex),
        enabled = listState.isScrollInProgress
    )

    LaunchedEffect(listState.isScrollInProgress, centerListIndex, startIndex) {
        if (!listState.isScrollInProgress) {
            val normalizedIndex = normalize(centerListIndex)
            if (normalizedIndex != startIndex) {
                onSnapped(normalizedIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(
            vertical = itemHeight * (visibleCount / 2)
        ),
        modifier = Modifier
            .width(columnWidth)
            .height(itemHeight * visibleCount),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(total) { index ->
            val normalizedIndex = normalize(index)
            val isCenter = index == centerListIndex

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = values[normalizedIndex],
                    fontSize = if (isCenter) selectedFontSize else unselectedFontSize,
                    fontWeight = if (isCenter) selectedFontWeight else unselectedFontWeight,
                    color = if (isCenter) {
                        Color(0xFF111114)
                    } else {
                        Color(0xFF8E8E93)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
private fun DurationSelectionBandBehind(
    modifier: Modifier = Modifier
) {
    val bandHeight = 44.dp
    val bandRadius = 10.dp
    val bandColor = Color(0xFFFAFAFA)
    val lineColor = Color(0xFFD1D1D6)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .height(bandHeight)
                .clip(RoundedCornerShape(bandRadius))
                .background(bandColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -bandHeight / 2)
                .fillMaxWidth(0.92f)
                .height(1.dp)
                .background(lineColor)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = bandHeight / 2)
                .fillMaxWidth(0.92f)
                .height(1.dp)
                .background(lineColor)
        )
    }
}
@Composable
private fun localizedStringResource(
    localeTag: String,
    @StringRes resId: Int
): String {
    val context = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val normalizedTag = remember(localeTag) { LanguageManager.normalizeTag(localeTag) }
    val localizedContext = rememberLocalizedContext(
        context = context,
        baseConfiguration = baseConfiguration,
        localeTag = normalizedTag
    )

    return remember(localizedContext, resId) {
        localizedContext.getString(resId)
    }
}

@Composable
private fun rememberLocalizedContext(
    context: Context,
    baseConfiguration: Configuration,
    localeTag: String
): Context {
    return remember(context, baseConfiguration, localeTag) {
        val locale = Locale.forLanguageTag(localeTag)
        val localizedConfiguration = Configuration(baseConfiguration).apply {
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(localizedConfiguration)
    }
}
