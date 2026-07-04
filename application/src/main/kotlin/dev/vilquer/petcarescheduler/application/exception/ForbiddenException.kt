package dev.vilquer.petcarescheduler.application.exception

// O mapeamento HTTP (403) é responsabilidade do ApiExceptionHandler, no adapter REST.
class ForbiddenException(msg: String) : RuntimeException(msg)