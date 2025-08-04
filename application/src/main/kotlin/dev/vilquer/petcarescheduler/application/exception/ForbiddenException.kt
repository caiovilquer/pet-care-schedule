package dev.vilquer.petcarescheduler.application.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(msg: String) : RuntimeException(msg)