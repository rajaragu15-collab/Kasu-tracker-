package com.nanba.financetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettings)

    @Update
    suspend fun update(settings: AppSettings)

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettings?
}
