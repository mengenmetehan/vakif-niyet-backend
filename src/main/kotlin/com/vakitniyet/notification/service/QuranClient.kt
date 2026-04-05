package com.vakitniyet.notification.service

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

data class QuranVersesPage(
    val verses: List<QuranVerse>,
    val pagination: QuranPagination
)

data class QuranPagination(
    @JsonProperty("total_pages") val totalPages: Int,
    @JsonProperty("current_page") val currentPage: Int
)

data class QuranVerse(
    @JsonProperty("verse_key") val verseKey: String,
    val translations: List<QuranTranslation>
)

data class QuranTranslation(
    val text: String
)

@Component
class QuranClient(
    @Value("\${quran.api-url}") apiUrl: String,
    @Value("\${quran.translation-id}") private val translationId: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = WebClient.create(apiUrl)

    /**
     * Tüm ayetleri sayfalı olarak getirir.
     * Her sayfa için callback çağrılır — bellek dostu.
     */
    fun fetchAllVerses(perPage: Int = 50, onPage: (List<QuranVerse>) -> Unit) {
        var page = 1
        var totalPages = Int.MAX_VALUE

        while (page <= totalPages) {
            var success = false
            repeat(3) retry@{ attempt ->
                if (success) return@retry
                try {
                    val response = webClient.get()
                        .uri("/verses?language=tr&words=false&translations=$translationId&fields=verse_key&per_page=$perPage&page=$page")
                        .retrieve()
                        .bodyToMono(QuranVersesPage::class.java)
                        .timeout(Duration.ofSeconds(30))
                        .block() ?: return@retry

                    totalPages = response.pagination.totalPages
                    log.info("Quran API sayfa $page/$totalPages alındı (${response.verses.size} ayet)")
                    onPage(response.verses)
                    success = true
                } catch (e: Exception) {
                    log.warn("Quran API sayfa hatası (deneme ${attempt + 1}/3): page=$page — ${e.message}")
                }
            }
            if (!success) log.error("Quran API sayfa $page 3 denemede alınamadı, atlanıyor")
            page++
        }
    }

    // "4:137" → "Nisa 4:137"
    private fun formatVerseKey(verseKey: String): String {
        val sureNo = verseKey.substringBefore(":").toIntOrNull() ?: return verseKey
        val sureAdi = sureNames[sureNo] ?: return verseKey
        return "$sureAdi $verseKey"
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
