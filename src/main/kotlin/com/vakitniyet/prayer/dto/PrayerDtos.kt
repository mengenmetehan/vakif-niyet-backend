package com.vakitniyet.prayer.dto

import java.time.LocalDate

data class TogglePrayerRequest(
    val prayerName: String,
    val date: String? = null  // yyyy-MM-dd, null ise bugün
)

data class PrayerLogResponse(
    val prayerName: String,
    val date: String,
    val isDone: Boolean,
    val prayedAt: String?
)

data class DayResponse(
    val date: String,
    val prayers: List<PrayerLogResponse>,
    val doneCount: Int
)

data class MonthResponse(
    val year: Int,
    val month: Int,
    val days: List<DaySummary>,
    val prevYear: Int,
    val prevMonth: Int,
    val nextYear: Int,
    val nextMonth: Int
)

data class DaySummary(
    val date: String,
    val doneCount: Int,
    val prayers: List<DonePrayer>
)

data class DonePrayer(
    val prayerName: String,
    val prayedAt: String?
)

data class StreakResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastFullDay: String?
)

data class StatsResponse(
    val streak: StreakResponse,
    val thisMonthTotal: Int,
    val thisMonthDays: Int,
    val completionRate: Int,  // yüzde
    val mostMissed: String?
)

data class PrayerTimesResponse(
    val date: String,
    val ilceId: String,
    val imsak: String,
    val gunes: String,
    val ogle: String,
    val ikindi: String,
    val aksam: String,
    val yatsi: String
)
