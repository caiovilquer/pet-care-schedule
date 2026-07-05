package dev.vilquer.petcarescheduler.application.exception

// O mapeamento HTTP (404) é responsabilidade do ApiExceptionHandler, no adapter REST.
class NotFoundException(message: String) : RuntimeException(message)
