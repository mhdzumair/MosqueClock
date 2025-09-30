package com.mosque.prayerclock.data.cache

/**
 * Interface for invalidating prayer times cache.
 * This allows SettingsRepository to invalidate PrayerTimesRepository cache
 * without creating a circular dependency.
 */
interface PrayerTimesCacheInvalidator {
    /**
     * Invalidate the prayer times cache when manual settings change
     */
    fun invalidatePrayerTimesCache()
}
