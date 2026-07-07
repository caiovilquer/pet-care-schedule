package dev.vilquer.petcarescheduler.application.exception

/** Falha de um serviço externo (ex.: Google Places retornando erro de quota/permissão). */
class UpstreamServiceException(message: String) : RuntimeException(message)
