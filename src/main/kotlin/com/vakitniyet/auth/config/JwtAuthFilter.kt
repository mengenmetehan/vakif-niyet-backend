package com.vakitniyet.auth.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token == null) {
            log.debug("No Bearer token found for {} {}", request.method, request.requestURI)
        } else if (jwtProvider.validateToken(token) && !jwtProvider.isRefreshToken(token)) {
            val userId = jwtProvider.getUserIdFromToken(token)
            authenticate(userId)
        } else {
            // Token geçersiz veya expired — expired ama imzası geçerliyse yeni token üret
            val userId = jwtProvider.getUserIdFromExpiredToken(token)
            if (userId != null) {
                log.info("Access token expired, auto-renewing for user {}", userId)
                val newAccessToken = jwtProvider.generateAccessToken(userId)
                response.setHeader("X-New-Access-Token", newAccessToken)
                authenticate(userId)
            } else {
                log.warn("Invalid token (bad signature or format) for {} {}", request.method, request.requestURI)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(userId: java.util.UUID) {
        val auth = UsernamePasswordAuthenticationToken(
            userId,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }
}
