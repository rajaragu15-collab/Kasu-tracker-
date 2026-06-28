package com.nanba.financetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): Budget?
}
