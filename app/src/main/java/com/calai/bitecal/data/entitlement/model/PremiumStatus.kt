package com.calai.bitecal.data.entitlement.model

enum class PremiumStatus {
    TRIAL,
    PREMIUM,
    FREE;

    companion object {
        fun from(raw: String?): PremiumStatus {
            return when (raw?.trim()?.uppercase()) {
                "TRIAL" -> TRIAL
                "PREMIUM" -> PREMIUM
                else -> FREE
            }
        }
    }
}
