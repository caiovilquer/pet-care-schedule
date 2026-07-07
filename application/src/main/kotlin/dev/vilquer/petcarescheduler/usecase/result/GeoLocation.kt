package dev.vilquer.petcarescheduler.usecase.result

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String,
    val city: String,
    val state: String,
    val country: String,
    val zipCode: String
)
