package dev.vilquer.petcarescheduler.usecase.command

data class NearbySearchQuery(
    val zipCode: String,
    val radiusKm: Double,
    val category: PlaceCategory,
    val isOpenNow: Boolean = false,
    val sortBy: String = "distance"
) {
    init {
        require(Regex("^\\d{5}-?\\d{3}$").matches(zipCode)) { "CEP inválido: $zipCode" }
        require(radiusKm > 0 && radiusKm <= 50) { "radius deve estar entre 0 e 50 km" }
        require(sortBy in setOf("distance", "rating", "name")) { "ordenação inválida" }
    }
}
