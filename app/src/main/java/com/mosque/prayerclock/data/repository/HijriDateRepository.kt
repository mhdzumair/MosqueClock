package com.mosque.prayerclock.data.repository

import com.mosque.prayerclock.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HijriDateRepository @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    data class HijriDate(
        val day: Int,
        val month: Int,
        val year: Int
    )
    
    suspend fun getCurrentHijriDate(): HijriDate {
        val settings = settingsRepository.getSettings().first()
        
        if (settings.useApiForHijriDate) {
            // TODO: Implement API call to get Hijri date
            // For now, fall back to manual calculation
            return calculateCurrentHijriDate(settings)
        } else {
            return calculateCurrentHijriDate(settings)
        }
    }
    
    private fun calculateCurrentHijriDate(settings: AppSettings): HijriDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val lastUpdatedDate = LocalDate.parse(settings.lastUpdatedGregorianDate)
        
        // Calculate how many days have passed since the manual date was set
        val daysPassed = today.toEpochDays() - lastUpdatedDate.toEpochDays()
        
        if (daysPassed == 0) {
            // Same day, return the manual date as is
            return HijriDate(
                day = settings.manualHijriDay,
                month = settings.manualHijriMonth,
                year = settings.manualHijriYear
            )
        }
        
        // Calculate the new Hijri date by adding the days
        return addDaysToHijriDate(
            HijriDate(
                day = settings.manualHijriDay,
                month = settings.manualHijriMonth,
                year = settings.manualHijriYear
            ),
            daysPassed.toInt()
        )
    }
    
    private fun addDaysToHijriDate(hijriDate: HijriDate, daysToAdd: Int): HijriDate {
        var day = hijriDate.day
        var month = hijriDate.month
        var year = hijriDate.year
        var remainingDays = daysToAdd
        
        while (remainingDays > 0) {
            val daysInCurrentMonth = getDaysInHijriMonth(month, year)
            val daysLeftInMonth = daysInCurrentMonth - day + 1
            
            if (remainingDays < daysLeftInMonth) {
                // Can add the remaining days within this month
                day += remainingDays
                remainingDays = 0
            } else {
                // Move to next month
                remainingDays -= daysLeftInMonth
                day = 1
                month++
                
                if (month > 12) {
                    month = 1
                    year++
                }
            }
        }
        
        return HijriDate(day, month, year)
    }
    
    private fun getDaysInHijriMonth(month: Int, year: Int): Int {
        // Islamic calendar months alternate between 29 and 30 days
        // with some variations based on lunar observations
        // This is a simplified approach - in reality, each month length
        // should be determined by lunar observations or precise calculations
        
        return when (month) {
            1, 3, 5, 7, 9, 11 -> 30  // Muharram, Rabi al-Awwal, Jumada al-Awwal, Rajab, Ramadan, Dhul Qidah
            2, 4, 6, 8, 10 -> 29     // Safar, Rabi al-Thani, Jumada al-Thani, Shaban, Shawwal
            12 -> if (isLeapYear(year)) 30 else 29  // Dhul Hijjah
            else -> 29
        }
    }
    
    private fun isLeapYear(hijriYear: Int): Boolean {
        // In the Islamic calendar, there are 11 leap years in every 30-year cycle
        // Years 2, 5, 7, 10, 13, 16, 18, 21, 24, 26, and 29 of each cycle are leap years
        val yearInCycle = hijriYear % 30
        return yearInCycle in listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
    }
    
    suspend fun updateManualHijriDate(day: Int, month: Int, year: Int) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        
        settingsRepository.updateHijriDate(day, month, year, today)
    }
    
    fun getHijriMonthNames(isEnglish: Boolean): List<String> {
        return if (isEnglish) {
            listOf(
                "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
                "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Shaban",
                "Ramadan", "Shawwal", "Dhul Qidah", "Dhul Hijjah"
            )
        } else {
            listOf(
                "முஹர்ரம்", "ஸபர்", "ரபீ அவ்வல்", "ரபீ ஆகிர்",
                "ஜுமாதா அவ்வல்", "ஜுமாதா ஆகிர்", "ரஜப்", "ஷக்பான்",
                "ரமலான்", "ஷவ்வல்", "துல் க்வித்தா", "துல் ஹிஜ்ஜா"
            )
        }
    }
}