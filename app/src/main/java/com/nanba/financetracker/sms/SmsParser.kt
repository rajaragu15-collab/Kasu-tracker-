package com.nanba.financetracker.sms

/**
 * Parses bank debit-alert SMS text into structured data.
 *
 * Built and verified against REAL sample messages from the user's SCB and KVB accounts
 * (Nov 2025). Both banks use the same underlying UPI/NPCI message template:
 *
 *   KVB:  "Your a/c XXXXXXXXXXXX7628 is debited Rs. 9000.00 on 19-Oct-2025 to
 *          MR RAGURAMAN SELVARAJ info :P2A/529245350228. Not You? call 18005721916-KVB"
 *
 *   SCB:  "Your a/c XX4203 is debited for Rs. 142.00 on 03-11-2025 18:35 and
 *          credited to a/c XX0051 (UPI Ref no 530767743863).Plz call 18002586465
 *          if not done by you."
 *
 * KEY FINDINGS from real samples (confirmed with the user):
 * - Both words appear in the SAME single transaction: "debited" describes money leaving
 *   the user's own account, and "credited to a/c XXXX" describes the receiver's account
 *   in that same transfer. This is NOT two separate transactions, and the presence of
 *   "credited" elsewhere in the message does not mean it's an incoming payment. The
 *   reliable anchor is "Your a/c ... is debited" appearing at the start of the message.
 * - NEITHER bank includes an available-balance figure in these messages. The low-balance
 *   warning feature can only work if some other SMS format includes it -- for now it
 *   will simply not trigger for these message types, which is expected/normal, not a bug.
 * - NEITHER bank includes a shop/merchant name -- only a destination account number
 *   (e.g. "a/c XX0051") or occasionally a person's name (for P2A/person transfers).
 *   So category-matching is done by destination account number, not merchant text.
 * - Customer-care callback numbers vary between messages and cannot be used to identify
 *   which bank sent the SMS -- bank identity should come from the SMS sender ID instead.
 *
 * The debug screen in Settings shows raw SMS next to parsed output so future format
 * mismatches (e.g. a genuine shop-name SMS, if one ever appears) can be caught and the
 * patterns below adjusted accordingly.
 */

data class ParsedSms(
    val amount: Double?,
    val isDebit: Boolean,
    val destinationAccount: String?, // e.g. "XX0051" -- used as the category-matching key
    val payeeName: String?, // e.g. "MR RAGURAMAN SELVARAJ" if a person's name was present
    val isLikelySelfOrPersonTransfer: Boolean,
    val availableBalance: Double?, // usually null -- neither bank includes this so far
    val rawBody: String
)

object SmsParser {

    // Matches amounts like "Rs. 9000.00", "Rs.250", "Rs 250.00"
    private val amountRegex = Regex(
        """Rs\.?\s?([0-9][0-9,]*\.?[0-9]{0,2})""",
        RegexOption.IGNORE_CASE
    )

    // The reliable anchor phrase confirming this is a debit from the user's account.
    // Both real samples start with "Your a/c ... is debited"
    private val debitAnchorRegex = Regex(
        """your a/?c\s*[X0-9]+\s+is\s+debited""",
        RegexOption.IGNORE_CASE
    )

    // Captures destination account number, e.g. "credited to a/c XX0051"
    private val destinationAccountRegex = Regex(
        """credited to a/?c\s*([X0-9]+)""",
        RegexOption.IGNORE_CASE
    )

    // Captures a person's name after "to", when it's NOT followed by "a/c"
    // e.g. "debited Rs. 9000.00 on 19-Oct-2025 to MR RAGURAMAN SELVARAJ info"
    private val payeeNameRegex = Regex(
        """\bto\s+((?:MR|MRS|MS|DR)\.?\s+[A-Z][A-Z ]{2,40}?)\s+info\b""",
        RegexOption.IGNORE_CASE
    )

    // Kept in case a future SMS format does include a balance figure
    private val balanceRegex = Regex(
        """(?:Avl|Avail|Available)\.?\s*Bal(?:ance)?\.?:?\s?(?:Rs\.?|INR|₹)?\s?([0-9][0-9,]*\.?[0-9]{0,2})""",
        RegexOption.IGNORE_CASE
    )

    fun isLikelyBankSms(body: String): Boolean {
        return debitAnchorRegex.containsMatchIn(body) && amountRegex.containsMatchIn(body)
    }

    fun parse(body: String): ParsedSms {
        val isDebit = debitAnchorRegex.containsMatchIn(body)

        val amount = amountRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        val destinationAccount = destinationAccountRegex.find(body)?.groupValues?.get(1)?.trim()

        val payeeName = payeeNameRegex.find(body)?.groupValues?.get(1)?.trim()

        // If a person's name (MR/MRS/MS/DR ...) is present instead of a destination
        // account, treat it as a likely person-to-person or self transfer rather than
        // a retail purchase -- these shouldn't be force-categorized into spending buckets.
        val isLikelyPersonTransfer = payeeName != null

        val balance = balanceRegex.find(body)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()

        return ParsedSms(
            amount = amount,
            isDebit = isDebit,
            destinationAccount = destinationAccount,
            payeeName = payeeName,
            isLikelySelfOrPersonTransfer = isLikelyPersonTransfer,
            availableBalance = balance,
            rawBody = body
        )
    }
}
