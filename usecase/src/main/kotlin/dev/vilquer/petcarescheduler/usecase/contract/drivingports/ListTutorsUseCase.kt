package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.result.TutorsPageResult

fun interface ListTutorsUseCase {
    fun list(page: Int, size: Int): TutorsPageResult
}