package com.vakitniyet.mosque.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.security.interfaces.ECPrivateKey
import java.time.Instant
import java.util.Date

@Component
class AppleMapKitTokenProvider(
    @Value("\${apple.mapkit.key-id}") private val keyId: String,
    @Value("\${apple.mapkit.team-id}") private val teamId: String,
    @Value("\${apple.mapkit.private-key}") private val privateKeyPem: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val appleTokenClient = RestClient.create("https://maps-api.apple.com")

    @Volatile
    private var cachedJwt: Pair<String, Instant>? = null

    @Volatile
    private var cachedAccessToken: Pair<String, Instant>? = null

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class AppleTokenResponse(val accessToken: String? = null, val expiresInSeconds: Long? = null)

    private val privateKey: ECPrivateKey? = runCatching { loadPrivateKey() }
        .onFailure { log.error("MapKit private key yüklenemedi: {}", it.message) }
        .getOrNull()

    val isConfigured: Boolean
        get() = keyId.isNotBlank() && teamId.isNotBlank() && privateKeyPem.isNotBlank() && privateKey != null

    @Volatile
    private var tokenError: Boolean = false  // hata durumu flag'i

    fun getToken(): String {
        check(isConfigured) { "MapKit yapılandırması eksik (apple.mapkit.*)" }
        check(!tokenError) { "MapKit private key yüklenemedi, servis devre dışı" }

        val now = Instant.now()
        val existing = cachedJwt
        if (existing != null && now.isBefore(existing.second.minusSeconds(60))) {
            return existing.first
        }

        return try {
            val exp = now.plusSeconds(1800)
            val token = Jwts.builder()
                .header().type("JWT").keyId(keyId).and()
                .issuer(teamId)
                .subject("maps.VakitNiyetMaps")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact()
            cachedJwt = token to exp
            log.debug("MapKit JWT yenilendi, exp={}, token_preview={}", exp, token.take(40))
            token
        } catch (e: Exception) {
            log.error("MapKit JWT üretilemedi", e)
            throw e
        }
    }

    fun getAccessToken(): String {
        val now = Instant.now()
        val existing = cachedAccessToken
        if (existing != null && now.isBefore(existing.second.minusSeconds(60))) {
            return existing.first
        }

        val jwt = getToken()
        val response = appleTokenClient.get()
            .uri("/v1/token")
            .header("Authorization", "Bearer $jwt")
            .retrieve()
            .body(AppleTokenResponse::class.java)

        val accessToken = response?.accessToken
            ?: throw IllegalStateException("Apple token exchange yanıtında accessToken yok")
        val ttl = response.expiresInSeconds ?: 1800
        cachedAccessToken = accessToken to now.plusSeconds(ttl)
        log.debug("MapKit access token alındı, ttl={}s", ttl)
        return accessToken
    }


    private fun loadPrivateKey(): ECPrivateKey {
        val cleaned = privateKeyPem
            .replace("\\n", "\n")
            .replace(Regex("-----BEGIN[^-]*-----"), "")   // header'daki boşlukları da yakalar
            .replace(Regex("-----END[^-]*-----"), "")     // footer'daki boşlukları da yakalar
            .replace(Regex("\\s+"), "")
            .trim()

        log.debug("Cleaned base64 length: {}, preview: {}", cleaned.length, cleaned.take(30))

        if (cleaned.isEmpty()) throw IllegalStateException("Private key boş!")

        // PKCS#8 formatı (BEGIN PRIVATE KEY) → direkt decode
        return try {
            val bytes = java.util.Base64.getDecoder().decode(cleaned)
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(bytes)
            java.security.KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
        } catch (e: Exception) {
            log.error("Key parse hatası - cleaned uzunluk: {}", cleaned.length, e)
            throw e
        }
    }
}
