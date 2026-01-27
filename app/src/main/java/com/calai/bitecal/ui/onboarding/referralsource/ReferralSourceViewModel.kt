package com.calai.bitecal.ui.onboarding.referralsource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.R
import com.calai.bitecal.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReferralKey {
    APP_STORE, GOOGLE_PLAY, YOUTUBE, INSTAGRAM, GOOGLE, FACEBOOK, TIKTOK, X, FRIEND, OTHER
}

data class ReferralUiOption(
    val key: ReferralKey,
    val label: String,
    val iconRes: Int? = null
)

// ★ 預設不選（null）→ 首次進入反白、Continue disabled
data class ReferralUiState(
    val selected: ReferralKey? = null,
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
        // 若先前已存過，啟動時帶回填；若沒有則維持 null（不預選）
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
        val key = _uiState.value.selected ?: return   // 沒選就不寫（UI 也不會讓它發生）
        store.setReferralSource(key.name)
    }

    private fun defaultOptions(): List<ReferralUiOption> = listOf(
        // ReferralUiOption(ReferralKey.APP_STORE,  "App Store",   R.drawable.app_store), // iOS 用
        ReferralUiOption(ReferralKey.GOOGLE_PLAY, "Google Play", R.drawable.googleplay),
        ReferralUiOption(ReferralKey.YOUTUBE,     "YouTube",     R.drawable.youtube),
        ReferralUiOption(ReferralKey.INSTAGRAM,   "Instagram",   R.drawable.instagram),
        ReferralUiOption(ReferralKey.GOOGLE,      "Google",      R.drawable.google),
        ReferralUiOption(ReferralKey.FACEBOOK,    "Facebook",    R.drawable.facebook),
        ReferralUiOption(ReferralKey.TIKTOK,      "TikTok",      R.drawable.tiktok),
        ReferralUiOption(ReferralKey.X,           "X",           R.drawable.twitter),
        ReferralUiOption(ReferralKey.OTHER,       "Other",       R.drawable.other),
    )
}
