package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.result.PlaceDetails

fun interface GetPlaceDetailsUseCase {
    fun getDetails(placeId: String): PlaceDetails
}
