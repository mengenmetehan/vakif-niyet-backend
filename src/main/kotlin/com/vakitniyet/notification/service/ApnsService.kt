package com.vakitniyet.notification.service

import com.eatthepath.pushy.apns.ApnsClient
import com.eatthepath.pushy.apns.ApnsClientBuilder
import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification
import com.eatthepath.pushy.apns.util.TokenUtil
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class ApnsService(
    @Value("\${apple.team-id}") private val teamId: String,
    @Value("\${apple.key-id}") private val keyId: String,
    @Value("\${apple.private-key}") private val privateKey: String,
    @Value("\${apple.bundle-id}") private val bundleId: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client: ApnsClient? by lazy {
        if (teamId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
            log.warn("APNs yapılandırması eksik, bildirimler devre dışı")
            null
        } else {
            try {
                System.setProperty("java.net.preferIPv4Stack", "true")
                val normalizedKey = buildNormalizedKey(privateKey)
                ApnsClientBuilder()
                    .setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST)
                    .setSigningKey(
                        ApnsSigningKey.loadFromInputStream(
                            ByteArrayInputStream(normalizedKey.toByteArray()),
                            teamId, keyId
                        )
                    )
                    .build()
            } catch (e: Exception) {
                log.error("APNs client oluşturulamadı", e)
                null
            }
        }
    }

    private fun buildNormalizedKey(raw: String): String {
        // Normalize: replace literal \n, then extract pure base64 body
        val base64Body = raw
            .replace("\\n", "\n")
            .replace("\r", "")
            .lines()
            .map { it.trim() }
            .filterNot { it.startsWith("-----") || it.isBlank() }
            .joinToString("")
        val wrapped = base64Body.chunked(64).joinToString("\n")
        return "-----BEGIN PRIVATE KEY-----\n$wrapped\n-----END PRIVATE KEY-----"
    }

    fun send(deviceToken: String, title: String, body: String) {
        val apnsClient = client ?: run {
            log.error("APNs client null, bildirim gönderilemedi (key yapılandırması hatalı)")
            return
        }

        val payload = SimpleApnsPayloadBuilder()
            .setAlertTitle(title)
            .setAlertBody(body)
            .build()

        val notification = SimpleApnsPushNotification(
            TokenUtil.sanitizeTokenString(deviceToken),
            bundleId,
            payload
        )

        apnsClient.sendNotification(notification).whenComplete { response, ex ->
            if (ex != null) {
                log.error("APNs gönderim hatası: deviceToken=$deviceToken", ex)
            } else if (!response.isAccepted) {
                log.warn("APNs reddedildi: reason=${response.rejectionReason}, deviceToken=$deviceToken")
            } else {
                log.info("APNs başarılı: deviceToken=$deviceToken")
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        client?.close()
    }
}
