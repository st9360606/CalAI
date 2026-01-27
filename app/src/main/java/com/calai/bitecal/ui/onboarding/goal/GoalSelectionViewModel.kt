package com.calai.bitecal.ui.onboarding.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GoalKey { LOSE, MAINTAIN, GAIN, HEALTHY_EATING }

data class GoalUiState(
    val selected: GoalKey? = null
)

@HiltViewModel
class GoalSelectionViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState

    init {
        viewModelScope.launch {
            store.goal()?.let { saved ->
                runCatching { GoalKey.valueOf(saved) }
                    .getOrNull()
                    ?.let { key -> _uiState.update { it.copy(selected = key) } }
            }
        }
    }

    fun select(key: GoalKey) {
        _uiState.update { it.copy(selected = key) }
    }

    fun saveSelected() {
        val sel = _uiState.value.selected ?: return
        viewModelScope.launch { store.setGoal(sel.name) }
    }
}
