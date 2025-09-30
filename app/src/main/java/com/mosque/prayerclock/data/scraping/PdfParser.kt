package com.mosque.prayerclock.data.scraping

import android.util.Log
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.mosque.prayerclock.data.model.PrayerTimes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfParser
    @Inject
    constructor() {
        companion object {
            private const val TAG = "PdfParser"
        }

        /**
         * Extract text from PDF bytes using iText
         */
        fun extractTextFromPdf(pdfBytes: ByteArray): String =
            try {
                Log.d(TAG, "📄 Starting PDF text extraction...")
                val pdfReader = PdfReader(ByteArrayInputStream(pdfBytes))
                val numberOfPages = pdfReader.numberOfPages
                Log.d(TAG, "📖 PDF has $numberOfPages pages")
                val extractedText = StringBuilder()

                for (i in 1..numberOfPages) {
                    Log.d(TAG, "📄 Extracting text from page $i/$numberOfPages")
                    val pageText = PdfTextExtractor.getTextFromPage(pdfReader, i).trim()
                    extractedText.append(pageText).append("\n")
                    Log.d(TAG, "📝 Page $i extracted ${pageText.length} characters")
                }

                pdfReader.close()
                val finalText = extractedText.toString()
                Log.d(TAG, "✅ PDF text extraction complete. Total: ${finalText.length} characters")
                finalText
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error extracting text from PDF", e)
                throw PdfParsingException("Failed to extract text from PDF: ${e.message}", e)
            }

        /**
         * Parse prayer times from extracted PDF text
         */
        fun parsePrayerTimes(text: String): PrayerTimesData {
            try {
                // Extract metadata
                val metadata = extractMetadata(text)

                // Extract prayer times data
                val prayerTimes = extractPrayerTimes(text, metadata.year)

                // Apartment adjustments (standard for all zones)
                val apartmentAdjustments =
                    ApartmentAdjustments(
                        description = "Prayer Time Differences for Apartments",
                        heightRange =
                            HeightRange(
                                stories = "06-35",
                                meters = "24-140",
                            ),
                        adjustments =
                            TimeAdjustments(
                                fajr = -1,
                                sunrise = -1,
                                maghrib = 1,
                                isha = 1,
                            ),
                    )

                return PrayerTimesData(
                    metadata = metadata,
                    apartmentAdjustments = apartmentAdjustments,
                    prayerTimes = prayerTimes,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing prayer times from text", e)
                throw PdfParsingException("Failed to parse prayer times: ${e.message}", e)
            }
        }

        private fun extractMetadata(text: String): PrayerTimesMetadata {
            // Extract zone
            val zoneRegex = Regex("""Zone:\s*(\d+)""")
            val zoneMatch = zoneRegex.find(text)
            val zone = zoneMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

            // Extract districts
            val districts = extractDistricts(text)

            // Extract month and year
            val (month, year) = extractMonthAndYear(text)

            return PrayerTimesMetadata(
                source = "All Ceylon Jamiyyathul Ulama (ACJU)",
                website = "www.acju.lk",
                email = "info@acju.lk",
                phone = "+94 117 490 490",
                country = "Sri Lanka",
                zone = zone,
                districts = districts,
                month = month,
                year = year,
            )
        }

        private fun extractDistricts(text: String): List<String> {
            val districtPatterns =
                listOf(
                    Regex("""(GALLE DISTRICT[^-]*-[^A-Z]*)""", RegexOption.IGNORE_CASE),
                    Regex("""(COLOMBO DISTRICT[^-]*-[^A-Z]*)""", RegexOption.IGNORE_CASE),
                    Regex("""(KANDY DISTRICT[^-]*-[^A-Z]*)""", RegexOption.IGNORE_CASE),
                    Regex("""(\w+ DISTRICT)""", RegexOption.IGNORE_CASE),
                )

            for (pattern in districtPatterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val districtsText = match.groupValues[1]
                    val foundDistricts =
                        districtsText
                            .replace("-", ",")
                            .split(",")
                            .map { it.trim().replaceFirstChar { char -> char.titlecase() } }
                            .filter { it.contains("district", ignoreCase = true) }

                    if (foundDistricts.isNotEmpty()) {
                        return foundDistricts
                    }
                }
            }

            return listOf("Unknown District")
        }

        private fun extractMonthAndYear(text: String): Pair<String, Int> {
            // Try to find month from actual date entries first
            val dateRegex = Regex("""(\d{1,2})-(\w{3})""")
            val dateMatches = dateRegex.findAll(text).toList()

            val month =
                if (dateMatches.isNotEmpty()) {
                    val monthAbbr = dateMatches.first().groupValues[2]
                    val monthMapping =
                        mapOf(
                            "Jan" to "January",
                            "Feb" to "February",
                            "Mar" to "March",
                            "Apr" to "April",
                            "May" to "May",
                            "Jun" to "June",
                            "Jul" to "July",
                            "Aug" to "August",
                            "Sep" to "September",
                            "Oct" to "October",
                            "Nov" to "November",
                            "Dec" to "December",
                        )
                    monthMapping[monthAbbr] ?: monthAbbr
                } else {
                    // Fallback: Look for full month names
                    val monthRegex =
                        Regex(
                            """(?<!ACJU)(?<!News)\b(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)\b""",
                            RegexOption.IGNORE_CASE,
                        )
                    val monthMatch = monthRegex.find(text)
                    monthMatch?.groupValues?.get(1)?.replaceFirstChar { it.titlecase() } ?: "Unknown"
                }

            // Extract year
            val yearRegex = Regex("""20\d{2}""")
            val yearMatch = yearRegex.find(text)
            val year =
                yearMatch?.value?.toIntOrNull()
                    ?: Clock.System
                        .now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .year

            return Pair(month, year)
        }

        private fun extractPrayerTimes(
            text: String,
            year: Int,
        ): List<DailyPrayerTime> {
            val prayerTimes = mutableListOf<DailyPrayerTime>()

            // Pattern to match prayer time rows
            val timePattern =
                Regex(
                    """(\d{1,2})-(\w{3})\s+(\d{1,2}:\d{2})\s*AM\s+(\d{1,2}:\d{2})\s*AM\s+(\d{1,2}:\d{2})\s*PM\s+(\d{1,2}:\d{2})\s*PM\s+(\d{1,2}:\d{2})\s*PM\s+(\d{1,2}:\d{2})\s*PM""",
                )

            val matches = timePattern.findAll(text)

            for (match in matches) {
                val (dayNum, monthAbbr, fajr, sunrise, dhuhr, asr, maghrib, isha) = match.destructured

                // Convert times to 24-hour format
                val fajr24 = convertTo24Hour(fajr, "AM")
                val sunrise24 = convertTo24Hour(sunrise, "AM")
                val dhuhr24 = convertTo24Hour(dhuhr, "PM")
                val asr24 = convertTo24Hour(asr, "PM")
                val maghrib24 = convertTo24Hour(maghrib, "PM")
                val isha24 = convertTo24Hour(isha, "PM")

                // Create date string
                val monthNum =
                    mapOf(
                        "Jan" to "01",
                        "Feb" to "02",
                        "Mar" to "03",
                        "Apr" to "04",
                        "May" to "05",
                        "Jun" to "06",
                        "Jul" to "07",
                        "Aug" to "08",
                        "Sep" to "09",
                        "Oct" to "10",
                        "Nov" to "11",
                        "Dec" to "12",
                    )[monthAbbr] ?: "01"

                val dateStr = "$year-$monthNum-${dayNum.padStart(2, '0')}"

                prayerTimes.add(
                    DailyPrayerTime(
                        date = dateStr,
                        day = dayNum.toInt(),
                        fajr = fajr24,
                        sunrise = sunrise24,
                        dhuhr = dhuhr24,
                        asr = asr24,
                        maghrib = maghrib24,
                        isha = isha24,
                    ),
                )
            }

            return prayerTimes
        }

        private fun convertTo24Hour(
            time: String,
            period: String,
        ): String =
            when {
                period == "PM" && !time.startsWith("12") -> {
                    val (hour, minute) = time.split(":")
                    val hour24 = (hour.toInt() + 12).toString().padStart(2, '0')
                    "$hour24:$minute"
                }
                period == "AM" && time.startsWith("12") -> {
                    "00:${time.split(":")[1]}"
                }
                else -> {
                    val parts = time.split(":")
                    "${parts[0].padStart(2, '0')}:${parts[1]}"
                }
            }

        /**
         * Convert parsed prayer times data to PrayerTimes entities for database storage
         */
        fun convertToPrayerTimesEntities(
            prayerTimesData: PrayerTimesData,
            zone: Int,
        ): List<PrayerTimes> {
            val providerKey = "ACJU_DIRECT:$zone"

            // Note: Iqamah times are no longer stored in the database
            // They will be calculated dynamically from user settings
            return prayerTimesData.prayerTimes.map { dailyTime ->
                PrayerTimes(
                    id = "${dailyTime.date}_$providerKey",
                    date = dailyTime.date,
                    providerKey = providerKey,
                    fajrAzan = dailyTime.fajr,
                    sunrise = dailyTime.sunrise,
                    dhuhrAzan = dailyTime.dhuhr,
                    asrAzan = dailyTime.asr,
                    maghribAzan = dailyTime.maghrib,
                    ishaAzan = dailyTime.isha,
                    hijriDate = null, // Will be populated separately if needed
                    location = "Zone $zone (${prayerTimesData.metadata.districts.joinToString(", ")})",
                )
            }
        }

        /**
         * Add minutes to a time string (HH:MM format)
         */
        private fun addMinutesToTime(
            timeStr: String,
            minutesToAdd: Int,
        ): String =
            try {
                val (hour, minute) = timeStr.split(":").map { it.toInt() }
                val totalMinutes = hour * 60 + minute + minutesToAdd
                val newHour = (totalMinutes / 60) % 24
                val newMinute = totalMinutes % 60
                "${newHour.toString().padStart(2, '0')}:${newMinute.toString().padStart(2, '0')}"
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add minutes to time $timeStr: ${e.message}")
                timeStr // Return original time if parsing fails
            }
    }

// Data classes for parsed PDF data
data class PrayerTimesData(
    val metadata: PrayerTimesMetadata,
    val apartmentAdjustments: ApartmentAdjustments,
    val prayerTimes: List<DailyPrayerTime>,
)

data class PrayerTimesMetadata(
    val source: String,
    val website: String,
    val email: String,
    val phone: String,
    val country: String,
    val zone: Int,
    val districts: List<String>,
    val month: String,
    val year: Int,
)

data class ApartmentAdjustments(
    val description: String,
    val heightRange: HeightRange,
    val adjustments: TimeAdjustments,
)

data class HeightRange(
    val stories: String,
    val meters: String,
)

data class TimeAdjustments(
    val fajr: Int,
    val sunrise: Int,
    val maghrib: Int,
    val isha: Int,
)

data class DailyPrayerTime(
    val date: String,
    val day: Int,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
)

class PdfParsingException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
