package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.result.PlacePhoto

fun interface GetPlacePhotoUseCase {
    fun getPhoto(photoReference: String, maxWidth: Int): PlacePhoto
}
