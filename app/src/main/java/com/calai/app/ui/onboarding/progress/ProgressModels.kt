package com.calai.app.ui.onboarding.progress

data class ProgressUiState(
    val percent: Int = 0,
    val phase: ProgressPhase = ProgressPhase.P1,
    val checks: Checks = Checks(),
    val done: Boolean = false
)

enum class ProgressPhase { P1, P2, P3, P4 }

data class Checks(
    val calories: Boolean = false,
    val carbs: Boolean = false,
    val protein: Boolean = false,
    val fats: Boolean = false,
    val healthScore: Boolean = false
)
