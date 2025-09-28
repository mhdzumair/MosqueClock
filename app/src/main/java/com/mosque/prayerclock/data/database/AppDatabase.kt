package com.mosque.prayerclock.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mosque.prayerclock.data.model.PrayerTimes

@Database(
    entities = [PrayerTimes::class, HijriDateEntity::class],
    version = 3, // Incremented for Hijri date caching
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun hijriDateDao(): HijriDateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                val inst =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "mosque_clock_database",
                        ).build()
                instance = inst
                inst
            }
    }
}
