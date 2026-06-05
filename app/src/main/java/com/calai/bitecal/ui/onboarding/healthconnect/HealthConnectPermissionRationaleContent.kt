package com.calai.bitecal.ui.onboarding.healthconnect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

/**
 * Shared Health Connect rationale copy.
 *
 * Used by:
 * - HealthConnectIntroScreen: in-app onboarding flow.
 * - PermissionsRationaleActivity: system / Health Connect permission rationale entry.
 */
@Composable
internal fun HealthConnectRationaleTextBlock(
    titlePrefix: String,
    titleService: String,
    body: String,
    modifier: Modifier = Modifier,
    titleWidthFraction: Float = 0.74f,
    bodyWidthFraction: Float = 0.72f,
    titleTextAlign: TextAlign = TextAlign.Start,
    bodyTextAlign: TextAlign = TextAlign.Start
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titlePrefix,
            modifier = Modifier.fillMaxWidth(titleWidthFraction),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp
            ),
            color = Color(0xFF111114),
            textAlign = titleTextAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = titleService,
            modifier = Modifier.fillMaxWidth(titleWidthFraction),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 42.sp
            ),
            color = Color(0xFF111114),
            textAlign = titleTextAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = body,
            modifier = Modifier.fillMaxWidth(bodyWidthFraction),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = Color(0xFF8F98A3),
            textAlign = bodyTextAlign
        )
    }
}

@Composable
internal fun HealthConnectPermissionRationaleContent(
    titlePrefix: String,
    titleService: String,
    body: String,
    buttonText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HealthConnectRationaleTextBlock(
                        titlePrefix = titlePrefix,
                        titleService = titleService,
                        body = body,
                        titleWidthFraction = 1f,
                        bodyWidthFraction = 1f,
                        titleTextAlign = TextAlign.Center,
                        bodyTextAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = rememberClickWithHaptic(onClick = onClose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = buttonText)
                    }
                }
            }
        }
    }
}
