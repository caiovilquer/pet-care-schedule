package dev.vilquer.petcarescheduler.core.domain.valueobject

@JvmInline
value class Email(val value: String) {

    companion object {
        /** Regex RFC 5322 simplificada */
        private val REGEX =
            Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

        /** Devolve Result para evitar lançar exceção em regra de domínio. */
        fun of(raw: String): Result<Email> =
            if (REGEX.matches(raw.trim()))
                Result.success(Email(raw.trim().lowercase()))
            else
                Result.failure(IllegalArgumentException("Invalid email: $raw"))
    }

    override fun toString() = value
}

