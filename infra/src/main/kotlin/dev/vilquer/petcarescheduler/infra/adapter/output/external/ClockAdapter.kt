package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.ZoneId

@Component
class ClockAdapter(
    @param:Value("\${app.timezone:America/Sao_Paulo}") private val timezone: String
) : ClockPort {
    override fun now(): ZonedDateTime = ZonedDateTime.now(parseZoneId(timezone))

    private fun parseZoneId(value: String): ZoneId =
        try {
            ZoneId.of(value)
        } catch (ex: Exception) {
            ZoneId.of("America/Sao_Paulo")
        }
}
