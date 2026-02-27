package com.calai.bitecal.ui.home.ui.camera.common

import androidx.annotation.StringRes
import com.calai.bitecal.R
import com.calai.bitecal.data.foodlog.model.ApiErrorDto
import com.calai.bitecal.data.foodlog.model.ClientAction
import java.util.Locale

object ApiErrorUiMapper {

    data class UiModel(
        @StringRes val titleResId: Int,
        @StringRes val messageResId: Int,
        @StringRes val primaryCtaResId: Int? = null,
        @StringRes val secondaryCtaResId: Int? = null,
        val primaryAction: ClientAction? = null,
        val secondaryAction: ClientAction? = null
    )

    fun map(err: ApiErrorDto?): UiModel? {
        if (err == null) return null

        val action = err.clientAction

        val code = err.errorCode
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase(Locale.ROOT)

        return when (code) {

            // ===== BARCODE =====
            "BARCODE_NOT_FOUND" -> UiModel(
                titleResId = R.string.err_barcode_not_found_title,
                messageResId = R.string.err_barcode_not_found_msg,
                primaryCtaResId = R.string.cta_try_label,
                secondaryCtaResId = R.string.cta_enter_manually,
                primaryAction = ClientAction.TRY_LABEL,
                secondaryAction = ClientAction.ENTER_MANUALLY
            )

            "BARCODE_LOOKUP_FAILED" -> UiModel(
                titleResId = R.string.err_barcode_lookup_failed_title,
                messageResId = R.string.err_barcode_lookup_failed_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.TRY_BARCODE,
                secondaryAction = ClientAction.CHECK_NETWORK
            )

            // ===== IMAGE / UPLOAD =====
            "IMAGE_TOO_LARGE",
            "FILE_REQUIRED",
            "UNSUPPORTED_IMAGE_FORMAT",
            "IMAGE_DECODE_FAILED" -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                primaryAction = ClientAction.RETAKE_PHOTO
            )

            // ===== FOOD VISION 常見失敗 =====
            "LOW_CONFIDENCE",
            "NO_FOOD_DETECTED" -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                secondaryCtaResId = R.string.cta_rescan_barcode,
                primaryAction = ClientAction.RETAKE_PHOTO,
                secondaryAction = ClientAction.TRY_BARCODE
            )

            // ===== NETWORK / SERVER =====
            "NETWORK_ERROR",
            "TIMEOUT",
            "UPSTREAM_TIMEOUT",
            "PROVIDER_UNAVAILABLE" -> UiModel(
                titleResId = R.string.err_network_title,
                messageResId = R.string.err_network_msg,
                primaryCtaResId = R.string.cta_retry,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.RETRY_LATER,
                secondaryAction = ClientAction.CHECK_NETWORK
            )

            // ===== generic / fallback =====
            null -> fallbackByAction(action)
            else -> fallbackByAction(action)
        }
    }

    private fun fallbackByAction(action: ClientAction?): UiModel {
        return when (action) {
            ClientAction.TRY_LABEL -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.cta_try_label,
                primaryAction = ClientAction.TRY_LABEL
            )

            ClientAction.CHECK_NETWORK -> UiModel(
                titleResId = R.string.err_network_title,
                messageResId = R.string.err_network_msg,
                primaryCtaResId = R.string.cta_retry,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.RETRY_LATER,
                secondaryAction = ClientAction.CHECK_NETWORK
            )

            ClientAction.RETAKE_PHOTO -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                primaryAction = ClientAction.RETAKE_PHOTO
            )

            ClientAction.ENTER_MANUALLY -> UiModel(
                titleResId = R.string.err_manual_title,
                messageResId = R.string.err_manual_msg,
                primaryCtaResId = R.string.cta_enter_manually,
                primaryAction = ClientAction.ENTER_MANUALLY
            )

            ClientAction.CONTACT_SUPPORT -> UiModel(
                titleResId = R.string.err_support_title,
                messageResId = R.string.err_support_msg,
                primaryCtaResId = R.string.cta_contact_support,
                primaryAction = ClientAction.CONTACT_SUPPORT
            )

            ClientAction.TRY_BARCODE -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                primaryAction = ClientAction.TRY_BARCODE
            )

            else -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.cta_retry,
                primaryAction = ClientAction.RETRY_LATER
            )
        }
    }
}