package dev.vilquer.petcarescheduler.infra.adapter.output.cache

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.PlacesCachePort
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache em memória, compartilhado entre todas as requisições da instância —
 * a primeira busca de um CEP/place paga o custo do Google, as seguintes
 * (de qualquer usuário) reaproveitam o resultado até o TTL expirar.
 *
 * Fase 1 deliberada: não sobrevive a restart nem replica entre instâncias.
 * Se isso passar a importar (múltiplas réplicas, restarts frequentes), trocar
 * por um adapter Postgres é uma implementação isolada de [PlacesCachePort] —
 * nenhum outro código muda.
 */
@Component
class InMemoryPlacesCacheAdapter : PlacesCachePort {

    private data class Entry(val value: Any?, val expiresAt: Instant)

    private val store = ConcurrentHashMap<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getOrCompute(key: String, ttlSeconds: Long, loader: () -> T): T {
        val now = Instant.now()

        store[key]?.let { entry ->
            if (entry.expiresAt.isAfter(now)) return entry.value as T
            store.remove(key, entry)
        }

        val value = loader()
        if (store.size >= MAX_ENTRIES) evictOldest()
        store[key] = Entry(value, now.plusSeconds(ttlSeconds))
        return value
    }

    private fun evictOldest() {
        store.entries.minByOrNull { it.value.expiresAt }?.let { store.remove(it.key, it.value) }
    }

    companion object {
        private const val MAX_ENTRIES = 1000
    }
}
