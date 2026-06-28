package com.nanba.financetracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CategoryRule)

    // Find a remembered category for a destination account, if one exists.
    // Matching is done case-insensitively against substrings in app code,
    // this just fetches all rules to check against.
    @Query("SELECT * FROM category_rules")
    suspend fun getAll(): List<CategoryRule>

    @Query("SELECT * FROM category_rules WHERE destinationAccount = :account LIMIT 1")
    suspend fun getByAccount(account: String): CategoryRule?
}
