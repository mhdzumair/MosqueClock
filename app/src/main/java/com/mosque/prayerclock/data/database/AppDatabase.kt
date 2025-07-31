package com.mosque.prayerclock.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.mosque.prayerclock.data.model.PrayerTimes

@Database(
    entities = [PrayerTimes::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun prayerTimesDao(): PrayerTimesDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mosque_clock_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}