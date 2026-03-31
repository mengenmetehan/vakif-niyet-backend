package com.vakitniyet.subscription.service

import com.vakitniyet.subscription.dto.SubscriptionStatusResponse
import com.vakitniyet.subscription.dto.VerifyReceiptRequest
import com.vakitniyet.subscription.entity.Subscription
import com.vakitniyet.subscription.entity.SubscriptionPlan
import com.vakitniyet.subscription.entity.SubscriptionStatus
import com.vakitniyet.subscription.repository.SubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    @Value("\${apple.subscription.shared-secret}") private val sharedSecret: String,
    @Value("\${apple.subscription.verify-url}") private val verifyUrl: String,
    @Value("\${apple.subscription.verify-url-sandbox}") private val sandboxUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.create()

    fun getStatus(userId: UUID): SubscriptionStatusResponse {
        val sub = subscriptionRepository.findByUserId(userId)
            .orElseThrow { RuntimeException("Abonelik bulunamadı") }

        val trialDaysLeft = if (sub.plan == SubscriptionPlan.TRIAL) {
            java.time.Duration.between(LocalDateTime.now(), sub.trialEnd).toDays().coerceAtLeast(0)
        } else null

        return SubscriptionStatusResponse(
            plan = sub.plan.name,
            status = sub.status.name,
            isActive = sub.isActive,
            trialDaysLeft = trialDaysLeft,
            currentPeriodEnd = sub.currentPeriodEnd
                ?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @Transactional
    fun verifyAndActivate(userId: UUID, request: VerifyReceiptRequest): SubscriptionStatusResponse {
        val sub = subscriptionRepository.findByUserId(userId)
            .orElseThrow { RuntimeException("Abonelik bulunamadı") }

        val receiptResponse = verifyReceipt(request.receiptData)
            ?: throw IllegalArgumentException("Apple receipt doğrulanamadı")

        val latestReceipt = receiptResponse.latestReceiptInfo
            ?.maxByOrNull { it.purchaseDateMs }
            ?: throw IllegalArgumentException("Receipt bilgisi bulunamadı")

        val plan = when {
            request.productId.contains("yearly") -> SubscriptionPlan.YEARLY
            else -> SubscriptionPlan.MONTHLY
        }

        sub.plan = plan
        sub.status = SubscriptionStatus.ACTIVE
        sub.appleOriginalTransactionId = latestReceipt.originalTransactionId
        sub.appleLatestReceipt = request.receiptData
        sub.currentPeriodStart = epochToLocalDateTime(latestReceipt.purchaseDateMs)
        sub.currentPeriodEnd = epochToLocalDateTime(latestReceipt.expiresDateMs)
        sub.updatedAt = LocalDateTime.now()

        subscriptionRepository.save(sub)

        log.info("Abonelik aktifleştirildi: userId=$userId plan=$plan")

        return getStatus(userId)
    }

    // Trial süresi dolmuş abonelikleri expire et (Scheduler çağırır)
    @Transactional
    fun expireTrials() {
        val expired = subscriptionRepository.findExpiredTrials()
        expired.forEach { sub ->
            sub.status = SubscriptionStatus.EXPIRED
            sub.updatedAt = LocalDateTime.now()
            log.info("Trial süresi doldu: userId=${sub.user.id}")
        }
        subscriptionRepository.saveAll(expired)
    }

    private fun verifyReceipt(receiptData: String): AppleVerifyResponse? {
        val body = mapOf(
            "receipt-data" to receiptData,
            "password" to sharedSecret,
            "exclude-old-transactions" to true
        )

        return try {
            // Önce production'ı dene
            val response = postToApple(verifyUrl, body)
            if (response?.status == 21007) {
                // Sandbox receipt, sandbox URL'e gönder
                postToApple(sandboxUrl, body)
            } else {
                response
            }
        } catch (e: Exception) {
            log.error("Apple receipt doğrulama hatası", e)
            null
        }
    }

    private fun postToApple(url: String, body: Map<String, Any>): AppleVerifyResponse? =
        webClient.post()
            .uri(url)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(AppleVerifyResponse::class.java)
            .block()

    private fun epochToLocalDateTime(epochMs: String?): LocalDateTime? {
        if (epochMs == null) return null
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMs.toLong()),
            ZoneId.systemDefault()
        )
    }
}

data class AppleVerifyResponse(
    val status: Int,
    val latestReceiptInfo: List<LatestReceiptInfo>?
)

data class LatestReceiptInfo(
    val originalTransactionId: String,
    val purchaseDateMs: String,
    val expiresDateMs: String?
)
