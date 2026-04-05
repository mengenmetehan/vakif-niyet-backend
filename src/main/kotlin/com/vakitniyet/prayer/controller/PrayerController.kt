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

    // Aylık takvim — year+month veya page (0=bu ay, -1=geçen ay, vb.) ile erişilebilir
    @GetMapping("/month")
    fun getMonth(
        authentication: Authentication,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
        @RequestParam(required = false, defaultValue = "0") page: Int
    ): ResponseEntity<MonthResponse> {
        val userId = authentication.principal as UUID
        val base = if (year != null && month != null) {
            java.time.LocalDate.of(year, month, 1)
        } else {
            java.time.LocalDate.now().withDayOfMonth(1)
        }.plusMonths(page.toLong())
        return ResponseEntity.ok(prayerService.getMonth(userId, base.year, base.monthValue))
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
