package com.mosque.prayerclock.data.scraping

/** Parses six- and seven-column ACJU rows, retaining both published Asr times. */
internal object AcjuTimetableRows {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val datePattern = Regex("""\b(?:(\d{1,2})-([A-Za-z]{3})|([A-Za-z]{3})-(\d{1,2}))\b""")
    private val timePattern = Regex("""(?<![\d:])(\d{1,2}):([0-5]\d)\s*(AM|PM)?\b""", RegexOption.IGNORE_CASE)

    fun parse(text: String, year: Int, expectedMonth: Int? = null): List<DailyPrayerTime> {
        val dates = datePattern.findAll(text).toList()
        return dates.mapIndexedNotNull { index, date ->
            val day = (date.groupValues[1].ifEmpty { date.groupValues[4] }).toInt()
            val month = date.groupValues[2].ifEmpty { date.groupValues[3] }
            val monthNumber = months.indexOfFirst { it.equals(month, ignoreCase = true) } + 1
            if (monthNumber == 0) return@mapIndexedNotNull null
            if (expectedMonth != null && monthNumber != expectedMonth) {
                throw PdfParsingException("Requested month $expectedMonth, but ACJU timetable contains month $monthNumber")
            }

            // Stop at the next date so an incomplete row cannot consume tomorrow's times.
            val row = text.substring(date.range.last + 1, dates.getOrNull(index + 1)?.range?.first ?: text.length)
            val times = timePattern.findAll(row).toList()
            if (times.size !in 6..7) {
                throw PdfParsingException("Expected 6 or 7 time columns for ${date.value}, found ${times.size}")
            }
            val converted = times.mapIndexed { column, time ->
                val hour = time.groupValues[1].toInt()
                if (hour !in 1..12) throw PdfParsingException("Invalid hour for ${date.value}: $hour")
                val period = time.groupValues[3].ifEmpty {
                    if (column < 2 || (column == 2 && hour == 11)) "AM" else "PM"
                }
                val hour24 = hour % 12 + if (period.equals("PM", ignoreCase = true)) 12 else 0
                "${hour24.toString().padStart(2, '0')}:${time.groupValues[2]}"
            }
            DailyPrayerTime(
                date = "$year-${monthNumber.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}",
                day = day,
                fajr = converted[0],
                sunrise = converted[1],
                dhuhr = converted[2],
                asr = converted[3],
                asrHanafi = if (converted.size == 7) converted[4] else null,
                // The final two columns are always Maghrib and Isha.
                maghrib = converted[converted.size - 2],
                isha = converted.last(),
            )
        }
    }
}
