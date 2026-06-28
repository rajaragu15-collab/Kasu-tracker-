package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.data.TransactionType
import com.nanba.financetracker.repository.FinanceRepository
import com.nanba.financetracker.util.TimeRange
import com.nanba.financetracker.util.TimeRangeCalculator
import com.nanba.financetracker.util.TimeRangeType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val currentRange: TimeRange = TimeRangeCalculator.calculate(TimeRangeType.LAST_7_DAYS),
    val selectedCategory: String? = null, // null = all categories
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true
)

class TransactionsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        setRange(TimeRangeType.LAST_7_DAYS)
    }

    fun setRange(type: TimeRangeType, customStart: Long? = null, customEnd: Long? = null) {
        val range = TimeRangeCalculator.calculate(type, customStart, customEnd)
        _uiState.value = _uiState.value.copy(currentRange = range, isLoading = true)
        reload()
    }

    fun setCategoryFilter(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        reload()
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val range = _uiState.value.currentRange
            val category = _uiState.value.selectedCategory

            repository.getTransactionsInRange(range).collect { all ->
                val filtered = if (category != null) all.filter { it.category == category } else all
                val total = filtered.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                _uiState.value = _uiState.value.copy(
                    transactions = filtered,
                    totalSpent = total,
                    isLoading = false
                )
            }
        }
    }

    fun recategorize(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            repository.recategorizeTransaction(transaction, newCategory)
        }
    }
}
