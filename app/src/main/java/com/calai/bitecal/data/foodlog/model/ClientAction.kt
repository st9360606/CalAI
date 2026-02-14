package com.calai.bitecal.data.foodlog.model

import kotlinx.serialization.Serializable

@Serializable
enum class ClientAction {
    RETRY_LATER,
    RETAKE_PHOTO,
    TRY_BARCODE,
    TRY_LABEL,
    ENTER_MANUALLY,
    CHECK_NETWORK,
    CONTACT_SUPPORT
}
