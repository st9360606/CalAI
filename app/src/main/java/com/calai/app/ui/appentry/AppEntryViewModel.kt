package com.calai.app.ui.appentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.auth.repo.AuthRepository
import com.calai.app.data.auth.store.UserProfileStore
import com.calai.app.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class StartDest { HOME, LANDING, ERROR }

@HiltViewModel
class AppEntryViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profile: ProfileRepository,
    private val store: UserProfileStore
) : ViewModel() {

    /** 冷啟：若未寫入 locale，補上裝置語系（避免上傳為 null） */
    suspend fun initLocaleIfAbsent() = withContext(Dispatchers.IO) {
        val cur = store.localeTag()
        if (cur.isNullOrBlank()) {
            store.setLocaleTag(Locale.getDefault().toLanguageTag())
        }
    }

    /** 導頁決策：已登入➜查伺服器是否已有檔；未登入➜Landing；錯誤➜ERROR */
    suspend fun decideStartDestination(): StartDest = withContext(Dispatchers.IO) {
        try {
            if (!auth.isSignedIn()) return@withContext StartDest.LANDING
            val exists = profile.existsOnServer()
            if (exists) StartDest.HOME else StartDest.LANDING
        } catch (_: Exception) {
            StartDest.ERROR
        }
    }
}
