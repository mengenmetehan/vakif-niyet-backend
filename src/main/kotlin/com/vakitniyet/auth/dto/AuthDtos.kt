package com.vakitniyet.auth.dto

import java.util.UUID

data class AppleSignInRequest(
    val identityToken: String,
    val authorizationCode: String,
    val name: String? = null,
    val deviceToken: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID,
    val name: String?,
    val subscription: SubscriptionInfo
)

data class SubscriptionInfo(
    val plan: String,
    val status: String,
    val isActive: Boolean,
    val trialDaysLeft: Long?,
    val currentPeriodEnd: String?
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UpdateDeviceTokenRequest(
    val deviceToken: String
)
