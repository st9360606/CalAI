package com.calai.bitecal.ui.home.ui.settings.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.bitecal.R
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import com.calai.bitecal.data.membership.api.MembershipSummaryDto
import com.calai.bitecal.data.membership.api.RewardHistoryItemDto
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.design.BiteCalPrimaryButton
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.home.ui.membership.MembershipUiMapper

@Composable
fun PremiumRewardsScreen(
    loading: Boolean,
    error: String?,
    summary: MembershipSummaryDto?,
    rewards: List<RewardHistoryItemDto>,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            BiteCalTopBar(
                title = stringResource(R.string.premium_rewards_subscription_title),
                onBack = onBack
            )
        }
    ) { inner ->
        when {
            loading -> LoadingState(Modifier.padding(inner))
            error != null -> ErrorState(
                modifier = Modifier.padding(inner),
                error = error,
                onRetry = onRetry
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = BiteCalScreenFrame.settingsHorizontal,
                        end = BiteCalScreenFrame.settingsHorizontal,
                        top = BiteCalScreenFrame.contentTopSmall,
                        bottom = BiteCalScreenFrame.settingsBottom,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SummaryCard(summary) }
                    item { LatestRewardCard(summary) }
                    item {
                        Text(
                            stringResource(R.string.premium_rewards_history_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    if (rewards.isEmpty()) {
                        item { EmptyRewardHistoryCard() }
                    } else {
                        items(rewards) { item ->
                            RewardHistoryRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.premium_rewards_loading_membership))
    }
}

@Composable
private fun ErrorState(
    modifier: Modifier,
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BiteCalScreenFrame.contentHorizontalWide),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        BiteCalPrimaryButton(
            text = stringResource(R.string.common_retry),
            onClick = onRetry,
            height = 50.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryCard(summary: MembershipSummaryDto?) {
    val status = PremiumStatus.from(summary?.premiumStatus)
    val display = MembershipUiMapper.map(
        status = status,
        currentPremiumUntil = summary?.currentPremiumUntil,
        trialDaysLeft = summary?.trialDaysLeft,
        paymentIssue = summary?.paymentIssue == true
    )
    val unavailable = stringResource(R.string.common_unavailable_dash)

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.premium_rewards_status_title),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))

            Text(
                text = display.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(6.dp))
            Text(display.subtitle.ifBlank { unavailable })
        }
    }
}

@Composable
private fun LatestRewardCard(summary: MembershipSummaryDto?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.premium_rewards_latest_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(8.dp))

            val unavailable = stringResource(R.string.common_unavailable_dash)
            Text(stringResource(R.string.premium_rewards_source_format, summary?.latestRewardSource ?: unavailable))
            Text(stringResource(R.string.premium_rewards_channel_format, friendlyRewardChannel(summary?.latestRewardChannel)))
            Text(stringResource(R.string.premium_rewards_grant_status_format, friendlyGrantStatus(summary?.latestRewardGrantStatus)))
            Text(stringResource(R.string.premium_rewards_google_defer_format, friendlyGoogleDeferStatus(summary?.latestGoogleDeferStatus)))
            Text(stringResource(R.string.premium_rewards_granted_at_format, MembershipUiMapper.formatDateOrNull(summary?.latestGrantedAtUtc) ?: unavailable))
            Text(stringResource(R.string.premium_rewards_old_expiry_format, MembershipUiMapper.formatDateOrNull(summary?.latestOldPremiumUntil) ?: unavailable))
            Text(stringResource(R.string.premium_rewards_new_expiry_format, MembershipUiMapper.formatDateOrNull(summary?.latestNewPremiumUntil) ?: unavailable))
        }
    }
}

@Composable
private fun RewardHistoryRow(item: RewardHistoryItemDto) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                item.sourceType,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(4.dp))

            val unavailable = stringResource(R.string.common_unavailable_dash)
            Text(stringResource(R.string.premium_rewards_status_format, friendlyGrantStatus(item.grantStatus)))
            Text(stringResource(R.string.premium_rewards_channel_format, friendlyRewardChannel(item.rewardChannel)))
            Text(stringResource(R.string.premium_rewards_google_defer_format, friendlyGoogleDeferStatus(item.googleDeferStatus)))
            item.errorCode?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = stringResource(R.string.premium_rewards_error_format, it),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(stringResource(R.string.premium_rewards_days_added_format, item.daysAdded))
            Text(stringResource(R.string.premium_rewards_granted_at_format, MembershipUiMapper.formatDateOrNull(item.grantedAtUtc) ?: unavailable))
            Text(stringResource(R.string.premium_rewards_old_expiry_format, MembershipUiMapper.formatDateOrNull(item.oldPremiumUntil) ?: unavailable))
            Text(stringResource(R.string.premium_rewards_new_expiry_format, MembershipUiMapper.formatDateOrNull(item.newPremiumUntil) ?: unavailable))
        }
    }
}

@Composable
private fun EmptyRewardHistoryCard() {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.premium_rewards_empty_title),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.premium_rewards_empty_body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun friendlyRewardChannel(channel: String?): String {
    return when (channel) {
        "GOOGLE_PLAY_DEFER" -> stringResource(R.string.premium_rewards_channel_google_play_defer)
        "BACKEND_ONLY" -> stringResource(R.string.premium_rewards_channel_backend_only)
        else -> channel ?: stringResource(R.string.common_unavailable_dash)
    }
}


@Composable
private fun friendlyGrantStatus(status: String?): String {
    return when (status) {
        "SUCCESS", "GRANTED" -> stringResource(R.string.premium_rewards_grant_success)
        "FAILED_RETRYABLE" -> stringResource(R.string.premium_rewards_status_retrying)
        "FAILED_FINAL" -> stringResource(R.string.premium_rewards_grant_not_granted)
        else -> status ?: stringResource(R.string.common_unavailable_dash)
    }
}

@Composable
private fun friendlyGoogleDeferStatus(status: String?): String {
    return when (status) {
        "SUCCESS" -> stringResource(R.string.premium_rewards_google_defer_success)
        "FAILED_RETRYABLE" -> stringResource(R.string.premium_rewards_status_retrying)
        "FAILED_FINAL" -> stringResource(R.string.premium_rewards_google_defer_failed)
        "NOT_REQUIRED" -> stringResource(R.string.premium_rewards_google_defer_not_required)
        else -> status ?: stringResource(R.string.common_unavailable_dash)
    }
}
