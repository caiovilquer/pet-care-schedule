package dev.vilquer.petcarescheduler.application.adapter.input.security

import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdAccess
import dev.vilquer.petcarescheduler.core.domain.household.HouseholdId
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.HouseholdContextUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.util.UUID

/** Resolve a família no servidor; o cabeçalho é apenas uma seleção, nunca uma autorização. */
@Component
class CurrentHousehold(
    private val context: HouseholdContextUseCase,
    private val request: HttpServletRequest,
) {
    fun resolve(jwt: CurrentJwt): HouseholdAccess {
        val raw = request.getHeader(HEADER)?.trim()?.takeIf(String::isNotEmpty)
        require(raw == null || raw.length == 36) { "household_header_invalid" }
        val requested = raw?.let {
            runCatching { HouseholdId(UUID.fromString(it)) }
                .getOrElse { throw IllegalArgumentException("household_header_invalid") }
        }
        return context.resolve(TutorId(jwt.tutorId()), requested)
    }

    companion object { const val HEADER = "X-Household-Id" }
}
