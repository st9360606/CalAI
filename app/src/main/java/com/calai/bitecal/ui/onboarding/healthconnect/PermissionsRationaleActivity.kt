package com.calai.bitecal.ui.onboarding.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.res.stringResource
import com.calai.bitecal.R

/**
 * Health Connect / Android system entry for explaining why BiteCal requests health permissions.
 *
 * Do not delete this Activity while Health Connect permissions are requested. The system opens it
 * from Health Connect permission screens through the manifest intent filters.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HealthConnectPermissionRationaleContent(
                titlePrefix = stringResource(R.string.onboard_hc_permission_rationale_title_prefix),
                titleService = stringResource(R.string.onboard_hc_permission_rationale_title_service),
                body = stringResource(R.string.onboard_hc_permission_rationale_body),
                buttonText = stringResource(R.string.onboard_hc_permission_rationale_button),
                onClose = { finish() }
            )
        }
    }
}
