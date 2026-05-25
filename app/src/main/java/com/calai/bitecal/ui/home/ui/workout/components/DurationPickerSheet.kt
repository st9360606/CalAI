package com.calai.bitecal.ui.home.ui.workout.components

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.i18n.LanguageManager
import com.calai.bitecal.i18n.ProvideComposeLocale
import com.calai.bitecal.ui.home.components.ScrollingNumberWheel
import java.util.Locale

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
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val sheetHeight = (screenHeightDp * 0.62f).dp

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }

    val rowItemHeight = 48.dp
    val visibleCount = 5
    val wheelAreaHeight = rowItemHeight * visibleCount

    val scrollState = rememberScrollState()

    val titleText = presetNameResId?.let { localizedStringResource(localeTag, it) }
        ?: fallbackPresetName
    val subtitleText = localizedStringResource(localeTag, R.string.workout_duration_sheet_subtitle)
    val hourText = localizedStringResource(localeTag, R.string.workout_duration_hour_short)
    val minuteText = localizedStringResource(localeTag, R.string.workout_duration_minute_short)
    val saveText = localizedStringResource(localeTag, R.string.workout_duration_save)
    val cancelText = localizedStringResource(localeTag, R.string.workout_duration_cancel)

    key(localeTag, titleText, subtitleText, hourText, minuteText, saveText, cancelText) {
        ProvideComposeLocale(localeTag) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { onCancel() },
                dragHandle = null,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                containerColor = Color.White,
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
                            .verticalScroll(scrollState)
                            .padding(start = 24.dp, end = 24.dp, top = 35.dp, bottom = 180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = titleText,
                            color = Color(0xFF111114),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = subtitleText,
                            color = Color(0xFF6B7280),
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(wheelAreaHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .height(rowItemHeight)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFF2F2F2))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ScrollingNumberWheel(
                                    value = hours,
                                    range = 0..12,
                                    onValueChange = { hours = it },
                                    textColor = Color(0xFF111114)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = hourText,
                                    color = Color(0xFF6B7280),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(24.dp))
                                ScrollingNumberWheel(
                                    value = minutes,
                                    range = 0..59,
                                    onValueChange = { minutes = it },
                                    textColor = Color(0xFF111114)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = minuteText,
                                    color = Color(0xFF6B7280),
                                    fontSize = 19.sp,
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
                            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                val total = hours * 60 + minutes
                                if (total > 0) {
                                    onSaveMinutes(total)
                                }
                            },
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
                            onClick = onCancel,
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
