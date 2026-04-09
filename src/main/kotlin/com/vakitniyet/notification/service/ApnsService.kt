package com.vakitniyet.notification.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

@Service
class ApnsService(
    @Value("\${apple.team-id}") private val teamId: String,
    @Value("\${apple.key-id}") private val keyId: String,
    @Value("\${apple.private-key}") private val privateKey: String,
    @Value("\${apple.bundle-id}") private val bundleId: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build()

    private val ecPrivateKey: ECPrivateKey? by lazy {
        if (teamId.isBlank() || keyId.isBlank() || privateKey.isBlank()) {
            log.warn("APNs yapılandırması eksik, bildirimler devre dışı")
            null
        } else {
            try {
                loadPrivateKey(privateKey)
            } catch (e: Exception) {
                log.error("APNs private key yüklenemedi", e)
                null
            }
        }
    }

    private fun loadPrivateKey(raw: String): ECPrivateKey {
        val base64Body = raw
            .replace("\\n", "\n")
            .replace("\r", "")
            .lines()
            .map { it.trim() }
            .filterNot { it.startsWith("-----") || it.isBlank() }
            .joinToString("")
        val keyBytes = Base64.getDecoder().decode(base64Body)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
    }

    private fun buildJwt(): String {
        val issuedAt = Instant.now().epochSecond
        val header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"alg":"ES256","kid":"$keyId"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"iss":"$teamId","iat":$issuedAt}""".toByteArray())
        val signingInput = "$header.$payload"

        val signer = java.security.Signature.getInstance("SHA256withECDSAinP1363Format")
        signer.initSign(ecPrivateKey)
        signer.update(signingInput.toByteArray())
        val signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign())
        return "$signingInput.$signature"
    }

    fun send(deviceToken: String, title: String, body: String) {
        if (ecPrivateKey == null) {
            log.error("APNs key yüklenemedi, bildirim gönderilemedi")
            return
        }

        val token = deviceToken.replace("[^a-fA-F0-9]".toRegex(), "")
        val payload = """{"aps":{"alert":{"title":"$title","body":"$body"}}}"""
        val jwt = buildJwt()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.push.apple.com/3/device/$token"))
            .version(HttpClient.Version.HTTP_2)
            .header("authorization", "bearer $jwt")
            .header("apns-topic", bundleId)
            .header("apns-push-type", "alert")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenComplete { response, ex ->
                if (ex != null) {
                    log.error("APNs gönderim hatası: deviceToken=$deviceToken", ex)
                } else if (response.statusCode() == 200) {
                    log.info("APNs başarılı: deviceToken=$deviceToken")
                } else {
                    log.warn("APNs reddedildi: status=${response.statusCode()}, body=${response.body()}, deviceToken=$deviceToken")
                }
            }
    }
}
