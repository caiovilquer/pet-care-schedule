package dev.vilquer.petcarescheduler.application.adapter.input.rest

import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException

@RestControllerAdvice
class ApiExceptionHandler {

    data class ValidationError(val field: String?, val message: String)
    data class ApiError(
        val status: Int,
        val error: String,
        val message: String?,
        val details: List<ValidationError>? = null
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val details = ex.bindingResult.fieldErrors
            .map { ValidationError(it.field, it.defaultMessage ?: "valor inválido") }
        val body = ApiError(400, "Bad Request", "Falha de validação", details)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraint(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        val details = ex.constraintViolations.map { v ->
            ValidationError(v.propertyPath?.toString(), v.message)
        }
        val body = ApiError(400, "Bad Request", "Parâmetros inválidos", details)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        val body = ApiError(400, "Bad Request", "JSON mal formado ou tipo inválido")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ApiError> {
        val message = if (ex.rootCause?.message?.contains("uq_tutor_email", ignoreCase = true) == true)
            "E-mail já cadastrado"
        else "Violação de integridade"
        val body = ApiError(409, "Conflict", message)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body)
    }
}
