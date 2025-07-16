package dev.vilquer.petcarescheduler.usecase.contract.drivingports

import dev.vilquer.petcarescheduler.usecase.command.LoginCommand

interface AuthUseCase {
    fun authenticate(cmd: LoginCommand): String   // devolve JWT
}