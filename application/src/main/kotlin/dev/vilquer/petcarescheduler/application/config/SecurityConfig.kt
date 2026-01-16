package dev.vilquer.petcarescheduler.application.config

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import dev.vilquer.petcarescheduler.application.security.JwtCacheProperties
import dev.vilquer.petcarescheduler.application.security.PasswordChangedAtCache
import dev.vilquer.petcarescheduler.core.domain.entity.TutorId
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TutorRepositoryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity, jwtDecoder: JwtDecoder): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/api/v1/public/**",
                        "/api/v1/auth/**",
                        "/api/v1/auth/password/forgot",
                        "/api/v1/auth/password/reset",
                        "/api/v1/auth/password/reset/validate"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt ->
                    jwt.decoder(jwtDecoder)
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        return http.build()
    }

    @Bean
    fun jwtDecoder(
        jwtParser: JwtParser,
        tutorRepo: TutorRepositoryPort,
        passwordChangedAtCache: PasswordChangedAtCache,
        jwtCacheProperties: JwtCacheProperties
    ): JwtDecoder {
        return JwtDecoder { token ->
            try {
                val jws = jwtParser.parseSignedClaims(token)
                val headers = jws.header
                val claims = jws.payload
                val subject = claims.subject?.toLongOrNull()
                    ?: throw BadJwtException("JWT sem subject valido")

                val issuedAt = claims.issuedAt?.toInstant()
                    ?: throw BadJwtException("JWT sem iat")
                val passwordChangedAt = passwordChangedAtCache.getOrLoad(subject) {
                    tutorRepo.findById(TutorId(subject))?.passwordChangedAt
                        ?: throw BadJwtException("Tutor nao encontrado")
                }
                val skew = jwtCacheProperties.invalidationSkew
                if (passwordChangedAt != null && issuedAt.isBefore(passwordChangedAt.minus(skew))) {
                    throw BadJwtException("JWT expirado por troca de senha")
                }

                Jwt(
                    token,
                    issuedAt,
                    claims.expiration?.toInstant(),
                    headers,
                    claims
                )
            } catch (e: JwtException) {
                throw BadJwtException("JWT inválido: ${e.message}", e)
            }
        }
    }
}
