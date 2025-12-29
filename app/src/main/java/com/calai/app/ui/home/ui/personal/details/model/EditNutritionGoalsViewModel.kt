package com.calai.app.ui.home.ui.personal.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.api.NutritionGoalsManualRequest
import com.calai.app.data.profile.api.UserProfileDto
import com.calai.app.data.profile.repo.NutritionGoalsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionGoals(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int,
    val sugarG: Int,
    val sodiumMg: Int
)

data class NutritionGoalsDraft(
    val kcal: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val fiberG: String = "",
    val sugarG: String = "",
    val sodiumMg: String = ""
)

data class NutritionGoalsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val expandedMicros: Boolean = false,
    val original: NutritionGoals? = null,
    val draft: NutritionGoalsDraft = NutritionGoalsDraft(),

    // 全域錯誤（網路/未知）
    val error: String? = null,

    // 欄位錯誤（顯示在每個輸入框下方）
    val fieldErrors: Map<Field, String> = emptyMap()
) {
    enum class Field { KCAL, PROTEIN, CARBS, FAT, FIBER, SUGAR, SODIUM }

    val isDirty: Boolean
        get() {
            val o = original ?: return false
            val d = parseDraftOrNull(draft) ?: return true
            return d != o
        }

    val canDone: Boolean get() = original != null && isDirty && !saving

    companion object {

        private fun toIntOrNull(s: String): Int? =
            s.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

        fun validateAll(d: NutritionGoalsDraft): Map<Field, String> {
            val errors = linkedMapOf<Field, String>()

            fun check(field: Field, label: String, raw: String) {
                val v = toIntOrNull(raw)
                if (v == null) {
                    errors[field] = "Please enter $label."
                } else if (v <= 0) {
                    errors[field] = "$label must be greater than 0."
                }
            }

            check(Field.KCAL, "calories", d.kcal)
            check(Field.PROTEIN, "protein", d.proteinG)
            check(Field.CARBS, "carbs", d.carbsG)
            check(Field.FAT, "fat", d.fatG)
            check(Field.FIBER, "fiber", d.fiberG)
            check(Field.SUGAR, "sugar", d.sugarG)
            check(Field.SODIUM, "sodium", d.sodiumMg)

            return errors
        }

        fun parseDraftOrNull(d: NutritionGoalsDraft): NutritionGoals? {
            val errs = validateAll(d)
            if (errs.isNotEmpty()) return null

            return NutritionGoals(
                kcal = d.kcal.trim().toInt(),
                proteinG = d.proteinG.trim().toInt(),
                carbsG = d.carbsG.trim().toInt(),
                fatG = d.fatG.trim().toInt(),
                fiberG = d.fiberG.trim().toInt(),
                sugarG = d.sugarG.trim().toInt(),
                sodiumMg = d.sodiumMg.trim().toInt()
            )
        }

        fun fromProfile(p: UserProfileDto): NutritionGoals {
            return NutritionGoals(
                kcal = p.kcal ?: 0,
                proteinG = p.proteinG ?: 0,
                carbsG = p.carbsG ?: 0,
                fatG = p.fatG ?: 0,
                fiberG = p.fiberG ?: 0,
                sugarG = p.sugarG ?: 0,
                sodiumMg = p.sodiumMg ?: 0
            )
        }
    }
}

