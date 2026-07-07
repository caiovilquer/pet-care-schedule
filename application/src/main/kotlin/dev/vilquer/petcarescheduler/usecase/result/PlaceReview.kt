package dev.vilquer.petcarescheduler.usecase.result

data class PlaceReview(
    val authorName: String,
    val rating: Int,
    val text: String,
    val relativeTime: String,
    val profilePhotoUrl: String?
)
