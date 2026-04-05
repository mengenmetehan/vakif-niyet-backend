package com.vakitniyet.prayer.service

import com.vakitniyet.auth.repository.UserRepository
import com.vakitniyet.prayer.client.DiyanetClient
import com.vakitniyet.prayer.dto.*
import com.vakitniyet.prayer.entity.PrayerLog
import com.vakitniyet.prayer.entity.PrayerName
import com.vakitniyet.prayer.entity.Streak
import com.vakitniyet.prayer.repository.PrayerLogRepository
import com.vakitniyet.prayer.repository.StreakRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class PrayerService(
    private val prayerLogRepository: PrayerLogRepository,
    private val streakRepository: StreakRepository,
    private val userRepository: UserRepository,
    private val diyanetClient: DiyanetClient
) {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dtFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // Bugünkü namazları getir (5 vakit, işaretli veya değil)
    fun getToday(userId: UUID): DayResponse {
        val today = LocalDate.now()
        return getDayResponse(userId, today)
    }

    // Belirli gün
    fun getDay(userId: UUID, date: LocalDate): DayResponse {
        return getDayResponse(userId, date)
    }

    // Aylık özet (takvim grid için) - hangi namazların kılındığı dahil
    fun getMonth(userId: UUID, year: Int, month: Int): MonthResponse {
        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())

        val logs = prayerLogRepository.findByUserIdAndPrayerDateBetween(userId, start, end)
        val logsByDate = logs.filter { it.isDone }.groupBy { it.prayerDate }

        val days = (1..start.lengthOfMonth()).map { day ->
            val date = LocalDate.of(year, month, day)
            val dayLogs = logsByDate[date] ?: emptyList()
            DaySummary(
                date = date.format(dateFmt),
                doneCount = dayLogs.size,
                prayers = dayLogs
                    .sortedBy { it.prayerName.ordinal }
                    .map { DonePrayer(prayerName = it.prayerName.name, prayedAt = it.prayedAt?.format(dtFmt)) }
            )
        }

        val prev = start.minusMonths(1)
        val next = start.plusMonths(1)

        return MonthResponse(
            year = year,
            month = month,
            days = days,
            prevYear = prev.year,
            prevMonth = prev.monthValue,
            nextYear = next.year,
            nextMonth = next.monthValue
        )
    }

    // Namaz tikle / tikini kaldır
    @Transactional
    fun togglePrayer(userId: UUID, request: TogglePrayerRequest): PrayerLogResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("Kullanıcı bulunamadı") }

        val date = request.date?.let { LocalDate.parse(it, dateFmt) } ?: LocalDate.now()
        val prayerName = PrayerName.valueOf(request.prayerName.uppercase())

        val existing = prayerLogRepository.findByUserIdAndPrayerNameAndPrayerDate(
            userId, prayerName, date
        )

        val log = if (existing.isPresent) {
            val log = existing.get()
            log.isDone = !log.isDone
            log.prayedAt = if (log.isDone) LocalDateTime.now() else null
            prayerLogRepository.save(log)
        } else {
            prayerLogRepository.save(
                PrayerLog(
                    user = user,
                    prayerName = prayerName,
                    prayerDate = date,
                    isDone = true,
                    prayedAt = LocalDateTime.now()
                )
            )
        }

        // Streak güncelle
        updateStreak(userId, date)

        return log.toResponse()
    }

    // Streak bilgisi
    fun getStreak(userId: UUID): StreakResponse {
        val streak = streakRepository.findByUserId(userId)
            .orElse(Streak(user = userRepository.findById(userId).get()))
        return streak.toResponse()
    }

    // İstatistikler
    fun getStats(userId: UUID): StatsResponse {
        val now = LocalDate.now()
        val monthStart = now.withDayOfMonth(1)

        val logs = prayerLogRepository.findByUserIdAndPrayerDateBetween(userId, monthStart, now)
        val doneLogs = logs.filter { it.isDone }

        val totalPossible = now.dayOfMonth * 5
        val completionRate = if (totalPossible > 0)
            (doneLogs.size * 100) / totalPossible else 0

        val fullDays = doneLogs.groupBy { it.prayerDate }
            .count { (_, prayers) -> prayers.size == 5 }

        // En çok kaçırılan namaz
        val missedByPrayer = PrayerName.entries.associateWith { name ->
            val expected = now.dayOfMonth
            val done = doneLogs.count { it.prayerName == name }
            expected - done
        }
        val mostMissed = missedByPrayer.maxByOrNull { it.value }?.key?.name

        return StatsResponse(
            streak = getStreak(userId),
            thisMonthTotal = doneLogs.size,
            thisMonthDays = fullDays,
            completionRate = completionRate,
            mostMissed = mostMissed
        )
    }

    // Günlük namaz vakitleri (Diyanet)
    fun getTodayTimes(ilceId: String): PrayerTimesResponse {
        val vakit = diyanetClient.getTodayTimes(ilceId)
            ?: throw RuntimeException("Namaz vakitleri alınamadı: ilceId=$ilceId")
        return PrayerTimesResponse(
            date = LocalDate.now().format(dateFmt),
            ilceId = ilceId,
            imsak = vakit.imsak,
            gunes = vakit.gunes,
            ogle = vakit.ogle,
            ikindi = vakit.ikindi,
            aksam = vakit.aksam,
            yatsi = vakit.yatsi
        )
    }

    // Streak hesaplama
    @Transactional
    fun updateStreak(userId: UUID, changedDate: LocalDate) {
        val user = userRepository.findById(userId).orElseThrow()
        val streak = streakRepository.findByUserId(userId)
            .orElseGet { Streak(user = user) }

        val today = LocalDate.now()

        // Bugün 5 vakit tamamlandı mı?
        val todayLogs = prayerLogRepository.findByUserIdAndPrayerDate(userId, today)
        val todayComplete = todayLogs.count { it.isDone } == 5

        if (todayComplete) {
            val yesterday = today.minusDays(1)
            val wasYesterdayFull = streak.lastFullDay == yesterday || streak.lastFullDay == today

            if (wasYesterdayFull || streak.lastFullDay == null) {
                streak.currentStreak = if (streak.lastFullDay == today)
                    streak.currentStreak
                else
                    streak.currentStreak + 1
            } else {
                streak.currentStreak = 1
            }

            streak.lastFullDay = today
            if (streak.currentStreak > streak.longestStreak) {
                streak.longestStreak = streak.currentStreak
            }
        }

        streak.updatedAt = LocalDateTime.now()
        streakRepository.save(streak)
    }

    private fun getDayResponse(userId: UUID, date: LocalDate): DayResponse {
        val logs = prayerLogRepository.findByUserIdAndPrayerDate(userId, date)
        val logMap = logs.associateBy { it.prayerName }

        val prayers = PrayerName.entries.map { name ->
            val log = logMap[name]
            PrayerLogResponse(
                prayerName = name.name,
                date = date.format(dateFmt),
                isDone = log?.isDone ?: false,
                prayedAt = log?.prayedAt?.format(dtFmt)
            )
        }

        return DayResponse(
            date = date.format(dateFmt),
            prayers = prayers,
            doneCount = prayers.count { it.isDone }
        )
    }

    private fun PrayerLog.toResponse() = PrayerLogResponse(
        prayerName = prayerName.name,
        date = prayerDate.format(dateFmt),
        isDone = isDone,
        prayedAt = prayedAt?.format(dtFmt)
    )

    private fun Streak.toResponse() = StreakResponse(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastFullDay = lastFullDay?.format(dateFmt)
    )
}
