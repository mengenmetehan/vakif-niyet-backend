package com.vakitniyet.notification.controller

import com.vakitniyet.auth.repository.UserRepository
import com.vakitniyet.notification.dto.NotificationSettingsResponse
import com.vakitniyet.notification.dto.NotificationTestResponse
import com.vakitniyet.notification.dto.UpdateNotificationSettingsRequest
import com.vakitniyet.notification.entity.NotificationContentType
import com.vakitniyet.notification.scheduler.DailyNotificationScheduleBuilder
import com.vakitniyet.notification.scheduler.PrayerNotificationDispatcher
import com.vakitniyet.notification.service.ApnsService
import com.vakitniyet.notification.service.NotificationContentProvider
import com.vakitniyet.notification.service.NotificationSettingsService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notification")
class NotificationSettingsController(
    private val service: NotificationSettingsService,
    private val contentProvider: NotificationContentProvider,
    private val apnsService: ApnsService,
    private val userRepository: UserRepository,
    private val dispatcher: PrayerNotificationDispatcher,
    private val scheduleBuilder: DailyNotificationScheduleBuilder
) {
    private val log = LoggerFactory.getLogger(NotificationSettingsController::class.java)

    @GetMapping("/settings")
    fun getSettings(authentication: Authentication): ResponseEntity<NotificationSettingsResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(service.getSettings(userId))
    }

    @PostMapping("/test-send")
    fun testSend(
        authentication: Authentication,
        @RequestParam(defaultValue = "KARMA") type: String
    ): ResponseEntity<Map<String, String>> {
        val userId = authentication.principal as UUID
        val deviceToken = userRepository.findById(userId).orElseThrow().deviceToken
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Device token kayıtlı değil"))

        val contentType = NotificationContentType.valueOf(type.uppercase())
        val (metin, kaynak) = contentProvider.getContent(contentType)

        log.info("[test-send] userId={} contentType={} metin='{}' kaynak='{}'", userId, contentType, metin, kaynak)

        apnsService.send(
            deviceToken = deviceToken,
            title = "Test Bildirimi",
            body = "$metin ($kaynak)"
        )

        return ResponseEntity.ok(mapOf("metin" to metin, "kaynak" to kaynak))
    }

    @GetMapping("/content/preview")
    fun previewContent(
        @RequestParam(defaultValue = "KARMA") type: String
    ): ResponseEntity<Map<String, String>> {
        val contentType = NotificationContentType.valueOf(type.uppercase())
        val (metin, kaynak) = contentProvider.getContent(contentType)
        return ResponseEntity.ok(mapOf("title" to metin, "kaynak" to kaynak))
    }

    @PostMapping("/developer/test-send")
    fun developerTestSend(
        authentication: Authentication
    ): ResponseEntity<NotificationTestResponse> {
        val userId = authentication.principal as UUID
        val settings = service.getSettings(userId)
        val contentType = NotificationContentType.valueOf(settings.contentType)
        val (metin, kaynak) = contentProvider.getContent(contentType)

        val title = "Vakit Yaklaşıyor"
        val body = "$metin ($kaynak)"

        val deviceToken = userRepository.findById(userId).orElseThrow().deviceToken
        val sent = if (deviceToken != null) {
            apnsService.send(deviceToken = deviceToken, title = title, body = body)
            true
        } else {
            false
        }

        return ResponseEntity.ok(
            NotificationTestResponse(
                contentType = settings.contentType,
                title = title,
                body = body,
                metin = metin,
                kaynak = kaynak,
                sent = sent
            )
        )
    }

    @PostMapping("/developer/apns-test")
    fun apnsTest(@RequestParam deviceToken: String): ResponseEntity<Map<String, String>> {
        apnsService.send(
            deviceToken = deviceToken,
            title = "APNs Test",
            body = "Bu bir test bildirimidir"
        )
        return ResponseEntity.ok(mapOf("status" to "gönderildi, logları kontrol et", "deviceToken" to deviceToken))
    }

    @PostMapping("/developer/dispatch")
    fun triggerDispatch(): ResponseEntity<Map<String, String>> {
        dispatcher.dispatch()
        return ResponseEntity.ok(mapOf("status" to "dispatch tetiklendi, logları kontrol et"))
    }

    @PostMapping("/developer/rebuild-schedule")
    fun rebuildSchedule(): ResponseEntity<Map<String, String>> {
        scheduleBuilder.buildDailySchedule()
        return ResponseEntity.ok(mapOf("status" to "günlük plan yeniden oluşturuldu, logları kontrol et"))
    }

    @PutMapping("/settings")
    fun updateSettings(
        authentication: Authentication,
        @RequestBody request: UpdateNotificationSettingsRequest
    ): ResponseEntity<NotificationSettingsResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(service.updateSettings(userId, request))
    }
}
