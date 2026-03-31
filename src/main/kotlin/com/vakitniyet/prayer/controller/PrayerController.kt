package com.vakitniyet.prayer.controller

import com.vakitniyet.prayer.dto.*
import com.vakitniyet.prayer.service.PrayerService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/prayer")
class PrayerController(
    private val prayerService: PrayerService
) {

    // Bugünkü namazlar
    @GetMapping("/today")
    fun getToday(authentication: Authentication): ResponseEntity<DayResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.getToday(userId))
    }

    // Belirli gün
    @GetMapping("/day/{date}")
    fun getDay(
        authentication: Authentication,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DayResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.getDay(userId, date))
    }

    // Aylık takvim
    @GetMapping("/month")
    fun getMonth(
        authentication: Authentication,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<MonthResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.getMonth(userId, year, month))
    }

    // Namaz tikle
    @PostMapping("/toggle")
    fun toggle(
        authentication: Authentication,
        @RequestBody request: TogglePrayerRequest
    ): ResponseEntity<PrayerLogResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.togglePrayer(userId, request))
    }

    // Streak
    @GetMapping("/streak")
    fun getStreak(authentication: Authentication): ResponseEntity<StreakResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.getStreak(userId))
    }

    // İstatistikler
    @GetMapping("/stats")
    fun getStats(authentication: Authentication): ResponseEntity<StatsResponse> {
        val userId = authentication.principal as UUID
        return ResponseEntity.ok(prayerService.getStats(userId))
    }

    // Bugünkü namaz vakitleri (Diyanet)
    @GetMapping("/times/today")
    fun getTodayTimes(
        authentication: Authentication,
        @RequestParam ilceId: String
    ): ResponseEntity<PrayerTimesResponse> {
        return ResponseEntity.ok(prayerService.getTodayTimes(ilceId))
    }
}
