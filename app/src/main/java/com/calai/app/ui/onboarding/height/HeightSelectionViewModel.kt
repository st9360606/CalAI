package com.calai.app.ui.onboarding.height

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HeightSelectionViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    // ✅ 預設 165 cm（英制顯示 5ft 4in）
    val heightCmState = usr.heightCmFlow
        .map { it ?: 165 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 165)

    // 預設顯示英制；如果想預設顯示公制改成 HeightUnit.CM
    val heightUnitState = usr.heightUnitFlow
        .map { it ?: UserProfileStore.HeightUnit.FT_IN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileStore.HeightUnit.FT_IN)

    fun saveHeightCm(cm: Int) = viewModelScope.launch { usr.setHeightCm(cm) }
    fun saveHeightUnit(unit: UserProfileStore.HeightUnit) =
        viewModelScope.launch { usr.setHeightUnit(unit) }

    fun saveHeightImperial(feet: Int, inches: Int) = viewModelScope.launch {
        usr.setHeightImperial(feet, inches)
    }
    fun clearHeightImperial() = viewModelScope.launch {
        usr.clearHeightImperial()
    }
}

/** 換算工具（無條件捨去） */
fun cmToFeetInches(cm: Int): Pair<Int, Int> {
    val totalInches = (cm / 2.54).toInt()  // floor
    val feet = totalInches / 12
    val inch = totalInches % 12
    return feet to inch
}

fun feetInchesToCm(feet: Int, inches: Int): Int {
    val totalInches = feet * 12 + inches
    return (totalInches * 2.54).toInt()     // floor
}
