package com.vakitniyet.auth.controller

import com.vakitniyet.auth.dto.*
import com.vakitniyet.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/apple")
    fun signInWithApple(
        @RequestBody @Valid request: AppleSignInRequest
    ): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.signInWithApple(request))
    }

    @PostMapping("/refresh")
    fun refreshToken(
        @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(authService.refreshToken(request))
    }

    @PutMapping("/device-token")
    fun updateDeviceToken(
        authentication: Authentication,
        @RequestBody request: UpdateDeviceTokenRequest
    ): ResponseEntity<Void> {
        val userId = authentication.principal as UUID
        authService.updateDeviceToken(userId, request.deviceToken)
        return ResponseEntity.ok().build()
    }
}
