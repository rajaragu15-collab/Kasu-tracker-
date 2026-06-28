package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nanba.financetracker.repository.FinanceRepository

/**
 * Generic factory so any ViewModel taking a FinanceRepository in its constructor
 * can be created via Compose's viewModel(factory = ...) call.
 */
class ViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(repository) as T
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(repository) as T
            modelClass.isAssignableFrom(BudgetsViewModel::class.java) ->
                BudgetsViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(repository) as T
            modelClass.isAssignableFrom(CigaretteViewModel::class.java) ->
                CigaretteViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
