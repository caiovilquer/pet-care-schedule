package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import java.time.Duration
import java.time.Instant

interface RateLimitStorePort {
    /**
     * Registra uma tentativa dentro da janela vigente (criando uma nova se a
     * anterior expirou) e devolve o total acumulado, incluindo esta.
     */
    fun registerAttempt(key: String, now: Instant, window: Duration): Int

    fun delete(key: String)

    fun deleteOlderThan(cutoff: Instant): Int
}
