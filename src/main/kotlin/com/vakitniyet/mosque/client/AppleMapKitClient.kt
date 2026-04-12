package com.vakitniyet.mosque.client

import com.vakitniyet.mosque.dto.MapKitSearchResponse
import com.vakitniyet.mosque.dto.MosqueDto
import com.vakitniyet.mosque.dto.MosqueSource
import com.vakitniyet.mosque.dto.haversineDistance
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient

@Component
class AppleMapKitClient(
    private val tokenProvider: AppleMapKitTokenProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create("https://api.apple-mapkit.com")

    fun searchMosques(lat: Double, lon: Double, radius: Int): List<MosqueDto> {
        if (!tokenProvider.isConfigured) {
            log.warn("MapKit yapılandırması eksik, arama atlandı: lat={}, lon={}", lat, lon)
            return emptyList()
        }
        return try {
            // radius metre → derece (1 derece ≈ 111km)
            val radiusDeg = radius / 111_000.0
            val searchRegion = "${lat - radiusDeg},${lon - radiusDeg},${lat + radiusDeg},${lon + radiusDeg}"
            log.debug("MapKit isteği: GET https://api.apple-mapkit.com/v1/search lat={}, lon={}, radius={}", lat, lon, radius)
            val response = restClient.get()
                .uri { builder ->
                    builder.path("/v1/search")
                        .queryParam("q", "cami")
                        .queryParam("userLocation", "$lat,$lon")
                        .queryParam("searchRegion", searchRegion)
                        .queryParam("lang", "tr-TR")
                        .queryParam("limitToCountries", "TR")
                        .build()
                }
                .header("Authorization", "Bearer ${tokenProvider.getAccessToken()}")
                .retrieve()
                .body(MapKitSearchResponse::class.java)
            log.debug("MapKit ham response: results={}", response?.results?.size)

            val results = response?.results.orEmpty().filter { it.center != null }
            log.info("MapKit {} sonuç döndürdü: lat={}, lon={}, radius={}", results.size, lat, lon, radius)

            results.mapIndexed { idx, place ->
                    MosqueDto(
                        id = place.muid ?: "mapkit-$idx",
                        name = place.name ?: "Cami",
                        lat = place.center!!.lat,
                        lon = place.center.lng,
                        distanceMeters = haversineDistance(lat, lon, place.center.lat, place.center.lng),
                        source = MosqueSource.MAPKIT
                    )
                }
        } catch (e: HttpClientErrorException) {
            log.error("MapKit API {} hatası: body={}", e.statusCode.value(), e.responseBodyAsString)
            emptyList()
        } catch (e: HttpServerErrorException) {
            log.error("MapKit API {} hatası: body={}", e.statusCode.value(), e.responseBodyAsString)
            emptyList()
        } catch (e: Exception) {
            log.error("MapKit API hatası", e)
            emptyList()
        }
    }
}
