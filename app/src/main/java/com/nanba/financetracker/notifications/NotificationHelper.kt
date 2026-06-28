package com.nanba.financetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nanba.financetracker.MainActivity
import com.nanba.financetracker.data.AppDatabase
import com.nanba.financetracker.sms.ParsedSms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_CATEGORIZE = "categorize_channel"
    const val CHANNEL_GOALS = "goals_channel"

    const val ACTION_CATEGORIZE = "com.nanba.financetracker.ACTION_CATEGORIZE"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_DESTINATION_ACCOUNT = "extra_destination_account"
    const val EXTRA_RAW_SMS = "extra_raw_sms"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    private var notificationIdCounter = 1000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val categorizeChannel = NotificationChannel(
                CHANNEL_CATEGORIZE,
                "Categorize Transactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Prompts to tag new SMS-detected expenses with a category"
            }

            val goalsChannel = NotificationChannel(
                CHANNEL_GOALS,
                "Spending Goals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when daily spending goal is exceeded or balance is low"
            }

            manager.createNotificationChannel(categorizeChannel)
            manager.createNotificationChannel(goalsChannel)
        }
    }

    /**
     * Shows a notification asking the user which category this new SMS-detected
     * expense belongs to. Tapping a quick-action button saves the transaction with
     * that category directly -- no need to open the app. Tapping the notification
     * body opens the app's categorize screen with the full category list.
     */
    fun showCategorizePrompt(context: Context, parsed: ParsedSms, sender: String) {
        val amount = parsed.amount ?: return
        val notificationId = notificationIdCounter++

        val manager = context.getSystemService(NotificationManager::class.java)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CATEGORIZE
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DESTINATION_ACCOUNT, parsed.destinationAccount ?: "")
            putExtra(EXTRA_RAW_SMS, parsed.rawBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleSuffix = when {
            parsed.isLikelySelfOrPersonTransfer -> " — looks like a transfer"
            !parsed.destinationAccount.isNullOrBlank() -> " to a/c ${parsed.destinationAccount}"
            else -> ""
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_CATEGORIZE)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("₹${"%.0f".format(amount)} spent$titleSuffix")
            .setContentText("Tap to choose a category")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        // Quick-action buttons (Android caps actions at 3). If this looks like a person
        // transfer, offer "Transfer" as a one-tap option; otherwise show 3 common
        // spending categories. Either way, tapping the notification body opens the
        // full category list in-app.
        val quickCategories = if (parsed.isLikelySelfOrPersonTransfer) {
            listOf("Transfer", "Tea/Snacks", "Groceries")
        } else {
            listOf("Tea/Snacks", "Groceries", "Petrol")
        }
        for (category in quickCategories) {
            val categorizeIntent = Intent(context, CategorizeActionReceiver::class.java).apply {
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_DESTINATION_ACCOUNT, parsed.destinationAccount ?: "")
                putExtra(EXTRA_RAW_SMS, parsed.rawBody)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + category.hashCode(),
                categorizeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, category, actionPendingIntent)
        }

        manager.notify(notificationId, builder.build())
    }

    fun checkLowBalance(context: Context, parsed: ParsedSms) {
        val balance = parsed.availableBalance ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val settings = db.settingsDao().get()
            val threshold = settings?.lowBalanceThreshold ?: 2000.0

            if (balance < threshold) {
                showLowBalanceWarning(context, balance)
            }
        }
    }

    private fun showLowBalanceWarning(context: Context, balance: Double) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Balance low: ₹${"%.0f".format(balance)} left")
            .setContentText("Go easy nanba, spend carefully")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(notificationIdCounter++, builder.build())
    }

    /**
     * Call this after any new expense transaction is saved. Sums today's expenses
     * and fires a notification if the daily goal has been crossed.
     */
    suspend fun checkDailyGoal(context: Context) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val settings = db.settingsDao().get() ?: return@withContext
            val goal = settings.dailyGoal

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000

            val todayTotal = db.transactionDao().getTotalExpenseInRange(startOfDay, endOfDay).first()

            if (todayTotal > goal) {
                showDailyGoalExceeded(context, todayTotal, goal)
            }
        }
    }

    private fun showDailyGoalExceeded(context: Context, spent: Double, goal: Double) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Daily goal exceeded nanba!")
            .setContentText("Spent ₹${"%.0f".format(spent)} today, goal was ₹${"%.0f".format(goal)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Use a fixed ID so repeated crossings today update the same notification
        // instead of spamming a new one for every transaction.
        manager.notify(999, builder.build())
    }
}
