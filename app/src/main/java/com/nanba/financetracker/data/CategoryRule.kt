package com.nanba.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Once the user tags a transaction going to a particular destination account as a
 * category, we remember it here so future SMS transactions to the same account get
 * auto-tagged the same way. Real bank SMS (SCB, KVB) only include the destination
 * account number -- not a shop/merchant name -- so matching is done on account number.
 */
@Entity(tableName = "category_rules")
data class CategoryRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val destinationAccount: String, // e.g. "XX0051", matched exactly against future SMS
    val category: String
)
