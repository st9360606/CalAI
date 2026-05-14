package com.calai.bitecal.data.billing

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.delay

class FakeBillingGateway : BillingGateway {

    override suspend fun queryActiveSubscriptions(): List<ActiveSub> {
        Log.d(TAG, "queryActiveSubscriptions fake empty; backend /entitlements/me is source of truth")
        return emptyList()
    }


    override suspend fun querySubscriptionOfferPrice(
        productId: String,
        offerTag: String?
    ): SubscriptionOfferPriceText? {
        Log.d(TAG, "FAKE querySubscriptionOfferPrice productId=$productId, offerTag=$offerTag")

        if (productId != BiteCalBillingProducts.YEARLY) {
            return null
        }

        val isDiscountOffer = offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_DISCOUNT_YEARLY ||
                offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY

        return SubscriptionOfferPriceText(
            productId = productId,
            offerTag = offerTag,
            formattedPrice = if (isDiscountOffer) "NT$649.00" else "NT$999.00",
            formattedMonthlyEquivalent = if (isDiscountOffer) "NT$54.08/mo" else "NT$83.25/mo"
        )
    }

    override suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        offerTag: String?
    ): BillingPurchaseResult {
        Log.d(TAG, "FAKE launchSubscriptionPurchase productId=$productId, offerTag=$offerTag")

        delay(500)

        val phase =
            if (offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY) {
                "trial"
            } else {
                "paid"
            }

        val fakeToken = "fake-dev-sub::$productId::$phase::${System.currentTimeMillis()}"

        return BillingPurchaseResult.Success(
            ActiveSub(
                productId = productId,
                purchaseToken = fakeToken,
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
