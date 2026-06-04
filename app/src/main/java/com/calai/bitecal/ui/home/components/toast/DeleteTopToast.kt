package com.calai.bitecal.ui.home.components.toast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.calai.bitecal.R

@Composable
fun DeleteSuccessTopToast(
    message: String,
    modifier: Modifier = Modifier
) {
    DeleteTopToast(
        message = message,
        icon = Icons.Filled.Check,
        iconBackgroundColor = Color(0xFF22C55E),
        contentDescription = stringResource(R.string.toast_delete_success_content_description),
        modifier = modifier
    )
}

@Composable
fun DeleteFailedTopToast(
    message: String,
    modifier: Modifier = Modifier
) {
    DeleteTopToast(
        message = message,
        icon = Icons.Filled.Error,
        iconBackgroundColor = Color(0xFFEF4444),
        contentDescription = stringResource(R.string.toast_delete_failed_content_description),
        modifier = modifier
    )
}

@Composable
private fun DeleteTopToast(
    message: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current

    val horizontalSafePadding = 24.dp
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = screenWidth - horizontalSafePadding * 2
    val toastMaxWidth = if (availableWidth < 360.dp) availableWidth else 360.dp

    val textMaxWidth = (toastMaxWidth - 18.dp * 2 - 24.dp - 10.dp)
        .coerceAtLeast(120.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = topInset + 8.dp,
                start = horizontalSafePadding,
                end = horizontalSafePadding
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .widthIn(
                    min = 0.dp,
                    max = toastMaxWidth
                )
                .heightIn(min = 38.dp),
            shape = MaterialTheme.shapes.large,
            color = Color.White,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 9.dp
                ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = message,
                    modifier = Modifier.widthIn(max = textMaxWidth),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111114)
                    )
                )
            }
        }
    }
}
