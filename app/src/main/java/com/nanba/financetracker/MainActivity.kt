package com.nanba.financetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.nanba.financetracker.data.AppDatabase
import com.nanba.financetracker.data.AppTheme
import com.nanba.financetracker.notifications.NotificationHelper
import com.nanba.financetracker.repository.FinanceRepository
import com.nanba.financetracker.ui.KasuTrackerApp
import com.nanba.financetracker.ui.theme.KasuTrackerTheme
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: FinanceRepository

    // Mutable state read by Compose to reflect current permission status, updated
    // after the system permission dialog result comes back.
    private val smsPermissionState = mutableStateOf(false)

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantedMap ->
        val granted = grantedMap.values.all { it }
        if (granted) {
            lifecycleScope.launch {
                val current = repository.getSettings()
                repository.updateSettings(current.copy(smsTrackingEnabled = true))
            }
        }
        smsPermissionState.value = hasSmsPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() and before setContent.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext)
        repository = FinanceRepository.getInstance(db)
        NotificationHelper.createChannels(applicationContext)

        lifecycleScope.launch {
            repository.ensureSettingsExist()
        }

        smsPermissionState.value = hasSmsPermission()

        setContent {
            val viewModelFactory = remember { ViewModelFactory(repository) }
            val settings by repository.observeSettings().collectAsState(initial = null)
            val hasSmsPermission by remember { smsPermissionState }

            KasuTrackerTheme(appTheme = settings?.theme ?: AppTheme.SYSTEM) {
                KasuTrackerApp(
                    viewModelFactory = viewModelFactory,
                    hasSmsPermission = hasSmsPermission,
                    onRequestSmsPermission = { requestSmsPermission() }
                )
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        val receive = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        return receive == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        requestSmsPermissionLauncher.launch(
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        )
    }
}
