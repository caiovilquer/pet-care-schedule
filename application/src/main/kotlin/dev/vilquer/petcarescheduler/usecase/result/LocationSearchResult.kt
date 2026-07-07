package dev.vilquer.petcarescheduler.usecase.result

data class LocationSearchResult(
    val locations: List<LocationItem>,
    val total: Int
)
