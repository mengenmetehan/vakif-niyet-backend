package com.vakitniyet.quran.service

import com.vakitniyet.notification.service.QuranClient
import com.vakitniyet.quran.entity.QuranVerse
import com.vakitniyet.quran.repository.QuranVerseRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QuranVerseService(
    private val quranVerseRepository: QuranVerseRepository,
    private val quranClient: QuranClient,
    @Value("\${quran.translation-id}") private val translationId: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun count(): Long = quranVerseRepository.countByTranslationId(translationId)

    fun findRandom(count: Int): List<QuranVerse> = quranVerseRepository.findRandom(count)

    /**
     * Quran API'den random ayet çeker ve DB'ye kaydeder.
     * verse_key unique olduğu için zaten var olan ayetler sessizce atlanır.
     * @return kaydedilen ayet, duplicate veya API hatası durumunda null
     */
    @Transactional
    fun fetchAndSave(): QuranVerse? {
        val (text, source) = quranClient.fetchRandomVerse() ?: run {
            log.warn("Quran API'den ayet alınamadı")
            return null
        }

        // source formatı: "Bakara 2:148" → verseKey = "2:148"
        val verseKey = source.substringAfterLast(" ")
        if (quranVerseRepository.existsByVerseKey(verseKey)) return null

        val sureNo = verseKey.substringBefore(":").toIntOrNull() ?: return null
        val ayetNo = verseKey.substringAfter(":").toIntOrNull() ?: return null
        val sureName = source.substringBeforeLast(" ").trim()

        val verse = QuranVerse(
            verseKey = verseKey,
            sureNo = sureNo,
            ayetNo = ayetNo,
            sureName = sureName,
            text = text,
            translationId = translationId
        )

        return quranVerseRepository.save(verse).also {
            log.info("Ayet DB'ye kaydedildi: $verseKey ($sureName)")
        }
    }
}
