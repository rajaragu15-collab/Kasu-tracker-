package com.nanba.financetracker.repository

import com.nanba.financetracker.data.AppDatabase
import com.nanba.financetracker.data.AppSettings
import com.nanba.financetracker.data.Budget
import com.nanba.financetracker.data.CategoryRule
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.data.TransactionSource
import com.nanba.financetracker.data.TransactionType
import com.nanba.financetracker.util.TimeRange
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point ViewModels use to read/write app data. Wraps the Room DAOs so
 * UI code doesn't need to know about the database directly.
 */
class FinanceRepository(private val db: AppDatabase) {

    // ---- Transactions ----

    fun getAllTransactions(): Flow<List<Transaction>> = db.transactionDao().getAll()

    fun getTransactionsInRange(range: TimeRange): Flow<List<Transaction>> =
        db.transactionDao().getInRange(range.startMillis, range.endMillis)

    fun getTotalExpenseInRange(range: TimeRange): Flow<Double> =
        db.transactionDao().getTotalExpenseInRange(range.startMillis, range.endMillis)

    fun getTotalIncomeInRange(range: TimeRange): Flow<Double> =
        db.transactionDao().getTotalIncomeInRange(range.startMillis, range.endMillis)

    fun getCategorySpendInRange(category: String, range: TimeRange): Flow<Double> =
        db.transactionDao().getCategorySpendInRange(category, range.startMillis, range.endMillis)

    fun getUncategorizedTransactions(): Flow<List<Transaction>> = db.transactionDao().getUncategorized()

    suspend fun addTransaction(transaction: Transaction): Long = db.transactionDao().insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) = db.transactionDao().update(transaction)

    /**
     * Updates just the category of an existing transaction (used when the user
     * corrects a wrongly auto-tagged or wrongly manually-tagged transaction).
     * Also updates the remembered CategoryRule for that destination account, if any,
     * so future SMS to the same account get the corrected category.
     */
    suspend fun recategorizeTransaction(transaction: Transaction, newCategory: String) {
        db.transactionDao().update(transaction.copy(category = newCategory))
        if (transaction.destinationAccount.isNotBlank()) {
            db.categoryRuleDao().insert(
                CategoryRule(destinationAccount = transaction.destinationAccount, category = newCategory)
            )
        }
    }

    // ---- Cigarette tracking ----

    fun getCigaretteCountInRange(range: TimeRange): Flow<Int> =
        db.transactionDao().getCigaretteCountInRange(range.startMillis, range.endMillis)

    fun getCigaretteSpendInRange(range: TimeRange): Flow<Double> =
        db.transactionDao().getCigaretteSpendInRange(range.startMillis, range.endMillis)

    /**
     * The "+1 Cigarette" quick button. Logs one cigarette as a normal EXPENSE
     * transaction in the Cigarettes category at the current configured price, so it
     * folds naturally into daily goal, weekly/monthly totals, and all time filters.
     * Returns the new transaction's amount so the caller can trigger a daily-goal
     * check (e.g. NotificationHelper.checkDailyGoal) immediately after.
     */
    suspend fun logCigarette(): Double {
        val settings = db.settingsDao().get()
        val price = settings?.cigarettePrice ?: 25.0
        db.transactionDao().insert(
            Transaction(
                amount = price,
                type = TransactionType.EXPENSE,
                category = "Cigarettes",
                note = "+1 quick log",
                dateMillis = System.currentTimeMillis(),
                source = TransactionSource.MANUAL
            )
        )
        return price
    }

    // ---- Budgets ----

    fun getAllBudgets(): Flow<List<Budget>> = db.budgetDao().getAll()

    suspend fun setBudget(budget: Budget) = db.budgetDao().insert(budget)

    suspend fun deleteBudget(budget: Budget) = db.budgetDao().delete(budget)

    // ---- Settings ----

    fun observeSettings(): Flow<AppSettings?> = db.settingsDao().observe()

    suspend fun getSettings(): AppSettings = db.settingsDao().get() ?: AppSettings()

    suspend fun updateSettings(settings: AppSettings) = db.settingsDao().update(settings)

    suspend fun ensureSettingsExist() {
        if (db.settingsDao().get() == null) {
            db.settingsDao().insert(AppSettings())
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FinanceRepository? = null

        fun getInstance(db: AppDatabase): FinanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinanceRepository(db).also { INSTANCE = it }
            }
        }
    }
}
