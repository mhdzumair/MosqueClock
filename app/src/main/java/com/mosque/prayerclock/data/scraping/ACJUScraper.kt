package com.mosque.prayerclock.data.scraping

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ACJUScraper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val TAG = "ACJUScraper"
        private const val ACJU_BASE_URL = "https://www.acju.lk/prayer-times/"
        private const val TIMEOUT_SECONDS = 30L

        // Zone mappings based on ACJU website structure
        private val ZONE_MAPPINGS = mapOf(
            1 to "COLOMBO DISTRICT, GAMPAHA DISTRICT, KALUTARA DISTRICT",
            2 to "JAFFNA DISTRICT, NALLUR",
            3 to "MULLAITIVU DISTRICT (EXCEPT NALLUR), KILINOCHCHI DISTRICT, VAVUNIYA DISTRICT",
            4 to "MANNAR DISTRICT, PUTTALAM DISTRICT",
            5 to "ANURADHAPURA DISTRICT, POLONNARUWA DISTRICT",
            6 to "KURUNEGALA DISTRICT",
            7 to "KANDY DISTRICT, MATALE DISTRICT, NUWARA ELIYA DISTRICT",
            8 to "BATTICALOA DISTRICT, AMPARA DISTRICT",
            9 to "TRINCOMALEE DISTRICT",
            10 to "BADULLA DISTRICT, MONARAGALA DISTRICT, PADIYATALAWA ,DEHIATHTHAKANDIYA",
            11 to "RATNAPURA DISTRICT, KEGALLE DISTRICT",
            12 to "GALLE DISTRICT, MATARA DISTRICT",
            13 to "HAMBANTOTA DISTRICT"
        )

        private val MONTH_MAPPINGS = mapOf(
            1 to "January", 2 to "February", 3 to "March", 4 to "April",
            5 to "May", 6 to "June", 7 to "July", 8 to "August",
            9 to "September", 10 to "October", 11 to "November", 12 to "December"
        )
    }

    /**
     * Get PDF URL for prayer times from ACJU website
     */
    suspend fun getPrayerTimesPdfUrl(zone: Int, year: Int, month: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Scraping ACJU website for zone $zone, month $month, year $year")
                
                val request = Request.Builder()
                    .url(ACJU_BASE_URL)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch ACJU website: ${response.code}")
                    return@withContext null
                }

                val html = response.body?.string()
                if (html == null) {
                    Log.e(TAG, "Empty response from ACJU website")
                    return@withContext null
                }

                val document = Jsoup.parse(html)
                
                // Find the zone section
                val zoneText = ZONE_MAPPINGS[zone]
                if (zoneText == null) {
                    Log.e(TAG, "Invalid zone: $zone")
                    return@withContext null
                }

                Log.d(TAG, "Looking for zone text: $zoneText")

                // Find the accordion section for this zone
                // Look for the specific text within the accordion structure
                val zoneElements = document.select("div.e-n-accordion-item-title-text:contains($zoneText)")
                Log.d(TAG, "Found ${zoneElements.size} zone elements matching text")
                val zoneElement = zoneElements.firstOrNull()
                if (zoneElement == null) {
                    Log.e(TAG, "Zone element not found for: $zoneText")
                    return@withContext null
                }

                Log.d(TAG, "Found zone element: ${zoneElement.tagName()}")

                // Navigate up to find the details parent
                var detailsParent = zoneElement.parent()
                while (detailsParent != null && detailsParent.tagName() != "details") {
                    detailsParent = detailsParent.parent()
                }

                if (detailsParent == null) {
                    Log.e(TAG, "Could not find details parent element")
                    return@withContext null
                }

                Log.d(TAG, "Found details parent, looking for PDF links...")

                // Find all PDF links within this details section
                val pdfLinks = detailsParent.select("a[href$=.pdf]")
                Log.d(TAG, "Found ${pdfLinks.size} PDF links")

                if (pdfLinks.size < month) {
                    Log.e(TAG, "Not enough PDF links found. Expected at least $month, found ${pdfLinks.size}")
                    return@withContext null
                }

                // Get the PDF for the specified month (month-1 because list is 0-indexed)
                val pdfLink = pdfLinks[month - 1]
                val pdfUrl = pdfLink.attr("abs:href")
                Log.d(TAG, "Found PDF URL for month $month: $pdfUrl")
                return@withContext pdfUrl

            } catch (e: Exception) {
                Log.e(TAG, "Error scraping ACJU website", e)
                return@withContext null
            }
        }
    }

    /**
     * Download PDF from URL
     */
    suspend fun downloadPdf(url: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading PDF from: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download PDF: ${response.code}")
                    return@withContext null
                }

                val pdfBytes = response.body?.bytes()
                if (pdfBytes == null) {
                    Log.e(TAG, "Empty PDF response")
                    return@withContext null
                }

                Log.d(TAG, "Successfully downloaded PDF: ${pdfBytes.size} bytes")
                return@withContext pdfBytes

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading PDF", e)
                return@withContext null
            }
        }
    }

    /**
     * Scrape and parse prayer times for a specific zone and month
     */
    suspend fun scrapePrayerTimes(
        zone: Int,
        year: Int,
        month: Int,
        pdfParser: PdfParser
    ): PrayerTimesData? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "🌐 Starting ACJU website scraping - Zone: $zone, Year: $year, Month: $month")
            try {
                // Get PDF URL from website
                Log.d(TAG, "🔗 Searching for PDF URL on ACJU website...")
                val pdfUrl = getPrayerTimesPdfUrl(zone, year, month)
                if (pdfUrl == null) {
                    val monthName = MONTH_MAPPINGS[month] ?: "Unknown"
                    Log.e(TAG, "❌ No prayer times PDF found for zone $zone, $monthName $year")
                    throw ScrapingException("No prayer times PDF found for zone $zone, $monthName $year")
                }
                Log.d(TAG, "✅ Found PDF URL: $pdfUrl")

                // Download PDF
                Log.d(TAG, "⬇️ Downloading PDF from ACJU...")
                val pdfBytes = downloadPdf(pdfUrl)
                if (pdfBytes == null) {
                    Log.e(TAG, "❌ Failed to download PDF from $pdfUrl")
                    throw ScrapingException("Failed to download PDF from $pdfUrl")
                }
                Log.d(TAG, "✅ PDF downloaded successfully (${pdfBytes.size} bytes)")

                // Parse PDF
                Log.d(TAG, "📄 Extracting text from PDF...")
                val text = pdfParser.extractTextFromPdf(pdfBytes)
                Log.d(TAG, "📝 Extracted ${text.length} characters from PDF")
                
                Log.d(TAG, "🔍 Parsing prayer times from extracted text...")
                val parsedData = pdfParser.parsePrayerTimes(text)

                Log.d(TAG, "✅ Successfully scraped and parsed prayer times for zone $zone, month $month")
                Log.d(TAG, "📊 Parsed ${parsedData.prayerTimes.size} daily prayer time entries")
                return@withContext parsedData

            } catch (e: Exception) {
                Log.e(TAG, "Error scraping prayer times", e)
                throw ScrapingException("Failed to scrape prayer times: ${e.message}", e)
            }
        }
    }

    /**
     * Get zone mapping for display purposes
     */
    fun getZoneDescription(zone: Int): String? {
        return ZONE_MAPPINGS[zone]
    }

    /**
     * Get month name for display purposes
     */
    fun getMonthName(month: Int): String? {
        return MONTH_MAPPINGS[month]
    }

    /**
     * Get all available zones
     */
    fun getAvailableZones(): List<Int> {
        return ZONE_MAPPINGS.keys.sorted()
    }
}

class ScrapingException(message: String, cause: Throwable? = null) : Exception(message, cause)
