package com.vakitniyet.notification.entity

import com.vakitniyet.auth.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class NotificationContentType { HADIS, AYET, KARMA }

@Entity
@Table(name = "notification_settings")
class NotificationSettings(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "offset_minutes", nullable = false)
    var offsetMinutes: Int = 10,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    var contentType: NotificationContentType = NotificationContentType.KARMA,

    @Column(name = "ilce_id") var ilceId: String? = null,

    @Column(name = "fajr_enabled") var fajrEnabled: Boolean = true,
    @Column(name = "dhuhr_enabled") var dhuhrEnabled: Boolean = true,
    @Column(name = "asr_enabled") var asrEnabled: Boolean = true,
    @Column(name = "maghrib_enabled") var maghribEnabled: Boolean = true,
    @Column(name = "isha_enabled") var ishaEnabled: Boolean = true,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
