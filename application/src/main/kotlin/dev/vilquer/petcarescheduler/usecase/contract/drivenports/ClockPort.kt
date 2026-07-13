package dev.vilquer.petcarescheduler.usecase.contract.drivenports

import java.time.ZonedDateTime
import java.time.ZoneId

fun interface ClockPort {
    fun now(): ZonedDateTime
    fun now(zoneId: ZoneId): ZonedDateTime = now().withZoneSameInstant(zoneId)
}
