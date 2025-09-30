package com.calai.app.ui.onboarding.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.store.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ExerciseFrequencyViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    // 預設 3~5 次/週 → 5（取上限代表值，與 0–2→2、6+→7 一致）
    val freqState = store.exerciseFreqPerWeekFlow
        .map { it ?: 5 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    fun save(v: Int) = viewModelScope.launch {
        store.setExerciseFreqPerWeek(v.coerceIn(0, 7))
    }
}
