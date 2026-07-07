package dev.vilquer.petcarescheduler.infra.adapter.output.places

import dev.vilquer.petcarescheduler.usecase.result.DaySchedule
import dev.vilquer.petcarescheduler.usecase.result.OpeningHoursWeek

/**
 * Traduz o `weekday_text` do Google (texto livre, ex.: "segunda-feira: 09:00
 * – 18:00") para a estrutura semanal do domínio. Isolado no adapter porque é
 * puramente uma peculiaridade do formato do Google — nada disso é lógica de
 * negócio da aplicação.
 *
 * O Google documenta `weekday_text` como sempre começando na **segunda-feira**
 * (índice 0), independente do locale.
 */
internal object GoogleOpeningHoursParser {

    private val CLOSED = DaySchedule(isOpen = false)

    fun parse(weekdayText: List<String>): OpeningHoursWeek {
        if (weekdayText.size < 7) return emptyWeek()
        return OpeningHoursWeek(
            monday = parseDay(weekdayText[0]),
            tuesday = parseDay(weekdayText[1]),
            wednesday = parseDay(weekdayText[2]),
            thursday = parseDay(weekdayText[3]),
            friday = parseDay(weekdayText[4]),
            saturday = parseDay(weekdayText[5]),
            sunday = parseDay(weekdayText[6])
        )
    }

    fun emptyWeek(): OpeningHoursWeek =
        OpeningHoursWeek(CLOSED, CLOSED, CLOSED, CLOSED, CLOSED, CLOSED, CLOSED)

    private val WITH_PERIOD =
        Regex("(\\d{1,2}):(\\d{2})\\s?(AM|PM)?\\s?[–-]\\s?(\\d{1,2}):(\\d{2})\\s?(AM|PM)?", RegexOption.IGNORE_CASE)
    private val WITHOUT_PERIOD =
        Regex("(\\d{1,2}):(\\d{2})\\s?[–-]\\s?(\\d{1,2}):(\\d{2})")

    private fun parseDay(dayText: String): DaySchedule {
        val parts = dayText.split(": ", limit = 2)
        val timeText = if (parts.size > 1) parts[1] else dayText
        val lower = timeText.lowercase()

        if (timeText.isBlank() || lower.contains("closed") || lower.contains("fechado") || lower.contains("unavailable")) {
            return DaySchedule(isOpen = false)
        }
        if (lower.contains("24 hours") || lower.contains("24 horas") || lower.contains("open 24 hours")) {
            return DaySchedule(isOpen = true, openTime = "00:00", closeTime = "23:59")
        }

        WITH_PERIOD.find(timeText)?.let { match ->
            val g = match.groupValues
            var openHour = g[1].toInt()
            val openMin = g[2].toInt()
            val openPeriod = g[3]
            var closeHour = g[4].toInt()
            val closeMin = g[5].toInt()
            val closePeriod = g[6]

            if (openPeriod.equals("pm", true) && openHour != 12) openHour += 12
            if (openPeriod.equals("am", true) && openHour == 12) openHour = 0
            if (closePeriod.equals("pm", true) && closeHour != 12) closeHour += 12
            if (closePeriod.equals("am", true) && closeHour == 12) closeHour = 0

            return DaySchedule(
                isOpen = true,
                openTime = "%02d:%02d".format(openHour, openMin),
                closeTime = "%02d:%02d".format(closeHour, closeMin)
            )
        }

        WITHOUT_PERIOD.find(timeText)?.let { match ->
            val g = match.groupValues
            return DaySchedule(
                isOpen = true,
                openTime = "%02d:%02d".format(g[1].toInt(), g[2].toInt()),
                closeTime = "%02d:%02d".format(g[3].toInt(), g[4].toInt())
            )
        }

        // Texto presente e não identificado como fechado, mas em formato não
        // reconhecido — assume aberto em horário comercial padrão.
        return DaySchedule(isOpen = true, openTime = "09:00", closeTime = "18:00")
    }
}
