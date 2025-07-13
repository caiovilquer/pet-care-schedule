package dev.vilquer.petcarescheduler.application.config

import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import dev.vilquer.petcarescheduler.core.domain.valueobject.Email
import dev.vilquer.petcarescheduler.core.domain.valueobject.PhoneNumber
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class JacksonVoModule {

    @Bean
    open fun voModule(): Module = SimpleModule().apply {

        /* ---- Email ---- */
        addSerializer(
            Email::class.java,
            object : JsonSerializer<Email>() {
                override fun serialize(
                    value: Email,
                    gen: JsonGenerator,
                    serializers: SerializerProvider
                ) {
                    gen.writeString(value.value)
                }
            }
        )

        addDeserializer(
            Email::class.java,
            object : JsonDeserializer<Email>() {
                override fun deserialize(
                    p: JsonParser,
                    ctxt: DeserializationContext
                ): Email =
                    Email.of(p.text).getOrThrow()
            }
        )

        /* ---- PhoneNumber ---- */
        addSerializer(PhoneNumber::class.java, ToStringSerializer.instance)

        addDeserializer(
            PhoneNumber::class.java,
            object : JsonDeserializer<PhoneNumber>() {
                override fun deserialize(
                    p: JsonParser,
                    ctxt: DeserializationContext
                ): PhoneNumber =
                    PhoneNumber.of(p.text).getOrThrow()
            }
        )
    }
}
