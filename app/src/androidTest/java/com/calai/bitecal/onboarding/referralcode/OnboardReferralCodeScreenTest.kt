package com.calai.bitecal.ui.onboarding.referralcode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.calai.bitecal.R
import org.junit.Rule
import org.junit.Test

class OnboardReferralCodeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_submitDisabled_continueDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val submit = context.getString(R.string.onboard_referral_code_submit)
        val continueText = context.getString(R.string.common_continue_btn)

        composeRule.setContent {
            OnboardReferralCodeScreen(
                ui = OnboardReferralCodeUiState(),
                onCodeChanged = {},
                onSubmit = {},
                onContinue = {},
                onSkipAndContinue = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText(submit).assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithText(continueText).assertIsDisplayed()
    }

    @Test
    fun appliedState_successHintDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val applied = context.getString(R.string.onboard_referral_code_applied)

        composeRule.setContent {
            OnboardReferralCodeScreen(
                ui = OnboardReferralCodeUiState(
                    code = "GOOD123",
                    applied = true,
                ),
                onCodeChanged = {},
                onSubmit = {},
                onContinue = {},
                onSkipAndContinue = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText(applied).assertIsDisplayed()
    }

    @Test
    fun errorState_skipAndContinueDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val error = context.getString(R.string.onboard_referral_code_error_risk)
        val skip = context.getString(R.string.onboard_referral_code_skip_and_continue)

        composeRule.setContent {
            OnboardReferralCodeScreen(
                ui = OnboardReferralCodeUiState(
                    code = "GOOD123",
                    errorCode = "RISK_REJECTED",
                    showSkipAndContinue = true,
                ),
                onCodeChanged = {},
                onSubmit = {},
                onContinue = {},
                onSkipAndContinue = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText(error).assertIsDisplayed()
        composeRule.onNodeWithText(skip).assertIsDisplayed()
    }
}
