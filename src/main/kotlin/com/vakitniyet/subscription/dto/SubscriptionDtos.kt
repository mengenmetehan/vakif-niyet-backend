package com.vakitniyet.subscription.dto

data class SubscriptionStatusResponse(
    val plan: String,
    val status: String,
    val isActive: Boolean,
    val trialDaysLeft: Long?,
    val currentPeriodEnd: String?
)

data class VerifyReceiptRequest(
    val receiptData: String,
    val productId: String
)
