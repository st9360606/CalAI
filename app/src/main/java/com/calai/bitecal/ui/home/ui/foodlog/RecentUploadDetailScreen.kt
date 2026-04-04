package com.calai.bitecal.ui.home.ui.foodlog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calai.bitecal.R
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.ui.home.components.RingColors
import com.calai.bitecal.ui.home.ui.foodlog.dialog.DeleteFoodLogDialog
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private object DetailStyle {
    val AppBg = Color(0xFFF3F3F3)
    val SheetBg = Color.White
    val Border = Color(0xFFEAEAEA)
    val TextPrimary = Color(0xFF151515)
    val HeroFallback = Color(0xFF202124)
    val Scrim = Color.Black.copy(alpha = 0.16f)
    val FooterBtn = Color(0xFF171625)
    val ChipBg = Color(0xFFF5F5F7)
    val ProteinTone = Color(0xFFFF6B7B)
    val CarbsTone = Color(0xFFF6B24D)
    val FatTone = Color(0xFF6FA3FF)
    val FiberTone = Color(0xFF8E7DF2)
    val SugarTone = Color(0xFFFF8A5B)
    val SodiumTone = Color(0xFF4CB7A5)
}

private data class ScaledNutrients(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodium: Double
)

private val FoodLogEnvelopeDto.safePortionMultiplier: Int
    get() = portionMultiplier.coerceAtLeast(1)

/**
 * 將目前 envelope 中「已保存」的 nutritionResult，先反推回 base(1x)，
 * 再依 editingMultiplier 做即時預覽。
 */
private fun FoodLogEnvelopeDto.previewScaledNutrients(
    editingMultiplier: Int
): ScaledNutrients {
    val persistedMultiplier = safePortionMultiplier.toDouble()
    val n = nutritionResult?.nutrients

    fun rescale(value: Double?): Double {
        val current = value ?: 0.0
        val base = if (persistedMultiplier <= 0.0) current else current / persistedMultiplier
        return base * editingMultiplier
    }

    return ScaledNutrients(
        kcal = rescale(n?.kcal),
        protein = rescale(n?.protein),
        carbs = rescale(n?.carbs),
        fat = rescale(n?.fat),
        fiber = rescale(n?.fiber),
        sugar = rescale(n?.sugar),
        sodium = rescale(n?.sodium)
    )
}

private fun resolveFoodLogDisplayTime(
    env: FoodLogEnvelopeDto,
    fallbackTimeText: String = ""
): String {
    return FoodLogTimeResolver.resolveDisplayTimeText(
        zoneId = ZoneId.systemDefault(),
        updatedAtUtc = env.updatedAtUtc,
        serverReceivedAtUtc = env.serverReceivedAtUtc,
        capturedAtUtc = env.capturedAtUtc,
        capturedLocalDate = env.capturedLocalDate
    ).ifBlank { fallbackTimeText }
}

