package com.calai.app.ui.onboarding.height

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.store.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HeightSelectionViewModel @Inject constructor(
    private val usr: UserProfileStore
) : ViewModel() {

    // 預設值 170 cm
    val heightCmState = usr.heightCmFlow
        .map { it ?: 170 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 170)

    // 單位：預設顯示 ft/in（你想預設 cm 就改成 HeightUnit.CM）
    val heightUnitState = usr.heightUnitFlow
        .map { it ?: UserProfileStore.HeightUnit.FT_IN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileStore.HeightUnit.FT_IN)

    fun saveHeightCm(cm: Int) = viewModelScope.launch { usr.setHeightCm(cm) }
    fun saveHeightUnit(unit: UserProfileStore.HeightUnit) =
        viewModelScope.launch { usr.setHeightUnit(unit) }
}

/** 換算工具（提供給畫面用） */
fun cmToFeetInches(cm: Int): Pair<Int, Int> {
    val inches = (cm / 2.54).roundToInt()  // 四捨五入到最近的英吋
    val feet = inches / 12
    val inch = inches % 12
    return feet to inch
}

fun feetInchesToCm(feet: Int, inches: Int): Int {
    val totalInches = feet * 12 + inches       // UI 已限制 0..11，否則可先做 normalize
    return (totalInches * 2.54).roundToInt() // 四捨五入到最近的整數公分
}
