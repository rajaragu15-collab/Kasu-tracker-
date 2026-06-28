package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanba.financetracker.data.Budget
import com.nanba.financetracker.repository.FinanceRepository
import com.nanba.financetracker.util.TimeRangeCalculator
import com.nanba.financetracker.util.TimeRangeType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetWithSpend(
    val budget: Budget,
    val spentThisMonth: Double
)

data class BudgetsUiState(
    val dailyGoal: Double = 300.0,
    val todaySpent: Double = 0.0,
    val categoryBudgets: List<Budget> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val todayRange = TimeRangeCalculator.calculate(TimeRangeType.TODAY)
    private val monthRange = TimeRangeCalculator.calculate(TimeRangeType.THIS_MONTH)

    val uiState: StateFlow<BudgetsUiState> = combine(
        repository.observeSettings(),
        repository.getTotalExpenseInRange(todayRange),
        repository.getAllBudgets()
    ) { settings, todaySpent, budgets ->
        BudgetsUiState(
            dailyGoal = settings?.dailyGoal ?: 300.0,
            todaySpent = todaySpent,
            categoryBudgets = budgets,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun setDailyGoal(amount: Double) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(current.copy(dailyGoal = amount))
        }
    }

    fun setCategoryBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            repository.setBudget(Budget(category = category, monthlyLimit = monthlyLimit))
        }
    }

    fun deleteCategoryBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // UI calls this per-category row to show "spent X of Y this month" -- kept as a
    // separate Flow getter rather than baked into uiState so adding/removing budgets
    // doesn't require recomputing every category's spend each time.
    fun getCategorySpendThisMonth(category: String) = repository.getCategorySpendInRange(category, monthRange)
}
