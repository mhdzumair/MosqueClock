package com.mosque.prayerclock.data.scraping

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HijriDateScraper @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "HijriDateScraper"
        private const val BASE_URL = "https://www.acju.lk"
        private const val CALENDAR_URL = "$BASE_URL/calenders-en/"
        private const val ADMIN_AJAX_URL = "$BASE_URL/wp-admin/admin-ajax.php"
        private const val HIJRI_CALENDAR_ACTION = "hijri_calendar_get_uploads_paged"
    }

    /**
     * Get current Hijri date information
     */
    suspend fun getCurrentHijriDate(): HijriDateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching current Hijri date from ACJU")
                
                val hijriMonthsData = getACJUHijriMonthsData()
                if (hijriMonthsData.isNullOrEmpty()) {
                    Log.e(TAG, "No Hijri months data available")
                    return@withContext null
                }

                // Get the most recent/current month data
                val currentMonth = hijriMonthsData.firstOrNull()
                if (currentMonth == null) {
                    Log.e(TAG, "No current month data available")
                    return@withContext null
                }

                Log.d(TAG, "Found current Hijri month: ${currentMonth.hijriMonth} ${currentMonth.hijriYear}")
                return@withContext currentMonth

            } catch (e: Exception) {
                Log.e(TAG, "Error getting current Hijri date", e)
                return@withContext null
            }
        }
    }

    /**
     * Get Hijri date for a specific Gregorian date
     */
    suspend fun getHijriDateForGregorian(year: Int, month: Int, day: Int): HijriDateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Converting Gregorian date $year-$month-$day to Hijri using ACJU calendar")
                
                // Step 1: Get the date ranges from the calendar page
                val dateRanges = scrapeHijriDateRanges()
                if (dateRanges == null) {
                    Log.e(TAG, "❌ Failed to scrape date ranges from ACJU calendar page")
                    return@withContext null
                }
                
                // Step 2: Get the month names from the AJAX endpoint
                val monthData = scrapeHijriMonthData()
                if (monthData == null) {
                    Log.e(TAG, "❌ Failed to scrape month data from ACJU AJAX endpoint")
                    return@withContext null
                }
                
                // Step 3: Combine both to calculate the correct Hijri date
                val result = calculateHijriDate(dateRanges, monthData, year, month, day)
                if (result != null) {
                    Log.d(TAG, "✅ Found Hijri date: ${result.hijriDay}/${result.hijriMonth}/${result.hijriYear}")
                } else {
                    Log.w(TAG, "⚠️ Could not calculate Hijri date for $year-$month-$day")
                }
                return@withContext result

            } catch (e: Exception) {
                Log.e(TAG, "Error converting Gregorian to Hijri date", e)
                return@withContext null
            }
        }
    }

    /**
     * Get ACJU Hijri months data from admin-ajax endpoint
     */
    private suspend fun getACJUHijriMonthsData(): List<HijriDateInfo>? {
        return try {
            val formBody = FormBody.Builder()
                .add("action", "hijri_calendar_get_uploads_paged")
                .add("page", "1")
                .build()

            val request = Request.Builder()
                .url(ADMIN_AJAX_URL)
                .post(formBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Origin", BASE_URL)
                .addHeader("Referer", CALENDAR_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch Hijri months data: ${response.code}")
                return null
            }

            val responseBody = response.body?.string()
            if (responseBody == null) {
                Log.e(TAG, "Empty response from admin-ajax")
                return null
            }

            val ajaxResponse = gson.fromJson(responseBody, AjaxResponse::class.java)
            
            if (ajaxResponse.success == true) {
                val htmlContent = ajaxResponse.data?.html ?: ""
                return parseHijriMonthsHtml(htmlContent)
            }

            return null

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing JSON response", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ACJU Hijri months data", e)
            null
        }
    }

    /**
     * Parse HTML content to extract Hijri month data
     */
    private fun parseHijriMonthsHtml(htmlContent: String): List<HijriDateInfo> {
        val hijriMonths = mutableListOf<HijriDateInfo>()
        
        try {
            val document = Jsoup.parse(htmlContent)
            val monthElements = document.select(".hijri-month-item") // Adjust selector as needed
            
            for (element in monthElements) {
                // Extract Hijri month information from HTML
                // This would need to be adjusted based on actual HTML structure
                val monthText = element.text()
                
                // Parse month text to extract Hijri date info
                val hijriInfo = parseHijriMonthText(monthText)
                if (hijriInfo != null) {
                    hijriMonths.add(hijriInfo)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Hijri months HTML", e)
        }
        
        return hijriMonths
    }

    /**
     * Parse Hijri month text to extract date information
     */
    private fun parseHijriMonthText(text: String): HijriDateInfo? {
        return try {
            // This would need to be implemented based on the actual format
            // of the Hijri month data from ACJU
            // For now, return a placeholder
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Hijri month text: $text", e)
            null
        }
    }

    /**
     * Scrape Hijri date ranges from the ACJU calendar page
     * This gets the hijriCalendarData with startDate and endDate
     */
    private suspend fun scrapeHijriDateRanges(): Map<String, Any>? {
        return try {
            Log.d(TAG, "🌐 Fetching Hijri date ranges from: $CALENDAR_URL")
            val request = Request.Builder()
                .url(CALENDAR_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to fetch calendar page: ${response.code} - ${response.message}")
                return null
            }
            
            Log.d(TAG, "✅ Successfully fetched calendar page (${response.code})")

            val html = response.body?.string()
            if (html == null) {
                Log.e(TAG, "Empty response from calendar page")
                return null
            }

            // Look for hijriCalendarData directly in the HTML string
            val scriptPattern = Regex("""var hijriCalendarData = (\{[^}]*\});""")
            val match = scriptPattern.find(html)
            
            if (match != null) {
                val calendarDataStr = match.groupValues[1]
                    .replace("\\/", "/") // Replace JavaScript-style escaping
                Log.d(TAG, "✅ Found hijriCalendarData: $calendarDataStr")
                
                try {
                    @Suppress("UNCHECKED_CAST")
                    val calendarData = gson.fromJson(calendarDataStr, Map::class.java) as Map<String, Any>
                    Log.d(TAG, "✅ Successfully parsed date ranges with ${calendarData.size} properties")
                    return calendarData
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "❌ Error parsing calendar data JSON: $calendarDataStr", e)
                    return null
                }
            } else {
                Log.e(TAG, "❌ hijriCalendarData not found in HTML")
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching calendar page", e)
            null
        }
    }

    /**
     * Scrape Hijri month data from ACJU website using AJAX endpoint
     * This fetches the actual calendar data with Hijri month information
     */
    private suspend fun scrapeHijriMonthData(): Map<String, Any>? {
        return try {
            Log.d(TAG, "🌐 Fetching Hijri calendar data from AJAX endpoint: $ADMIN_AJAX_URL")
            
            // Create form data for the AJAX request
            val formBody = FormBody.Builder()
                .add("action", HIJRI_CALENDAR_ACTION)
                .add("page", "1")
                .build()

            val request = Request.Builder()
                .url(ADMIN_AJAX_URL)
                .post(formBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:143.0) Gecko/20100101 Firefox/143.0")
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "en-US,en;q=0.5")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Origin", BASE_URL)
                .addHeader("Referer", CALENDAR_URL)
                .build()

            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to fetch AJAX calendar data: ${response.code} - ${response.message}")
                return null
            }
            
            Log.d(TAG, "✅ Successfully fetched AJAX calendar data (${response.code})")

            val jsonResponse = response.body?.string()
            if (jsonResponse == null) {
                Log.e(TAG, "❌ Empty response from AJAX endpoint")
                return null
            }

            Log.d(TAG, "📄 AJAX Response: ${jsonResponse.take(200)}...")
            
            try {
                @Suppress("UNCHECKED_CAST")
                val ajaxResponse = gson.fromJson(jsonResponse, Map::class.java) as Map<String, Any>
                
                if (ajaxResponse["success"] == true) {
                    val data = ajaxResponse["data"] as? Map<String, Any>
                    val html = data?.get("html") as? String
                    
                    if (html != null) {
                        Log.d(TAG, "✅ Successfully extracted HTML from AJAX response")
                        
                        // Parse the HTML to extract Hijri calendar information
                        val calendarData = parseHijriCalendarFromHtml(html)
                        Log.d(TAG, "✅ Successfully parsed calendar data with ${calendarData.size} entries")
                        return calendarData
                    } else {
                        Log.e(TAG, "❌ No HTML data found in AJAX response")
                    }
                } else {
                    Log.e(TAG, "❌ AJAX request was not successful")
                }
                
                return null
                
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "❌ Error parsing AJAX response JSON", e)
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching AJAX calendar data", e)
            null
        }
    }

    /**
     * Parse Hijri calendar information from the HTML response
     */
    private fun parseHijriCalendarFromHtml(html: String): Map<String, Any> {
        val calendarData = mutableMapOf<String, Any>()
        
        try {
            Log.d(TAG, "🔍 Parsing Hijri calendar HTML data...")
            
            // Parse HTML to extract calendar entries
            // Pattern: <h3 class='upload_section_media_title'>2025-09</h3>
            // Pattern: <p class='upload_section_media_description'>Rabee`unith Thaani - 1447</p>
            
            val titlePattern = Regex("""<h3 class='upload_section_media_title'>([^<]+)</h3>""")
            val descriptionPattern = Regex("""<p class='upload_section_media_description'>([^<]+)</p>""")
            
            val titleMatches = titlePattern.findAll(html).toList()
            val descriptionMatches = descriptionPattern.findAll(html).toList()
            
            Log.d(TAG, "📊 Found ${titleMatches.size} titles and ${descriptionMatches.size} descriptions")
            
            // Match titles with descriptions
            val entries = mutableListOf<Map<String, String>>()
            for (i in titleMatches.indices) {
                if (i < descriptionMatches.size) {
                    val gregorianDate = titleMatches[i].groupValues[1].trim()
                    val hijriDescription = descriptionMatches[i].groupValues[1].trim()
                    
                    entries.add(mapOf(
                        "gregorianDate" to gregorianDate,
                        "hijriDescription" to hijriDescription
                    ))
                    
                    Log.d(TAG, "📅 Entry: $gregorianDate -> $hijriDescription")
                }
            }
            
            calendarData["entries"] = entries
            Log.d(TAG, "✅ Successfully parsed ${entries.size} calendar entries")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing Hijri calendar HTML", e)
        }
        
        return calendarData
    }

    /**
     * Calculate Hijri date by combining date ranges and month data
     */
    private fun calculateHijriDate(
        dateRanges: Map<String, Any>,
        monthData: Map<String, Any>,
        year: Int,
        month: Int,
        day: Int
    ): HijriDateInfo? {
        try {
            // Get the date ranges (startDate, endDate)
            val startDateStr = dateRanges["startDate"] as? String
            val endDateStr = dateRanges["endDate"] as? String
            
            if (startDateStr == null || endDateStr == null) {
                Log.w(TAG, "⚠️ Missing startDate or endDate in date ranges")
                return null
            }
            
            Log.d(TAG, "📅 Hijri month range: $startDateStr to $endDateStr")
            
            val targetDate = LocalDate(year, month, day)
            val startDate = LocalDate.parse(startDateStr)
            val endDate = LocalDate.parse(endDateStr)
            
            // Check if target date is within the calendar range
            if (targetDate < startDate || targetDate > endDate) {
                Log.w(TAG, "⚠️ Target date $targetDate is outside calendar range ($startDate to $endDate)")
                return null
            }
            
            // Calculate days from start of Hijri month
            val daysFromStart = (targetDate.toEpochDays() - startDate.toEpochDays()).toInt()
            val hijriDay = daysFromStart + 1 // Start from day 1
            
            // Get the month name from the AJAX data
            val targetYearMonth = String.format("%04d-%02d", year, month)
            @Suppress("UNCHECKED_CAST")
            val entries = monthData["entries"] as? List<Map<String, String>>
            
            if (entries != null) {
                val matchingEntry = entries.find { entry ->
                    val gregorianDate = entry["gregorianDate"]
                    gregorianDate == targetYearMonth
                }
                
                if (matchingEntry != null) {
                    val hijriDescription = matchingEntry["hijriDescription"] ?: ""
                    Log.d(TAG, "✅ Found matching month entry: $targetYearMonth -> $hijriDescription")
                    
                    // Parse the Hijri description: "Rabee`unith Thaani - 1447"
                    val hijriInfo = parseHijriDescription(hijriDescription)
                    if (hijriInfo != null) {
                        Log.d(TAG, "✅ Calculated Hijri date: $hijriDay ${hijriInfo.first} ${hijriInfo.second}")
                        
                        return HijriDateInfo(
                            hijriDay = hijriDay,
                            hijriMonth = hijriInfo.first,
                            hijriYear = hijriInfo.second,
                            gregorianDate = String.format("%04d-%02d-%02d", year, month, day),
                            monthStartDate = dateRanges["startDate"] as? String,
                            monthEndDate = dateRanges["endDate"] as? String
                        )
                    } else {
                        Log.w(TAG, "⚠️ Could not parse Hijri description: $hijriDescription")
                    }
                } else {
                    Log.w(TAG, "⚠️ No matching month entry found for $targetYearMonth")
                }
            } else {
                Log.w(TAG, "⚠️ No entries found in month data")
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calculating Hijri date", e)
            return null
        }
    }

    /**
     * Find Hijri date in calendar data for a specific Gregorian date
     * Based on the ACJU calendar data structure which provides date ranges
     */
    private fun findHijriDateInCalendar(
        calendarData: Map<String, Any>,
        year: Int,
        month: Int,
        day: Int
    ): HijriDateInfo? {
        try {
            @Suppress("UNCHECKED_CAST")
            val entries = calendarData["entries"] as? List<Map<String, String>>
            
            if (entries == null) {
                Log.w(TAG, "⚠️ No entries found in calendar data")
                return null
            }
            
            val targetYearMonth = String.format("%04d-%02d", year, month)
            Log.d(TAG, "🔍 Looking for Hijri month for Gregorian: $targetYearMonth")
            
            // Find the entry that matches our target year-month
            val matchingEntry = entries.find { entry ->
                val gregorianDate = entry["gregorianDate"]
                gregorianDate == targetYearMonth
            }
            
            if (matchingEntry != null) {
                val hijriDescription = matchingEntry["hijriDescription"] ?: ""
                Log.d(TAG, "✅ Found matching entry: $targetYearMonth -> $hijriDescription")
                
                // Parse the Hijri description: "Rabee`unith Thaani - 1447"
                val hijriInfo = parseHijriDescription(hijriDescription)
                if (hijriInfo != null) {
                    // Calculate the Hijri day based on the Gregorian day
                    // This is a simplified approach - in reality, we'd need more complex calculation
                    val hijriDay = day // For now, assume same day number
                    
                    Log.d(TAG, "✅ Calculated Hijri date: $hijriDay ${hijriInfo.first} ${hijriInfo.second}")
                    
                    return HijriDateInfo(
                        hijriDay = hijriDay,
                        hijriMonth = hijriInfo.first,
                        hijriYear = hijriInfo.second
                    )
                } else {
                    Log.w(TAG, "⚠️ Could not parse Hijri description: $hijriDescription")
                }
            } else {
                Log.w(TAG, "⚠️ No matching entry found for $targetYearMonth")
                // Log all available entries for debugging
                entries.forEach { entry ->
                    Log.d(TAG, "📅 Available entry: ${entry["gregorianDate"]} -> ${entry["hijriDescription"]}")
                }
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding Hijri date in calendar data", e)
            return null
        }
    }
    
    /**
     * Parse Hijri description like "Rabee`unith Thaani - 1447" into month and year
     */
    private fun parseHijriDescription(description: String): Pair<String, Int>? {
        try {
            // Split by " - " to separate month and year
            val parts = description.split(" - ")
            if (parts.size == 2) {
                val hijriMonth = parts[0].trim()
                val hijriYear = parts[1].trim().toIntOrNull()
                
                if (hijriYear != null) {
                    return Pair(hijriMonth, hijriYear)
                }
            }
            
            Log.w(TAG, "⚠️ Could not parse Hijri description format: $description")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing Hijri description: $description", e)
            return null
        }
    }
}

// Data classes for Hijri date information
data class HijriDateInfo(
    val hijriDay: Int,
    val hijriMonth: String,
    val hijriYear: Int,
    val gregorianDate: String? = null,
    val monthStartDate: String? = null,
    val monthEndDate: String? = null
)

data class AjaxResponse(
    val success: Boolean?,
    val data: AjaxData?
)

data class AjaxData(
    val html: String?
)

class HijriScrapingException(message: String, cause: Throwable? = null) : Exception(message, cause)
