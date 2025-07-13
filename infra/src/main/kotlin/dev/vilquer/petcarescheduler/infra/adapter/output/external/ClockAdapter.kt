package dev.vilquer.petcarescheduler.dev.vilquer.petcarescheduler.infra.adapter.output.external

import dev.vilquer.petcarescheduler.usecase.contract.drivenports.ClockPort
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class ClockAdapter : ClockPort {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}