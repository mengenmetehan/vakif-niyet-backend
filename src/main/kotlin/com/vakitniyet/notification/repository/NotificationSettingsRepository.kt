package com.vakitniyet.notification.repository

import com.vakitniyet.notification.entity.NotificationSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface NotificationSettingsRepository : JpaRepository<NotificationSettings, UUID> {
    fun findByUserId(userId: UUID): Optional<NotificationSettings>

    @Query("SELECT ns FROM NotificationSettings ns JOIN FETCH ns.user u WHERE ns.enabled = true AND ns.ilceId IS NOT NULL AND u.deviceToken IS NOT NULL")
    fun findAllActiveWithDeviceToken(): List<NotificationSettings>
}
