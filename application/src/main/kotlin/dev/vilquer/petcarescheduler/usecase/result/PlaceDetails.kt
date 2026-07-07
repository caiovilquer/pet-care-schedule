package dev.vilquer.petcarescheduler.usecase.result

data class PlaceDetails(
    val placeId: String,
    val description: String,
    val phone: String?,
    val website: String?,
    val googleMapsUrl: String?,
    val types: List<String>,
    val vicinity: String,
    val priceLevel: Int?,
    val businessStatus: String?,
    val openingHours: OpeningHoursWeek?,
    val photoReferences: List<String>
)
