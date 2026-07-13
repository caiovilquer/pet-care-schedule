package dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.ZoneId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdTimezone

@Component
class ClockAdapter(
    @param:Value("\${app.timezone}") private val timezone: String
) : ClockPort {
    override fun now(): ZonedDateTime = ZonedDateTime.now(parseZoneId(timezone))

    private fun parseZoneId(value: String): ZoneId =
        try {
            ZoneId.of(value)
        } catch (ex: Exception) {
            HouseholdTimezone.parse(null)
        }
}
