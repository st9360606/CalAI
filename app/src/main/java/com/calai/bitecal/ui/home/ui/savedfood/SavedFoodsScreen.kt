package com.calai.bitecal.ui.home.ui.savedfood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R

private val ScreenBg = Color(0xFFFFF7FC)
private val FoodCardBg = Color(0xFFF8EEF5)
private val FoodCardBorder = Color(0xFFF0E5EC)
private val ActionBlack = Color(0xFF0F1115)
private val SecondaryText = Color(0xFF5D5D66)
private val MacroText = Color(0xFF3D3D45)

private data class SavedFoodUi(
    val name: String,
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

private val SampleIcedCoffee = SavedFoodUi(
    name = "Iced Coffee",
    kcal = 5,
    proteinG = 0,
    carbsG = 1,
    fatG = 0
)

@Composable
fun SavedFoodsScreen(
    onBack: () -> Unit,
    onRecordToday: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var removed by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .testTag("saved_foods_screen")
    ) {
        SavedFoodsTopBar(onBack = onBack)

        if (removed) {
            SavedFoodsEmptyState()
        } else {
            SavedFoodCard(
                item = SampleIcedCoffee,
                modifier = Modifier.padding(start = 24.dp, top = 18.dp),
                onRemove = { removed = true },
                onRecordToday = onRecordToday
            )
        }
    }
}

@Composable
private fun SavedFoodsTopBar(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .testTag("saved_foods_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF202124)
            )
        }

        Text(
            text = stringResource(R.string.saved_foods_title),
            modifier = Modifier.align(Alignment.Center),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF202124)
        )
    }
}

@Composable
private fun SavedFoodCard(
    item: SavedFoodUi,
    onRemove: () -> Unit,
    onRecordToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(170.dp)
            .height(270.dp)
            .testTag("saved_food_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = FoodCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FoodCardBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.72f))
                        .clickable(onClick = onRemove)
                        .align(Alignment.TopStart)
                        .testTag("saved_food_remove"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove saved food",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF4A4A53)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEAE1E8))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.9f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "☕",
                            fontSize = 30.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = item.name,
                        color = Color(0xFF111114),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "🔥 ${item.kcal} 卡路里",
                        color = Color(0xFF111114),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacroValue(text = "🥦 ${item.proteinG}g")
                        MacroValue(text = "🌾 ${item.carbsG}g")
                        MacroValue(text = "🥑 ${item.fatG}g")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(ActionBlack)
                    .clickable(onClick = onRecordToday)
                    .testTag("saved_food_record_today"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.saved_foods_record_today),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "→",
                        color = Color.White,
                        fontSize = 20.sp,
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
        color = MacroText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SavedFoodsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.saved_foods_empty_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF202124)
        )

        Text(
            text = stringResource(R.string.saved_foods_empty_body),
            fontSize = 15.sp,
            color = SecondaryText
        )
    }
}