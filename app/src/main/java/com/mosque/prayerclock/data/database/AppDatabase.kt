package com.mosque.prayerclock.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mosque.prayerclock.data.model.PrayerTimes

@Database(
    entities = [PrayerTimes::class],
    version = 2, // Incremented for schema change
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "mosque_clock_database",
                        )
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
