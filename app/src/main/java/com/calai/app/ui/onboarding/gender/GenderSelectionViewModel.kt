// app/src/main/java/com/calai/app/ui/onboarding/gender/GenderSelectionViewModel.kt
package com.calai.app.ui.onboarding.gender

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 供畫面與導航使用的性別列舉
enum class GenderKey { MALE, FEMALE, OTHER }

// 預設不選（null）→ 進頁面反白，Continue disabled
data class GenderUiState(
    val selected: GenderKey? = null
)

@HiltViewModel
class GenderSelectionViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenderUiState())
    val uiState: StateFlow<GenderUiState> = _uiState

    init {
        // 啟動時回填既有選擇（若有）；首次進入保持 null（不預選）
        viewModelScope.launch {
            val saved = store.gender()
            runCatching { GenderKey.valueOf(saved ?: "") }
                .getOrNull()
                ?.let { key -> _uiState.update { it.copy(selected = key) } }
        }
    }

    /** UI 點選時更新暫存狀態 */
    fun select(key: GenderKey) {
        _uiState.update { it.copy(selected = key) }
    }

    /** 將目前選擇寫入 DataStore（僅在非空時） */
    suspend fun saveSelectedGender() {
        val sel = _uiState.value.selected ?: return
        store.setGender(sel.name)
    }
}
