package com.pay.tracker.core.parser

import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * Robust parser for Google Pay (UPI) alerts and Indian Bank SMS debit notifications
 * (Axis Bank, HDFC, ICICI, SBI, Google Messages, etc.).
 */
class GPayParser {

    companion object {
        const val GPAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"

        // Pattern 1: UPI URI / Info path:
        // Examples:
        // UPI/P2M/211837387634/ZEPTONOW (Axis Bank SMS)
        // UPI/DR/428918291016/ZOMATO/SBIN (SBI Bank SMS)
        // UPI-428918291018-SWIGGY (ICICI Bank SMS)
        // UPI/428918291019/ZEPTO/Paytm (ICICI Bank SMS)
        // UPI/P2P/123456789012/Rahul
        private val PATTERN_UPI_PATH = Pattern.compile(
            """(?:UPI|Info:\s*UPI)[/_\-:](?:(?:P2M|P2P|REV|DR)[/_\-:])?([A-Za-z0-9]{6,})[/_\-:]([A-Za-z0-9_.\-]+?)(?:[/_\-\s.,]|$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 2: "INR 496.00 debited" or "Rs. 450 debited" or "₹500 debited" or "INR 496.00 has been debited"
        private val PATTERN_AMOUNT_DEBITED = Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:has\s+been\s+|is\s+|was\s+)?debited""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 3: "debited INR 540.00" or "debited by INR 320" or "debited for Rs 1200" or "debited with INR 650"
        private val PATTERN_DEBITED_AMOUNT = Pattern.compile(
            """debited\s*(?:from\s+[^.,\n]+?\s+(?:for|with|by|of|towards)\s*)?(?:by|for|with|of|[:\-])?\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 4: "Spent INR 850.00" or "You have spent INR 1,499.00" or "spent: Rs. 450"
        private val PATTERN_SPENT_AMOUNT = Pattern.compile(
            """(?:spent|spent\s+a\s+total\s+of)\s*(?:on\s+[^.,\n]+?\s+(?:for|with|of)\s*)?(?:of|for|with|[:\-])?\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 5: "INR 850.00 spent"
        private val PATTERN_AMOUNT_SPENT = Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:has\s+been\s+|was\s+)?spent""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 6: "Paid ₹450 to Swiggy..." or "Paid ₹ 1,250.50 to Merchant using..."
        private val PATTERN_PAID_TO = Pattern.compile(
            """(?:paid|sent)\s*(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:to|at|towards)\s*([^.,\n]+?)(?:\s+(?:using|with|from|via|upi|on|for)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 7: "₹450 paid to Swiggy" or "₹ 1,200.00 sent to John"
        private val PATTERN_AMOUNT_PAID_TO = Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:paid|sent|debited)\s*(?:to|at|for|towards)\s*([^.,\n]+?)(?:\s+(?:using|with|from|via|upi|on)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 8: "Payment of ₹450 to Swiggy..."
        private val PATTERN_PAYMENT_OF = Pattern.compile(
            """payment\s+(?:of\s*)?(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:to|at|towards)\s*([^.,\n]+?)(?:\s+(?:using|with|from|via|was)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 9: "You paid ₹500 to Starbucks"
        private val PATTERN_YOU_PAID = Pattern.compile(
            """you\s+paid\s*(?:INR|Rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:to|at|towards)\s*([^.,\n]+?)(?:\s+(?:using|with|from|via)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 10: Bank SMS "to VPA swiggy@icici"
        private val PATTERN_TO_VPA = Pattern.compile(
            """to\s+VPA\s+([^\s.,()]+)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 11: Bank SMS "transfer to SWIGGY" / "by transfer to SWIGGY"
        private val PATTERN_TRANSFER_TO = Pattern.compile(
            """(?:transfer\s+to|by\s+transfer\s+to)\s+([^.,\n]+?)(?:\s+(?:Ref|UPI|Txn|Bal|Avl|on)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 12: Bank SMS "towards Zomato UPI txn ..." / "towards SWIGGY"
        private val PATTERN_TOWARDS = Pattern.compile(
            """towards\s+([^.,\n]+?)(?:\s+(?:UPI|txn|ref|rrn|Bal|Avl|on|\()\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 13: Card spent "at SWIGGY on 15-OCT-26" / "at AMAZON INDIA"
        private val PATTERN_AT_MERCHANT = Pattern.compile(
            """\bat\s+([^.,\n]+?)(?:\s+(?:on\s+\d|avl|limit|bal|using|via)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 13b: "debited ... on FLY IO 02-10-2026"
        private val PATTERN_ON_MERCHANT = Pattern.compile(
            """\bon\s+([A-Za-z0-9_.\- ]+?)\s+\d{1,2}[-/]\d{1,2}""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 14: Bank SMS "debited ... to ZEPTO (UPI Ref ...)"
        private val PATTERN_TO_MERCHANT = Pattern.compile(
            """(?:debited|paid|sent)\s+.*?to\s+([^.,\n]+?)(?:\s+(?:UPI|txn|ref|rrn|Bal|Avl|\()\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 15: "Paid to Starbucks ₹150" or "Sent to Rahul: Rs. 500" or "Payment to Swiggy of ₹350"
        private val PATTERN_PAID_TO_MERCHANT_AMOUNT = Pattern.compile(
            """(?:paid|sent|payment)\s+(?:of\s+)?to\s+([^.,\n:]+?)\s*(?:of|for|with|[:\-])?\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 16: "debited towards Google Cloud for INR 2.00" or "debited for INR 499 towards Netflix"
        private val PATTERN_DEBITED_TOWARDS = Pattern.compile(
            """(?:debited\s+towards|debited\s+from\s+[^.,\n]+?\s+towards|debited\s+for)\s+([^.,\n]+?)\s+(?:for|of|with|[:\-])\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 17: "INR 149.00 auto-debited" or "auto-debited for INR 199.00"
        private val PATTERN_AUTO_DEBITED = Pattern.compile(
            """(?:auto-debited|auto\s+debited|automatically\s+debited)\s*(?:from\s+[^.,\n]+?)?\s*(?:for|of|with|by|[:\-])?\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        private val PATTERN_AMOUNT_AUTO_DEBITED = Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)\s*(?:has\s+been\s+|is\s+|was\s+)?(?:auto-debited|auto\s+debited|automatically\s+debited)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 17b: "Paid ₹120 using HDFC Bank account" or "Sent Rs 500"
        private val PATTERN_PAID_AMOUNT = Pattern.compile(
            """(?:paid|sent)\s*(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Pattern 18: "for Spotify subscription on 15-10-26" or "for Netflix AutoPay"
        private val PATTERN_FOR_MERCHANT = Pattern.compile(
            """\bfor\s+(?!(?:INR|Rs\.?|₹)\b)([^.,\n]+?)(?:\s+(?:subscription|AutoPay|on\s+\d|avl|limit|bal|using|via)\b|\s*\.|\s*$)""",
            Pattern.CASE_INSENSITIVE
        )

        // UPI Reference ID pattern: "UPI Ref: 123456789012" or "UPI Ref No 123456789012" or "Ref No 123456789012"
        private val PATTERN_UPI_REF = Pattern.compile(
            """(?:upi\s*ref(?:erence)?(?:\s*no\.?)?|ref\s*no\.?|\bref\b|txn\s*id|upi\s*txn(?:\s*id)?|rrn)\s*[:\-]?\s*([A-Za-z0-9]{6,})""",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Checks if notification text represents an actual, completed debit/payment.
     * Rejects:
     * - Mandate / AutoPay lifecycle management (creation, activation, cancellation, revocation, modification)
     * - Subscription renewal reminders, scheduled payment alerts, upcoming auto-debit notices
     * - Failed, declined, timed out, reversed, or pending authorization transactions
     * - Credits, refunds, cashbacks, incoming transfers
     * - OTPs, money requests, credit card bill/statement generation alerts
     * - Telco recharge confirmations and promotional messages
     *
     * Requires explicit positive evidence of an actual completed debit.
     */
    fun isDebitNotification(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()

        // 1. Mandatory exclusion: OTPs, verification codes, money requests
        val requestOrSecurityKeywords = listOf(
            "otp", "verification code", "security code", "one time password",
            "requested money", "request from", "request for inr", "request for rs",
            "has requested", "request received", "collect request"
        )
        if (requestOrSecurityKeywords.any { combined.contains(it) }) {
            return false
        }

        // 2. Mandatory exclusion: Failed, declined, cancelled, reversed, or timed out transactions
        val failureOrReversalPatterns = listOf(
            Regex("""\b(?:failed|declined|unsuccessful|reversed|reversal|bounced|rejected)\b"""),
            Regex("""\b(?:payment|transaction|txn|transfer)\s+(?:failed|declined|unsuccessful|cancelled|canceled)\b"""),
            Regex("""\b(?:unable\s+to\s+process|could\s+not\s+be\s+processed|timed\s+out|expired)\b""")
        )
        if (failureOrReversalPatterns.any { it.containsMatchIn(combined) }) {
            return false
        }

        // 3. Mandatory exclusion: Mandate / AutoPay / Standing Instruction lifecycle & subscription management
        // (Creation, setup, revocation, cancellation, modification, renewal reminders, upcoming payment alerts)
        val mandateLifecyclePatterns = listOf(
            // "mandate has been successfully created/revoked", "mandate cancelled", "mandate registered"
            Regex("""(?:mandate|e-mandate|emandate|standing\s+instruction|autopay)\s+.*?\b(?:created|revoked|cancelled|canceled|modified|registered|paused|scheduled|initiated|setup|set\s+up|deleted|expired)\b"""),
            Regex("""\b(?:created|revoked|cancelled|canceled|modified|registered|paused|scheduled|setup|set\s+up)\b.*?\b(?:mandate|e-mandate|emandate|standing\s+instruction|autopay)\b"""),
            // Specific terms like "aspresented mandate", "upi mandate has been", "autopay mandate created"
            Regex("""(?:aspresented\s+mandate|upi\s+mandate\s+has\s+been|e-mandate\s+registered|autopay\s+mandate\b)"""),
            // Subscription management / renewal reminders
            Regex("""\b(?:subscription\s+created|subscription\s+cancelled|subscription\s+canceled|subscription\s+activated)\b"""),
            Regex("""\b(?:renewal\s+reminder|renew\s+your\s+subscription|subscription\s+renewal|upcoming\s+(?:payment|debit|autopay))\b"""),
            Regex("""\b(?:scheduled\s+for\s+(?:auto-debit|debit|payment)|scheduled\s+to\s+be\s+debited|due\s+for\s+auto-debit)\b"""),
            Regex("""\b(?:mandate\s+request|autopay\s+request)\b""")
        )
        if (mandateLifecyclePatterns.any { it.containsMatchIn(combined) }) {
            return false
        }

        // 4. Mandatory exclusion: Credits, deposits, refunds, cashbacks (income / inward transfers)
        val creditPatterns = listOf(
            Regex("""\b(?:credited\s+to|credited\s+with|has\s+been\s+credited|is\s+credited|was\s+credited)\b"""),
            Regex("""\b(?:refund|refunded|cashback\s+received|cashback\s+credited|amount\s+deposited)\b"""),
            Regex("""\b(?:received\s+(?:inr|rs\.?|₹)?\s*[\d,]+|credited\s+(?:inr|rs\.?|₹)?\s*[\d,]+)\b"""),
            Regex("""\b(?:credited\s+(?:for|on|in|towards)\b)"""),
            Regex("""\bINR\s*[\d,]+(?:\.\d{1,2})?\s*credited\b""", RegexOption.IGNORE_CASE)
        )
        if (creditPatterns.any { it.containsMatchIn(combined) }) {
            return false
        }

        // 5. Mandatory exclusion: Bill generation & due notices (not an actual debit yet)
        val billReminderPatterns = listOf(
            Regex("""\b(?:bill\s+generated|statement\s+generated|payment\s+due|amount\s+due|minimum\s+due|total\s+due)\b"""),
            Regex("""\b(?:bill\s+is\s+due|payment\s+is\s+due|bill\s+amount\s+is)\b""")
        )
        if (billReminderPatterns.any { it.containsMatchIn(combined) }) {
            return false
        }

        // 6. Mandatory exclusion: Telco duplicate recharge confirmations (bank debit SMS is already captured)
        val telcoDuplicatePatterns = listOf(
            Regex("""\b(?:recharge\s+of\s+(?:inr|rs\.?|₹)|prepaid\s+bill\s+payment\s+of)\b"""),
            Regex("""\b(?:recharge\s+successful|recharge\s+of\s+rs\.?|recharge\s+of\s+inr)\b""")
        )
        if (telcoDuplicatePatterns.any { it.containsMatchIn(combined) }) {
            return false
        }

        // 7. Positive requirement: Must have explicit evidence of an actual completed debit/expense
        val completedDebitPatterns = listOf(
            Regex("""\b(?:has\s+been\s+|is\s+|was\s+)?debited\b"""),
            Regex("""\b(?:auto-debited|auto\s+debited|automatically\s+debited)\b"""),
            Regex("""\b(?:spent|spent\s+a\s+total\s+of)\b"""),
            Regex("""\b(?:you\s+paid|paid\s+(?:inr|rs\.?|₹)?\s*[\d,]+|paid\s+to)\b"""),
            Regex("""\b(?:payment\s+of\s+(?:inr|rs\.?|₹)?\s*[\d,]+|payment\s+to\s+[^.,\n]+?\s+of)\b"""),
            Regex("""\b(?:sent\s+(?:inr|rs\.?|₹)?\s*[\d,]+|sent\s+to)\b"""),
            Regex("""\b(?:withdrawn|withdrew)\b""")
        )
        return completedDebitPatterns.any { it.containsMatchIn(combined) }
    }

    /**
     * Parses notification title, text, and metadata into a [ParsedTransaction].
     * Returns null if no valid payment pattern matches or if the message is not an actual completed debit.
     */
    fun parse(
        title: String,
        text: String,
        postTime: Long = System.currentTimeMillis(),
        packageName: String = GPAY_PACKAGE
    ): ParsedTransaction? {
        val fullContent = "$title\n$text"

        if (!isDebitNotification(title, text)) {
            return null
        }

        var amount: Double? = null
        var merchant: String? = null
        var referenceId: String? = null

        // Strategy 1: Check Indian Bank SMS format: UPI/P2M/<ref>/<merchant> or UPI-428918291018-SWIGGY
        val upiPathMatcher = PATTERN_UPI_PATH.matcher(fullContent)
        if (upiPathMatcher.find()) {
            referenceId = upiPathMatcher.group(1)
            merchant = cleanMerchant(upiPathMatcher.group(2))
            amount = extractAmount(fullContent)
        }

        // Strategy 2: "Debited towards <merchant> for INR <amount>" (e.g. Axis Bank subscription/mandate debits)
        if (amount == null || merchant.isNullOrBlank()) {
            val mDebTowards = PATTERN_DEBITED_TOWARDS.matcher(fullContent)
            if (mDebTowards.find()) {
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(mDebTowards.group(1))
                if (amount == null) amount = parseAmount(mDebTowards.group(2))
            }
        }

        // Strategy 3: Standard Google Pay notification patterns
        if (amount == null || merchant.isNullOrBlank()) {
            val m1 = PATTERN_PAID_TO.matcher(fullContent)
            if (m1.find()) {
                if (amount == null) amount = parseAmount(m1.group(1))
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(m1.group(2))
            }
        }

        if (amount == null || merchant.isNullOrBlank()) {
            val m2 = PATTERN_AMOUNT_PAID_TO.matcher(fullContent)
            if (m2.find()) {
                if (amount == null) amount = parseAmount(m2.group(1))
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(m2.group(2))
            }
        }

        // Strategy 3b: "Paid to <merchant> ₹<amount>" / "Payment to <merchant> of ₹<amount>"
        if (amount == null || merchant.isNullOrBlank()) {
            val mPaidTo = PATTERN_PAID_TO_MERCHANT_AMOUNT.matcher(fullContent)
            if (mPaidTo.find()) {
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(mPaidTo.group(1))
                if (amount == null) amount = parseAmount(mPaidTo.group(2))
            }
        }

        if (amount == null || merchant.isNullOrBlank()) {
            val m3 = PATTERN_PAYMENT_OF.matcher(fullContent)
            if (m3.find()) {
                if (amount == null) amount = parseAmount(m3.group(1))
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(m3.group(2))
            }
        }

        if (amount == null || merchant.isNullOrBlank()) {
            val m4 = PATTERN_YOU_PAID.matcher(fullContent)
            if (m4.find()) {
                if (amount == null) amount = parseAmount(m4.group(1))
                if (merchant.isNullOrBlank()) merchant = cleanMerchant(m4.group(2))
            }
        }

        // Strategy 4: Bank SMS recipient / merchant patterns (HDFC, SBI, ICICI, etc.)
        if (merchant.isNullOrBlank()) {
            val vpaMatcher = PATTERN_TO_VPA.matcher(fullContent)
            if (vpaMatcher.find()) {
                merchant = cleanMerchant(vpaMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val transferMatcher = PATTERN_TRANSFER_TO.matcher(fullContent)
            if (transferMatcher.find()) {
                merchant = cleanMerchant(transferMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val towardsMatcher = PATTERN_TOWARDS.matcher(fullContent)
            if (towardsMatcher.find()) {
                merchant = cleanMerchant(towardsMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val atMatcher = PATTERN_AT_MERCHANT.matcher(fullContent)
            if (atMatcher.find()) {
                merchant = cleanMerchant(atMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val onMatcher = PATTERN_ON_MERCHANT.matcher(fullContent)
            if (onMatcher.find()) {
                merchant = cleanMerchant(onMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val toMatcher = PATTERN_TO_MERCHANT.matcher(fullContent)
            if (toMatcher.find()) {
                merchant = cleanMerchant(toMatcher.group(1))
            }
        }

        if (merchant.isNullOrBlank()) {
            val forMatcher = PATTERN_FOR_MERCHANT.matcher(fullContent)
            if (forMatcher.find()) {
                merchant = cleanMerchant(forMatcher.group(1))
            }
        }

        // Strategy 5: Ensure amount is strictly extracted via completed debit/spent patterns
        if (amount == null) {
            amount = extractAmount(fullContent)
        }

        // NOTE: We deliberately DO NOT fall back to generic standalone amount patterns (like PATTERN_GENERIC_AMOUNT).
        // This strictly prevents inferring expenses from account balances, credit limits, or promo numbers.
        if (amount == null || amount <= 0.0) {
            return null
        }

        // Strategy 6: Fallback merchant extraction if title is merchant name
        if (merchant.isNullOrBlank()) {
            val genericTitles = setOf("google pay", "phonepe", "paytm", "cred", "bhim", "gpay", "payment", "bank", "upi", "messages", "sms", "alert")
            val cleanTitle = cleanMerchant(title)
            val titleHasAmt = Regex("""(?:INR|Rs\.?|₹)\s*[\d,]+""", RegexOption.IGNORE_CASE).containsMatchIn(title)
            if (!cleanTitle.isNullOrBlank() && !titleHasAmt && cleanTitle.lowercase() !in genericTitles) {
                merchant = cleanTitle
            } else {
                merchant = "UPI Merchant"
            }
        }

        // Extract UPI reference if not yet extracted
        if (referenceId == null) {
            val refMatcher = PATTERN_UPI_REF.matcher(fullContent)
            if (refMatcher.find()) {
                referenceId = refMatcher.group(1)
            }
        }

        val hasExplicitMerchant = !merchant.isNullOrBlank() && merchant != "UPI Merchant"
        val hasRefId = !referenceId.isNullOrBlank()

        val confidence = when {
            hasRefId && hasExplicitMerchant -> 0.98f
            hasExplicitMerchant -> 0.90f
            hasRefId -> 0.85f
            else -> 0.70f // Uncertain merchant without ref ID
        }

        val fingerprint = generateFingerprint(amount, merchant, postTime, referenceId)

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            referenceId = referenceId,
            timestamp = postTime,
            sourceApp = packageName,
            rawNotification = fullContent.trim(),
            fingerprint = fingerprint,
            confidence = confidence
        )
    }

    private fun extractAmount(content: String): Double? {
        val m1 = PATTERN_AMOUNT_DEBITED.matcher(content)
        if (m1.find()) {
            val amt = parseAmount(m1.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val mAuto1 = PATTERN_AMOUNT_AUTO_DEBITED.matcher(content)
        if (mAuto1.find()) {
            val amt = parseAmount(mAuto1.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val m2 = PATTERN_DEBITED_AMOUNT.matcher(content)
        if (m2.find()) {
            val amt = parseAmount(m2.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val mAuto2 = PATTERN_AUTO_DEBITED.matcher(content)
        if (mAuto2.find()) {
            val amt = parseAmount(mAuto2.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val mDebTowards = PATTERN_DEBITED_TOWARDS.matcher(content)
        if (mDebTowards.find()) {
            val amt = parseAmount(mDebTowards.group(2))
            if (amt != null && amt > 0.0) return amt
        }

        val mPaid = PATTERN_PAID_AMOUNT.matcher(content)
        if (mPaid.find()) {
            val amt = parseAmount(mPaid.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val m3 = PATTERN_SPENT_AMOUNT.matcher(content)
        if (m3.find()) {
            val amt = parseAmount(m3.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val m4 = PATTERN_AMOUNT_SPENT.matcher(content)
        if (m4.find()) {
            val amt = parseAmount(m4.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        return null
    }

    private fun parseAmount(amountStr: String?): Double? {
        if (amountStr == null) return null
        return try {
            val cleaned = amountStr.replace(",", "").trim()
            cleaned.toDouble()
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanMerchant(rawMerchant: String?): String? {
        if (rawMerchant == null) return null
        var cleaned = rawMerchant.trim()

        // Ignore if merchant is a customer service number or SMS block instructions
        if (cleaned.startsWith("+91") || cleaned.matches(Regex("""^\+?\d{8,15}.*""")) || cleaned.contains("BLOCK", ignoreCase = true)) {
            return null
        }

        // 1. Remove VPA prefix if present (e.g. "VPA swiggy@icici" -> "swiggy@icici")
        cleaned = cleaned.replace(Regex("""^VPA\s+""", RegexOption.IGNORE_CASE), "")

        // 2. If it's a UPI handle (e.g. "swiggy@icici" or "zeptonow@axisbank"), strip handle
        if (cleaned.contains("@")) {
            cleaned = cleaned.substringBefore("@").trim()
        }

        // 3. Strip leading prepositions
        cleaned = cleaned
            .replace(Regex("""^(using|with|via|to|at|towards|for|transfer\s+to|by\s+transfer\s+to)\s+""", RegexOption.IGNORE_CASE), "")
            .trim()

        // 4. Strip trailing bank/account metadata
        cleaned = cleaned
            .replace(Regex("""\s+(using|with|via)\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+a/c\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+acct\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+account\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+card\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+ending\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:on|dated)\s+\d{1,2}[-/]\d{1,2}.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:upi|ref|txn|rrn|info|bal|avl|avail|balance)\b.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\([^)]*\)$"""), "")
            .trim()

        // Remove trailing punctuation
        while (cleaned.endsWith(".") || cleaned.endsWith(",") || cleaned.endsWith("-") || cleaned.endsWith(":")) {
            cleaned = cleaned.dropLast(1).trim()
        }

        // Normalize common Indian UPI merchant tags
        if (cleaned.equals("ZEPTONOW", ignoreCase = true) || cleaned.startsWith("ZEPTO", ignoreCase = true)) {
            return "Zepto"
        }
        if (cleaned.startsWith("SWIGGY", ignoreCase = true) || cleaned.equals("BUNDL TECHNOLOGIES", ignoreCase = true)) {
            return "Swiggy"
        }
        if (cleaned.startsWith("ZOMATO", ignoreCase = true)) {
            return "Zomato"
        }
        if (cleaned.startsWith("BLINKIT", ignoreCase = true) || cleaned.startsWith("GROFERS", ignoreCase = true)) {
            return "Blinkit"
        }
        if (cleaned.startsWith("AMAZON", ignoreCase = true) || cleaned.startsWith("AMZN", ignoreCase = true)) {
            return "Amazon"
        }
        if (cleaned.startsWith("FLIPKART", ignoreCase = true)) {
            return "Flipkart"
        }
        if (cleaned.startsWith("MYNTRA", ignoreCase = true)) {
            return "Myntra"
        }
        if (cleaned.startsWith("UBER", ignoreCase = true)) {
            return "Uber"
        }
        if (cleaned.startsWith("OLA", ignoreCase = true) || cleaned.startsWith("ANI TECH", ignoreCase = true)) {
            return "Ola"
        }
        if (cleaned.startsWith("BIGBASKET", ignoreCase = true) || cleaned.startsWith("BIG BASKET", ignoreCase = true)) {
            return "BigBasket"
        }
        if (cleaned.startsWith("STARBUCKS", ignoreCase = true)) {
            return "Starbucks"
        }
        if (cleaned.startsWith("MCDONALD", ignoreCase = true) || cleaned.startsWith("MC DONALD", ignoreCase = true)) {
            return "McDonald's"
        }
        if (cleaned.startsWith("KFC", ignoreCase = true)) {
            return "KFC"
        }
        if (cleaned.startsWith("DOMINO", ignoreCase = true)) {
            return "Domino's"
        }
        if (cleaned.startsWith("BOOKMYSHOW", ignoreCase = true)) {
            return "BookMyShow"
        }
        if (cleaned.startsWith("APOLLO", ignoreCase = true)) {
            return "Apollo Pharmacy"
        }
        if (cleaned.startsWith("NETFLIX", ignoreCase = true)) {
            return "Netflix"
        }
        if (cleaned.startsWith("SPOTIFY", ignoreCase = true)) {
            return "Spotify"
        }
        if (cleaned.startsWith("HOTSTAR", ignoreCase = true)) {
            return "Hotstar"
        }
        if (cleaned.startsWith("BESCOM", ignoreCase = true)) {
            return "BESCOM Electricity"
        }
        if (cleaned.startsWith("IRCTC", ignoreCase = true)) {
            return "IRCTC"
        }
        if (cleaned.startsWith("RAPIDO", ignoreCase = true)) {
            return "Rapido"
        }

        // If all characters are uppercase, convert to Title Case for better readability
        if (cleaned.isNotEmpty() && cleaned.none { it.isLowerCase() }) {
            cleaned = toTitleCase(cleaned)
        }

        return cleaned.ifBlank { null }
    }

    private fun toTitleCase(input: String): String {
        return input.split(Regex("""\s+""")).joinToString(" ") { word ->
            if (word.length <= 3 && word.all { it.isLetter() }) {
                word.uppercase()
            } else {
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private fun generateFingerprint(
        amount: Double,
        merchant: String,
        timestamp: Long,
        referenceId: String?
    ): String {
        // If reference ID exists, it is the most reliable deduplication key
        if (!referenceId.isNullOrBlank()) {
            return "ref:$referenceId"
        }
        // Fallback: amount + merchant + rounded timestamp (within 2-minute bucket to handle re-deliveries)
        val timeBucket = timestamp / (1000 * 60 * 2)
        val rawKey = "${"%.2f".format(amount)}_${merchant.lowercase()}_$timeBucket"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(rawKey.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }
}
