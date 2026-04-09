package com.vakitniyet.quran.initializer

import com.vakitniyet.quran.repository.QuranVerseRepository
import com.vakitniyet.quran.service.QuranVerseService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class QuranDataInitializer(
    private val quranVerseService: QuranVerseService,
    private val quranVerseRepository: QuranVerseRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun seedIfEmpty() {
        if (quranVerseRepository.existsByVerseKey("1:1")) {
            log.info("Quran DB zaten dolu, seed atlandı")
            return
        }

        log.info("Quran DB boş, tüm ayetler çekiliyor...")
        val saved = quranVerseService.fetchAndSaveAll()
        log.info("Quran DB seed tamamlandı: $saved ayet eklendi")
    }
}
