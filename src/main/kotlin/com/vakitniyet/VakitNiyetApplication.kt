package com.vakitniyet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class VakitNiyetApplication

fun main(args: Array<String>) {
    runApplication<VakitNiyetApplication>(*args)
}
