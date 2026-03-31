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
    val days: List<DaySummary>
)

data class DaySummary(
    val date: String,
    val doneCount: Int
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
