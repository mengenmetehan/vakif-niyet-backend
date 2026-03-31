package com.vakitniyet.subscription.controller

import com.vakitniyet.subscription.dto.SubscriptionStatusResponse
import com.vakitniyet.subscription.dto.VerifyReceiptRequest
import com.vakitniyet.subscription.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/subscription")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {

    @GetMapping("/status")
    fun getStatus(
        authentication: Authentication
    ): ResponseEntity<SubscriptionStatusResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(subscriptionService.getStatus(userId))
    }

    @PostMapping("/verify")
    fun verifyReceipt(
        authentication: Authentication,
        @RequestBody request: VerifyReceiptRequest
    ): ResponseEntity<SubscriptionStatusResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(subscriptionService.verifyAndActivate(userId, request))
    }
}
