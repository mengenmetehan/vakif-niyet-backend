package com.vakitniyet.notification.service

import com.vakitniyet.notification.entity.NotificationContentType
import org.springframework.stereotype.Component

@Component
class NotificationContentProvider(
    private val quranClient: QuranClient
) {

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

    fun getContent(type: NotificationContentType): Pair<String, String> {
        return when (type) {
            NotificationContentType.AYET -> fetchAyet()
            NotificationContentType.HADIS -> hadisler.random()
            NotificationContentType.KARMA -> if ((0..1).random() == 0) fetchAyet() else hadisler.random()
        }
    }

    private fun fetchAyet(): Pair<String, String> =
        quranClient.getRandomVerse() ?: ayetFallback.random()
}
