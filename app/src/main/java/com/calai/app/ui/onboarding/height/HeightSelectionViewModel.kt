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

    // ★ 改成 Float，預設 165.0f
    val heightCmState = usr.heightCmFlow
        .map { it ?: 165.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 165.0f)

    val heightUnitState = usr.heightUnitFlow
        .map { it ?: UserProfileStore.HeightUnit.FT_IN }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserProfileStore.HeightUnit.FT_IN
        )

    /**
     * ✅ 關鍵：給 Continue 用
     * - 不要在 VM 內 launch，讓 UI 呼叫端可以 await 完成後再 onNext()
     * - 一律以 cm 為主 SSOT
     */
    suspend fun saveAll(
        cm: Float,
        useMetric: Boolean,
        feet: Int,
        inches: Int
    ) {
        usr.setHeightCm(cm)

        if (useMetric) {
            usr.setHeightUnit(UserProfileStore.HeightUnit.CM)
            usr.clearHeightImperial()
        } else {
            usr.setHeightUnit(UserProfileStore.HeightUnit.FT_IN)
            usr.setHeightImperial(feet, inches)
        }
    }

    // ====== 你原本的 API：保留（但不建議 Continue 用這些） ======
    fun saveHeightCm(cm: Float) = viewModelScope.launch { usr.setHeightCm(cm) }

    fun saveHeightUnit(unit: UserProfileStore.HeightUnit) =
        viewModelScope.launch { usr.setHeightUnit(unit) }

    fun saveHeightImperial(feet: Int, inches: Int) =
        viewModelScope.launch { usr.setHeightImperial(feet, inches) }

    fun clearHeightImperial() = viewModelScope.launch { usr.clearHeightImperial() }
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
