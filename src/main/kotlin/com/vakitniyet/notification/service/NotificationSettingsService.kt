package com.vakitniyet.notification.service

import com.vakitniyet.auth.repository.UserRepository
import com.vakitniyet.notification.dto.NotificationSettingsResponse
import com.vakitniyet.notification.dto.UpdateNotificationSettingsRequest
import com.vakitniyet.notification.entity.NotificationContentType
import com.vakitniyet.notification.entity.NotificationSettings
import com.vakitniyet.notification.repository.NotificationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class NotificationSettingsService(
    private val repository: NotificationSettingsRepository,
    private val userRepository: UserRepository
) {

    fun getSettings(userId: UUID): NotificationSettingsResponse {
        val settings = repository.findByUserId(userId)
            .orElseGet { defaultSettings(userId) }
        return settings.toResponse()
    }

    @Transactional
    fun updateSettings(userId: UUID, request: UpdateNotificationSettingsRequest): NotificationSettingsResponse {
        val settings = repository.findByUserId(userId)
            .orElseGet {
                val user = userRepository.findById(userId).orElseThrow()
                repository.save(NotificationSettings(user = user))
            }

        request.enabled?.let { settings.enabled = it }
        request.ilceId?.let { settings.ilceId = it }
        request.offsetMinutes?.let { settings.offsetMinutes = it }
        request.contentType?.let { settings.contentType = NotificationContentType.valueOf(it.uppercase()) }
        request.fajrEnabled?.let { settings.fajrEnabled = it }
        request.dhuhrEnabled?.let { settings.dhuhrEnabled = it }
        request.asrEnabled?.let { settings.asrEnabled = it }
        request.maghribEnabled?.let { settings.maghribEnabled = it }
        request.ishaEnabled?.let { settings.ishaEnabled = it }
        settings.updatedAt = LocalDateTime.now()

        return repository.save(settings).toResponse()
    }

    private fun defaultSettings(userId: UUID): NotificationSettings {
        val user = userRepository.findById(userId).orElseThrow()
        return NotificationSettings(user = user)
    }

    private fun NotificationSettings.toResponse() = NotificationSettingsResponse(
        enabled = enabled,
        ilceId = ilceId,
        offsetMinutes = offsetMinutes,
        contentType = contentType.name,
        fajrEnabled = fajrEnabled,
        dhuhrEnabled = dhuhrEnabled,
        asrEnabled = asrEnabled,
        maghribEnabled = maghribEnabled,
        ishaEnabled = ishaEnabled
    )
}
