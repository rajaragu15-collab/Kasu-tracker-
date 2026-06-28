package com.nanba.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

enum class TransactionSource { MANUAL, SMS }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val category: String, // "Uncategorized" until tagged
    val note: String,
    val dateMillis: Long,
    val source: TransactionSource = TransactionSource.MANUAL,
    val destinationAccount: String = "", // e.g. "XX0051", used for auto-matching future SMS to same payee
    val rawSmsBody: String = "" // kept for the debug screen so the user can verify parsing
)
