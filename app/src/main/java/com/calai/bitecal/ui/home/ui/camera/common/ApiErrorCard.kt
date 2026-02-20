package com.calai.bitecal.ui.home.ui.camera.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.foodlog.model.ClientAction

@Composable
fun ApiErrorCard(
    ui: ApiErrorUiMapper.UiModel,
    onAction: (ClientAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(ui.titleResId),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(ui.messageResId),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ui.primaryCtaResId != null && ui.primaryAction != null) {
                    Button(onClick = { onAction(ui.primaryAction) }) {
                        Text(stringResource(ui.primaryCtaResId))
                    }
                }
                if (ui.secondaryCtaResId != null && ui.secondaryAction != null) {
                    OutlinedButton(onClick = { onAction(ui.secondaryAction) }) {
                        Text(stringResource(ui.secondaryCtaResId))
                    }
                }
            }
        }
    }
}