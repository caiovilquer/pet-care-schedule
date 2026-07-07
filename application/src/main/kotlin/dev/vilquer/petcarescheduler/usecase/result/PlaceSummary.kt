package dev.vilquer.petcarescheduler.usecase.result

/**
 * Resultado bruto (normalizado) de um Nearby Search. Não inclui telefone,
 * website ou grade de horários — a API legada do Google só devolve esses
 * campos via Place Details, chamado sob demanda (ver [PlaceDetails]).
 */
data class PlaceSummary(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val businessStatus: String?,
    val openNow: Boolean?,
    val photoReference: String?,
    val types: List<String>,
    val priceLevel: Int?
)
