package com.nanba.financetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanba.financetracker.data.AppSettings
import com.nanba.financetracker.data.AppTheme
import com.nanba.financetracker.data.WallpaperType
import com.nanba.financetracker.repository.FinanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: FinanceRepository) : ViewModel() {

    val settings: StateFlow<AppSettings?> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setDailyGoal(amount: Double) = update { it.copy(dailyGoal = amount) }

    fun setLowBalanceThreshold(amount: Double) = update { it.copy(lowBalanceThreshold = amount) }

    fun setCigarettePrice(price: Double) = update { it.copy(cigarettePrice = price) }

    fun setSmsTrackingEnabled(enabled: Boolean) = update { it.copy(smsTrackingEnabled = enabled) }

    fun setScbEnabled(enabled: Boolean) = update { it.copy(scEnabled = enabled) }

    fun setKvbEnabled(enabled: Boolean) = update { it.copy(kvbEnabled = enabled) }

    fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }

    fun setWallpaper(type: WallpaperType, value: String) = update {
        it.copy(wallpaperType = type, wallpaperValue = value)
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.updateSettings(transform(current))
        }
    }
}
