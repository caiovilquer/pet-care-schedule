package dev.vilquer.petcarescheduler.usecase.result

data class PetsPageResult(
    val items: List<PetSummary>,
    val total: Long,
    val page: Int,
    val size: Int
)