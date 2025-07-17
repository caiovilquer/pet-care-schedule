package dev.vilquer.petcarescheduler.application.config

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
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
open class SecurityConfig {

    @Bean
    open fun filterChain(http: HttpSecurity, jwtDecoder: JwtDecoder): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/v1/public/**",
                        "/h2-console/**",
                        "/api/v1/auth/**"
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
    open fun jwtDecoder(jwtParser: JwtParser): JwtDecoder {
        return JwtDecoder { token ->
            try {
                val jws = jwtParser.parseSignedClaims(token)
                val headers = jws.header
                val claims = jws.payload

                Jwt(
                    token,
                    claims.issuedAt?.toInstant(),
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
