package com.nanba.financetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter
    fun fromAppTheme(value: AppTheme): String = value.name

    @TypeConverter
    fun toAppTheme(value: String): AppTheme = AppTheme.valueOf(value)

    @TypeConverter
    fun fromWallpaperType(value: WallpaperType): String = value.name

    @TypeConverter
    fun toWallpaperType(value: String): WallpaperType = WallpaperType.valueOf(value)
}

@Database(
    entities = [Transaction::class, Budget::class, CategoryRule::class, AppSettings::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasu_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
