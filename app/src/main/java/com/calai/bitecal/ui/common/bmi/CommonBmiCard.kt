package com.calai.bitecal.ui.common.bmi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R

enum class CommonBmiTone {
    Underweight,
    Healthy,
    Overweight,
    Obese,
    Unknown
}

data class CommonBmiCardModel(
    val bmiText: String = "--.--",
    val statusText: String = "--",
    val statusTone: CommonBmiTone = CommonBmiTone.Unknown,
    val markerProgress: Float = 0.5f,
    val titleText: String = "",
    val subtitleText: String = ""
)

private val CardBg = Color.White
private val CardBorder = Color(0xFFD9D9DB)
private val TitleColor = Color(0xFF1B1B21)
private val PrimaryText = Color(0xFF17171C)
private val SecondaryText = Color(0xFF74747A)
private val HelpTint = Color(0xFF2B2E34)
private val UnknownPill = Color(0xFFB8BDC7)

private val BarBlue = Color(0xFF2D9CDB)
private val BarGreen = Color(0xFF35C36C)
private val BarYellow = Color(0xFFF2C94C)
private val BarOrange = Color(0xFFF2994A)
private val BarRed = Color(0xFFEB5757)
private val MarkerColor = Color(0xFF17171C)

@Composable
fun CommonBmiCard(
    model: CommonBmiCardModel,
    modifier: Modifier = Modifier
) {
    var showBmiInfoDialog by rememberSaveable { mutableStateOf(false) }
    val dialogModel = rememberCommonBmiInfoDialogModel()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(28.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
            .padding(start = 22.dp, top = 28.dp, end = 22.dp, bottom = 20.dp)
    ) {
        Text(
            text = model.titleText,
            color = TitleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.bmiText,
                    color = PrimaryText,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 35.sp,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = model.subtitleText,
                    color = SecondaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 92.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                CommonBmiStatusPill(
                    text = model.statusText,
                    tone = model.statusTone
                )
            }

            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable { showBmiInfoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = stringResource(R.string.bmi_card_info_cd),
                    tint = HelpTint,
                    modifier = Modifier.size(23.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        CommonBmiRangeBar(
            markerProgress = model.markerProgress
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            CommonBmiLegendItem(
                color = BarBlue,
                label = stringResource(R.string.bmi_status_underweight),
                rangeText = stringResource(R.string.bmi_range_underweight)
            )
            CommonBmiLegendItem(
                color = BarGreen,
                label = stringResource(R.string.bmi_status_healthy),
                rangeText = stringResource(R.string.bmi_range_healthy)
            )
            CommonBmiLegendItem(
                color = BarYellow,
                label = stringResource(R.string.bmi_status_overweight),
                rangeText = stringResource(R.string.bmi_range_overweight)
            )
            CommonBmiLegendItem(
                color = BarRed,
                label = stringResource(R.string.bmi_status_obese),
                rangeText = stringResource(R.string.bmi_range_obese)
            )
        }
    }

    if (showBmiInfoDialog) {
        CommonBmiInfoDialog(
            model = dialogModel,
            onDismiss = { showBmiInfoDialog = false }
        )
    }
}

@Composable
private fun rememberCommonBmiInfoDialogModel(): CommonBmiInfoDialogModel {
    val title = stringResource(R.string.bmi_info_title)
    val subtitle = stringResource(R.string.bmi_info_subtitle)
    val formulaTitle = stringResource(R.string.bmi_info_formula_title)
    val formulaValue = stringResource(R.string.bmi_info_formula_value)
    val meaningTitle = stringResource(R.string.bmi_info_meaning_title)
    val meaningBody = stringResource(R.string.bmi_info_meaning_body)
    val underweight = stringResource(R.string.bmi_status_underweight)
    val healthy = stringResource(R.string.bmi_status_healthy)
    val overweight = stringResource(R.string.bmi_status_overweight)
    val obese = stringResource(R.string.bmi_status_obese)
    val noteTitle = stringResource(R.string.bmi_info_note_title)
    val noteBody = stringResource(R.string.bmi_info_note_body)
    val cta = stringResource(R.string.bmi_info_cta)

    return remember(
        title,
        subtitle,
        formulaTitle,
        formulaValue,
        meaningTitle,
        meaningBody,
        underweight,
        healthy,
        overweight,
        obese,
        noteTitle,
        noteBody,
        cta
    ) {
        CommonBmiInfoDialogModel(
            title = title,
            subtitle = subtitle,
            formulaTitle = formulaTitle,
            formulaValue = formulaValue,
            meaningTitle = meaningTitle,
            meaningBody = meaningBody,
            underweightText = underweight,
            healthyText = healthy,
            overweightText = overweight,
            obeseText = obese,
            noteTitle = noteTitle,
            noteBody = noteBody,
            ctaText = cta
        )
    }
}

@Composable
private fun CommonBmiStatusPill(
    text: String,
    tone: CommonBmiTone
) {
    val bg = when (tone) {
        CommonBmiTone.Underweight -> BarBlue
        CommonBmiTone.Healthy -> BarGreen
        CommonBmiTone.Overweight -> BarYellow
        CommonBmiTone.Obese -> BarRed
        CommonBmiTone.Unknown -> UnknownPill
    }

    Box(
        modifier = Modifier
            .widthIn(max = 96.dp)
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommonBmiRangeBar(
    markerProgress: Float,
    modifier: Modifier = Modifier
) {
    val clamped = markerProgress.coerceIn(0f, 1f)
    val markerWidth = 3.dp
    val markerHeight = 24.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to BarBlue,
                            0.34f to BarGreen,
                            0.64f to BarYellow,
                            0.82f to BarOrange,
                            1.00f to BarRed
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth - markerWidth) * clamped)
                .width(markerWidth)
                .height(markerHeight)
                .background(MarkerColor, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun CommonBmiLegendItem(
    color: Color,
    label: String,
    rangeText: String,
    modifier: Modifier = Modifier
) {
    val dotSize = 8.dp
    val dotGap = 4.dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(dotSize)
                .background(color, CircleShape)
        )

        Spacer(modifier = Modifier.width(dotGap))

        Column {
            Text(
                text = label,
                color = Color(0xFF6F727A),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Clip
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = rangeText,
                color = Color(0xFF8A8F98),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}
