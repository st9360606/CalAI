package com.calai.app.ui.onboarding.referralsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.R
import com.calai.app.data.store.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReferralKey {
    GOOGLE_PLAY,APP_STORE, YOUTUBE, INSTAGRAM, GOOGLE, FACEBOOK, TIKTOK, X, FRIEND, OTHER
}

data class ReferralUiOption(
    val key: ReferralKey,
    val label: String,
    val iconRes: Int? = null // 允許沒有品牌圖時採用預設
)

data class ReferralUiState(
    val selected: ReferralKey = ReferralKey.APP_STORE,
    val options: List<ReferralUiOption> = emptyList()
)

@HiltViewModel
class ReferralSourceViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReferralUiState(
            options = defaultOptions()
        )
    )
    val uiState: StateFlow<ReferralUiState> = _uiState

    init {
        // 若先前已存過，啟動時帶回填
        viewModelScope.launch {
            val saved = store.referralSource()
            saved?.let { str ->
                runCatching { ReferralKey.valueOf(str) }.getOrNull()?.let { key ->
                    _uiState.update { it.copy(selected = key) }
                }
            }
        }
    }

    fun select(key: ReferralKey) {
        _uiState.update { it.copy(selected = key) }
    }

    suspend fun saveAndContinue() {
        val key = _uiState.value.selected
        store.setReferralSource(key.name)
    }

    private fun defaultOptions(): List<ReferralUiOption> = listOf(
        ReferralUiOption(ReferralKey.GOOGLE_PLAY,  "Google Play",   /*R.drawable.ic_brand_appstore*/ R.drawable.googleplay),
//        ReferralUiOption(ReferralKey.APP_STORE,  "App Store",   /*R.drawable.ic_brand_appstore*/ R.drawable.app_store),//For ios app
        ReferralUiOption(ReferralKey.YOUTUBE,    "YouTube",     /*R.drawable.ic_brand_youtube*/ R.drawable.youtube),
        ReferralUiOption(ReferralKey.INSTAGRAM,  "Instagram",   /*R.drawable.ic_brand_instagram*/ R.drawable.instagram),
        ReferralUiOption(ReferralKey.GOOGLE,     "Google",      /*R.drawable.ic_brand_google*/ R.drawable.google),
        ReferralUiOption(ReferralKey.FACEBOOK,   "Facebook",    /*R.drawable.ic_brand_facebook*/ R.drawable.facebook),
        ReferralUiOption(ReferralKey.TIKTOK,     "TikTok",      /*R.drawable.ic_brand_tiktok*/ R.drawable.tiktok),
        ReferralUiOption(ReferralKey.X,          "X",           /*R.drawable.ic_brand_x*/ R.drawable.twitter),
        ReferralUiOption(ReferralKey.OTHER,      "Other",       /*R.drawable.ic_brand_other*/ R.drawable.other),
    )
}
