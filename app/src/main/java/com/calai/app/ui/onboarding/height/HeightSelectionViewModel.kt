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

    fun saveHeightCm(cm: Int) = viewModelScope.launch { usr.setHeightCm(cm) }
}

/** 換算工具（提供給畫面用） */
fun cmToFeetInches(cm: Int): Pair<Int, Int> {
    val totalInches = (cm / 2.54).roundToInt()
    val feet = totalInches / 12
    val inches = totalInches % 12
    return feet to inches
}
fun feetInchesToCm(feet: Int, inches: Int): Int =
    ((feet * 12 + inches) * 2.54).roundToInt()
