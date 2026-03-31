package com.vakitniyet.prayer.controller

import com.vakitniyet.prayer.client.DiyanetClient
import com.vakitniyet.prayer.client.DiyanetIlce
import com.vakitniyet.prayer.client.DiyanetSehir
import com.vakitniyet.prayer.client.DiyanetUlke
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/public/location")
class LocationController(
    private val diyanetClient: DiyanetClient
) {

    @GetMapping("/countries")
    fun getCountries(): ResponseEntity<List<DiyanetUlke>> =
        ResponseEntity.ok(diyanetClient.getCountries())

    @GetMapping("/cities")
    fun getCities(@RequestParam ulkeId: String): ResponseEntity<List<DiyanetSehir>> =
        ResponseEntity.ok(diyanetClient.getCities(ulkeId))

    @GetMapping("/districts")
    fun getDistricts(@RequestParam ilId: String): ResponseEntity<List<DiyanetIlce>> =
        ResponseEntity.ok(diyanetClient.getDistricts(ilId))
}
