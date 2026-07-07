package dev.vilquer.petcarescheduler.usecase.result

data class DaySchedule(
    val isOpen: Boolean,
    val openTime: String? = null,
    val closeTime: String? = null
)

data class OpeningHoursWeek(
    val monday: DaySchedule,
    val tuesday: DaySchedule,
    val wednesday: DaySchedule,
    val thursday: DaySchedule,
    val friday: DaySchedule,
    val saturday: DaySchedule,
    val sunday: DaySchedule
)
