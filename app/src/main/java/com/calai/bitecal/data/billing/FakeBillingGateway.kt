package com.calai.bitecal.data.billing

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.delay

class FakeBillingGateway : BillingGateway {

    override suspend fun queryActiveSubscriptions(): List<ActiveSub> {
        Log.d(TAG, "queryActiveSubscriptions fake empty")
        return emptyList()
    }

    override suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        offerTag: String?
    ): BillingPurchaseResult {
        Log.d(TAG, "FAKE launchSubscriptionPurchase productId=$productId, offerTag=$offerTag")

        delay(500)

        return BillingPurchaseResult.Success(
            ActiveSub(
                productId = productId,
                purchaseToken = "fake-dev-purchase-token-${System.currentTimeMillis()}",
                acknowledged = false
            )
        )
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
        Log.d(TAG, "FAKE acknowledgePurchase purchaseToken=$purchaseToken")
        delay(100)
        return true
    }

    private companion object {
        const val TAG = "FakeBillingGateway"
    }
}
