package dev.vilquer.petcarescheduler.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
open class SecurityBeans {
    /** strength = 10 (default)*/
    @Bean
    open fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
