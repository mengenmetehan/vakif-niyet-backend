package com.vakitniyet.auth.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long
) {
    private val key by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID): String = buildToken(userId.toString(), expiration)

    fun generateRefreshToken(userId: UUID): String = buildToken(userId.toString(), refreshExpiration, isRefresh = true)

    private fun buildToken(subject: String, expMs: Long, isRefresh: Boolean = false): String =
        Jwts.builder()
            .subject(subject)
            .claim("type", if (isRefresh) "refresh" else "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expMs))
            .signWith(key)
            .compact()

    fun validateToken(token: String): Boolean = runCatching {
        getClaims(token)
        true
    }.getOrDefault(false)

    fun getUserIdFromToken(token: String): UUID =
        UUID.fromString(getClaims(token).subject)

    fun isRefreshToken(token: String): Boolean =
        getClaims(token)["type"] == "refresh"

    // İmzası geçerli ama süresi dolmuş token'dan userId çıkarır
    fun getUserIdFromExpiredToken(token: String): UUID? = try {
        getClaims(token)
        null // expired değil, buraya düşmemeli
    } catch (e: ExpiredJwtException) {
        runCatching { UUID.fromString(e.claims.subject) }.getOrNull()
    } catch (e: Exception) {
        null // imza geçersiz veya başka hata
    }

    private fun getClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}
