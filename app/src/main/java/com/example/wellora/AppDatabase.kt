package com.example.wellora

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HabitEntity::class,
        MoodEntryEntity::class,
        HydrationEntity::class,
        UserProfileEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun moodDao(): MoodDao
    abstract fun hydrationDao(): HydrationDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- MIGRATIONS ---

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mood_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        emoji TEXT NOT NULL,
                        moodValue INTEGER NOT NULL,
                        note TEXT,
                        timestamp INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='hydration_entries'"
                )
                val tableExists = cursor.count > 0
                cursor.close()

                if (tableExists) {
                    db.execSQL("ALTER TABLE hydration_entries RENAME TO hydration_entries_old")
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS hydration_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        glassesDrunk INTEGER NOT NULL,
                        reminderEnabled INTEGER NOT NULL DEFAULT 0,
                        reminderInterval INTEGER NOT NULL DEFAULT 30,
                        date INTEGER NOT NULL DEFAULT (strftime('%s','now'))
                    )
                    """.trimIndent()
                )

                if (tableExists) {
                    db.execSQL(
                        """
                        INSERT INTO hydration_entries (id, glassesDrunk, reminderEnabled, reminderInterval, date)
                        SELECT id, waterAmount, 0, 30, timestamp FROM hydration_entries_old
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE hydration_entries_old")
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT,
                        age INTEGER NOT NULL,
                        habitsGoal INTEGER NOT NULL,
                        waterGoal INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // --- INSTANCE BUILDER ---
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wellora_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            // Insert default user on DB creation
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                val dao = database.userProfileDao()

                                // Preload test user
                                val testUser = UserProfileEntity(
                                    name = "Test User",
                                    email = "test@example.com",
                                    password = "123456",
                                    age = 25,
                                    habitsGoal = 5,
                                    waterGoal = 8
                                )
                                dao.insertUser(testUser)
                            }
                        }
                    })
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
