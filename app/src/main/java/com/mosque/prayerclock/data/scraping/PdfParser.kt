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
                val pdfReader = PdfReader(ByteArrayInputStream(pdfBytes))
                val numberOfPages = pdfReader.numberOfPages
                val extractedText = StringBuilder()

                for (i in 1..numberOfPages) {
                    val pageText = PdfTextExtractor.getTextFromPage(pdfReader, i).trim()
                    extractedText.append(pageText).append("\n")
                    Log.d(TAG, "📄 Page $i text (first 300 chars): ${pageText.take(300)}")
                }

                pdfReader.close()
                val finalText = extractedText.toString()
                Log.d(TAG, "✅ Extracted ${finalText.length} characters from $numberOfPages page(s)")
                
                // Log more text for debugging if it's a reasonable size
                if (finalText.length > 0 && finalText.length < 5000) {
                    Log.d(TAG, "📋 Full extracted text:\n$finalText")
                } else if (finalText.length >= 5000) {
                    Log.d(TAG, "📋 First 1000 chars:\n${finalText.take(1000)}")
                    Log.d(TAG, "📋 Last 500 chars:\n${finalText.takeLast(500)}")
                }
                
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
                Log.d(TAG, "📋 Starting to parse prayer times from text (${text.length} chars)")
                Log.d(TAG, "📋 First 500 chars of text: ${text.take(500)}")
                
                // Extract metadata
                val metadata = extractMetadata(text)
                Log.d(TAG, "📋 Extracted metadata: Zone=${metadata.zone}, Month=${metadata.month}, Year=${metadata.year}")

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
            val monthMapping =
                mapOf(
                    "Jan" to "January", "Feb" to "February", "Mar" to "March", "Apr" to "April",
                    "May" to "May", "Jun" to "June", "Jul" to "July", "Aug" to "August",
                    "Sep" to "September", "Oct" to "October", "Nov" to "November", "Dec" to "December",
                )

            // Support both "1-Apr" (day-month) and "Apr-01" (month-day) formats
            val dayMonthMatches = Regex("""(\d{1,2})-([A-Za-z]{3})""").findAll(text).toList()
            val monthDayMatches = Regex("""([A-Za-z]{3})-(\d{1,2})""").findAll(text).toList()

            val monthAbbr =
                when {
                    dayMonthMatches.isNotEmpty() -> dayMonthMatches.first().groupValues[2]
                    monthDayMatches.isNotEmpty() -> monthDayMatches.first().groupValues[1]
                    else -> null
                }

            val month =
                if (monthAbbr != null) {
                    monthMapping[monthAbbr.replaceFirstChar { it.titlecase() }] ?: monthAbbr
                } else {
                    val monthRegex =
                        Regex(
                            """(?<!ACJU)(?<!News)\b(JANUARY|FEBRUARY|MARCH|APRIL|MAY|JUNE|JULY|AUGUST|SEPTEMBER|OCTOBER|NOVEMBER|DECEMBER)\b""",
                            RegexOption.IGNORE_CASE,
                        )
                    monthRegex.find(text)?.groupValues?.get(1)?.replaceFirstChar { it.titlecase() } ?: "Unknown"
                }

            val yearRegex = Regex("""20\d{2}""")
            val year =
                yearRegex.find(text)?.value?.toIntOrNull()
                    ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

            return Pair(month, year)
        }

        private fun extractPrayerTimes(
            text: String,
            year: Int,
        ): List<DailyPrayerTime> {
            return AcjuTimetableRows.parse(text, year)
        }

        /**
         * Convert parsed prayer times data to PrayerTimes entities for database storage
         * 
         * Note: Prayer times are year-agnostic (cyclical based on solar calendar).
         * We store them with the full date for display, but use MM-DD for lookup to enable
         * multi-year caching. This means prayer times scraped in 2025 will work for 2026+.
         */
        fun convertToPrayerTimesEntities(
            prayerTimesData: PrayerTimesData,
            zone: Int,
        ): List<PrayerTimes> {
            val providerKey = "ACJU_DIRECT:$zone"

            // Note: Iqamah times are no longer stored in the database
            // They will be calculated dynamically from user settings
            return prayerTimesData.prayerTimes.map { dailyTime ->
                // Extract month-day for year-agnostic storage
                // Format: YYYY-MM-DD -> MM-DD
                val monthDay = dailyTime.date.substring(5) // Gets "MM-DD" from "YYYY-MM-DD"
                
                PrayerTimes(
                    // Use MM-DD based ID for year-agnostic storage
                    id = "${monthDay}_$providerKey",
                    // Store full date for display purposes
                    date = dailyTime.date,
                    providerKey = providerKey,
                    fajrAzan = dailyTime.fajr,
                    sunrise = dailyTime.sunrise,
                    dhuhrAzan = dailyTime.dhuhr,
                    asrAzan = dailyTime.asr,
                    asrShafiAzan = dailyTime.asr,
                    asrHanafiAzan = dailyTime.asrHanafi,
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
    val asrHanafi: String? = null,
)

class PdfParsingException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
