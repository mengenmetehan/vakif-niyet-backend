package com.vakitniyet.quran.repository

import com.vakitniyet.quran.entity.QuranVerse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface QuranVerseRepository : JpaRepository<QuranVerse, UUID> {

    fun existsByVerseKey(verseKey: String): Boolean

    fun countByTranslationId(translationId: Int): Long

    @Query(value = "SELECT * FROM quran_verses ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    fun findRandom(count: Int): List<QuranVerse>
}
