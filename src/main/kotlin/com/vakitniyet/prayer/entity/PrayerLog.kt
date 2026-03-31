package com.vakitniyet.prayer.entity

import com.vakitniyet.auth.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class PrayerName { FAJR, DHUHR, ASR, MAGHRIB, ISHA }

@Entity
@Table(
    name = "prayer_logs",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "prayer_name", "prayer_date"])]
)
class PrayerLog(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "prayer_name", nullable = false)
    val prayerName: PrayerName,

    @Column(name = "prayer_date", nullable = false)
    val prayerDate: LocalDate,

    @Column(name = "prayed_at")
    var prayedAt: LocalDateTime? = null,

    @Column(name = "is_done", nullable = false)
    var isDone: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
