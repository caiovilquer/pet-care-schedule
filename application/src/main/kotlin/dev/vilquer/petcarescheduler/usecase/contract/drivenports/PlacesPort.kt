package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import dev.vilquer.petcarescheduler.usecase.command.PlaceCategory
import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails
import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto
import dev.vilquer.petcarescheduler.usecase.result.PlaceReview
import dev.vilquer.petcarescheduler.usecase.result.PlaceSummary

interface PlacesPort {
    fun searchNearby(latitude: Double, longitude: Double, radiusMeters: Int, category: PlaceCategory): List<PlaceSummary>

    /** @return null se o place_id não existir (mais) na base do Google. */
    fun details(placeId: String): PlaceDetails?

    fun reviews(placeId: String): List<PlaceReview>

    fun fetchPhoto(photoReference: String, maxWidth: Int): PlacePhoto
}
