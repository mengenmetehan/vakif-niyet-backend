package com.vakitniyet.mosque.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.vakitniyet.mosque.client.AppleMapKitClient
import com.vakitniyet.mosque.client.OverpassClient
import com.vakitniyet.mosque.dto.MosqueDto
import com.vakitniyet.mosque.dto.MosqueListResponse
import com.vakitniyet.mosque.dto.MosqueSource
import com.vakitniyet.mosque.dto.haversineDistance
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.CompletableFuture

@Service
class MosqueService(
    private val mapKitClient: AppleMapKitClient,
    private val overpassClient: OverpassClient,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val cacheTtl = Duration.ofDays(7)

    fun getMosques(lat: Double, lon: Double, radius: Int): MosqueListResponse {
        val key = cacheKey(lat, lon, radius)

        val cached = redisTemplate.opsForValue().get(key)
        if (cached != null) {
            log.debug("Cache hit: key={}", key)
            val mosques = objectMapper.readValue(cached, object : TypeReference<List<MosqueDto>>() {})
            return MosqueListResponse(mosques, MosqueSource.CACHE, mosques.size)
        }

        log.debug("Cache miss: key={}, calling MapKit", key)
        val mosques = mapKitClient.searchMosques(lat, lon, radius).sortedBy { it.distanceMeters }
        if (mosques.isNotEmpty()) {
            log.info("MapKit {} cami döndürdü: key={}, en yakın={:.0f}m, en uzak={:.0f}m",
                mosques.size, key, mosques.first().distanceMeters, mosques.last().distanceMeters)
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(mosques), cacheTtl)
        } else {
            log.warn("MapKit sonuç döndürmedi: key={}", key)
        }
        return MosqueListResponse(mosques, MosqueSource.MAPKIT, mosques.size)
    }

    @Async("mosqueTaskExecutor")
    fun enrichAsync(lat: Double, lon: Double, radius: Int): CompletableFuture<Void?> {
        try {
            val key = cacheKey(lat, lon, radius)
            val overpassResults = overpassClient.searchMosques(lat, lon, radius)

            if (overpassResults.isEmpty()) {
            log.debug("Overpass sonuç döndürmedi, cache güncellenmedi: key={}", key)
                return CompletableFuture.completedFuture(null)
            }

            val existingJson = redisTemplate.opsForValue().get(key)
            val existing: List<MosqueDto> = if (existingJson != null)
                objectMapper.readValue(existingJson, object : TypeReference<List<MosqueDto>>() {})
            else emptyList()

            val merged = merge(existing, overpassResults).sortedBy { it.distanceMeters }
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(merged), cacheTtl)
            log.debug("Cache enriched: key={}, total={}", key, merged.size)
        } catch (e: Exception) {
            log.error("enrichAsync hatası: lat={}, lon={}", lat, lon, e)
        }
        return CompletableFuture.completedFuture(null)
    }

    private fun merge(existing: List<MosqueDto>, overpass: List<MosqueDto>): List<MosqueDto> {
        val result = existing.toMutableList()
        for (candidate in overpass) {
            val tooClose = existing.any {
                haversineDistance(candidate.lat, candidate.lon, it.lat, it.lon) <= 50.0
            }
            if (!tooClose) result.add(candidate)
        }
        return result
    }

    private fun cacheKey(lat: Double, lon: Double, radius: Int) =
        "mosques:${"%.3f".format(java.util.Locale.US, lat)}:${"%.3f".format(java.util.Locale.US, lon)}:$radius"
}
