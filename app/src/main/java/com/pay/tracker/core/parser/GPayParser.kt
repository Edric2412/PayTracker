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

        // Pattern 16: Generic INR / Rs. / ₹ amount anywhere in text
        private val PATTERN_GENERIC_AMOUNT = Pattern.compile(
            """(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )

        // UPI Reference ID pattern: "UPI Ref: 123456789012" or "UPI Ref No 123456789012" or "Ref No 123456789012"
        private val PATTERN_UPI_REF = Pattern.compile(
            """(?:upi\s*ref(?:erence)?(?:\s*no\.?)?|ref\s*no\.?|\bref\b|txn\s*id|upi\s*txn(?:\s*id)?|rrn)\s*[:\-]?\s*([A-Za-z0-9]{6,})""",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Checks if notification text represents a completed debit/payment.
     * Rejects OTPs, cashbacks received, money requests, and failed transactions.
     */
    fun isDebitNotification(title: String, text: String): Boolean {
        val combined = "$title $text".lowercase()

        // Ignore OTP messages, money requests, or received money
        val ignoreKeywords = listOf(
            "otp", "verification code", "requested money", "request from",
            "received ₹", "received rs", "received inr",
            "has credited", "failed", "declined"
        )
        if (ignoreKeywords.any { combined.contains(it) }) {
            return false
        }

        val isCreditOnly = Regex("""(?:credited\s+to|credited\s+with)\s+(?:your\s+)?(?:a/c|acct|account)""").containsMatchIn(combined)
        val debitKeywords = listOf(
            "paid", "sent", "debited", "payment to", "payment of", "you paid", "spent",
            "payment successful", "successful payment", "transferred", "auto-debited",
            "debited from", "paid to", "sent to", "withdrawn", "txn", "transaction", "upi"
        )
        val hasDebit = debitKeywords.any { combined.contains(it) }

        if (isCreditOnly && !hasDebit) {
            return false
        }

        return hasDebit
    }

    /**
     * Parses notification title, text, and metadata into a [ParsedTransaction].
     * Returns null if no valid payment pattern matches.
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

            // Look for amount in "INR 496.00 debited" / "debited INR 540.00" / "spent INR 850.00"
            amount = extractAmount(fullContent)
        }

        // Strategy 2: Standard Google Pay notification patterns
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

        // Strategy 2b: "Paid to <merchant> ₹<amount>" / "Payment to <merchant> of ₹<amount>"
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

        // Strategy 3: Bank SMS recipient / merchant patterns (HDFC, SBI, ICICI, etc.)
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
            val toMatcher = PATTERN_TO_MERCHANT.matcher(fullContent)
            if (toMatcher.find()) {
                merchant = cleanMerchant(toMatcher.group(1))
            }
        }

        // Strategy 4: Fallback amount extraction
        if (amount == null) {
            amount = extractAmount(fullContent)
        }
        if (amount == null) {
            val genericAmtMatcher = PATTERN_GENERIC_AMOUNT.matcher(fullContent)
            if (genericAmtMatcher.find()) {
                amount = parseAmount(genericAmtMatcher.group(1))
            }
        }

        if (amount == null || amount <= 0.0) {
            return null
        }

        // Strategy 5: Fallback merchant extraction if title is merchant name
        if (merchant.isNullOrBlank()) {
            val genericTitles = setOf("google pay", "phonepe", "paytm", "cred", "bhim", "gpay", "payment", "bank", "upi", "messages", "sms", "alert")
            val cleanTitle = cleanMerchant(title)
            val titleHasAmt = PATTERN_GENERIC_AMOUNT.matcher(title).find()
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

        val fingerprint = generateFingerprint(amount, merchant, postTime, referenceId)

        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            referenceId = referenceId,
            timestamp = postTime,
            sourceApp = packageName,
            rawNotification = fullContent.trim(),
            fingerprint = fingerprint,
            confidence = if (referenceId != null) 0.98f else 0.85f
        )
    }

    private fun extractAmount(content: String): Double? {
        val m1 = PATTERN_AMOUNT_DEBITED.matcher(content)
        if (m1.find()) {
            val amt = parseAmount(m1.group(1))
            if (amt != null && amt > 0.0) return amt
        }

        val m2 = PATTERN_DEBITED_AMOUNT.matcher(content)
        if (m2.find()) {
            val amt = parseAmount(m2.group(1))
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
