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
    private val restClient = RestClient.create("https://maps-api.apple.com")

    fun searchMosques(lat: Double, lon: Double, radius: Int): List<MosqueDto> {
        if (!tokenProvider.isConfigured) {
            log.warn("MapKit yapılandırması eksik, arama atlandı: lat={}, lon={}", lat, lon)
            return emptyList()
        }
        return try {
            val response = restClient.get()
                .uri { builder ->
                    builder.path("/v1/search")
                        .queryParam("q", "mosque cami")
                        .queryParam("userLocation", "$lat,$lon")
                        .queryParam("lang", "tr-TR")
                        .queryParam("limitToCountries", "TR")
                        .build()
                }
                .header("Authorization", "Bearer ${tokenProvider.getAccessToken()}")
                .retrieve()
                .body(MapKitSearchResponse::class.java)

            response?.results.orEmpty()
                .filter { it.coordinate != null }
                .mapIndexed { idx, place ->
                    MosqueDto(
                        id = place.id ?: "mapkit-$idx",
                        name = place.name ?: "Cami",
                        lat = place.coordinate!!.latitude,
                        lon = place.coordinate.longitude,
                        distanceMeters = haversineDistance(lat, lon, place.coordinate.latitude, place.coordinate.longitude),
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
