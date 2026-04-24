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
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.membership.api.MembershipSummaryDto
import com.calai.bitecal.data.membership.api.RewardHistoryItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumRewardsScreen(
    summary: MembershipSummaryDto?,
    rewards: List<RewardHistoryItemDto>,
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
                Text("Reward history", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            items(rewards) { item ->
                RewardHistoryRow(item)
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: MembershipSummaryDto?) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Premium Status", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(summary?.premiumStatus ?: "FREE", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Premium until: ${summary?.currentPremiumUntil ?: "—"}")
        }
    }
}

@Composable
private fun LatestRewardCard(summary: MembershipSummaryDto?) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Latest reward", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            Text("Source: ${summary?.latestRewardSource ?: "—"}")
            Text("Granted at: ${summary?.latestGrantedAtUtc ?: "—"}")
            Text("Old expiry: ${summary?.latestOldPremiumUntil ?: "—"}")
            Text("New expiry: ${summary?.latestNewPremiumUntil ?: "—"}")
        }
    }
}

@Composable
private fun RewardHistoryRow(item: RewardHistoryItemDto) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.sourceType, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(4.dp))
            Text("Days added: ${item.daysAdded}")
            Text("Granted at: ${item.grantedAtUtc ?: "—"}")
            Text("Old expiry: ${item.oldPremiumUntil ?: "—"}")
            Text("New expiry: ${item.newPremiumUntil ?: "—"}")
        }
    }
}
