    package com.calai.bitecal.ui.home.ui.settings.dialog

    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Close
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.Surface
    import androidx.compose.material3.Text
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.window.Dialog
    import androidx.compose.ui.window.DialogProperties
    import com.calai.bitecal.R
    import com.calai.bitecal.ui.home.ui.settings.model.RestoreSubscriptionDialogState
    import com.calai.bitecal.ui.home.ui.settings.model.RestoreSubscriptionUiState

    @Composable
    fun RestoreSubscriptionDialog(
        uiState: RestoreSubscriptionUiState,
        onDismiss: () -> Unit,
        onMaybeLater: () -> Unit,
        onRestore: () -> Unit
    ) {
        if (!uiState.visible) return

        val isRestoring = uiState.dialogState == RestoreSubscriptionDialogState.Restoring
        val isResultState = when (uiState.dialogState) {
            RestoreSubscriptionDialogState.Restored,
            RestoreSubscriptionDialogState.NoActivePurchase,
            RestoreSubscriptionDialogState.Failed,
            RestoreSubscriptionDialogState.BoundToAnotherAccount -> true
            RestoreSubscriptionDialogState.Hidden,
            RestoreSubscriptionDialogState.CandidateFound,
            RestoreSubscriptionDialogState.Restoring -> false
        }

        val title = when (uiState.dialogState) {
            RestoreSubscriptionDialogState.Restored -> stringResource(R.string.restore_subscription_dialog_success_title)
            RestoreSubscriptionDialogState.NoActivePurchase -> stringResource(R.string.restore_subscription_dialog_no_active_title)
            RestoreSubscriptionDialogState.Failed -> stringResource(R.string.restore_subscription_dialog_failed_title)
            RestoreSubscriptionDialogState.BoundToAnotherAccount -> stringResource(R.string.restore_subscription_dialog_bound_title)
            RestoreSubscriptionDialogState.Hidden,
            RestoreSubscriptionDialogState.CandidateFound,
            RestoreSubscriptionDialogState.Restoring -> stringResource(R.string.restore_subscription_dialog_title)
        }

        val body = when (uiState.dialogState) {
            RestoreSubscriptionDialogState.Restored -> stringResource(R.string.restore_subscription_dialog_success_body)
            RestoreSubscriptionDialogState.NoActivePurchase -> stringResource(R.string.restore_subscription_dialog_no_active_body)
            RestoreSubscriptionDialogState.Failed -> stringResource(R.string.restore_subscription_dialog_failed_body)
            RestoreSubscriptionDialogState.BoundToAnotherAccount -> stringResource(R.string.restore_subscription_dialog_bound_body)
            RestoreSubscriptionDialogState.Hidden,
            RestoreSubscriptionDialogState.CandidateFound,
            RestoreSubscriptionDialogState.Restoring -> stringResource(R.string.restore_subscription_dialog_body)
        }

        Dialog(
            onDismissRequest = { if (!isRestoring) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !isRestoring,
                dismissOnClickOutside = !isRestoring,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111114),
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(33.dp)
                                    .background(Color(0xFFECECEC), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { if (!isRestoring) onDismiss() },
                                    enabled = !isRestoring,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.common_close),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = body,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF2F3A4A),
                            textAlign = TextAlign.Start
                        )

                        Spacer(Modifier.height(22.dp))

                        if (isResultState) {
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF111114),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(47.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.common_close),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (!isRestoring) onMaybeLater() },
                                    enabled = !isRestoring,
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(0.8.dp, Color(0xFF24252A)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF111114)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(47.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.common_maybe_later),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = { if (!isRestoring) onRestore() },
                                    enabled = !isRestoring,
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF111114),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(47.dp)
                                ) {
                                    Text(
                                        text = if (isRestoring) {
                                            stringResource(R.string.restore_subscription_dialog_restoring)
                                        } else {
                                            stringResource(R.string.settings_restore_subscription)
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
