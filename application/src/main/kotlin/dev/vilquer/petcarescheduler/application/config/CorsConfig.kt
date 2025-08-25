package dev.vilquer.petcarescheduler.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = System.getenv("ALLOWED_ORIGINS")
            ?.split(",")
            ?.map(String::trim)
            ?.toTypedArray()
            ?: arrayOf("http://localhost:4200")
        registry.addMapping("/api/**")
            .allowedOrigins("https://petcare.vilquer.dev")
            .allowedMethods("*")
    }
}