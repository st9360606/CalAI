package com.calai.bitecal.ui.home.ui.settings.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.data.profile.api.UserProfileDto
import com.calai.bitecal.data.profile.repo.UserProfileStore
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    profile: UserProfileDto?,
    unit: UserProfileStore.WeightUnit,
    goalKgFromWeightVm: Double? = null,
    goalLbsFromWeightVm: Double? = null,
    currentKgFromTimeseries: Double? = null,
    currentLbsFromTimeseries: Double? = null,
    onBack: () -> Unit,
    onChangeGoal: () -> Unit,
    onEditCurrentWeight: () -> Unit = {},
    onEditHeight: () -> Unit = {},
    onEditAge: () -> Unit = {},
    onEditGender: () -> Unit = {},
    onEditDailyStepGoal: () -> Unit = {},
    onEditStartingWeight: () -> Unit = {},
    onEditDailyWaterGoal: () -> Unit = {},
    onEditDailyWorkoutGoal: () -> Unit = {},
) {
    val bg = Color(0xFFF6F7F9)
    val cardShape = RoundedCornerShape(22.dp)
    val outline = Color(0xFFE5E7EB)
    val scroll = rememberScrollState()
    val contentMaxWidth = 520.dp
    val titleSize = 15.sp
    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Personal details",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111114),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.height(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = bg),
                modifier = Modifier.statusBarsPadding()
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = contentMaxWidth)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 20.dp)
                    .navigationBarsPadding()
            ) {
                // ===== Goal Weight Card =====
                val (goalMain, _) = formatWeightBothLines(
                    kg = goalKgFromWeightVm ?: profile?.goalWeightKg,
                    lbs = goalLbsFromWeightVm ?: profile?.goalWeightLbs,
                    unit = unit
                )

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, outline),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Goal Weight",
                                fontSize = titleSize,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF374151)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = goalMain,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold),
                                color = Color(0xFF111114)
                            )
                        }

                        Button(
                            onClick = onChangeGoal,
                            modifier = Modifier
                                .height(28.dp)
                                .width(93.dp), // ✅ 改小：88/84/80 自己調
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111114),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Change Goal",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ===== List Card =====
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, outline),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val (curMain, _) = formatWeightBothLines(
                        kg = currentKgFromTimeseries ?: profile?.weightKg,
                        lbs = currentLbsFromTimeseries ?: profile?.weightLbs,
                        unit = unit
                    )

                    PersonalDetailsRow(
                        title = "Current Weight",
                        valueMain = curMain,
                        titleOffsetY = 2.dp,
                        valueOffsetY = 2.dp,
                        titleFontSize = titleSize,
                        onClick = onEditCurrentWeight
                    )

                    PersonalRowDivider(outline)

                    PersonalDetailsRow(
                        title = "Height",
                        titleOffsetY = 1.dp,
                        valueOffsetY = 1.dp,
                        titleFontSize = titleSize,
                        valueMain = formatHeight(profile),
                        onClick = onEditHeight
                    )

                    PersonalRowDivider(outline)

                    PersonalDetailsRow(
                        title = "Age",
                        titleOffsetY = 1.dp,
                        valueOffsetY = 1.dp,
                        titleFontSize = titleSize,
                        valueMain = profile?.age?.let { "$it years" } ?: "—",
                        onClick = onEditAge
                    )

                    PersonalRowDivider(outline)

                    PersonalDetailsRow(
                        title = "Gender",
                        titleOffsetY = 1.dp,
                        valueOffsetY = 1.dp,
                        titleFontSize = titleSize,
                        valueMain = formatGender(profile?.gender),
                        onClick = onEditGender
                    )

                    val (startMain,  _) = formatWeightBothLines(
                        kg = profile?.weightKg,
                        lbs = profile?.weightLbs,
                        unit = unit
                    )

                    PersonalRowDivider(outline)

                    PersonalDetailsRow(
                        title = "Starting Weight",
                        valueMain = startMain,
                        titleOffsetY = 1.dp,
                        valueOffsetY = 1.dp,
                        titleFontSize = titleSize,
                        onClick = onEditStartingWeight
                    )

                    PersonalRowDivider(outline)

                    val stepText = profile?.dailyStepGoal?.let { "$it steps" } ?: "—"
                    PersonalDetailsRow(
                        title = "Daily Step Goal",
                        valueMain = stepText,
                        titleOffsetY = (-2).dp,
                        valueOffsetY = (-2).dp,
                        titleFontSize = titleSize,
                        onClick = onEditDailyStepGoal
                    )

                    PersonalRowDivider(outline)

                    val waterText = profile?.waterMl?.let { "$it ml" } ?: "—"
                    PersonalDetailsRow(
                        title = "Daily Water Goal",
                        valueMain = waterText,
                        titleOffsetY = (-2).dp,
                        valueOffsetY = (-2).dp,
                        titleFontSize = titleSize,
                        onClick = onEditDailyWaterGoal
                    )

                    PersonalRowDivider(outline)

                    val workoutGoalText = profile?.dailyWorkoutGoalKcal?.let { "$it kcal" } ?: "—"
                    PersonalDetailsRow(
                        title = "Daily Workout Goal",
                        valueMain = workoutGoalText,
                        titleOffsetY = (-2).dp,
                        valueOffsetY = (-2).dp,
                        titleFontSize = titleSize,
                        onClick = onEditDailyWorkoutGoal
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PersonalDetailsRow(
    title: String,
    valueMain: String,
    valueSub: String? = null,
    titleOffsetY: Dp = 0.dp,
    valueOffsetY: Dp = 0.dp,
    titleFontSize: TextUnit = 15.sp,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF374151),
            modifier = Modifier
                .weight(1f)
                .offset(y = titleOffsetY)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueMain,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111114),
                    modifier = Modifier.offset(y = valueOffsetY)
                )
                if (valueSub != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = valueSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "edit",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

internal fun formatHeight(p: UserProfileDto?): String {
    val ft = p?.heightFeet
    val inch = p?.heightInches
    return if (ft != null && inch != null) {
        return "$ft ft $inch in"
    } else {
        val cm = p?.heightCm
        if (cm == null) "—" else "${formatSmartNumber(cm)} cm"
    }
}

private fun formatGender(raw: String?): String {
    val s = raw?.trim()?.lowercase(Locale.US).orEmpty()
    return when (s) {
        "male", "m" -> "Male"
        "female", "f" -> "Female"
        "other" -> "Other"
        "" -> "—"
        else -> raw ?: "—"
    }
}

private fun formatWeightBothLines(
    kg: Double?,
    lbs: Double?,
    unit: UserProfileStore.WeightUnit
): Pair<String, String?> {

    val main = when (unit) {
        UserProfileStore.WeightUnit.KG -> when {
            kg != null -> "${formatWeight1dp(kg)} kg"
            lbs != null -> "${formatWeight1dp(lbs)} lbs"
            else -> "—"
        }

        UserProfileStore.WeightUnit.LBS -> when {
            lbs != null -> "${formatWeight1dp(lbs)} lbs"
            kg != null -> "${formatWeight1dp(kg)} kg"
            else -> "—"
        }
    }

    val sub = when (unit) {
        UserProfileStore.WeightUnit.KG -> lbs?.let { "${formatWeight1dp(it)} lbs" }
        UserProfileStore.WeightUnit.LBS -> kg?.let { "${formatWeight1dp(it)} kg" }
    }

    return main to sub
}


internal fun formatSmartNumber(v: Double): String {
    val isInt = abs(v - v.toInt()) < 1e-9
    return if (isInt) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}

private fun formatWeight1dp(v: Double): String {
    return String.format(Locale.US, "%.1f", v)
}

@Composable
private fun PersonalRowDivider(color: Color) {
    HorizontalDivider(
        // ✅ 左邊對齊 row 的 16dp padding
        // ✅ 右邊多留一段空間給「value + spacing + 鉛筆」，讓鉛筆下方沒有線
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 50.dp),
        thickness = 1.dp,
        color = color
    )
}

