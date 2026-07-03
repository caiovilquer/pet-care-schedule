package dev.vilquer.petcarescheduler.application.adapter.input.security

import dev.vilquer.petcarescheduler.core.domain.entity.Tutor
import dev.vilquer.petcarescheduler.usecase.contract.drivenports.TokenIssuerPort
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenIssuerAdapter(
    private val signingKey: SecretKey,
    private val props: JwtProperties
) : TokenIssuerPort {

    override fun issueAccessToken(tutor: Tutor): String {
        val now = Instant.now()
        val expirationTime = now.plus(props.expiration)

        return Jwts.builder()
            .issuer(props.issuer)
            .signWith(signingKey)
            .subject(tutor.id!!.value.toString())
            .claim("name", tutor.firstName)
            .claim("role", "TUTOR")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expirationTime))
            .compact()
    }
}
