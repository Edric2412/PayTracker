package com.pay.tracker.core.database

import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionClassification
import com.pay.tracker.core.model.TransactionDirection
import com.pay.tracker.core.model.TransactionStatus

/**
 * Clean Architecture mappers between Room Entity and Pure Kotlin Domain Model.
 */
fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        timestamp = timestamp,
        sourceApp = sourceApp,
        status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.PENDING),
        rawNotification = rawNotification,
        referenceId = referenceId,
        fingerprint = fingerprint,
        direction = runCatching { TransactionDirection.valueOf(direction) }.getOrDefault(TransactionDirection.DEBIT),
        classification = runCatching { TransactionClassification.valueOf(classification) }.getOrDefault(TransactionClassification.EXPENSE),
        originalExpenseId = originalExpenseId
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        timestamp = timestamp,
        sourceApp = sourceApp,
        status = status.name,
        rawNotification = rawNotification,
        referenceId = referenceId,
        fingerprint = fingerprint,
        direction = direction.name,
        classification = classification.name,
        originalExpenseId = originalExpenseId
    )
}
