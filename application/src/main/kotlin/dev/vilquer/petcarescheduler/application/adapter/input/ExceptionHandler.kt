package dev.vilquer.petcarescheduler.application.adapter.input

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler as SpringExceptionHandler
import java.time.Instant

@ControllerAdvice
class ExceptionHandler {

    data class ErrorResponse(
        val message: String,
        val timestamp: Instant = Instant.now()
    )

    @SpringExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Invalid request"))

    @SpringExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(e.message ?: "Resource not found"))

    @SpringExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(e: BadCredentialsException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(e.message ?: "Unauthorized"))
}