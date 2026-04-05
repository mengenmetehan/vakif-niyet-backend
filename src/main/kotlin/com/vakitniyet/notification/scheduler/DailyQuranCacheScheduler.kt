package com.vakitniyet.notification.scheduler

import com.vakitniyet.notification.service.NotificationContentProvider
import com.vakitniyet.quran.service.QuranVerseService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DailyQuranCacheScheduler(
    private val quranVerseService: QuranVerseService,
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Her gün 01:00'de çalışır — bildirim planından (02:00) önce hazır olur
    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Istanbul")
    fun cacheDailyVerses() {
        log.info("Günlük ayet cache'i oluşturuluyor (5 ayet)...")

        val verses = quranVerseService.findRandom(5)
        if (verses.isEmpty()) {
            log.warn("DB'de ayet bulunamadı, cache güncellenemiyor")
            return
        }

        verses.forEachIndexed { index, verse ->
            redisTemplate.opsForValue().set(NotificationContentProvider.ayetKey(index), verse.text, Duration.ofHours(26))
            redisTemplate.opsForValue().set(NotificationContentProvider.ayetSourceKey(index), "${verse.sureName} ${verse.verseKey}", Duration.ofHours(26))
            log.info("Ayet[$index] cache'lendi: ${verse.sureName} ${verse.verseKey}")
        }

        log.info("Günlük ayet cache'i tamamlandı: ${verses.size}/5 cache'lendi")
    }

    @PostConstruct
    fun cacheOnStartupIfMissing() {
        val anyMissing = (0..4).any {
            redisTemplate.hasKey(NotificationContentProvider.ayetKey(it)) != true
        }
        if (anyMissing) {
            log.info("Başlangıçta ayet cache'i eksik, oluşturuluyor...")
            cacheDailyVerses()
        } else {
            log.info("Başlangıçta ayet cache'i mevcut, atlanıyor.")
        }
    }
}
