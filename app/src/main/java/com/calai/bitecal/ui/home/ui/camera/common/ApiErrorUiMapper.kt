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
            "BARCODE_REQUIRED",
            "BARCODE_INVALID" -> UiModel(
                titleResId = R.string.err_barcode_lookup_failed_title,
                messageResId = R.string.err_barcode_lookup_failed_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                primaryAction = ClientAction.SCAN_AGAIN
            )

            "BARCODE_NOT_FOUND" -> UiModel(
                titleResId = R.string.err_barcode_not_found_title,
                messageResId = R.string.err_barcode_not_found_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                secondaryCtaResId = R.string.cta_try_label,
                primaryAction = ClientAction.SCAN_AGAIN,
                secondaryAction = ClientAction.TRY_LABEL
            )

            "BARCODE_NUTRITION_UNAVAILABLE" -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.cta_retake,
                secondaryCtaResId = R.string.cta_try_label,
                primaryAction = ClientAction.TRY_PHOTO,
                secondaryAction = ClientAction.TRY_LABEL
            )

            "BARCODE_LOOKUP_FAILED" -> UiModel(
                titleResId = R.string.err_barcode_lookup_failed_title,
                messageResId = R.string.err_barcode_lookup_failed_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.SCAN_AGAIN,
                secondaryAction = ClientAction.CHECK_NETWORK
            )

            // ===== IMAGE / UPLOAD =====
            "IMAGE_TOO_LARGE",
            "FILE_TOO_LARGE",
            "FILE_REQUIRED",
            "UNSUPPORTED_IMAGE_FORMAT",
            "UNSUPPORTED_CONTENT_TYPE",
            "IMAGE_DECODE_FAILED" -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                primaryAction = ClientAction.RETAKE_PHOTO
            )

            // ===== FOOD / LABEL VISION =====
            "LOW_CONFIDENCE",
            "NO_FOOD_DETECTED",
            "NO_LABEL_DETECTED",
            "BLURRY_IMAGE" -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                secondaryCtaResId = R.string.cta_rescan_barcode,
                primaryAction = ClientAction.RETAKE_PHOTO,
                secondaryAction = ClientAction.TRY_BARCODE
            )

            // ===== RATE LIMIT / COOL DOWN =====
            "COOLDOWN_ACTIVE",
            "RATE_LIMITED",
            "TOO_MANY_IN_FLIGHT",
            "QUOTA_EXCEEDED",
            "REQUEST_IN_PROGRESS" -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.common_retry,
                primaryAction = ClientAction.RETRY_LATER
            )

            // ===== NETWORK / PROVIDER =====
            "NETWORK_ERROR",
            "TIMEOUT",
            "UPSTREAM_TIMEOUT",
            "PROVIDER_TIMEOUT",
            "PROVIDER_NETWORK_ERROR",
            "PROVIDER_RATE_LIMITED",
            "PROVIDER_UNAVAILABLE" -> UiModel(
                titleResId = R.string.err_network_title,
                messageResId = R.string.err_network_msg,
                primaryCtaResId = R.string.common_retry,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.RETRY_LATER,
                secondaryAction = ClientAction.CHECK_NETWORK
            )

            // ===== SUPPORT / CONFIG =====
            "PROVIDER_BAD_REQUEST",
            "PROVIDER_BAD_RESPONSE",
            "PROVIDER_AUTH_FAILED",
            "GEMINI_API_KEY_MISSING",
            "PROVIDER_NOT_AVAILABLE",
            "PROVIDER_NOT_CONFIGURED",
            "IMAGE_OBJECT_KEY_MISSING" -> UiModel(
                titleResId = R.string.err_support_title,
                messageResId = R.string.err_support_msg,
                primaryCtaResId = R.string.cta_contact_support,
                primaryAction = ClientAction.CONTACT_SUPPORT
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

            ClientAction.SCAN_AGAIN,
            ClientAction.TRY_BARCODE -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.cta_rescan_barcode,
                primaryAction = ClientAction.SCAN_AGAIN
            )

            ClientAction.TRY_PHOTO,
            ClientAction.RETAKE_PHOTO -> UiModel(
                titleResId = R.string.err_photo_title,
                messageResId = R.string.err_photo_msg,
                primaryCtaResId = R.string.cta_retake,
                primaryAction = action
            )

            ClientAction.CHECK_NETWORK -> UiModel(
                titleResId = R.string.err_network_title,
                messageResId = R.string.err_network_msg,
                primaryCtaResId = R.string.common_retry,
                secondaryCtaResId = R.string.cta_check_network,
                primaryAction = ClientAction.RETRY_LATER,
                secondaryAction = ClientAction.CHECK_NETWORK
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

            else -> UiModel(
                titleResId = R.string.err_generic_title,
                messageResId = R.string.err_generic_msg,
                primaryCtaResId = R.string.common_retry,
                primaryAction = ClientAction.RETRY_LATER
            )
        }
    }
}
