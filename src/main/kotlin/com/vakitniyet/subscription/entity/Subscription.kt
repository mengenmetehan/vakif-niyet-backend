package com.vakitniyet.subscription.entity

import com.vakitniyet.auth.entity.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

enum class SubscriptionPlan { TRIAL, MONTHLY, YEARLY }
enum class SubscriptionStatus { ACTIVE, EXPIRED, CANCELLED }

@Entity
@Table(name = "subscriptions")
class Subscription(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var plan: SubscriptionPlan = SubscriptionPlan.TRIAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE,

    @Column(name = "trial_start", nullable = false)
    val trialStart: LocalDateTime = LocalDateTime.now(),

    @Column(name = "trial_end", nullable = false)
    var trialEnd: LocalDateTime,

    @Column(name = "current_period_start")
    var currentPeriodStart: LocalDateTime? = null,

    @Column(name = "current_period_end")
    var currentPeriodEnd: LocalDateTime? = null,

    @Column(name = "apple_original_transaction_id")
    var appleOriginalTransactionId: String? = null,

    @Column(name = "apple_latest_receipt", columnDefinition = "TEXT")
    var appleLatestReceipt: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val isActive: Boolean
        get() = when {
            status == SubscriptionStatus.CANCELLED -> false
            plan == SubscriptionPlan.TRIAL -> LocalDateTime.now().isBefore(trialEnd)
            else -> currentPeriodEnd?.isAfter(LocalDateTime.now()) ?: false
        }
}
