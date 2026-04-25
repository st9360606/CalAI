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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import com.calai.bitecal.data.membership.api.MembershipSummaryDto
import com.calai.bitecal.data.membership.api.RewardHistoryItemDto
import com.calai.bitecal.ui.home.ui.membership.MembershipUiMapper

@OptIn(ExperimentalMaterial3Api::class)
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
            CenterAlignedTopAppBar(
                title = { Text("Premium & Rewards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        when {
            loading -> {
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Loading membership...")
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SummaryCard(summary)
                    }

                    item {
                        LatestRewardCard(summary)
                    }

                    item {
                        Text(
                            "Reward history",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    if (rewards.isEmpty()) {
                        item {
                            EmptyRewardHistoryCard()
                        }
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
private fun SummaryCard(summary: MembershipSummaryDto?) {
    val status = PremiumStatus.from(summary?.premiumStatus)
    val currentUntil = MembershipUiMapper.formatDate(summary?.currentPremiumUntil)
    val trialEndsAt = MembershipUiMapper.formatDate(summary?.trialEndsAt)

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Premium Status", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            Text(
                text = status.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(8.dp))

            if (status == PremiumStatus.TRIAL) {
                Text("Trial ends at: $trialEndsAt")
                Spacer(Modifier.height(4.dp))
            }

            Text("Premium until: $currentUntil")
        }
    }
}

@Composable
private fun LatestRewardCard(summary: MembershipSummaryDto?) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Latest reward",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(8.dp))

            Text("Source: ${summary?.latestRewardSource ?: "—"}")
            Text("Granted at: ${MembershipUiMapper.formatDate(summary?.latestGrantedAtUtc)}")
            Text("Old expiry: ${MembershipUiMapper.formatDate(summary?.latestOldPremiumUntil)}")
            Text("New expiry: ${MembershipUiMapper.formatDate(summary?.latestNewPremiumUntil)}")
        }
    }
}

@Composable
private fun RewardHistoryRow(item: RewardHistoryItemDto) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                item.sourceType,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(4.dp))

            Text("Days added: ${item.daysAdded}")
            Text("Granted at: ${MembershipUiMapper.formatDate(item.grantedAtUtc)}")
            Text("Old expiry: ${MembershipUiMapper.formatDate(item.oldPremiumUntil)}")
            Text("New expiry: ${MembershipUiMapper.formatDate(item.newPremiumUntil)}")
        }
    }
}

@Composable
private fun EmptyRewardHistoryCard() {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "No rewards yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Successful referrals will appear here after verification.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
