package dev.vilquer.petcarescheduler.usecase.result

data class EventsPageResult(
    val items: List<EventSummary>,
    val total: Long,
    val page: Int,
    val size: Int
)