package com.calai.bitecal.ui.home.ui.savedfood

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.CalaiCenteredTopBar
import com.calai.bitecal.ui.common.CalaiConfirmDialog
import com.calai.bitecal.ui.home.components.CardStyles
import com.calai.bitecal.ui.home.ui.savedfood.model.SavedFoodCardUi
import com.calai.bitecal.ui.home.ui.savedfood.model.SavedFoodsViewModel

private val ScreenBg = Color(0xFFF5F5F5)
private val TitleColor = Color(0xFF111827)
private val KcalColor = Color(0xFF0F172A)
private val MacroColor = Color(0xFF344054)
private val ActionBlack = Color(0xFF0F1115)
private val SecondaryText = Color(0xFF5D5D66)

private val TitleTextStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Bold,
    color = TitleColor
)

private val KcalTextStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 19.sp,
    fontWeight = FontWeight.SemiBold,
    color = KcalColor
)

private val MacroTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
    color = MacroColor
)

@Composable
fun SavedFoodsScreen(
    onBack: () -> Unit,
    onOpenDetail: (foodLogId: String, previewUri: String?, timeText: String) -> Unit,
    vm: SavedFoodsViewModel,
    modifier: Modifier = Modifier
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    var pendingUnsaveFoodLogId by rememberSaveable { mutableStateOf<String?>(null) }
    var unsaveSubmitting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadIfNeeded()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .testTag("saved_foods_screen")
    ) {
        SavedFoodsTopBar(onBack = onBack)

        Text(
            text = stringResource(R.string.saved_foods_keep_15_days_hint),
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp)
                .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 2.dp),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = SecondaryText,
            textAlign = TextAlign.Center
        )

        when {
            ui.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator()
                }
            }

            !ui.error.isNullOrBlank() -> {
                SavedFoodsErrorState(
                    message = ui.error!!,
                    onRetry = vm::refresh
                )
            }

            ui.items.isEmpty() -> Unit

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = ui.items,
                        key = { it.foodLogId }
                    ) { item ->
                        SavedFoodCard(
                            item = item,
                            onRemove = {
                                pendingUnsaveFoodLogId = item.foodLogId
                            },
                            onOpenDetail = {
                                onOpenDetail(
                                    item.foodLogId,
                                    item.previewUri,
                                    item.timeText
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    CalaiConfirmDialog(
        visible = pendingUnsaveFoodLogId != null,
        onDismiss = {
            if (!unsaveSubmitting) {
                pendingUnsaveFoodLogId = null
            }
        },
        onCancel = {
            if (!unsaveSubmitting) {
                pendingUnsaveFoodLogId = null
            }
        },
        onConfirm = {
            val targetId = pendingUnsaveFoodLogId ?: return@CalaiConfirmDialog
            if (unsaveSubmitting) return@CalaiConfirmDialog

            unsaveSubmitting = true
            vm.unsave(
                foodLogId = targetId,
                onSuccess = {
                    unsaveSubmitting = false
                    pendingUnsaveFoodLogId = null
                },
                onFailure = {
                    unsaveSubmitting = false
                }
            )
        },
        loading = unsaveSubmitting,
        title = stringResource(R.string.saved_foods_unsave_dialog_title),
        message = stringResource(R.string.saved_foods_unsave_dialog_message),
        confirmText = stringResource(R.string.saved_foods_unsave_dialog_confirm),
        cancelText = stringResource(R.string.cancel)
    )
}

@Composable
private fun SavedFoodsTopBar(
    onBack: () -> Unit
) {
    CalaiCenteredTopBar(
        title = stringResource(R.string.saved_foods_title),
        onBack = onBack
    )
}

@Composable
private fun SavedFoodCard(
    item: SavedFoodCardUi,
    onRemove: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unsaveContentDescription = stringResource(R.string.saved_foods_unsave_content_description)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .testTag("saved_food_card")
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardStyles.Bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardStyles.Border,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8E8ED))
                        .clickable(onClick = onRemove)
                        .align(Alignment.TopStart)
                        .testTag("saved_food_remove"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = unsaveContentDescription,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4A4A53)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAE1E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!item.previewUri.isNullOrBlank()) {
                            AsyncImage(
                                model = item.previewUri,
                                contentDescription = item.displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "🍽️",
                                fontSize = 26.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier.height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.displayTitle,
                            style = TitleTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.saved_foods_kcal, item.kcal),
                            style = KcalTextStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("recent_upload_kcal")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MacroValue(
                                text = stringResource(
                                    R.string.saved_foods_protein,
                                    item.proteinG
                                )
                            )
                            MacroValue(
                                text = stringResource(
                                    R.string.saved_foods_carbs,
                                    item.carbsG
                                )
                            )
                            MacroValue(
                                text = stringResource(
                                    R.string.saved_foods_fat,
                                    item.fatG
                                )
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(ActionBlack)
                    .clickable(onClick = onOpenDetail)
                    .testTag("saved_food_detail"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.saved_foods_detail),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroValue(text: String) {
    Text(
        text = text,
        style = MacroTextStyle,
        maxLines = 1,
        overflow = TextOverflow.Clip
    )
}

@Composable
private fun SavedFoodsErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Saved foods 載入失敗",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF202124)
        )

        Text(
            text = message,
            fontSize = 15.sp,
            color = SecondaryText
        )

        TextButton(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}
