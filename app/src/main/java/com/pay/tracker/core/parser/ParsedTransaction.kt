package com.pay.tracker.core.parser

import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus

/**
 * Intermediate data structure holding extracted fields from payment notifications
 * before converting to a persistent domain [Transaction].
 */
data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val referenceId: String? = null,
    val timestamp: Long,
    val sourceApp: String,
    val rawNotification: String,
    val fingerprint: String,
    val confidence: Float = 1.0f
) {
    /**
     * Converts this parsed transaction into a domain [Transaction].
     */
    fun toTransaction(
        category: String,
        status: TransactionStatus = TransactionStatus.PENDING
    ): Transaction {
        return Transaction(
            id = 0L,
            amount = amount,
            merchant = merchant,
            category = category,
            timestamp = timestamp,
            sourceApp = sourceApp,
            status = status,
            rawNotification = rawNotification,
            referenceId = referenceId,
            fingerprint = fingerprint
        )
    }
}
