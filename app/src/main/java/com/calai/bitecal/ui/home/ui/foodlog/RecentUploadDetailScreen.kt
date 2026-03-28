package com.calai.bitecal.ui.home.ui.foodlog

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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel
import kotlin.math.roundToInt

private data class ScaledNutrients(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodium: Double
)

private fun FoodLogEnvelopeDto.scaledNutrients(multiplier: Int): ScaledNutrients {
    val n = nutritionResult?.nutrients
    return ScaledNutrients(
        kcal = (n?.kcal ?: 0.0) * multiplier,
        protein = (n?.protein ?: 0.0) * multiplier,
        carbs = (n?.carbs ?: 0.0) * multiplier,
        fat = (n?.fat ?: 0.0) * multiplier,
        fiber = (n?.fiber ?: 0.0) * multiplier,
        sugar = (n?.sugar ?: 0.0) * multiplier,
        sodium = (n?.sodium ?: 0.0) * multiplier
    )
}

@Composable
fun RecentUploadDetailScreen(
    foodLogId: String,
    previewUri: String?,
    timeText: String,
    vm: FoodLogFlowViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val st by vm.state.collectAsState()
    val env = st.envelope?.takeIf { it.foodLogId == foodLogId }

    LaunchedEffect(foodLogId) {
        vm.clearTransient()
        vm.startPolling(foodLogId)
    }

    DisposableEffect(foodLogId) {
        onDispose { vm.stopPolling() }
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
            Text(text = st.error ?: "讀取失敗")
        }
        return
    }

    var multiplier by rememberSaveable(foodLogId) { mutableIntStateOf(1) }
    val scaled = remember(env, multiplier) { env.scaledNutrients(multiplier) }
    val isSaved = env.status == FoodLogStatus.SAVED
    val displayName = env.nutritionResult?.foodName?.takeIf { it.isNotBlank() } ?: "Unknown Food"
    val healthScore = env.nutritionResult?.healthScore ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6F6))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Header image =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
            ) {
                if (!previewUri.isNullOrBlank()) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF202124)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Image",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f))
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 8.dp)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            // ===== Content =====
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF4F4F4),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = !st.loading) {
                                            if (multiplier > 1) {
                                                vm.persistMultiplierThenToggleSaved(
                                                    foodLogId = foodLogId,
                                                    baseEnv = env,
                                                    multiplier = multiplier,
                                                    onSuccess = { multiplier = 1 }
                                                )
                                            } else {
                                                vm.toggleSaved(foodLogId)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Save",
                                        tint = Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = timeText.ifBlank { "--:--" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6A6A6A),
                                modifier = Modifier.weight(1f)
                            )

                            Stepper(
                                value = multiplier,
                                enabled = !st.loading,
                                onMinus = { multiplier = (multiplier - 1).coerceAtLeast(1) },
                                onPlus = { multiplier += 1 }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        CaloriesCard(kcal = scaled.kcal.roundToInt())

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MacroCard(
                                modifier = Modifier.weight(1f),
                                title = "Protein",
                                value = "${scaled.protein.roundToInt()}g"
                            )
                            MacroCard(
                                modifier = Modifier.weight(1f),
                                title = "Carbs",
                                value = "${scaled.carbs.roundToInt()}g"
                            )
                            MacroCard(
                                modifier = Modifier.weight(1f),
                                title = "Fats",
                                value = "${scaled.fat.roundToInt()}g"
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        DetailRow(title = "Fiber", value = "${scaled.fiber.roundToInt()}g")
                        DetailRow(title = "Sugar", value = "${scaled.sugar.roundToInt()}g")
                        DetailRow(title = "Sodium", value = "${scaled.sodium.roundToInt()}mg")
                        DetailRow(title = "Health Score", value = "$healthScore")

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Button(
                        onClick = {
                            if (multiplier > 1) {
                                vm.persistMultiplierThenDone(
                                    foodLogId = foodLogId,
                                    baseEnv = env,
                                    multiplier = multiplier,
                                    onSuccess = {
                                        multiplier = 1
                                        onDone()
                                    }
                                )
                            } else {
                                onDone()
                            }
                        },
                        enabled = !st.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF171625)
                        )
                    ) {
                        Text(
                            text = "DONE",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (st.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(2.dp, Color(0xFF171717), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        IconButton(
            onClick = onMinus,
            enabled = enabled,
            modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Minus",
                tint = Color(0xFF171717)
            )
        }

        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF171717)
        )

        IconButton(
            onClick = onPlus,
            enabled = enabled,
            modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Plus",
                tint = Color(0xFF171717)
            )
        }
    }
}

@Composable
private fun CaloriesCard(kcal: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9E9E9))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Calories",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF303030)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = kcal.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
        }
    }
}

@Composable
private fun MacroCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9E9E9)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF303030)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
        }
    }
}

@Composable
private fun DetailRow(
    title: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF303030)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111111)
            )
        }
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
