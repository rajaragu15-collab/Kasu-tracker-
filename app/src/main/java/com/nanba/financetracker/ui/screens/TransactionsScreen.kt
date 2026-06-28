package com.nanba.financetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanba.financetracker.data.Categories
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.data.TransactionType
import com.nanba.financetracker.ui.viewmodel.TransactionsViewModel
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory
import com.nanba.financetracker.util.TimeRangeType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(viewModelFactory: ViewModelFactory) {
    val viewModel: TransactionsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Time range filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val ranges = listOf(
                TimeRangeType.TODAY to "Today",
                TimeRangeType.THIS_WEEK to "This Week",
                TimeRangeType.LAST_7_DAYS to "Last 7 Days",
                TimeRangeType.LAST_10_DAYS to "Last 10 Days",
                TimeRangeType.THIS_MONTH to "This Month",
                TimeRangeType.LAST_30_DAYS to "Last 30 Days"
            )
            items(ranges) { (type, label) ->
                FilterChip(
                    selected = uiState.currentRange.label == label,
                    onClick = { viewModel.setRange(type) },
                    label = { Text(label) }
                )
            }
        }

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All") }
                )
            }
            items(Categories.EXPENSE_CATEGORIES) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick = { viewModel.setCategoryFilter(category) },
                    label = { Text(category) }
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(uiState.currentRange.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "₹${"%.0f".format(uiState.totalSpent)} spent",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (uiState.transactions.isEmpty() && !uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Text(
                    "No transactions in this range, nanba.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.transactions, key = { it.id }) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { editingTransaction = transaction }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editingTransaction?.let { transaction ->
        CategoryEditDialog(
            transaction = transaction,
            onDismiss = { editingTransaction = null },
            onCategorySelected = { newCategory ->
                viewModel.recategorize(transaction, newCategory)
                editingTransaction = null
            }
        )
    }
}

@Composable
private fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val isExpense = transaction.type == TransactionType.EXPENSE

    ListItem(
        headlineContent = { Text(transaction.category) },
        supportingContent = {
            Text(dateFormat.format(Date(transaction.dateMillis)))
        },
        trailingContent = {
            Text(
                "${if (isExpense) "-" else "+"}₹${"%.0f".format(transaction.amount)}",
                color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Single dialog showing the transaction's current details plus a scrollable list of
 * categories to pick a new one from. Tapping any category applies it immediately.
 */
@Composable
private fun CategoryEditDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("₹${"%.0f".format(transaction.amount)} — pick a category") },
        text = {
            Column {
                Categories.EXPENSE_CATEGORIES.forEach { category ->
                    val isCurrent = category == transaction.category
                    Text(
                        text = if (isCurrent) "$category (current)" else category,
                        style = if (isCurrent) {
                            MaterialTheme.typography.bodyLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelected(category) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