@Composable
fun RecentUploadDetailScreen(
    foodLogId: String,
    previewUri: String?,
    timeText: String,
    vm: FoodLogFlowViewModel,
    onBack: () -> Unit,
    onDone: (FoodLogEnvelopeDto) -> Unit,
    onDeleted: (String) -> Unit
) {
    val st by vm.state.collectAsState()

    val liveEnv = st.envelope?.takeIf { it.foodLogId == foodLogId }

    var lastStableEnv by remember(foodLogId) {
        mutableStateOf<FoodLogEnvelopeDto?>(null)
    }

    val handleBack = remember(onBack, vm) {
        {
            vm.stopPolling()
            onBack()
        }
    }

    BackHandler(enabled = !st.loading) {
        handleBack()
    }

    LaunchedEffect(liveEnv) {
        val candidate = liveEnv
        if (
            candidate != null &&
            candidate.status != FoodLogStatus.DELETED &&
            candidate.nutritionResult != null
        ) {
            lastStableEnv = candidate
        }
    }

    val env = when {
        liveEnv != null &&
                liveEnv.status != FoodLogStatus.DELETED &&
                liveEnv.nutritionResult != null -> liveEnv

        else -> lastStableEnv
    }

    LaunchedEffect(foodLogId) {
        vm.clearTransient()
        vm.startPolling(foodLogId)
    }

    DisposableEffect(foodLogId) {
        onDispose { vm.stopPolling() }
    }

    var stablePreviewUri by rememberSaveable(foodLogId) { mutableStateOf(previewUri) }
    var stableTimeText by rememberSaveable(foodLogId) { mutableStateOf(timeText) }
    var showDeleteDialog by rememberSaveable(foodLogId) { mutableStateOf(false) }
    var deleteRequested by rememberSaveable(foodLogId) { mutableStateOf(false) }

    LaunchedEffect(
        deleteRequested,
        st.loading,
        st.error,
        st.apiError,
        st.cooldown,
        st.refused
    ) {
        if (!deleteRequested || st.loading) return@LaunchedEffect

        if (
            st.error != null ||
            st.apiError != null ||
            st.cooldown != null ||
            st.refused != null
        ) {
            deleteRequested = false
        }
    }

    if (st.loading && env == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (env == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = st.error ?: stringResource(R.string.foodlog_detail_load_failed))
        }
        return
    }

    val persistedMultiplier = env.safePortionMultiplier

    var multiplier by rememberSaveable(foodLogId) {
        mutableIntStateOf(persistedMultiplier)
    }

    LaunchedEffect(foodLogId, persistedMultiplier) {
        multiplier = persistedMultiplier
    }

    val scaled = remember(env, multiplier) {
        env.previewScaledNutrients(multiplier)
    }

    val persistedSaved = env.status == FoodLogStatus.SAVED

    var editingSaved by rememberSaveable(foodLogId) {
        mutableStateOf(persistedSaved)
    }

    LaunchedEffect(foodLogId) {
        editingSaved = persistedSaved
    }

    val displayName = env.nutritionResult?.foodName
        ?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.foodlog_unknown_food)

    val healthScore = env.nutritionResult?.healthScore ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailStyle.AppBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.46f)
        ) {
            if (!stablePreviewUri.isNullOrBlank()) {
                AsyncImage(
                    model = stablePreviewUri,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(DetailStyle.HeroFallback),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.foodlog_no_image),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(DetailStyle.Scrim)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !st.loading, onClick = handleBack),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.foodlog_detail_back),
                            tint = DetailStyle.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.foodlog_detail_title),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !st.loading) {
                            showDeleteDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.trash),
                            contentDescription = stringResource(R.string.foodlog_detail_delete),
                            modifier = Modifier.size(22.dp),
                            colorFilter = ColorFilter.tint(DetailStyle.TextPrimary)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = DetailStyle.SheetBg,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaveBadge(
                            isSaved = editingSaved,
                            enabled = !st.loading &&
                                    env.status != FoodLogStatus.PENDING &&
                                    env.status != FoodLogStatus.FAILED &&
                                    env.status != FoodLogStatus.DELETED,
                            onClick = {
                                editingSaved = !editingSaved
                            }
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        TimeChip(timeText = stableTimeText)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = displayName,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                lineHeight = 26.sp
                            ),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DetailStyle.TextPrimary
                        )

                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp, top = 1.dp)
                        ) {
                            Stepper(
                                value = multiplier,
                                enabled = !st.loading,
                                onMinus = { multiplier = (multiplier - 1).coerceAtLeast(1) },
                                onPlus = { multiplier += 1 }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CaloriesHeroCard(kcal = scaled.kcal.roundToInt())

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_protein),
                            value = "${scaled.protein.roundToInt()}g",
                            tone = DetailStyle.ProteinTone,
                            emoji = "🥩"
                        )
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_carbs),
                            value = "${scaled.carbs.roundToInt()}g",
                            tone = DetailStyle.CarbsTone,
                            emoji = "🌾"
                        )
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_fats),
                            value = "${scaled.fat.roundToInt()}g",
                            tone = DetailStyle.FatTone,
                            emoji = "🥑"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_fiber),
                            value = "${scaled.fiber.roundToInt()}g",
                            tone = DetailStyle.FiberTone,
                            emoji = "🌿"
                        )
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_sugar),
                            value = "${scaled.sugar.roundToInt()}g",
                            tone = DetailStyle.SugarTone,
                            emoji = "🍯"
                        )
                        MacroCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.foodlog_detail_sodium),
                            value = "${scaled.sodium.roundToInt()}mg",
                            tone = DetailStyle.SodiumTone,
                            emoji = "🧂"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HealthScoreCard(score = healthScore)

                    Spacer(modifier = Modifier.height(24.dp))
                }

                val hasMultiplierChange = multiplier != persistedMultiplier
                val hasSavedChange = editingSaved != persistedSaved

                FooterDoneBar(
                    enabled = !st.loading,
                    onDone = {
                        if (hasMultiplierChange || hasSavedChange) {
                            vm.commitDetailChanges(
                                foodLogId = foodLogId,
                                baseEnv = env,
                                multiplier = multiplier,
                                targetSaved = editingSaved,
                                onSuccess = { updatedEnv ->
                                    multiplier = updatedEnv.portionMultiplier.coerceAtLeast(1)
                                    stableTimeText = resolveFoodLogDisplayTime(
                                        env = updatedEnv,
                                        fallbackTimeText = stableTimeText
                                    )
                                    onDone(updatedEnv)
                                }
                            )
                        } else {
                            onDone(env)
                        }
                    }
                )
            }
        }

        DeleteFoodLogDialog(
            visible = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            onCancel = { showDeleteDialog = false },
            onDelete = {
                if (!st.loading) {
                    showDeleteDialog = false
                    deleteRequested = true
                    vm.delete(foodLogId) {
                        deleteRequested = false
                        onDeleted(foodLogId)
                    }
                }
            },
            deleting = deleteRequested && st.loading
        )
    }
}

