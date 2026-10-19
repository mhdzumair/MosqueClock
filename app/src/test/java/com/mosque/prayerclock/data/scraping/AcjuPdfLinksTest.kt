package com.mosque.prayerclock.data.scraping

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assert.assertNull
import org.jsoup.Jsoup

class AcjuPdfLinksTest {
    private val prefix = "https://www.acju.lk/wp-content/uploads/2026/10/12.-Galle-District-Matara-District-Sri-lanka-Hanafi"

    @Test
    fun `month headings work with missing months reordered links and duplicate old downloads`() {
        val section = Jsoup.parse("""
            <details><div><strong>November</strong><a href="november.pdf">Download</a></div>
            <div><strong>October</strong><a href="october-hanafi.pdf">New</a><a href="october-old.pdf">Old</a></div>
            <div><strong>September</strong><a href="september.pdf">Download</a></div></details>
        """.trimIndent(), "https://www.acju.lk/").selectFirst("details")!!
        assertEquals("https://www.acju.lk/october-hanafi.pdf", AcjuPdfLinks.selectMonthPdf(section, 10))
        assertNull(AcjuPdfLinks.selectMonthPdf(section, 1))
    }

    @Test
    fun `month container corrects ACJU mislabeled September Hanafi download`() {
        val section = Jsoup.parse("""
            <details><div role="region"><p><strong>October</strong></p>
            <div><a href="${prefix}__09_009.pdf"><i>Download</i></a></div></div></details>
        """.trimIndent()).selectFirst("details")!!
        assertEquals("${prefix}__10_010.pdf", AcjuPdfLinks.selectMonthPdf(section, 10))
    }

    @Test
    fun `new Hanafi timetable takes priority over older PDF in the same month group`() {
        val section = Jsoup.parse("""
            <details><div><strong>October</strong>
            <a href="https://www.acju.lk/wp-content/uploads/2025/07/12-GALLE-10-Oct.pdf">Old</a>
            <a href="${prefix}__09_009.pdf">Updated Hanafi</a></div></details>
        """.trimIndent()).selectFirst("details")!!
        assertEquals("${prefix}__10_010.pdf", AcjuPdfLinks.selectMonthPdf(section, 10))
    }

    @Test
    fun `unlabelled links require filename month rather than list position`() {
        val section = Jsoup.parse("""
            <details><a href="https://www.acju.lk/wp-content/uploads/2025/07/12-GALLE-10-Oct.pdf"></a>
            <a href="${prefix}__11_011.pdf"></a>
            <a href="${prefix}__10_010.pdf"></a><a href="unknown.pdf"></a></details>
        """.trimIndent()).selectFirst("details")!!
        assertEquals("${prefix}__10_010.pdf", AcjuPdfLinks.selectMonthPdf(section, 10))
        assertNull(AcjuPdfLinks.selectMonthPdf(section, 1))
    }

    @Test
    fun `October link to September numbered file resolves to published October file`() {
        assertEquals("${prefix}__10_010.pdf", AcjuPdfLinks.forMonth("${prefix}__09_009.pdf", 10))
    }

    @Test
    fun `correct numbered links retain their original URL`() {
        for (month in 1..12) {
            val url = "${prefix}__${month.toString().padStart(2, '0')}_${month.toString().padStart(3, '0')}.pdf"
            assertEquals(url, AcjuPdfLinks.forMonth(url, month))
        }
    }

    @Test
    fun `legacy filenames and inconsistent numbering are not guessed`() {
        for (url in listOf("https://www.acju.lk/12-GALLE-9-Sep.pdf", "${prefix}__01_002.pdf")) {
            assertEquals(url, AcjuPdfLinks.forMonth(url, 10))
        }
    }
}
