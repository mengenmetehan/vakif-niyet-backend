package com.vakitniyet.mosque.dto

import kotlin.math.*

enum class MosqueSource { MAPKIT, OVERPASS, CACHE }

data class MosqueDto(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double,
    val source: MosqueSource
)

data class MosqueListResponse(
    val mosques: List<MosqueDto>,
    val source: MosqueSource,
    val count: Int
)

// --- MapKit response parsing ---
data class MapKitSearchResponse(val results: List<MapKitPlace>?)
data class MapKitPlace(val id: String?, val name: String?, val coordinate: MapKitCoordinate?)
data class MapKitCoordinate(val latitude: Double, val longitude: Double)

// --- Overpass response parsing ---
data class OverpassResponse(val elements: List<OverpassElement>?)
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?
)
data class OverpassCenter(val lat: Double, val lon: Double)

// Package-level utility — imported by clients and service
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * asin(sqrt(a))
}
