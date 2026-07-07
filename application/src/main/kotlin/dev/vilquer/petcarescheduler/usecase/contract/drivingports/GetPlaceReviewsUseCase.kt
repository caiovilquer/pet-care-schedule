package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.result.PlaceReview

fun interface GetPlaceReviewsUseCase {
    fun getReviews(placeId: String): List<PlaceReview>
}
