package com.nanba.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppTheme { LIGHT, DARK, SYSTEM }

enum class WallpaperType { NONE, PRESET, CUSTOM }

/**
 * Single-row table holding app-wide settings, like the daily spending goal.
 * id is always 1 so there's only ever one row.
 */
@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val dailyGoal: Double = 300.0,
    val lowBalanceThreshold: Double = 2000.0,
    val smsTrackingEnabled: Boolean = false,
    val scEnabled: Boolean = true,
    val kvbEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val wallpaperType: WallpaperType = WallpaperType.NONE,
    val wallpaperValue: String = "", // preset name OR file path to custom uploaded image
    val cigarettePrice: Double = 25.0 // editable in Settings, used by the +1 Cigarette quick button
)
