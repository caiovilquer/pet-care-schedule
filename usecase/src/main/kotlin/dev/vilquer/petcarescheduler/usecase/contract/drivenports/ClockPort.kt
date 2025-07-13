package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import java.time.ZonedDateTime

fun interface ClockPort {
    fun now(): ZonedDateTime
}