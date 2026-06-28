package com.nanba.financetracker.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nanba.financetracker.data.AppDatabase
import com.nanba.financetracker.data.CategoryRule
import com.nanba.financetracker.data.Transaction
import com.nanba.financetracker.data.TransactionSource
import com.nanba.financetracker.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired when the user taps a quick-category action button directly on the
 * SMS-categorize notification. Saves the transaction immediately with the chosen
 * category, and remembers the destinationAccount -> category mapping for next time
 * (real bank SMS only contain account numbers, not merchant names -- see SmsParser).
 */
class CategorizeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val amount = intent.getDoubleExtra(NotificationHelper.EXTRA_AMOUNT, -1.0)
        val destinationAccount = intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION_ACCOUNT) ?: ""
        val rawSms = intent.getStringExtra(NotificationHelper.EXTRA_RAW_SMS) ?: ""
        val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: return

        if (amount < 0) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)

            db.transactionDao().insert(
                Transaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    category = category,
                    note = "",
                    dateMillis = System.currentTimeMillis(),
                    source = TransactionSource.SMS,
                    destinationAccount = destinationAccount,
                    rawSmsBody = rawSms
                )
            )

            // Remember this destination account -> category mapping for future
            // auto-suggestions (only meaningful if we actually captured an account number;
            // person-transfers tagged as "Transfer" won't have one).
            if (destinationAccount.isNotBlank()) {
                db.categoryRuleDao().insert(
                    CategoryRule(
                        destinationAccount = destinationAccount,
                        category = category
                    )
                )
            }

            NotificationHelper.checkDailyGoal(context)

            // Dismiss the categorize-prompt notification now that it's been handled.
            val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.cancel(notificationId)
            }
        }
    }
}
