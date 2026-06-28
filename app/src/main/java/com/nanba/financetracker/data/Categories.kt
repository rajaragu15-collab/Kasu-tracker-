package com.nanba.financetracker.data

/**
 * Central list of expense categories. Keep this as the single source of truth —
 * UI dropdowns, SMS auto-categorization suggestions, and budget screens all read from here.
 */
object Categories {
    const val UNCATEGORIZED = "Uncategorized"

    val EXPENSE_CATEGORIES = listOf(
        "Groceries",
        "Tea/Snacks",
        "Cigarettes",
        "Food/Dining",
        "Transport",
        "Petrol",
        "Travel",
        "EB Bills",
        "House Rent",
        "Bills",
        "Shopping",
        "Entertainment",
        "Transfer",
        "Other"
    )

    val INCOME_CATEGORIES = listOf(
        "Salary",
        "Freelance",
        "Gift",
        "Refund",
        "Other Income"
    )
}
