package com.vakitniyet.notification.scheduler

import com.vakitniyet.notification.repository.NotificationSettingsRepository
import com.vakitniyet.prayer.client.DiyanetClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.StringRedisTemplate
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class DailyNotificationScheduleBuilder(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val diyanetClient: DiyanetClient,
    private val redisTemplate: StringRedisTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val zone = ZoneId.of("Europe/Istanbul")

    companion object {
        const val SCHEDULE_KEY_PREFIX = "notif:daily:"
        const val ILCE_PAGE_SIZE = 50
        const val USER_PAGE_SIZE = 200
    }

    @PostConstruct
    fun buildOnStartupIfMissing() {
        val todayKey = "$SCHEDULE_KEY_PREFIX${LocalDate.now(zone).format(dateFmt)}"
        if (redisTemplate.hasKey(todayKey) != true) {
            log.info("Başlangıçta günlük plan bulunamadı, oluşturuluyor...")
            buildDailySchedule()
        } else {
            log.info("Başlangıçta günlük plan zaten mevcut, atlanıyor.")
        }
    }

    // Her gün 02:00'de çalışır
    @Scheduled(cron = "0 0 2 * * *", zone = "Europe/Istanbul")
    fun buildDailySchedule() {
        val today = LocalDate.now(zone)
        val todayKey = "$SCHEDULE_KEY_PREFIX${today.format(dateFmt)}"

        // Önceki günün schedule'ını temizle (dün kalan)
        val yesterday = today.minusDays(1)
        val yesterdayKey = "$SCHEDULE_KEY_PREFIX${yesterday.format(dateFmt)}"
        val deletedCount = redisTemplate.opsForZSet().size(yesterdayKey) ?: 0
        redisTemplate.delete(yesterdayKey)
        log.info("Önceki günün planı silindi: key=$yesterdayKey, $deletedCount kayıt temizlendi")

        log.info("Günlük bildirim planı oluşturuluyor: $todayKey")

        var ilcePage = 0
        var totalEntries = 0
        var hasMoreIlce = true

        while (hasMoreIlce) {
            val ilceler = notificationSettingsRepository
                .findDistinctActiveIlceIds(PageRequest.of(ilcePage, ILCE_PAGE_SIZE))

            for (ilceId in ilceler.content) {
                val vakit = diyanetClient.getTodayTimes(ilceId)
                if (vakit == null) {
                    log.warn("Vakit alınamadı: ilceId=$ilceId")
                } else {
                    val added = buildScheduleForIlce(ilceId, vakit, todayKey)
                    log.info("Cache eklendi: ilceId=$ilceId, $added bildirim planlandı")
                    totalEntries += added
                }
            }

            hasMoreIlce = ilceler.hasNext()
            ilcePage++
        }

        // 24 saat TTL — ertesi gün 02:00'de yeni plan yazılana kadar geçerli
        redisTemplate.expire(todayKey, Duration.ofHours(26))
        log.info("Günlük bildirim planı tamamlandı: $totalEntries kayıt, key=$todayKey")
    }

    private fun buildScheduleForIlce(
        ilceId: String,
        vakit: com.vakitniyet.prayer.client.DiyanetVakit,
        scheduleKey: String
    ): Int {
        val prayerTimeMap = mapOf(
            "Imsak" to vakit.imsak,
            "Ogle" to vakit.ogle,
            "Ikindi" to vakit.ikindi,
            "Aksam" to vakit.aksam,
            "Yatsi" to vakit.yatsi
        )

        val prayerLabelMap = mapOf(
            "Imsak" to "İmsak",
            "Ogle" to "Öğle",
            "Ikindi" to "İkindi",
            "Aksam" to "Akşam",
            "Yatsi" to "Yatsı"
        )

        // 0=İmsak, 1=Öğle, 2=İkindi, 3=Akşam, 4=Yatsı — günlük ayet cache index'i
        val prayerIndexMap = mapOf(
            "Imsak" to 0,
            "Ogle" to 1,
            "Ikindi" to 2,
            "Aksam" to 3,
            "Yatsi" to 4
        )

        var count = 0
        var userCount = 0
        var userPage = 0
        var hasMoreUsers = true

        while (hasMoreUsers) {
            val users = notificationSettingsRepository
                .findActiveByIlceId(ilceId, PageRequest.of(userPage, USER_PAGE_SIZE))

            val entries = mutableMapOf<Double, MutableList<String>>()

            userCount += users.numberOfElements

            for (settings in users.content) {
                val deviceToken = settings.user.deviceToken ?: continue

                val enabledPrayers = buildList {
                    if (settings.fajrEnabled) add("Imsak")
                    if (settings.dhuhrEnabled) add("Ogle")
                    if (settings.asrEnabled) add("Ikindi")
                    if (settings.maghribEnabled) add("Aksam")
                    if (settings.ishaEnabled) add("Yatsi")
                }

                for (prayerKey in enabledPrayers) {
                    val timeStr = prayerTimeMap[prayerKey] ?: continue
                    val prayerTime = LocalTime.parse(timeStr, timeFmt)
                    val notifyAt = prayerTime.minusMinutes(settings.offsetMinutes.toLong())
                    val minuteOfDay = (notifyAt.hour * 60 + notifyAt.minute).toDouble()

                    val label = prayerLabelMap[prayerKey] ?: prayerKey
                    val prayerIndex = prayerIndexMap[prayerKey] ?: 0
                    // value format: deviceToken|label|contentType|prayerIndex
                    val value = "${deviceToken}|${label}|${settings.contentType}|${prayerIndex}"
                    entries.getOrPut(minuteOfDay) { mutableListOf() }.add(value)
                }
            }

            // Sorted Set'e toplu yaz
            if (entries.isNotEmpty()) {
                val ops = redisTemplate.opsForZSet()
                for ((score, values) in entries) {
                    for (value in values) {
                        ops.add(scheduleKey, value, score)
                        count++
                    }
                }
            }

            hasMoreUsers = users.hasNext()
            userPage++
        }

        log.info("İlçe planı tamamlandı: ilceId=$ilceId, $userCount kullanıcı işlendi, $count bildirim eklendi")
        return count
    }
}
