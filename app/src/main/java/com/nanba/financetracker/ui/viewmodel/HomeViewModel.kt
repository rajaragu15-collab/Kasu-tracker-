package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanba.financetracker.data.AppSettings
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.repository.FinanceRepository
import com.nanba.financetracker.util.TimeRangeCalculator
import com.nanba.financetracker.util.TimeRangeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todaySpent: Double = 0.0,
    val dailyGoal: Double = 300.0,
    val weekSpent: Double = 0.0,
    val monthSpent: Double = 0.0,
    val cigaretteCountToday: Int = 0,
    val isLoading: Boolean = true
)

// Small private holder so the 5-way combine below stays type-safe instead of
// relying on an untyped array of Any.
private data class HomeRawData(
    val todaySpent: Double,
    val weekSpent: Double,
    val monthSpent: Double,
    val settings: AppSettings?,
    val cigCount: Int
)

class HomeViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val todayRange = TimeRangeCalculator.calculate(TimeRangeType.TODAY)
    private val weekRange = TimeRangeCalculator.calculate(TimeRangeType.THIS_WEEK)
    private val monthRange = TimeRangeCalculator.calculate(TimeRangeType.THIS_MONTH)

    // Combine the first 4 flows, then combine the result with the 5th -- avoids the
    // untyped-array pitfall of combine() overloads with 5+ flows.
    private val firstFour = combine(
        repository.getTotalExpenseInRange(todayRange),
        repository.getTotalExpenseInRange(weekRange),
        repository.getTotalExpenseInRange(monthRange),
        repository.observeSettings()
    ) { todaySpent, weekSpent, monthSpent, settings ->
        Triple(todaySpent, weekSpent, monthSpent) to settings
    }

    val uiState: StateFlow<HomeUiState> = combine(
        firstFour,
        repository.getCigaretteCountInRange(todayRange)
    ) { (totals, settings), cigCount ->
        val (todaySpent, weekSpent, monthSpent) = totals
        HomeUiState(
            todaySpent = todaySpent,
            dailyGoal = settings?.dailyGoal ?: 300.0,
            weekSpent = weekSpent,
            monthSpent = monthSpent,
            cigaretteCountToday = cigCount,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions

    init {
        viewModelScope.launch {
            repository.getAllTransactions().collect { all ->
                _recentTransactions.value = all.take(10)
            }
        }
    }

    fun logCigarette() {
        viewModelScope.launch {
            repository.logCigarette()
        }
    }
}
