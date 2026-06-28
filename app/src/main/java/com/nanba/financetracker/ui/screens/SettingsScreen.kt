package com.nanba.financetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanba.financetracker.data.AppTheme
import com.nanba.financetracker.ui.viewmodel.SettingsViewModel
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory

private enum class EditableField { DAILY_GOAL, LOW_BALANCE, CIGARETTE_PRICE }

@Composable
fun SettingsScreen(
    viewModelFactory: ViewModelFactory,
    hasSmsPermission: Boolean,
    onRequestSmsPermission: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val settings by viewModel.settings.collectAsState()

    var editingField by remember { mutableStateOf<EditableField?>(null) }

    val currentSettings = settings ?: return // wait for settings to load from DB

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionCard(title = "SMS Tracking") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-detect SCB/KVB debits", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (hasSmsPermission) "Permission granted" else "Permission needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasSmsPermission) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Switch(
                        checked = currentSettings.smsTrackingEnabled && hasSmsPermission,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasSmsPermission) {
                                onRequestSmsPermission()
                            } else {
                                viewModel.setSmsTrackingEnabled(enabled)
                            }
                        }
                    )
                }
            }
        }

        item {
            SectionCard(title = "Daily Goal") {
                SettingRow(
                    label = "Daily spending limit",
                    value = "₹${currentSettings.dailyGoal.toInt()}",
                    onClick = { editingField = EditableField.DAILY_GOAL }
                )
            }
        }

        item {
            SectionCard(title = "Low Balance Warning") {
                SettingRow(
                    label = "Warn when balance below",
                    value = "₹${currentSettings.lowBalanceThreshold.toInt()}",
                    onClick = { editingField = EditableField.LOW_BALANCE }
                )
            }
        }

        item {
            SectionCard(title = "Cigarette Price") {
                SettingRow(
                    label = "Price per cigarette",
                    value = "₹${currentSettings.cigarettePrice.toInt()}",
                    onClick = { editingField = EditableField.CIGARETTE_PRICE }
                )
            }
        }

        item {
            SectionCard(title = "Theme") {
                Column {
                    ThemeOption(
                        label = "Light",
                        selected = currentSettings.theme == AppTheme.LIGHT,
                        onClick = { viewModel.setTheme(AppTheme.LIGHT) }
                    )
                    ThemeOption(
                        label = "Dark",
                        selected = currentSettings.theme == AppTheme.DARK,
                        onClick = { viewModel.setTheme(AppTheme.DARK) }
                    )
                    ThemeOption(
                        label = "System default",
                        selected = currentSettings.theme == AppTheme.SYSTEM,
                        onClick = { viewModel.setTheme(AppTheme.SYSTEM) }
                    )
                }
            }
        }

        item {
            SectionCard(title = "About") {
                Text(
                    "Kasu Tracker — built for tracking expenses, cigarettes, and daily goals.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    editingField?.let { field ->
        val title: String
        val initial: Double
        when (field) {
            EditableField.DAILY_GOAL -> {
                title = "Daily spending goal"
                initial = currentSettings.dailyGoal
            }
            EditableField.LOW_BALANCE -> {
                title = "Low balance threshold"
                initial = currentSettings.lowBalanceThreshold
            }
            EditableField.CIGARETTE_PRICE -> {
                title = "Price per cigarette"
                initial = currentSettings.cigarettePrice
            }
        }

        EditAmountDialog(
            title = title,
            initialValue = initial,
            onDismiss = { editingField = null },
            onConfirm = { value ->
                when (field) {
                    EditableField.DAILY_GOAL -> viewModel.setDailyGoal(value)
                    EditableField.LOW_BALANCE -> viewModel.setLowBalanceThreshold(value)
                    EditableField.CIGARETTE_PRICE -> viewModel.setCigarettePrice(value)
                }
                editingField = null
            }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EditAmountDialog(
    title: String,
    initialValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initialValue.toInt().toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { newValue -> text = newValue.filter { c -> c.isDigit() } },
                label = { Text("Amount (₹)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { text.toDoubleOrNull()?.let(onConfirm) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