@Composable
private fun SaveBadge(
    isSaved: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(27.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = stringResource(R.string.foodlog_detail_save),
            tint = DetailStyle.TextPrimary,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatDisplayTime(raw: String): String {
    val input = raw.trim()
    if (input.isBlank()) return "--:-- --"

    val outputFormatter = DateTimeFormatter.ofPattern("HH:mm a", Locale.US)

    val candidates = listOf(
        DateTimeFormatter.ofPattern("H:mm", Locale.US),
        DateTimeFormatter.ofPattern("HH:mm", Locale.US),
        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    )

    for (formatter in candidates) {
        runCatching {
            return LocalTime.parse(input.uppercase(Locale.US), formatter).format(outputFormatter)
        }
    }

    return input
}

@Composable
private fun TimeChip(timeText: String) {
    Surface(
        color = DetailStyle.ChipBg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = formatDisplayTime(timeText),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            color = Color(0xFF5C667A),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .height(43.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, DetailStyle.TextPrimary, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp)
    ) {
        IconButton(
            onClick = onMinus,
            enabled = enabled,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = stringResource(R.string.foodlog_detail_minus),
                tint = DetailStyle.TextPrimary
            )
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DetailStyle.TextPrimary
        )

        IconButton(
            onClick = onPlus,
            enabled = enabled,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.foodlog_detail_plus),
                tint = DetailStyle.TextPrimary
            )
        }
    }
}

@Composable
private fun CaloriesHeroCard(kcal: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DetailStyle.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(18.dp),
                color = RingColors.CenterFill
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.foodlog_detail_calories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = kcal.toString(),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = DetailStyle.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun MacroCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    tone: Color,
    emoji: String
) {
    Surface(
        modifier = modifier.height(62.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DetailStyle.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(22.dp)
                    .offset(y = (-8).dp),
                shape = CircleShape,
                color = tone.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    maxLines = 1
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetailStyle.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HealthScoreCard(
    score: Int
) {
    val safeScore = score.coerceIn(0, 10)
    val progress = safeScore / 10f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DetailStyle.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE7E8EC),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(1.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.apple_health),
                    contentDescription = stringResource(R.string.foodlog_detail_health_icon_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.foodlog_detail_health_score),
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "$safeScore/10",
                        style = MaterialTheme.typography.titleMedium,
                        color = DetailStyle.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFE9E7EF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF6BCB77))
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterDoneBar(
    enabled: Boolean,
    onDone: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Button(
            onClick = onDone,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
                .height(56.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DetailStyle.FooterBtn,
                contentColor = Color.White,
                disabledContainerColor = DetailStyle.FooterBtn,
                disabledContentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.foodlog_detail_done),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
