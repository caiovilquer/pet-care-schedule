package dev.vilquer.petcarescheduler.application.exception

// O mapeamento HTTP (401) é responsabilidade do ApiExceptionHandler, no adapter REST.
class InvalidCredentialsException(message: String) : RuntimeException(message)
