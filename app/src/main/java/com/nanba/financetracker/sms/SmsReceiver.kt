package com.nanba.financetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.nanba.financetracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS broadcasts. Filters by sender ID since banks send from
 * short alphanumeric IDs (e.g. "SCBANK", "KVBSMS", "VM-SCBANK") rather than normal
 * phone numbers. The exact sender IDs for SCB and KVB should be confirmed against
 * real messages and adjusted here if needed -- this list is a reasonable starting
 * guess based on common Indian bank SMS sender naming patterns.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        // Sender ID fragments to match against (case-insensitive, partial match).
        // Update this list once real SCB/KVB sender IDs are confirmed from actual SMS.
        val BANK_SENDER_HINTS = listOf("SCB", "STANCHART", "KVB", "KARURVYSYA")
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in messages) {
            val sender = message.originatingAddress ?: ""
            val body = message.messageBody ?: ""

            val looksLikeBank = BANK_SENDER_HINTS.any { sender.contains(it, ignoreCase = true) }

            // Even if sender doesn't match our hint list, still check if the message
            // content itself looks like a bank debit alert -- this catches cases where
            // the sender ID differs from what we expect.
            if ((looksLikeBank || SmsParser.isLikelyBankSms(body)) && SmsParser.isLikelyBankSms(body)) {
                handleBankSms(context, sender, body)
            }
        }
    }

    private fun handleBankSms(context: Context, sender: String, body: String) {
        val parsed = SmsParser.parse(body)
        if (!parsed.isDebit || parsed.amount == null) return // only care about debits with a found amount

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)

            // Avoid double-processing the exact same SMS body if it somehow fires twice
            val now = System.currentTimeMillis()
            val recentDuplicate = db.transactionDao().countDuplicates(body, now)
            if (recentDuplicate > 0) return@launch

            // Person-to-person/self transfers (e.g. "to MR RAGURAMAN SELVARAJ") aren't
            // retail spending -- still notify so the user can categorize or mark as a
            // transfer, but don't pre-suggest spending categories like Tea/Groceries for these.
            com.nanba.financetracker.notifications.NotificationHelper
                .showCategorizePrompt(context, parsed, sender)

            com.nanba.financetracker.notifications.NotificationHelper
                .checkLowBalance(context, parsed)
        }
    }
}
