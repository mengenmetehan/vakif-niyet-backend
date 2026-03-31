package com.vakitniyet.notification.scheduler

import com.vakitniyet.notification.repository.NotificationSettingsRepository
import com.vakitniyet.notification.service.ApnsService
import com.vakitniyet.notification.service.NotificationContentProvider
import com.vakitniyet.prayer.client.DiyanetClient
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class PrayerNotificationScheduler(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val diyanetClient: DiyanetClient,
    private val apnsService: ApnsService,
    private val contentProvider: NotificationContentProvider,
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val zone = ZoneId.of("Europe/Istanbul")

    // Namaz adı → enabled kontrolü için kullanılan etiketler
    private val prayerLabels = mapOf(
        "Imsak" to "İmsak",
        "Ogle" to "Öğle",
        "Ikindi" to "İkindi",
        "Aksam" to "Akşam",
        "Yatsi" to "Yatsı"
    )

    @Scheduled(fixedDelay = 60_000)
    fun sendPrayerNotifications() {
        val now = LocalTime.now(zone)
        val today = LocalDate.now(zone).format(dateFmt)
        val activeSettings = notificationSettingsRepository.findAllActiveWithDeviceToken()

        for (settings in activeSettings) {
            val ilceId = settings.ilceId ?: continue
            val deviceToken = settings.user.deviceToken ?: continue
            val vakit = diyanetClient.getTodayTimes(ilceId) ?: continue

            val prayerTimes = buildMap {
                if (settings.fajrEnabled) put("Imsak", vakit.imsak)
                if (settings.dhuhrEnabled) put("Ogle", vakit.ogle)
                if (settings.asrEnabled) put("Ikindi", vakit.ikindi)
                if (settings.maghribEnabled) put("Aksam", vakit.aksam)
                if (settings.ishaEnabled) put("Yatsi", vakit.yatsi)
            }

            for ((key, timeStr) in prayerTimes) {
                val prayerTime = LocalTime.parse(timeStr, timeFmt)
                val notifyAt = prayerTime.minusMinutes(settings.offsetMinutes.toLong())

                if (now.hour != notifyAt.hour || now.minute != notifyAt.minute) continue

                val sentKey = "notif:sent:${settings.user.id}:$key:$today"
                if (redisTemplate.hasKey(sentKey) == true) continue

                val label = prayerLabels[key] ?: key
                val (content, source) = contentProvider.getContent(settings.contentType)

                apnsService.send(
                    deviceToken = deviceToken,
                    title = "$label vakti yaklaşıyor",
                    body = "$content ($source)"
                )

                redisTemplate.opsForValue().set(sentKey, "1", Duration.ofHours(25))
                log.info("Bildirim gönderildi: userId=${settings.user.id} prayer=$key")
            }
        }
    }
}
