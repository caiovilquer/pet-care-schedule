package dev.vilquer.petcarescheduler.usecase.result

/**
 * Espelha o contrato consumido pelo frontend (`Location`/`Petshop`/`Veterinary`
 * em location.model.ts) — os nomes de campo aqui são o contrato JSON, não
 * apenas convenção interna.
 *
 * `openingHours` vem sempre vazio nesta etapa (busca): a grade semanal
 * completa só é buscada sob demanda na tela de detalhe, para não pagar um
 * Place Details por resultado a cada busca.
 */
data class LocationItem(
    val id: String,
    val name: String,
    val address: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String = "",
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val rating: Double,
    val reviewCount: Int,
    val distance: Double,
    val distanceText: String,
    val durationText: String = "",
    val isOpen: Boolean,
    val openingHours: OpeningHoursWeek,
    val services: List<String>,
    val type: String,
    val photos: List<String> = emptyList(),
    val website: String? = null,
    // Flags específicas de petshop (null quando type == "veterinary")
    val hasGrooming: Boolean? = null,
    val hasDaycare: Boolean? = null,
    val hasHotel: Boolean? = null,
    val hasVaccination: Boolean? = null,
    val acceptedPetTypes: List<String> = listOf("dog", "cat"),
    // Flags específicas de veterinário (null quando type == "petshop")
    val hasEmergency: Boolean? = null,
    // hasLaboratory/hasSurgery/hasRadiology/specialties nunca foram de fato
    // inferidos (herdado do comportamento original) — sempre false/vazio.
    val hasLaboratory: Boolean? = null,
    val hasSurgery: Boolean? = null,
    val hasRadiology: Boolean? = null,
    val specialties: List<String> = emptyList()
)
