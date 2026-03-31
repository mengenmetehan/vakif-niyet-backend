package com.vakitniyet.prayer.scheduler

import com.vakitniyet.prayer.repository.StreakRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class StreakResetScheduler(
    private val streakRepository: StreakRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Her gece 00:01'de çalış
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    fun resetBrokenStreaks() {
        val yesterday = LocalDate.now().minusDays(1)
        val broken = streakRepository.findBrokenStreaks(yesterday)

        if (broken.isEmpty()) return

        broken.forEach { streak ->
            streak.currentStreak = 0
            streak.updatedAt = LocalDateTime.now()
        }
        streakRepository.saveAll(broken)

        log.info("Streak sıfırlandı: ${broken.size} kullanıcı")
    }
}
