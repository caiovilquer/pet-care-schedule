package dev.vilquer.petcarescheduler.usecase.contract.drivenports

/**
 * Cache genérico usado para não pagar novamente por chamadas já feitas ao
 * Google (geocoding, nearby search, details, reviews, fotos) — compartilhado
 * entre TODOS os usuários da aplicação, ao contrário de um cache por
 * navegador. `T` pode ser nulável: resultados "não encontrado" (ex.: CEP
 * inválido) também são cacheados, para não bater no Google de novo a cada
 * tentativa com o mesmo CEP inválido.
 */
interface PlacesCachePort {
    fun <T> getOrCompute(key: String, ttlSeconds: Long, loader: () -> T): T
}
