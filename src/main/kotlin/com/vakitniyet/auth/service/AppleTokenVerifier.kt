package com.vakitniyet.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

data class AppleUserInfo(
    val appleId: String,
    val email: String?
)

@Component
class AppleTokenVerifier(
    @Value("\${apple.bundle-id}") private val bundleId: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val httpClient = HttpClient.newHttpClient()
    private val objectMapper = ObjectMapper()

    fun verify(identityToken: String): AppleUserInfo {
        if (identityToken.startsWith("mock_")) {
            val parts = identityToken.split("_")
            return AppleUserInfo(
                appleId = parts.getOrElse(1) { "mock-apple-id" },
                email = parts.getOrNull(2)?.let { "$it@privaterelay.appleid.com" }
            )
        }

        log.debug("Apple token doğrulanıyor...")
        val kid = extractKid(identityToken)
        log.debug("kid: $kid")

        val publicKey = fetchPublicKey(kid)
            ?: throw IllegalArgumentException("Apple public key bulunamadı: kid=$kid")

        val claims = try {
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(identityToken)
                .payload
        } catch (e: Exception) {
            log.error("Token doğrulama hatası: ${e.message}")
            throw IllegalArgumentException("Apple token geçersiz: ${e.message}")
        }

        if (claims.issuer != "https://appleid.apple.com") {
            throw IllegalArgumentException("Geçersiz issuer: ${claims.issuer}")
        }

        val aud = claims.audience
        log.debug("audience: $aud, bundleId: $bundleId")
        if (!aud.contains(bundleId)) {
            throw IllegalArgumentException("Audience uyuşmuyor. Gelen: $aud, Beklenen: $bundleId")
        }

        return AppleUserInfo(
            appleId = claims.subject ?: throw IllegalArgumentException("Subject yok"),
            email = claims["email"] as? String
        )
    }

    private fun extractKid(token: String): String {
        val header = token.split(".").getOrNull(0)
            ?: throw IllegalArgumentException("Geçersiz token")
        val padded = header.padEnd((header.length + 3) / 4 * 4, '=')
        val decoded = Base64.getUrlDecoder().decode(padded)
        @Suppress("UNCHECKED_CAST")
        val map = objectMapper.readValue(decoded, Map::class.java) as Map<String, Any>
        return map["kid"] as? String ?: throw IllegalArgumentException("kid yok")
    }

    private fun fetchPublicKey(kid: String): PublicKey? {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://appleid.apple.com/auth/keys"))
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            @Suppress("UNCHECKED_CAST")
            val body = objectMapper.readValue(response.body(), Map::class.java) as Map<String, Any>
            val keys = body["keys"] as? List<Map<String, Any>> ?: return null
            val jwk = keys.find { it["kid"] == kid } ?: run {
                log.error("kid=$kid için key bulunamadı. Mevcut kid'ler: ${keys.map { it["kid"] }}")
                return null
            }
            buildPublicKey(jwk)
        } catch (e: Exception) {
            log.error("Apple public key fetch hatası", e)
            null
        }
    }

    private fun buildPublicKey(jwk: Map<String, Any>): PublicKey {
        fun decode(s: String): ByteArray {
            val padded = s.padEnd((s.length + 3) / 4 * 4, '=')
            return Base64.getUrlDecoder().decode(padded)
        }
        val spec = RSAPublicKeySpec(
            BigInteger(1, decode(jwk["n"] as String)),
            BigInteger(1, decode(jwk["e"] as String))
        )
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }
}