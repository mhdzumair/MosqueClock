package com.mosque.prayerclock.data.scraping

import org.jsoup.nodes.Element

/** ACJU's numbered filenames encode the month twice, independently of the page's link label. */
internal object AcjuPdfLinks {
    private val numberedMonth = Regex("""__(\d{2})_(\d{3})\.pdf$""", RegexOption.IGNORE_CASE)
    private val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    fun selectMonthPdf(zoneSection: Element, month: Int): String? {
        require(month in 1..12)
        val name = monthNames[month - 1]
        // Match the month heading, then its enclosing download container. Do not assume
        // twelve links in calendar order: old and Hanafi PDFs can coexist on the page.
        for (heading in zoneSection.getAllElements().toList().filter { it.ownText().trim().equals(name, true) }) {
            var container: Element? = heading.parent()
            while (container != null && container != zoneSection) {
                // Crossing into another month's group would make this association ambiguous.
                val otherMonths = container.getAllElements().any { element ->
                    monthNames.any { it != name && element.ownText().trim().equals(it, true) }
                }
                if (otherMonths) break
                val links = container.select("a[href]").map { it.absUrl("href").ifEmpty { it.attr("href") } }
                    .filter { it.endsWith(".pdf", ignoreCase = true) }
                    .map { forMonth(it, month) }.distinct()
                    .sortedWith(pdfPriority())
                if (links.isNotEmpty()) {
                    return links.firstOrNull { fileMonth(it).let { identified -> identified == null || identified == month } }
                }
                container = container.parent()
            }
        }
        // Older pages can omit month headings; only use an explicit filename month.
        return zoneSection.select("a[href]").map { it.absUrl("href").ifEmpty { it.attr("href") } }
            .sortedWith(pdfPriority())
            .firstOrNull { fileMonth(it) == month }
    }

    private fun pdfPriority() = compareByDescending<String> { uploadDate(it) }
        .thenByDescending { it.contains("hanafi", ignoreCase = true) }
        .thenByDescending { numberedMonth.containsMatchIn(it) }

    private fun uploadDate(url: String): Int {
        val parts = Regex("""/uploads/(\d{4})/(\d{2})/""").find(url)?.groupValues ?: return 0
        return parts[1].toInt() * 100 + parts[2].toInt()
    }

    private fun fileMonth(url: String): Int? {
        val numbered = numberedMonth.find(url)
        if (numbered != null) {
            val month = numbered.groupValues[1].toInt()
            return month.takeIf { it in 1..12 && it == numbered.groupValues[2].toInt() }
        }
        val filename = url.substringAfterLast('/')
        val named = Regex("""-(\d{1,2})-([A-Za-z]{3})\.pdf$""", RegexOption.IGNORE_CASE).find(filename)
        if (named != null) {
            val month = monthNames.indexOfFirst { it.startsWith(named.groupValues[2], true) } + 1
            return month.takeIf { it == named.groupValues[1].toInt() }
        }
        return Regex("""-(\d{1,2})\.pdf$""").find(filename)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..12 }
    }

    fun forMonth(url: String, month: Int): String {
        require(month in 1..12)
        val match = numberedMonth.find(url) ?: return url
        val first = match.groupValues[1].toInt()
        val second = match.groupValues[2].toInt()
        if (first != second || first !in 1..12 || first == month) return url
        // Some October links point to __09_009.pdf although __10_010.pdf is published.
        // The downloaded timetable must still pass the requested-month validation.
        return url.replaceRange(
            match.range,
            "__${month.toString().padStart(2, '0')}_${month.toString().padStart(3, '0')}.pdf",
        )
    }
}
