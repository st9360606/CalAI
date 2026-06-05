package com.calai.bitecal.ui.home.ui.foodlog.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.CalaiConfirmDialog


@Composable
fun DeleteFoodLogDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    deleting: Boolean = false
) {
    CalaiConfirmDialog(
        visible = visible,
        onDismiss = onDismiss,
        onCancel = onCancel,
        onConfirm = onDelete,
        loading = deleting,
        title = stringResource(R.string.foodlog_delete_dialog_title),
        message = stringResource(R.string.foodlog_delete_dialog_message),
        confirmText = stringResource(R.string.foodlog_delete_dialog_confirm),
        cancelText = stringResource(R.string.common_cancel),
        confirmButtonColor = Color(0xFFE46A6A),
        confirmContentColor = Color.White
    )
}
