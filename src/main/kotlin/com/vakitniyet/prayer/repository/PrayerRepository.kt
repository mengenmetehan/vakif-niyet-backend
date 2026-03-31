package com.vakitniyet.prayer.repository

import com.vakitniyet.prayer.entity.PrayerLog
import com.vakitniyet.prayer.entity.PrayerName
import com.vakitniyet.prayer.entity.Streak
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

interface PrayerLogRepository : JpaRepository<PrayerLog, UUID> {

    fun findByUserIdAndPrayerDate(userId: UUID, date: LocalDate): List<PrayerLog>

    fun findByUserIdAndPrayerDateBetween(
        userId: UUID,
        start: LocalDate,
        end: LocalDate
    ): List<PrayerLog>

    fun findByUserIdAndPrayerNameAndPrayerDate(
        userId: UUID,
        prayerName: PrayerName,
        date: LocalDate
    ): Optional<PrayerLog>

    @Query("""
        SELECT p.prayerDate, COUNT(p) 
        FROM PrayerLog p 
        WHERE p.user.id = :userId 
        AND p.isDone = true 
        AND p.prayerDate BETWEEN :start AND :end 
        GROUP BY p.prayerDate
    """)
    fun countDoneByDate(userId: UUID, start: LocalDate, end: LocalDate): List<Array<Any>>
}

interface StreakRepository : JpaRepository<Streak, UUID> {
    fun findByUserId(userId: UUID): Optional<Streak>

    @Query("SELECT s FROM Streak s WHERE s.currentStreak > 0 AND (s.lastFullDay IS NULL OR s.lastFullDay < :yesterday)")
    fun findBrokenStreaks(yesterday: LocalDate): List<Streak>
}
