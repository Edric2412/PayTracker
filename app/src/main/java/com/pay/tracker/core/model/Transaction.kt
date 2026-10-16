package com.pay.tracker.core.model

enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

/**
 * Domain model representing a financial transaction captured from notifications
 * or manually created.
 */
data class Transaction(
    val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val sourceApp: String,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val rawNotification: String,
    val referenceId: String? = null,
    val fingerprint: String
)
