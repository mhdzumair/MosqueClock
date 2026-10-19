package com.mosque.prayerclock.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mosque.prayerclock.data.model.PrayerTimes

@Database(
    entities = [PrayerTimes::class, HijriDateEntity::class],
    version = 6, // Retain both published Asr times for mazhab selection.
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao

    abstract fun hijriDateDao(): HijriDateDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prayer_times ADD COLUMN asrShafiAzan TEXT")
                db.execSQL("ALTER TABLE prayer_times ADD COLUMN asrHanafiAzan TEXT")
                // Older ACJU caches discarded the Hanafi column; fetch both times again.
                db.execSQL("DELETE FROM prayer_times WHERE providerKey GLOB 'ACJU_DIRECT:*'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM prayer_times WHERE providerKey LIKE 'ACJU_DIRECT:%'")
            }
        }

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
                        ).addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                        .fallbackToDestructiveMigration() // Recreate DB on other schema changes
                        .build()
                instance = inst
                inst
            }
    }
}
