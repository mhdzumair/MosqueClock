package com.mosque.prayerclock.utils

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Centralized time utility functions to avoid code duplication
 * All time-related calculations should use these functions
 */
object TimeUtils {
    /**
     * Add minutes to a time string (HH:MM format)
     * Most robust implementation with proper validation
     */
    fun addMinutesToTime(
        timeString: String,
        minutes: Int,
    ): String {
        try {
            val parts = timeString.split(":")
            if (parts.size != 2) return timeString

            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val totalMinutes = hour * 60 + minute + minutes
            val newHour = (totalMinutes / 60) % 24
            val newMinute = totalMinutes % 60

            return String.format("%02d:%02d", newHour, newMinute)
        } catch (e: Exception) {
            return timeString
        }
    }

    /**
     * Calculate Iqamah time by adding minutes to Azan time
     * Uses formatTime for consistent time format handling
     */
    fun calculateIqamahTime(
        azanTime: String,
        minutesAfter: Int,
    ): String {
        try {
            val cleanTime = formatTime(azanTime)
            val parts = cleanTime.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()

            val totalMinutes = (hours * 60) + minutes + minutesAfter
            val newHours = (totalMinutes / 60) % 24
            val newMinutes = totalMinutes % 60

            return String.format("%02d:%02d", newHours, newMinutes)
        } catch (e: Exception) {
            return azanTime // Return original time if calculation fails
        }
    }

    /**
     * Compare two time strings (HH:MM format)
     * Returns: negative if time1 < time2, 0 if equal, positive if time1 > time2
     */
    fun compareTimeStrings(
        time1: String,
        time2: String,
    ): Int {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return try {
            val date1 = format.parse(time1)
            val date2 = format.parse(time2)
            date1?.compareTo(date2) ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Format time string by removing timezone info if present
     * AlAdhan API sometimes returns time with timezone info
     */
    private fun formatTime(time: String): String {
        // Remove timezone if present (e.g., "12:30 +05:30" -> "12:30")
        return time.split(" ").first()
    }
}
