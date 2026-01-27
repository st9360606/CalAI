package com.calai.bitecal.ui.home.ui.settings.details.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditWaterGoalViewModelStateTest {

    @Test
    fun canSave_false_when_same_as_previous() {
        val s = EditWaterGoalViewModel.UiState(previousGoalMl = 2000, input = "2000", isSaving = false)
        assertFalse(s.canSave())
    }

    @Test
    fun canSave_true_when_changed_and_in_range() {
        val s = EditWaterGoalViewModel.UiState(previousGoalMl = 2000, input = "2500", isSaving = false)
        assertTrue(s.canSave())
    }

    @Test
    fun canSave_false_when_out_of_range() {
        val s = EditWaterGoalViewModel.UiState(previousGoalMl = 2000, input = "50000", isSaving = false)
        assertFalse(s.canSave())
    }

    @Test
    fun canSave_false_when_saving() {
        val s = EditWaterGoalViewModel.UiState(previousGoalMl = 2000, input = "2500", isSaving = true)
        assertFalse(s.canSave())
    }
}
