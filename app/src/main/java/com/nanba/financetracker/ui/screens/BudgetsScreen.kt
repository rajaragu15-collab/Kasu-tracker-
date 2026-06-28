package com.nanba.financetracker.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nanba.financetracker.data.Budget
import com.nanba.financetracker.data.Categories
import com.nanba.financetracker.ui.viewmodel.BudgetsViewModel
import com.nanba.financetracker.ui.viewmodel.ViewModelFactory

@Composable
fun BudgetsScreen(viewModelFactory: ViewModelFactory) {
    val viewModel: BudgetsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showAddCategoryBudgetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DailyGoalCard(
                dailyGoal = uiState.dailyGoal,
                todaySpent = uiState.todaySpent,
                onEditClick = { showDailyGoalDialog = true }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Category Budgets", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showAddCategoryBudgetDialog = true }) {
                    Text("+ Add")
                }
            }
        }

        if (uiState.categoryBudgets.isEmpty()) {
            item {
                Text(
                    "No category budgets set yet, nanba. Tap + Add to set a monthly limit for groceries, cigarettes, etc.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            items(uiState.categoryBudgets) { budget ->
                CategoryBudgetRow(
                    budget = budget,
                    viewModel = viewModel,
                    onDelete = { viewModel.deleteCategoryBudget(budget) }
                )
            }
        }
    }

    if (showDailyGoalDialog) {
        AmountInputDialog(
            title = "Set daily spending goal",
            initialValue = uiState.dailyGoal,
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = { amount ->
                viewModel.setDailyGoal(amount)
                showDailyGoalDialog = false
            }
        )
    }

    if (showAddCategoryBudgetDialog) {
        AddCategoryBudgetDialog(
            existingCategories = uiState.categoryBudgets.map { it.category },
            onDismiss = { showAddCategoryBudgetDialog = false },
            onConfirm = { category, amount ->
                viewModel.setCategoryBudget(category, amount)
                showAddCategoryBudgetDialog = false
            }
        )
    }
}

@Composable
private fun DailyGoalCard(dailyGoal: Double, todaySpent: Double, onEditClick: () -> Unit) {
    val progress = if (dailyGoal > 0) (todaySpent / dailyGoal).toFloat().coerceIn(0f, 1f) else 0f
    val exceeded = todaySpent > dailyGoal

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Daily Goal", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onEditClick) { Text("Edit") }
            }
            Text("₹${"%.0f".format(dailyGoal)} per day", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Spent ₹${"%.0f".format(todaySpent)} today",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CategoryBudgetRow(
    budget: Budget,
    viewModel: BudgetsViewModel,
    onDelete: () -> Unit
) {
    val spent by viewModel.getCategorySpendThisMonth(budget.category).collectAsState(initial = 0.0)
    val progress = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
    val exceeded = spent > budget.monthlyLimit

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(budget.category, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onDelete) { Text("Remove") }
            }
            Text(
                "₹${"%.0f".format(spent)} of ₹${"%.0f".format(budget.monthlyLimit)} this month",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AmountInputDialog(
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

@Composable
private fun AddCategoryBudgetDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    val available = Categories.EXPENSE_CATEGORIES.filter { it !in existingCategories }
    var selectedCategory by remember { mutableStateOf(available.firstOrNull() ?: "") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add category budget") },
        text = {
            Column {
                Text("Selected: $selectedCategory", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                available.forEach { category ->
                    Text(
                        category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = category }
                            .padding(vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue -> amountText = newValue.filter { c -> c.isDigit() } },
                    label = { Text("Monthly limit (₹)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && selectedCategory.isNotBlank()) {
                    onConfirm(selectedCategory, amount)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
