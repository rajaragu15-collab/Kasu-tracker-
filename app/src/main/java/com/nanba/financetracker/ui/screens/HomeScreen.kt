package com.nanba.financetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.data.TransactionType
import com.nanba.financetracker.ui.viewmodel.HomeViewModel
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModelFactory: ViewModelFactory) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val recent by viewModel.recentTransactions.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TodaySpendCard(
                todaySpent = uiState.todaySpent,
                dailyGoal = uiState.dailyGoal
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryMiniCard(
                    label = "This Week",
                    amount = uiState.weekSpent,
                    modifier = Modifier.weight(1f)
                )
                SummaryMiniCard(
                    label = "This Month",
                    amount = uiState.monthSpent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CigaretteQuickLogCard(
                countToday = uiState.cigaretteCountToday,
                onLogCigarette = { viewModel.logCigarette() }
            )
        }

        item {
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    "No transactions yet, nanba — they'll show up here once SMS tracking starts or you add one manually.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            items(recent) { transaction ->
                TransactionRow(transaction)
            }
        }
    }
}

@Composable
private fun TodaySpendCard(todaySpent: Double, dailyGoal: Double) {
    val exceeded = todaySpent > dailyGoal
    val progress = if (dailyGoal > 0) (todaySpent / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (exceeded) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Today", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "₹${"%.0f".format(todaySpent)}",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "of ₹${"%.0f".format(dailyGoal)} daily goal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            if (exceeded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Goal exceeded nanba, go easy!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹${"%.0f".format(amount)}", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CigaretteQuickLogCard(countToday: Int, onLogCigarette: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SmokingRooms, contentDescription = null, tint = Color(0xFF8D6E63))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cigarettes today", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$countToday smoked", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onLogCigarette) {
                Text("+1")
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(transaction.category, style = MaterialTheme.typography.bodyLarge)
            Text(
                dateFormat.format(Date(transaction.dateMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        val isExpense = transaction.type == TransactionType.EXPENSE
        Text(
            "${if (isExpense) "-" else "+"}₹${"%.0f".format(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
        )
    }
}
