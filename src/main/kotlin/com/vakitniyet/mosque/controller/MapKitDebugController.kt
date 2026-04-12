package com.vakitniyet.mosque.controller

import com.vakitniyet.mosque.client.AppleMapKitTokenProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/public/mapkit")
class MapKitDebugController(
    private val tokenProvider: AppleMapKitTokenProvider
) {

    @GetMapping("/token")
    fun getToken(): ResponseEntity<Map<String, String>> {
        if (!tokenProvider.isConfigured) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "MapKit yapılandırması eksik"))
        }
        return try {
            val token = tokenProvider.getToken()
            ResponseEntity.ok(mapOf("token" to token))
        } catch (e: Exception) {
            ResponseEntity.internalServerError()
                .body(mapOf("error" to (e.message ?: "Token üretilemedi")))
        }
    }
}
