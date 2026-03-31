package com.vakitniyet.mosque.controller

import com.vakitniyet.mosque.dto.MosqueListResponse
import com.vakitniyet.mosque.dto.MosqueSource
import com.vakitniyet.mosque.service.MosqueService
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/mosques")
@Validated
class MosqueController(
    private val mosqueService: MosqueService
) {

    @GetMapping
    fun getNearestMosques(
        @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") lat: Double,
        @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") lon: Double,
        @RequestParam(defaultValue = "5000") @Min(1) @Max(10000) radius: Int
    ): ResponseEntity<MosqueListResponse> {
        val result = mosqueService.getMosques(lat, lon, radius)
        if (result.source != MosqueSource.CACHE) {
            mosqueService.enrichAsync(lat, lon, radius)
        }
        return ResponseEntity.ok(result)
    }
}
