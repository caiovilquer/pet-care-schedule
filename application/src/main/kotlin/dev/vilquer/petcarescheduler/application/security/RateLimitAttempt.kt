package dev.vilquer.petcarescheduler.application.security

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "rate_limit_attempt")
class RateLimitAttempt(
    @Id
    @Column(name = "id", nullable = false, length = 200)
    var id: String = "",

    @Column(name = "count", nullable = false)
    var count: Int = 0,

    @Column(name = "window_start", nullable = false)
    var windowStart: Instant = Instant.now(),

    @Version
    var version: Long? = null
)