@HiltViewModel
class NutritionGoalsViewModel @Inject constructor(
    private val repo: NutritionGoalsRepository
) : ViewModel() {

    sealed interface UiEvent {
        data object Saved : UiEvent
    }

    private val _ui = MutableStateFlow(NutritionGoalsUiState())
    val ui: StateFlow<NutritionGoalsUiState> = _ui

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /** ✅ 強制重新抓 profile（AutoGenerate 回來會用到） */
    fun reload() {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }

            val p = runCatching { repo.fetchProfileOrNull() }
                .getOrElse { e ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = e.message?.takeIf { it.isNotBlank() } ?: "Load failed",
                            fieldErrors = emptyMap()
                        )
                    }
                    return@launch
                }

            if (p == null) {
                _ui.update { it.copy(loading = false, error = "Profile not found") }
                return@launch
            }

            val g = NutritionGoalsUiState.fromProfile(p)
            _ui.update {
                it.copy(
                    loading = false,
                    original = g,
                    draft = NutritionGoalsUiState.toDraftFixed(g),
                    error = null,
                    fieldErrors = emptyMap()
                )
            }
        }
    }

    fun loadIfNeeded() {
        if (_ui.value.original != null || _ui.value.loading.not()) return

        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }

            val p = runCatching { repo.fetchProfileOrNull() }
                .getOrElse { e ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            error = e.message?.takeIf { it.isNotBlank() } ?: "Load failed",
                            fieldErrors = emptyMap()
                        )
                    }
                    return@launch
                }

            if (p == null) {
                _ui.update { it.copy(loading = false, error = "Profile not found") }
                return@launch
            }

            val g = NutritionGoalsUiState.fromProfile(p)
            _ui.update {
                it.copy(
                    loading = false,
                    original = g,
                    draft = NutritionGoalsUiState.toDraftFixed(g),
                    error = null,
                    fieldErrors = emptyMap()
                )
            }
        }
    }

    fun toggleMicros() = _ui.update { it.copy(expandedMicros = !it.expandedMicros) }

    private fun digitsOnly(s: String): String = s.filter { it.isDigit() }

    private fun setDraft(
        clearField: NutritionGoalsUiState.Field,
        transform: (NutritionGoalsDraft) -> NutritionGoalsDraft
    ) {
        _ui.update { state ->
            state.copy(
                draft = transform(state.draft),
                error = null,
                fieldErrors = state.fieldErrors - clearField
            )
        }
    }

    fun onKcal(v: String) = setDraft(NutritionGoalsUiState.Field.KCAL) { it.copy(kcal = digitsOnly(v)) }
    fun onProtein(v: String) = setDraft(NutritionGoalsUiState.Field.PROTEIN) { it.copy(proteinG = digitsOnly(v)) }
    fun onCarbs(v: String) = setDraft(NutritionGoalsUiState.Field.CARBS) { it.copy(carbsG = digitsOnly(v)) }
    fun onFat(v: String) = setDraft(NutritionGoalsUiState.Field.FAT) { it.copy(fatG = digitsOnly(v)) }
    fun onFiber(v: String) = setDraft(NutritionGoalsUiState.Field.FIBER) { it.copy(fiberG = digitsOnly(v)) }
    fun onSugar(v: String) = setDraft(NutritionGoalsUiState.Field.SUGAR) { it.copy(sugarG = digitsOnly(v)) }
    fun onSodium(v: String) = setDraft(NutritionGoalsUiState.Field.SODIUM) { it.copy(sodiumMg = digitsOnly(v)) }

    fun revert() {
        val o = _ui.value.original ?: return
        _ui.update {
            it.copy(
                draft = NutritionGoalsUiState.toDraftFixed(o),
                error = null,
                fieldErrors = emptyMap()
            )
        }
    }

    fun done() {
        val o = _ui.value.original ?: return

        val fieldErrors = NutritionGoalsUiState.validateAll(_ui.value.draft)
        if (fieldErrors.isNotEmpty()) {
            _ui.update { it.copy(fieldErrors = fieldErrors, error = null) }
            return
        }

        val parsed = NutritionGoalsUiState.parseDraftOrNull(_ui.value.draft)
            ?: run {
                _ui.update { it.copy(error = "Please check your inputs.", fieldErrors = fieldErrors) }
                return
            }

        if (parsed == o) return

        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }

            runCatching {
                val req = NutritionGoalsManualRequest(
                    kcal = parsed.kcal,
                    proteinG = parsed.proteinG,
                    carbsG = parsed.carbsG,
                    fatG = parsed.fatG,
                    fiberG = parsed.fiberG,
                    sugarG = parsed.sugarG,
                    sodiumMg = parsed.sodiumMg
                )
                repo.setManualGoalsAndRefresh(req)
            }.onSuccess { profile ->
                val g = NutritionGoalsUiState.fromProfile(profile)
                _ui.update {
                    it.copy(
                        saving = false,
                        original = g,
                        draft = NutritionGoalsUiState.toDraftFixed(g),
                        error = null,
                        fieldErrors = emptyMap()
                    )
                }
                // ✅ 儲存成功事件（給畫面/導航用）
                _events.tryEmit(UiEvent.Saved)
            }.onFailure { e ->
                _ui.update { it.copy(saving = false, error = e.message ?: "Update failed") }
            }
        }
    }
}

private fun NutritionGoalsUiState.Companion.toDraftFixed(g: NutritionGoals): NutritionGoalsDraft {
    return NutritionGoalsDraft(
        kcal = g.kcal.toString(),
        proteinG = g.proteinG.toString(),
        carbsG = g.carbsG.toString(),
        fatG = g.fatG.toString(),
        fiberG = g.fiberG.toString(),
        sugarG = g.sugarG.toString(),
        sodiumMg = g.sodiumMg.toString()
    )
}
