package com.calai.app.ui.home.ui.personal.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calai.app.data.profile.api.UserProfileDto
import com.calai.app.data.profile.repo.UserProfileStore
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    profile: UserProfileDto?,
    unit: UserProfileStore.WeightUnit,

    goalKgFromWeightVm: Double? = null,
    goalLbsFromWeightVm: Double? = null,

    // ✅ 新增：由 WeightViewModel(=weight_timeseries) 來的 current
    // 沒值時才 fallback 用 profile.weightKg/weightLbs
    currentKgFromTimeseries: Double? = null,
    currentLbsFromTimeseries: Double? = null,

    onBack: () -> Unit,
    onChangeGoal: () -> Unit,
    onEditCurrentWeight: () -> Unit = {},
    onEditHeight: () -> Unit = {},
    onEditAge: () -> Unit = {},
    onEditGender: () -> Unit = {},
    onEditDailyStepGoal: () -> Unit = {},
) {
    val bg = Color(0xFFF6F7F9)
    val cardShape = RoundedCornerShape(22.dp)
    val outline = Color(0xFFE5E7EB)
    val scroll = rememberScrollState()
    val contentMaxWidth = 520.dp

    Scaffold(
        containerColor = bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Personal details",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF111114)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F2F5))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back",
                            tint = Color(0xFF111114)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg),
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
                val (goalMain, goalSub) = formatWeightBothLines(
                    // ✅ 改成：WeightVM goal > profile goal > —
                    kg = goalKgFromWeightVm ?: profile?.goalWeightKg,
                    lbs = goalLbsFromWeightVm ?: profile?.goalWeightLbs,
                    unit = unit
                )

                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, outline, cardShape)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Goal Weight",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = goalMain,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF111114)
                            )
                            if (goalSub != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = goalSub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                        }

                        Button(
                            onClick = onChangeGoal,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF111114),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Change Goal",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ===== List Card =====
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, outline, cardShape)
                ) {
                    // ✅ Current Weight：優先用 WeightVM(=timeseries) current；沒有才 fallback profile
                    val (curMain, curSub) = formatWeightBothLines(
                        kg = currentKgFromTimeseries ?: profile?.weightKg,
                        lbs = currentLbsFromTimeseries ?: profile?.weightLbs,
                        unit = unit
                    )

                    PersonalDetailsRow(
                        title = "Current Weight",
                        valueMain = curMain,
                        valueSub = curSub,
                        onClick = onEditCurrentWeight
                    )

                    Divider(color = outline, thickness = 1.dp)

                    PersonalDetailsRow(
                        title = "Height",
                        valueMain = formatHeight(profile),
                        onClick = onEditHeight
                    )

                    Divider(color = outline, thickness = 1.dp)

                    PersonalDetailsRow(
                        title = "Age",
                        valueMain = profile?.age?.let { "$it years" } ?: "—",
                        onClick = onEditAge
                    )

                    Divider(color = outline, thickness = 1.dp)

                    PersonalDetailsRow(
                        title = "Gender",
                        valueMain = formatGender(profile?.gender),
                        onClick = onEditGender
                    )

                    Divider(color = outline, thickness = 1.dp)

                    val stepText = profile?.dailyStepGoal?.let { "${it} steps" } ?: "—"
                    PersonalDetailsRow(
                        title = "Daily Step Goal",
                        valueMain = stepText,
                        onClick = onEditDailyStepGoal
                    )
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PersonalDetailsRow(
    title: String,
    valueMain: String,
    valueSub: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
            color = Color(0xFF111114),
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = valueMain,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111114)
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

private fun formatHeight(p: UserProfileDto?): String {
    val ft = p?.heightFeet
    val inch = p?.heightInches
    return if (ft != null && inch != null) {
        "${ft} ft ${inch} in"
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
            kg != null -> "${formatSmartNumber(kg)} kg"
            lbs != null -> "${formatSmartNumber(lbs)} lbs"
            else -> "—"
        }

        UserProfileStore.WeightUnit.LBS -> when {
            lbs != null -> "${formatSmartNumber(lbs)} lbs"
            kg != null -> "${formatSmartNumber(kg)} kg"
            else -> "—"
        }
    }

    val sub = when (unit) {
        UserProfileStore.WeightUnit.KG -> lbs?.let { "${formatSmartNumber(it)} lbs" }
        UserProfileStore.WeightUnit.LBS -> kg?.let { "${formatSmartNumber(it)} kg" }
    }

    return main to sub
}

private fun formatSmartNumber(v: Double): String {
    val isInt = abs(v - v.toInt()) < 1e-9
    return if (isInt) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
}
