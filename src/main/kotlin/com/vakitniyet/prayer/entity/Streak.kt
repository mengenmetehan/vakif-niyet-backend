package com.vakitniyet.prayer.entity

import com.vakitniyet.auth.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "streaks")
class Streak(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "current_streak", nullable = false)
    var currentStreak: Int = 0,

    @Column(name = "longest_streak", nullable = false)
    var longestStreak: Int = 0,

    @Column(name = "last_full_day")
    var lastFullDay: LocalDate? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
