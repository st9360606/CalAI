package com.calai.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayItem(val date: LocalDate)

@Composable
fun DayPillCalendar(
    days: List<DayItem>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val fmtDay = DateTimeFormatter.ofPattern("dd")
        val fmtDow = DateTimeFormatter.ofPattern("EEE")
        for (d in days) {
            val isSel = d.date == selected
            val shape = RoundedCornerShape(18.dp)
            Column(
                modifier = Modifier
                    .width(68.dp)
                    .clip(shape)
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = shape
                    )
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(d.date) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(fmtDow.format(d.date), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    fmtDay.format(d.date),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
