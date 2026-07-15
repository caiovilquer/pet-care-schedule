package dev.vilquer.petcarescheduler.application.adapter.input.rest

import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentHousehold
import dev.vilquer.petcarescheduler.application.adapter.input.security.CurrentJwt
import dev.vilquer.petcarescheduler.application.adapter.input.security.tutorId
import dev.vilquer.petcarescheduler.application.service.RateLimitAction
import dev.vilquer.petcarescheduler.application.service.RateLimiterService
import dev.vilquer.petcarescheduler.core.domain.entity.PetId
import dev.vilquer.petcarescheduler.usecase.command.AddAssistantAnswerFeedbackCommand
import dev.vilquer.petcarescheduler.usecase.command.AskPetHistoryQuestionCommand
import dev.vilquer.petcarescheduler.usecase.command.ReindexKnowledgeSourceCommand
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.KnowledgeIndexUseCase
import dev.vilquer.petcarescheduler.usecase.contract.drivingports.PetHistoryAssistantUseCase
import dev.vilquer.petcarescheduler.usecase.result.KnowledgeSourceResult
import dev.vilquer.petcarescheduler.usecase.result.PetHistoryAnswerResult
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class AskPetHistoryQuestionRequest(
    @field:Positive val petId: Long,
    @field:NotBlank @field:Size(max = 1_000) val question: String,
)

data class AssistantAnswerFeedbackRequest(
    val positive: Boolean,
    @field:Size(max = 80) val reason: String? = null,
    @field:Size(max = 1_000) val comment: String? = null,
)

@RestController
@RequestMapping("/api/v1/assistant")
@Validated
class PetHistoryAssistantController(
    private val assistant: PetHistoryAssistantUseCase,
    private val knowledge: KnowledgeIndexUseCase,
    private val household: CurrentHousehold,
    private val rateLimiter: RateLimiterService,
) {
    @PostMapping("/questions")
    fun ask(
        @Valid @RequestBody body: AskPetHistoryQuestionRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
        request: HttpServletRequest,
    ): PetHistoryAnswerResult {
        val access = household.resolve(jwt)
        rateLimiter.check(
            RateLimitAction.ASSISTANT_QUESTION,
            "${request.remoteAddr ?: "unknown"}:${jwt.tutorId()}:${access.householdId.value}",
        )
        return assistant.ask(AskPetHistoryQuestionCommand(PetId(body.petId), body.question), access)
    }

    @PostMapping("/answers/{id}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun feedback(
        @PathVariable id: UUID,
        @Valid @RequestBody body: AssistantAnswerFeedbackRequest,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ) = assistant.addFeedback(
        AddAssistantAnswerFeedbackCommand(id, body.positive, body.reason?.trim()?.uppercase(), body.comment),
        household.resolve(jwt),
    )

    @GetMapping("/knowledge-sources")
    fun sources(
        @RequestParam @Positive petId: Long,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): List<KnowledgeSourceResult> = knowledge.listSources(PetId(petId), household.resolve(jwt))

    @PostMapping("/knowledge-sources/{id}/reindex")
    fun reindex(
        @PathVariable id: UUID,
        @AuthenticationPrincipal jwt: CurrentJwt,
    ): KnowledgeSourceResult = knowledge.reindex(ReindexKnowledgeSourceCommand(id), household.resolve(jwt))
}
