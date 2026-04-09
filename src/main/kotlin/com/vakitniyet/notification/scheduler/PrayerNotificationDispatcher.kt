package com.vakitniyet.notification.scheduler

import com.vakitniyet.common.util.TraceContext
import com.vakitniyet.notification.entity.NotificationContentType
import com.vakitniyet.notification.scheduler.DailyNotificationScheduleBuilder.Companion.SCHEDULE_KEY_PREFIX
import com.vakitniyet.notification.service.ApnsService
import com.vakitniyet.notification.service.NotificationContentProvider
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class PrayerNotificationDispatcher(
    private val redisTemplate: StringRedisTemplate,
    private val apnsService: ApnsService,
    private val contentProvider: NotificationContentProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val zone = ZoneId.of("Europe/Istanbul")

    @Scheduled(fixedDelay = 60_000)
    fun dispatch() {
        TraceContext.newTrace()
        val now = LocalTime.now(zone)
        val today = LocalDate.now(zone).format(dateFmt)
        val minuteOfDay = (now.hour * 60 + now.minute).toDouble()

        val scheduleKey = "$SCHEDULE_KEY_PREFIX$today"
        val ops = redisTemplate.opsForZSet()

        val entries = ops.rangeByScore(scheduleKey, minuteOfDay, minuteOfDay) ?: return
        if (entries.isEmpty()) return

        log.info("Bildirim gönderimi başlıyor: ${entries.size} kullanıcı, minute=$minuteOfDay (${now.hour}:${now.minute.toString().padStart(2, '0')})")

        for (value in entries) {
            val parts = value.split("|")
            if (parts.size != 4) {
                ops.remove(scheduleKey, value)
                continue
            }

            val (deviceToken, label, contentTypeStr, prayerIndexStr) = parts
            val contentType = runCatching {
                NotificationContentType.valueOf(contentTypeStr)
            }.getOrDefault(NotificationContentType.KARMA)
            val prayerIndex = prayerIndexStr.toIntOrNull() ?: 0

            val (content, source) = contentProvider.getContent(contentType, prayerIndex)

            apnsService.send(
                deviceToken = deviceToken,
                title = "$label vakti yaklaşıyor",
                body = "$content ($source)"
            )

            ops.remove(scheduleKey, value)
        }

        log.info("Bildirim gönderimi tamamlandı: ${entries.size} bildirim gönderildi, minute=$minuteOfDay")
        TraceContext.clear()
    }
}
