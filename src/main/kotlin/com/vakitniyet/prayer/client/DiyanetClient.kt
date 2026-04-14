package com.vakitniyet.prayer.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DiyanetUlke(
    @JsonProperty("UlkeID") val ulkeId: String,
    @JsonProperty("UlkeAdi") val ulkeAdi: String
)

data class DiyanetSehir(
    @JsonProperty("SehirID") val sehirId: String,
    @JsonProperty("SehirAdi") val sehirAdi: String
)

data class DiyanetIlce(
    @JsonProperty("IlceID") val ilceId: String,
    @JsonProperty("IlceAdi") val ilceAdi: String,
    @JsonProperty("DisplayID") val displayId: String = ilceId
)

data class DiyanetVakit(
    @JsonProperty("MiladiTarihKisa") val miladiTarihKisa: String,
    @JsonProperty("Imsak") val imsak: String,
    @JsonProperty("Gunes") val gunes: String,
    @JsonProperty("Ogle") val ogle: String,
    @JsonProperty("Ikindi") val ikindi: String,
    @JsonProperty("Aksam") val aksam: String,
    @JsonProperty("Yatsi") val yatsi: String
)

@Component
class DiyanetClient(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${diyanet.api-url}") private val apiUrl: String,
    private val webClientBuilder: WebClient.Builder
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val webClient = webClientBuilder.baseUrl(apiUrl).build()
    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun getCountries(): List<DiyanetUlke> = getCached(
        key = "diyanet:ulkeler",
        ttl = Duration.ofDays(30),
        fetch = { fetchList("/ulkeler", DiyanetUlke::class.java) }
    )

    fun getCities(ulkeId: String): List<DiyanetSehir> = getCached(
        key = "diyanet:sehirler:$ulkeId",
        ttl = Duration.ofDays(30),
        fetch = { fetchList("/sehirler/$ulkeId", DiyanetSehir::class.java) }
    )

    fun getDistricts(ilId: String): List<DiyanetIlce> {
        val districts = getCached(
            key = "diyanet:ilceler:$ilId",
            ttl = Duration.ofDays(30),
            fetch = { fetchList("/ilceler/$ilId", DiyanetIlce::class.java) }
        )
        return (districts + istanbulMissingDistricts(ilId, districts))
            .sortedBy { it.ilceAdi }
            .map { it.copy(displayId = "${it.ilceId}_${it.ilceAdi}") }
    }

    private fun istanbulMissingDistricts(ilId: String, existing: List<DiyanetIlce>): List<DiyanetIlce> {
        if (ilId != "539") return emptyList()
        val existingNames = existing.map { it.ilceAdi }.toSet()
        return ISTANBUL_MISSING_DISTRICTS
            .filter { it.ilceAdi !in existingNames }
    }

    companion object {
        private const val ISTANBUL_ILCE_ID = "9541"
        private val ISTANBUL_MISSING_DISTRICTS = listOf(
            "ADALAR", "ATAŞEHIR", "BAĞCILAR", "BAHÇELİEVLER", "BAKIRKÖY",
            "BAYRAMPAŞA", "BEŞİKTAŞ", "BEYOĞLU", "ESENLER", "EYÜPSULTAN",
            "FATİH", "GAZİOSMANPAŞA", "GÜNGÖREN", "KADIKÖY", "KAĞITHANE",
            "SARIYER", "ŞİŞLİ", "ÜMRANİYE", "ÜSKÜDAR", "ZEYTİNBURNU"
        ).map { DiyanetIlce(ilceId = ISTANBUL_ILCE_ID, ilceAdi = it) }
    }

    fun getTodayTimes(ilceId: String): DiyanetVakit? {
        val today = LocalDate.now()
        val monthly = getMonthlyTimes(ilceId, today)
        val todayStr = today.format(dateFmt)
        return monthly.find { it.miladiTarihKisa == todayStr }
    }

    private fun getMonthlyTimes(ilceId: String, date: LocalDate): List<DiyanetVakit> {
        val cacheKey = "diyanet:vakitler:$ilceId:${date.year}-${date.monthValue}"

        val cached = redisTemplate.opsForValue().get(cacheKey)
        if (cached != null) {
            return objectMapper.readValue(cached, object : TypeReference<List<DiyanetVakit>>() {})
        }

        val data = fetchFromApi(ilceId)
        if (data.isNotEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(data), Duration.ofDays(35))
        }
        return data
    }

    private fun fetchFromApi(ilceId: String): List<DiyanetVakit> =
        fetchList("/vakitler/$ilceId", DiyanetVakit::class.java)

    private fun <T> fetchList(path: String, clazz: Class<T>): List<T> {
        return try {
            webClient.get()
                .uri(path)
                .retrieve()
                .bodyToFlux(clazz)
                .collectList()
                .block() ?: emptyList()
        } catch (e: Exception) {
            log.error("Diyanet API hatası: path=$path", e)
            emptyList()
        }
    }

    private inline fun <reified T> getCached(
        key: String,
        ttl: Duration,
        fetch: () -> List<T>
    ): List<T> {
        val cached = redisTemplate.opsForValue().get(key)
        if (cached != null) {
            return objectMapper.readValue(cached, object : TypeReference<List<T>>() {})
        }
        val data = fetch()
        if (data.isNotEmpty()) {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttl)
        }
        return data
    }
}
