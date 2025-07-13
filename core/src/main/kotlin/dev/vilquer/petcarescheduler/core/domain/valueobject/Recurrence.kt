package dev.vilquer.petcarescheduler.core.domain.valueobject

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


data class Recurrence(
    val frequency: Frequency = Frequency.DAILY,
    val intervalCount: Long = 1,
    val repetitions: Int? = null,
    val finalDate: LocalDateTime? = null
) {
    init {
        require(intervalCount > 0) { "Interval must be higher than zero" }
        require(repetitions == null || repetitions > 0) { "If exists, repetitions must be higher than zero" }
    }

    fun nextOccurrence(date: LocalDateTime): LocalDateTime = when (frequency) {
        Frequency.DAILY  -> date.plus(intervalCount, ChronoUnit.DAYS)
        Frequency.WEEKLY -> date.plus(intervalCount, ChronoUnit.WEEKS)
        Frequency.MONTHLY -> date.plus(intervalCount, ChronoUnit.MONTHS)
        Frequency.YEARLY  -> date.plus(intervalCount, ChronoUnit.YEARS)
    }


    fun hasNext(executeCount: Int, lastDate: LocalDateTime): Boolean {

        if (repetitions != null && executeCount >= repetitions) return false

        if (finalDate != null && !lastDate.isBefore(finalDate)) return false

        return true
    }
}

enum class Frequency { YEARLY, MONTHLY, WEEKLY, DAILY }
