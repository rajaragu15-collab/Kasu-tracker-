package com.nanba.financetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis < :endMillis ORDER BY dateMillis DESC")
    fun getInRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getTotalExpenseInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME' AND dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getTotalIncomeInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND category = :category AND dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getCategorySpendInRange(category: String, startMillis: Long, endMillis: Long): Flow<Double>

    // Used to check if an SMS-derived transaction already exists (avoid duplicate inserts
    // if the same SMS gets processed twice, e.g. after a phone reboot re-scan).
    @Query("SELECT COUNT(*) FROM transactions WHERE rawSmsBody = :smsBody AND dateMillis = :dateMillis")
    suspend fun countDuplicates(smsBody: String, dateMillis: Long): Int

    @Query("SELECT * FROM transactions WHERE category = 'Uncategorized' ORDER BY dateMillis DESC")
    fun getUncategorized(): Flow<List<Transaction>>

    // Cigarette tracking: each +1 tap creates one EXPENSE transaction in the
    // "Cigarettes" category at the current per-cigarette price. Counting rows in
    // range gives the cigarette count; summing amount gives the money spent.
    @Query("SELECT COUNT(*) FROM transactions WHERE category = 'Cigarettes' AND dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getCigaretteCountInRange(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category = 'Cigarettes' AND dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getCigaretteSpendInRange(startMillis: Long, endMillis: Long): Flow<Double>
}
