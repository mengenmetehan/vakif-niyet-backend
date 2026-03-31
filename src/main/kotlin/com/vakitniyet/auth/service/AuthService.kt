package com.vakitniyet.auth.service

import com.vakitniyet.auth.config.JwtProvider
import com.vakitniyet.auth.dto.*
import com.vakitniyet.auth.entity.User
import com.vakitniyet.auth.repository.UserRepository
import com.vakitniyet.subscription.entity.Subscription
import com.vakitniyet.subscription.entity.SubscriptionPlan
import com.vakitniyet.subscription.entity.SubscriptionStatus
import com.vakitniyet.subscription.repository.SubscriptionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val jwtProvider: JwtProvider,
    private val appleTokenVerifier: AppleTokenVerifier,
    @Value("\${trial.days}") private val trialDays: Long
) {

    @Transactional
    fun signInWithApple(request: AppleSignInRequest): AuthResponse {
        // Apple token doğrula
        val appleUser = appleTokenVerifier.verify(request.identityToken)

        // Kullanıcıyı bul veya oluştur
        val user = userRepository.findByAppleId(appleUser.appleId)
            .orElseGet {
                val newUser = User(
                    appleId = appleUser.appleId,
                    email = appleUser.email,
                    name = request.name,
                    deviceToken = request.deviceToken
                )
                userRepository.save(newUser).also {
                    createTrialSubscription(it)
                }
            }

        // Device token güncelle
        if (request.deviceToken != null && user.deviceToken != request.deviceToken) {
            user.deviceToken = request.deviceToken
            user.updatedAt = LocalDateTime.now()
            userRepository.save(user)
        }

        val subscription = subscriptionRepository.findByUserId(user.id)
            .orElseThrow { RuntimeException("Subscription not found") }

        return buildAuthResponse(user, subscription)
    }

    @Transactional
    fun refreshToken(request: RefreshTokenRequest): TokenResponse {
        if (!jwtProvider.validateToken(request.refreshToken)) {
            throw IllegalArgumentException("Geçersiz refresh token")
        }
        if (!jwtProvider.isRefreshToken(request.refreshToken)) {
            throw IllegalArgumentException("Bu bir refresh token değil")
        }

        val userId = jwtProvider.getUserIdFromToken(request.refreshToken)

        return TokenResponse(
            accessToken = jwtProvider.generateAccessToken(userId),
            refreshToken = jwtProvider.generateRefreshToken(userId)
        )
    }

    @Transactional
    fun updateDeviceToken(userId: UUID, deviceToken: String) {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("Kullanıcı bulunamadı") }
        user.deviceToken = deviceToken
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    private fun createTrialSubscription(user: User): Subscription {
        val subscription = Subscription(
            user = user,
            plan = SubscriptionPlan.TRIAL,
            status = SubscriptionStatus.ACTIVE,
            trialEnd = LocalDateTime.now().plusDays(trialDays)
        )
        return subscriptionRepository.save(subscription)
    }

    private fun buildAuthResponse(user: User, subscription: Subscription): AuthResponse {
        val trialDaysLeft = if (subscription.plan == SubscriptionPlan.TRIAL) {
            java.time.Duration.between(LocalDateTime.now(), subscription.trialEnd).toDays()
                .coerceAtLeast(0)
        } else null

        return AuthResponse(
            accessToken = jwtProvider.generateAccessToken(user.id),
            refreshToken = jwtProvider.generateRefreshToken(user.id),
            userId = user.id,
            name = user.name,
            subscription = SubscriptionInfo(
                plan = subscription.plan.name,
                status = subscription.status.name,
                isActive = subscription.isActive,
                trialDaysLeft = trialDaysLeft,
                currentPeriodEnd = subscription.currentPeriodEnd
                    ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
        )
    }
}
