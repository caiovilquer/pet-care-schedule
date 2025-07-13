package dev.vilquer.petcarescheduler.core.domain.valueobject

import kotlin.Result

@JvmInline
value class PhoneNumber private constructor(val e164: String) {

    companion object {
        /**
         * Regex para validar números de telefone.
         * - `^`: Início da string.
         * - `(\+)?`: Um ou nenhum caractere '+' (para o código do país).
         * - `[\d\s().-]{8,17}`: De 8 a 17 caracteres, que podem ser dígitos, espaços, parênteses ou hífens.
         * - Mínimo de 8 para números locais curtos e máximo de 17 para formatos internacionais completos.
         * - `$`: Fim da string.
         */
        private val PHONE_NUMBER_REGEX = Regex("""^(\+)?[\d\s().-]{8,17}$""")

        fun of(raw: String): Result<PhoneNumber> {
            val cleaned = raw.trim()

            if (!PHONE_NUMBER_REGEX.matches(cleaned)) {
                return Result.failure(IllegalArgumentException("Invalid phone number format: $raw"))
            }

            // Normaliza o número removendo tudo exceto dígitos e o '+' inicial
            val normalized = normalize(cleaned)


            return if (normalized.length >= 10) { // Validação de comprimento mínimo (ex: 2 dígitos DDD + 8 dígitos número)
                Result.success(PhoneNumber(normalized))
            } else {
                Result.failure(IllegalArgumentException("Invalid phone number length: $raw"))
            }
        }
        private fun normalize(raw: String): String {
            // Remove caracteres de formatação
            val numericString = raw.filter { it.isDigit() }

            // Se o número original já tiver o DDI, use-o
            if (raw.startsWith("+")) {
                return "+$numericString"
            }

            // Se for um número brasileiro (com DDD), adiciona o DDI do Brasil
            if (numericString.length == 10 || numericString.length == 11) {
                return "+55$numericString"
            }

            return numericString
        }
    }
    override fun toString() = e164
}

