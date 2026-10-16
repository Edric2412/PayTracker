package com.pay.tracker.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisted transactions.
 * Indexed on referenceId, fingerprint, direction, classification, status, and timestamp.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["fingerprint"], unique = true),
        Index(value = ["referenceId"]),
        Index(value = ["status"]),
        Index(value = ["timestamp"]),
        Index(value = ["direction"]),
        Index(value = ["classification"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val sourceApp: String,
    val status: String, // PENDING, CONFIRMED, REJECTED
    val rawNotification: String,
    val referenceId: String?,
    val fingerprint: String,
    val direction: String = "DEBIT", // DEBIT, CREDIT
    val classification: String = "EXPENSE", // INCOME, EXPENSE, REFUND, CASHBACK, TRANSFER
    val originalExpenseId: Long? = null
)
