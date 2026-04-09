package com.vakitniyet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class VakitNiyetApplication

fun main(args: Array<String>) {
    System.setProperty("java.net.preferIPv4Stack", "true")
    runApplication<VakitNiyetApplication>(*args)
}
