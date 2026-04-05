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

    fun hasAny(): Boolean = quranVerseRepository.countByTranslationId(translationId) > 0

    fun findRandom(count: Int): List<QuranVerse> = quranVerseRepository.findRandom(count)

    /**
     * Quran API'den tüm ayetleri çekip DB'ye kaydeder.
     * Zaten var olan ayetler (verse_key unique) atlanır.
     */
    fun fetchAndSaveAll(): Int {
        var saved = 0

        quranClient.fetchAllVerses { verses ->
            val entities = verses.mapNotNull { verse ->
                if (quranVerseRepository.existsByVerseKey(verse.verseKey)) return@mapNotNull null

                val raw = verse.translations.firstOrNull()?.text ?: return@mapNotNull null
                val text = org.springframework.web.util.HtmlUtils.htmlUnescape(raw)
                val sureNo = verse.verseKey.substringBefore(":").toIntOrNull() ?: return@mapNotNull null
                val ayetNo = verse.verseKey.substringAfter(":").toIntOrNull() ?: return@mapNotNull null
                val sureName = sureNames[sureNo] ?: return@mapNotNull null

                QuranVerse(
                    verseKey = verse.verseKey,
                    sureNo = sureNo,
                    ayetNo = ayetNo,
                    sureName = sureName,
                    text = text,
                    translationId = translationId
                )
            }

            if (entities.isNotEmpty()) {
                quranVerseRepository.saveAll(entities)
                saved += entities.size
                log.info("${entities.size} ayet kaydedildi (toplam: $saved)")
            }
        }

        return saved
    }

    private val sureNames = mapOf(
        1 to "Fatiha", 2 to "Bakara", 3 to "Al-i İmran", 4 to "Nisa", 5 to "Maide",
        6 to "En'am", 7 to "A'raf", 8 to "Enfal", 9 to "Tevbe", 10 to "Yunus",
        11 to "Hud", 12 to "Yusuf", 13 to "Ra'd", 14 to "İbrahim", 15 to "Hicr",
        16 to "Nahl", 17 to "İsra", 18 to "Kehf", 19 to "Meryem", 20 to "Taha",
        21 to "Enbiya", 22 to "Hac", 23 to "Mü'minun", 24 to "Nur", 25 to "Furkan",
        26 to "Şuara", 27 to "Neml", 28 to "Kasas", 29 to "Ankebut", 30 to "Rum",
        31 to "Lokman", 32 to "Secde", 33 to "Ahzab", 34 to "Sebe", 35 to "Fatır",
        36 to "Yasin", 37 to "Saffat", 38 to "Sad", 39 to "Zümer", 40 to "Mümin",
        41 to "Fussilet", 42 to "Şura", 43 to "Zuhruf", 44 to "Duhan", 45 to "Casiye",
        46 to "Ahkaf", 47 to "Muhammed", 48 to "Fetih", 49 to "Hucurat", 50 to "Kaf",
        51 to "Zariyat", 52 to "Tur", 53 to "Necm", 54 to "Kamer", 55 to "Rahman",
        56 to "Vakia", 57 to "Hadid", 58 to "Mücadele", 59 to "Haşr", 60 to "Mümtehine",
        61 to "Saf", 62 to "Cuma", 63 to "Münafikun", 64 to "Tegabun", 65 to "Talak",
        66 to "Tahrim", 67 to "Mülk", 68 to "Kalem", 69 to "Hakka", 70 to "Mearic",
        71 to "Nuh", 72 to "Cin", 73 to "Müzzemmil", 74 to "Müddessir", 75 to "Kiyame",
        76 to "İnsan", 77 to "Mürselat", 78 to "Nebe", 79 to "Naziat", 80 to "Abese",
        81 to "Tekvir", 82 to "İnfitar", 83 to "Mutaffifin", 84 to "İnşikak", 85 to "Buruc",
        86 to "Tarık", 87 to "A'la", 88 to "Gaşiye", 89 to "Fecr", 90 to "Beled",
        91 to "Şems", 92 to "Leyl", 93 to "Duha", 94 to "İnşirah", 95 to "Tin",
        96 to "Alak", 97 to "Kadir", 98 to "Beyyine", 99 to "Zilzal", 100 to "Adiyat",
        101 to "Karia", 102 to "Tekasür", 103 to "Asr", 104 to "Hümeze", 105 to "Fil",
        106 to "Kureyş", 107 to "Maun", 108 to "Kevser", 109 to "Kafirun", 110 to "Nasr",
        111 to "Tebbet", 112 to "İhlas", 113 to "Felak", 114 to "Nas"
    )

}
