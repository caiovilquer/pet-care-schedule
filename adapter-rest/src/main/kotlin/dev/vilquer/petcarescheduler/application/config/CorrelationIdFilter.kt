package dev.vilquer.petcarescheduler.application.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private const val CORRELATION_ID_HEADER = "X-Request-Id"
private const val MDC_KEY = "correlationId"

/**
 * Gera (ou reaproveita) um id por requisição, propagado na resposta e
 * injetado no MDC, para correlacionar todas as linhas de log de uma mesma
 * requisição sem precisar de tracing distribuído.
 */
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        MDC.put(MDC_KEY, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }
}

@Configuration
class CorrelationIdConfig {

    @Bean
    fun correlationIdFilter(): FilterRegistrationBean<CorrelationIdFilter> =
        FilterRegistrationBean(CorrelationIdFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            urlPatterns = listOf("/*")
        }
}
