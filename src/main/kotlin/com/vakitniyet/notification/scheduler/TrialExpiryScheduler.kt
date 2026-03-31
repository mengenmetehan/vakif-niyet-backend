package com.vakitniyet.notification.scheduler

import com.vakitniyet.subscription.service.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TrialExpiryScheduler(
    private val subscriptionService: SubscriptionService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Her gece 00:05'te çalış
    @Scheduled(cron = "0 5 0 * * *")
    fun expireTrials() {
        log.info("Trial expiry job başlatıldı")
        subscriptionService.expireTrials()
        log.info("Trial expiry job tamamlandı")
    }
}
