package com.vakitniyet.mosque.client

import com.vakitniyet.mosque.dto.MosqueDto
import com.vakitniyet.mosque.dto.MosqueSource
import com.vakitniyet.mosque.dto.OverpassResponse
import com.vakitniyet.mosque.dto.haversineDistance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
class OverpassClient(
    @Value("\${overpass.base-url}") baseUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.create(baseUrl)

    fun searchMosques(lat: Double, lon: Double, radius: Int): List<MosqueDto> {
        val query = buildQuery(lat, lon, radius)
        return try {
            val body = LinkedMultiValueMap<String, String>().apply { add("data", query) }
            val response = restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(OverpassResponse::class.java)

            response?.elements.orEmpty().mapNotNull { el ->
                val elLat = el.lat ?: el.center?.lat ?: return@mapNotNull null
                val elLon = el.lon ?: el.center?.lon ?: return@mapNotNull null
                MosqueDto(
                    id = "${el.type}-${el.id}",
                    name = resolveOsmName(el.tags),
                    lat = elLat,
                    lon = elLon,
                    distanceMeters = haversineDistance(lat, lon, elLat, elLon),
                    source = MosqueSource.OVERPASS
                )
            }
        } catch (e: Exception) {
            log.debug("Overpass API hatası (mevcut cache korunacak): ${e.message}")
            emptyList()
        }
    }

    private fun resolveOsmName(tags: Map<String, String>?): String {
        if (tags == null) return "Cami"
        return tags["name"]
            ?: tags["name:tr"]
            ?: tags["official_name"]
            ?: tags["alt_name"]
            ?: tags["operator"]?.takeIf { it.length < 60 }
            ?: tags["addr:neighbourhood"]?.let { "$it Camii" }
            ?: tags["addr:street"]?.let { "$it Camii" }
            ?: "Cami"
    }

    private fun buildQuery(lat: Double, lon: Double, radius: Int) = """
        [out:json][timeout:10];
        (
          node["amenity"="place_of_worship"]["religion"="muslim"](around:$radius,$lat,$lon);
          way["amenity"="place_of_worship"]["religion"="muslim"](around:$radius,$lat,$lon);
          relation["amenity"="place_of_worship"]["religion"="muslim"](around:$radius,$lat,$lon);
        );
        out center;
    """.trimIndent()
}
