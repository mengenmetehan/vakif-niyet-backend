package com.vakitniyet.subscription.repository

import com.vakitniyet.subscription.entity.Subscription
import com.vakitniyet.subscription.entity.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface SubscriptionRepository : JpaRepository<Subscription, UUID> {

    fun findByUserId(userId: UUID): Optional<Subscription>

    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = :status 
        AND s.trialEnd < :now
    """)
    fun findExpiredTrials(
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        now: LocalDateTime = LocalDateTime.now()
    ): List<Subscription>
}
