package com.pay.tracker.core.model

enum class TransactionStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}

enum class TransactionDirection {
    DEBIT,  // Money Out
    CREDIT  // Money In
}

enum class TransactionClassification {
    INCOME,    // Salary and genuine income
    EXPENSE,   // Completed purchases and outgoing payments
    REFUND,    // Money returned from earlier purchase
    CASHBACK,  // Promotional cashback and rewards
    TRANSFER   // Transfers between user's own accounts
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
    val fingerprint: String,
    val direction: TransactionDirection = TransactionDirection.DEBIT,
    val classification: TransactionClassification = TransactionClassification.EXPENSE,
    val originalExpenseId: Long? = null
)
