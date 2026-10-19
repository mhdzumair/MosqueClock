package com.mosque.prayerclock.data.scraping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcjuTimetableRowsTest {
    @Test
    fun `Colombo October PDF retains Shafi Asr and reads both evening columns`() {
        // iText extraction from https://www.acju.lk/wp-content/uploads/2026/10/01.-Colombo-10.pdf
        val text = javaClass.getResource("/acju/colombo-october-2026.txt")!!.readText()
        val days = AcjuTimetableRows.parse(text, 2026)
        assertEquals((1..31).toList(), days.map { it.day })
        val today = days.single { it.day == 19 }
        assertEquals(DailyPrayerTime("2026-10-19", 19, "04:40", "05:57", "11:57", "15:17", "17:56", "19:05", "16:15"), today)
        assertEquals("12:02", days.first().dhuhr)
        assertEquals("19:02", days.last().isha)
        assertTrue(days.all { it.asr < it.maghrib && it.maghrib < it.isha })
    }

    @Test
    fun `six and seven columns work with either date order and optional periods`() {
        for (date in listOf("19-Oct", "Oct-19", "19-oct", "OCT-19")) {
            for (hanafi in listOf(false, true)) {
                for (periods in listOf(false, true)) {
                    val times = listOf("4:40 AM", "5:57 AM", "11:57 AM", "3:17 PM") +
                        (if (hanafi) listOf("4:15 PM") else emptyList()) + listOf("5:56 PM", "7:05 PM")
                    val row = times.joinToString("\n") { if (periods) it else it.substringBefore(' ') }
                    assertEquals(
                        DailyPrayerTime("2026-10-19", 19, "04:40", "05:57", "11:57", "15:17", "17:56", "19:05", if (hanafi) "16:15" else null),
                        AcjuTimetableRows.parse("$date\n$row", 2026).single(),
                    )
                }
            }
        }
    }

    @Test(expected = PdfParsingException::class)
    fun `incomplete row cannot borrow times from next date`() {
        AcjuTimetableRows.parse("19-Oct 4:40 AM 5:57 AM\n20-Oct 4:40 AM 5:57 AM 11:57 AM 3:17 PM 5:55 PM 7:05 PM", 2026)
    }

    @Test(expected = PdfParsingException::class)
    fun `extra time column cannot silently shift prayers`() {
        AcjuTimetableRows.parse("19-Oct 4:40 AM 5:57 AM 11:57 AM 3:17 PM 4:15 PM 5:56 PM 7:05 PM 8:00 PM", 2026)
    }
}
