package com.calai.bitecal.data.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

class FakeBillingGateway(
    app: Application
) : BillingGateway {

    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun queryActiveSubscriptions(): List<ActiveSub> {
        val productId = prefs.getString(KEY_PRODUCT_ID, null)
        val purchaseToken = prefs.getString(KEY_PURCHASE_TOKEN, null)
        val acknowledged = prefs.getBoolean(KEY_ACKNOWLEDGED, false)

        if (productId.isNullOrBlank() || purchaseToken.isNullOrBlank()) {
            Log.d(TAG, "queryActiveSubscriptions fake empty")
            return emptyList()
        }

        Log.d(
            TAG,
            "queryActiveSubscriptions fake active productId=$productId acknowledged=$acknowledged"
        )

        return listOf(
            ActiveSub(
                productId = productId,
                purchaseToken = purchaseToken,
                acknowledged = acknowledged
            )
        )
    }

    override suspend fun querySubscriptionOfferPrice(
        productId: String,
        offerTag: String?
    ): SubscriptionOfferPriceText? {
        Log.d(TAG, "FAKE querySubscriptionOfferPrice productId=$productId, offerTag=$offerTag")

        if (productId != BiteCalBillingProducts.YEARLY) {
            return null
        }

        val isDiscountOffer =
            offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_DISCOUNT_YEARLY ||
                    offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY

        return SubscriptionOfferPriceText(
            productId = productId,
            offerTag = offerTag,
            formattedPrice = if (isDiscountOffer) "NT$649.00" else "NT$999.00",
            formattedMonthlyEquivalent = if (isDiscountOffer) "NT$54.08" else "NT$83.25"
        )
    }

    override suspend fun launchSubscriptionPurchase(
        activity: Activity,
        productId: String,
        offerTag: String?
    ): BillingPurchaseResult {
        Log.d(TAG, "FAKE launchSubscriptionPurchase productId=$productId, offerTag=$offerTag")

        delay(500)

        val existingToken = prefs.getString(KEY_PURCHASE_TOKEN, null)
        if (!existingToken.isNullOrBlank()) {
            Log.d(TAG, "FAKE purchase already owned productId=$productId")
            return BillingPurchaseResult.AlreadyOwned
        }

        val phase =
            if (offerTag == BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY) {
                "trial"
            } else {
                "paid"
            }

        val fakeToken = "fake-dev-sub::$productId::$phase::${System.currentTimeMillis()}"

        prefs.edit()
            .putString(KEY_PRODUCT_ID, productId)
            .putString(KEY_PURCHASE_TOKEN, fakeToken)
            .putBoolean(KEY_ACKNOWLEDGED, false)
            .apply()

        Log.d(TAG, "FAKE purchase saved productId=$productId phase=$phase")

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

        val currentToken = prefs.getString(KEY_PURCHASE_TOKEN, null)

        if (currentToken == purchaseToken) {
            prefs.edit()
                .putBoolean(KEY_ACKNOWLEDGED, true)
                .apply()
        }

        return true
    }

    private companion object {
        const val TAG = "FakeBillingGateway"

        const val PREFS_NAME = "fake_billing_gateway"
        const val KEY_PRODUCT_ID = "product_id"
        const val KEY_PURCHASE_TOKEN = "purchase_token"
        const val KEY_ACKNOWLEDGED = "acknowledged"
    }
}
