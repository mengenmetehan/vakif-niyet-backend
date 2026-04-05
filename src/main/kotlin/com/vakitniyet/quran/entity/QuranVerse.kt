package com.vakitniyet.quran.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "quran_verses",
    uniqueConstraints = [UniqueConstraint(columnNames = ["verse_key"])]
)
class QuranVerse(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    // "2:148" formatı — sure_no:ayet_no
    @Column(name = "verse_key", nullable = false, unique = true, length = 20)
    val verseKey: String,

    @Column(name = "sure_no", nullable = false)
    val sureNo: Int,

    @Column(name = "ayet_no", nullable = false)
    val ayetNo: Int,

    @Column(name = "sure_name", nullable = false, length = 100)
    val sureName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val text: String,

    @Column(name = "translation_id", nullable = false)
    val translationId: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
