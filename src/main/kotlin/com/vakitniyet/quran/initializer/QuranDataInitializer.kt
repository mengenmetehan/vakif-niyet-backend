package com.vakitniyet.quran.initializer

import com.vakitniyet.quran.service.QuranVerseService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class QuranDataInitializer(
    private val quranVerseService: QuranVerseService,
    @Value("\${quran.seed-count:50}") private val seedCount: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun seedIfEmpty() {
        val existing = quranVerseService.count()
        if (existing >= seedCount) {
            log.info("Quran DB seed atlandı: $existing ayet mevcut (min: $seedCount)")
            return
        }

        val needed = seedCount - existing.toInt()
        log.info("Quran DB seed başlıyor: $existing mevcut, $needed ayet eklenecek...")

        var added = 0
        var attempts = 0
        val maxAttempts = needed * 3 // duplicate'ler için tolerans

        while (added < needed && attempts < maxAttempts) {
            if (quranVerseService.fetchAndSave() != null) added++
            attempts++
        }

        log.info("Quran DB seed tamamlandı: $added ayet eklendi ($attempts deneme)")
    }
}
