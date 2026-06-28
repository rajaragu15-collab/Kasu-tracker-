package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanba.financetracker.repository.FinanceRepository
import com.nanba.financetracker.util.TimeRange
import com.nanba.financetracker.util.TimeRangeCalculator
import com.nanba.financetracker.util.TimeRangeType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CigaretteUiState(
    val count: Int = 0,
    val totalSpent: Double = 0.0,
    val currentRange: TimeRange = TimeRangeCalculator.calculate(TimeRangeType.TODAY),
    val pricePerCigarette: Double = 25.0,
    val isLoading: Boolean = true
)

class CigaretteViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CigaretteUiState())
    val uiState: StateFlow<CigaretteUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        setRange(TimeRangeType.TODAY)
    }

    fun setRange(type: TimeRangeType, customStart: Long? = null, customEnd: Long? = null) {
        val range = TimeRangeCalculator.calculate(type, customStart, customEnd)
        _uiState.value = _uiState.value.copy(currentRange = range, isLoading = true)
        reload()
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val range = _uiState.value.currentRange
            val settings = repository.getSettings()

            // Combine count + spend together for the current range so both numbers
            // update in lockstep instead of nesting one collect inside another
            // (nested collects would leak the inner flow's collection on every
            // outer emission).
            combine(
                repository.getCigaretteCountInRange(range),
                repository.getCigaretteSpendInRange(range)
            ) { count, spend -> count to spend }
                .collect { (count, spend) ->
                    _uiState.value = _uiState.value.copy(
                        count = count,
                        totalSpent = spend,
                        pricePerCigarette = settings.cigarettePrice,
                        isLoading = false
                    )
                }
        }
    }

    fun logCigarette() {
        viewModelScope.launch {
            repository.logCigarette()
        }
    }
}
