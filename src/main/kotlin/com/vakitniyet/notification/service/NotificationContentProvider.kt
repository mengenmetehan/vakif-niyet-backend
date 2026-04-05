package com.vakitniyet.notification.service

import com.vakitniyet.notification.entity.NotificationContentType
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class NotificationContentProvider(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // index: 0=İmsak, 1=Öğle, 2=İkindi, 3=Akşam, 4=Yatsı
        fun ayetKey(index: Int) = "content:daily:ayet:$index"
        fun ayetSourceKey(index: Int) = "content:daily:ayet:$index:source"
    }

    private val hadisler = listOf(
        "Namaz dinin direğidir." to "Tirmizî",
        "İki namaz arasındaki günahlar, o iki namaz keffâreti olur." to "Müslim",
        "Kıyamet günü kuldan ilk sorulacak şey namazdır." to "Tirmizî",
        "Namaz nurdur." to "Müslim",
        "Gözümün nuru namazda kılındı." to "Nesâî",
        "Namaz vakti girince ezan okuyun, sonra ikamet getirin." to "Buharî",
        "Kim sabah ve ikindi namazını kılarsa cennete girer." to "Buharî"
    )

    private val ayetFallback = listOf(
        "Namazı kılın, zekâtı verin ve rükû edenlerle birlikte rükû edin." to "Bakara 2:43",
        "Şüphesiz namaz, müminlere belirli vakitlerde farz kılınmıştır." to "Nisa 4:103",
        "Rabbinin adını an ve her şeyden kendini çekerek yalnızca O'na yönel." to "Müzzemmil 73:8"
    )

    fun getContent(type: NotificationContentType, prayerIndex: Int = 0): Pair<String, String> {
        return when (type) {
            NotificationContentType.AYET -> getDailyAyet(prayerIndex)
            NotificationContentType.HADIS -> hadisler.random()
            NotificationContentType.KARMA -> if ((0..1).random() == 0) getDailyAyet(prayerIndex) else hadisler.random()
        }
    }

    fun getDailyAyet(prayerIndex: Int): Pair<String, String> {
        val text = redisTemplate.opsForValue().get(ayetKey(prayerIndex))
        val source = redisTemplate.opsForValue().get(ayetSourceKey(prayerIndex))
        if (text != null && source != null) return text to source
        log.warn("Günlük ayet cache'de bulunamadı: index=$prayerIndex, fallback kullanılıyor")
        return ayetFallback.random()
    }
}
