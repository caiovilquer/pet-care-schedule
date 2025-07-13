package dev.vilquer.petcarescheduler.usecase.result

data class TutorsPageResult(
    val items: List<TutorSummary>,
    val total: Long,
    val page: Int,
    val size: Int
)