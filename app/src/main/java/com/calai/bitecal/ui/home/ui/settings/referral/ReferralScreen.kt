package com.calai.bitecal.ui.home.ui.settings.referral

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calai.bitecal.data.referral.api.ReferralClaimItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    promoCode: String,
    successCount: Long,
    pendingCount: Long,
    rejectedCount: Long,
    recentClaims: List<ReferralClaimItemDto>,
    claimInFlight: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmitClaim: (String) -> Unit
) {
    var inputCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Referral") },
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
                SummaryCard(
                    promoCode = promoCode,
                    successCount = successCount,
                    pendingCount = pendingCount,
                    rejectedCount = rejectedCount
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Claim inviter code", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Promo code") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onSubmitClaim(inputCode) },
                            enabled = !claimInFlight && inputCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (claimInFlight) "Submitting..." else "Submit")
                        }
                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                Text("Recent claims", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            items(recentClaims) { item ->
                ClaimRow(item)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    promoCode: String,
    successCount: Long,
    pendingCount: Long,
    rejectedCount: Long
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Your promo code", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(promoCode, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            Text("Success: $successCount")
            Text("Pending verification: $pendingCount")
            Text("Rejected: $rejectedCount")
        }
    }
}

@Composable
private fun ClaimRow(item: ReferralClaimItemDto) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(4.dp))
            Text("Status: ${item.status}")
            item.verificationDeadlineUtc?.let { Text("Verification deadline: $it") }
            item.rewardedAtUtc?.let { Text("Rewarded at: $it") }
            item.rejectReason?.let { Text("Reason: $it") }
        }
    }
}
