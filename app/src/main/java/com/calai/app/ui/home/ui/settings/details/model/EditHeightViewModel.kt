package com.calai.app.ui.home.ui.settings.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.roundCm1
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.calai.app.data.profile.repo.round1Floor
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class EditHeightViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        val initializing: Boolean = true,   // ✅ NEW
        val saving: Boolean = false,
        val error: String? = null,
        val toastMessage: String? = null
    )

    data class InitialHeight(
        val unit: UserProfileStore.HeightUnit,
        val cm: Double,     // 一位小數
        val feet: Int,
        val inches: Int
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private val _initialHeight = MutableStateFlow(
        InitialHeight(
            unit = UserProfileStore.HeightUnit.CM,
            cm = 170.0,
            feet = 5,
            inches = 7
        )
    )
    val initialHeight: StateFlow<InitialHeight> = _initialHeight.asStateFlow()

    private var initOnce = false

    /**
     * ✅ 進畫面呼叫一次：
     * - DB 有 feet+inches → 初始用 FT/IN
     * - DB 沒 feet/inches → 初始用 CM（含小數 1 位）
     * - DB/網路拿不到 → fallback 用本機 DataStore，最後再 fallback 170.0
     */
    fun initIfNeeded() {
        if (initOnce) return
        initOnce = true

        viewModelScope.launch {
            _ui.update { it.copy(initializing = true, error = null) }

            // 先從本機拿 fallback（避免完全沒值）
            val local = runCatching { store.snapshot() }.getOrNull()
            val localCm = roundCm1((local?.heightCm ?: 170f).toDouble()).toDouble()
            val localFtIn = cmToFeetInches1(localCm)
            val localFeet = local?.heightFeet ?: localFtIn.first
            val localInches = local?.heightInches ?: localFtIn.second

            val resolved = runCatching {
                // ✅ 直接全量同步：讓 store 存到 DB 的 height/cm/ft/in
                profileRepo.syncServerProfileToStore()

                val snap = store.snapshot()

                val cm = roundCm1((snap.heightCm ?: localCm.toFloat()).toDouble()).toDouble()
                val hasImperial = (snap.heightFeet != null && snap.heightInches != null)

                if (hasImperial) {
                    InitialHeight(
                        unit = UserProfileStore.HeightUnit.FT_IN,
                        cm = cm,
                        feet = snap.heightFeet!!,
                        inches = snap.heightInches!!
                    )
                } else {
                    val (ft, inch) = cmToFeetInches1(cm)
                    InitialHeight(
                        unit = UserProfileStore.HeightUnit.CM,
                        cm = cm,
                        feet = ft,
                        inches = inch
                    )
                }
            }.getOrElse { e ->
                Log.w("EditHeightVM", "initIfNeeded failed: ${e.message}", e)
                // fallback：本機（或 170）
                InitialHeight(
                    unit = local?.heightUnit ?: UserProfileStore.HeightUnit.CM,
                    cm = localCm,
                    feet = localFeet,
                    inches = localInches
                )
            }

            _initialHeight.value = resolved
            _ui.update { it.copy(initializing = false) }
        }
    }

    fun saveAndSyncHeight(
        useMetric: Boolean,
        cmVal: Double,
        feet: Int,
        inches: Int,
        onSuccess: () -> Unit
    ) {
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, toastMessage = null) }

            val cmToSave: Float = if (useMetric) {
                roundCm1(cmVal)
            } else {
                feetInchesToCm1(feet, inches).toFloat()   // ✅ ft/in 為準
            }
            runCatching { store.setHeightCm(cmToSave) }

            if (useMetric) {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.CM) }
                runCatching { store.clearHeightImperial() }
            } else {
                runCatching { store.setHeightUnit(UserProfileStore.HeightUnit.FT_IN) }
                runCatching { store.setHeightImperial(feet, inches) }
            }

            val result = profileRepo.upsertFromLocal()
            result.onSuccess {
                //先結束 loading、先讓 UI 回上一頁（體感速度會快很多）
                _ui.update {
                    it.copy(
                        saving = false,
                        error = null,
                        toastMessage = "Saved successfully!"
                    )
                }
                onSuccess()
                // 把「回寫校正」放到背景做，不要卡住 UI
                viewModelScope.launch {
                    runCatching { profileRepo.syncServerProfileToStore() }
                        .onFailure { e ->
                            Log.w("EditHeightVM", "syncServerProfileToStore failed: ${e.message}", e)
                        }
                }

            }.onFailure { e ->
                val msg = e.message?.takeIf { it.isNotBlank() }
                    ?: "Network error. Saved locally, but failed to sync."

                _ui.update {
                    it.copy(
                        saving = false,
                        error = msg,
                        toastMessage = msg
                    )
                }
            }
        }
    }
}

// ★ cm ↔ ft/in：cm 允許 1 位小數，回傳的 cm 也做 1 位小數捨去
fun feetInchesToCm1(feet: Int, inches: Int): Double {
    val totalInches = feet * 12 + inches
    val cm = totalInches * 2.54
    return round1Floor(cm)
}

fun cmToFeetInches1(cm: Double): Pair<Int, Int> {
    val totalInches = (cm / 2.54).toInt()      // floor
    val feet = totalInches / 12
    val inch = totalInches % 12
    return feet to inch
}
