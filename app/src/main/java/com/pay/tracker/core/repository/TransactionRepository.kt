package com.pay.tracker.core.repository

import com.pay.tracker.core.database.TransactionDao
import com.pay.tracker.core.database.toDomain
import com.pay.tracker.core.database.toEntity
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TransactionRepository {
    fun observeAllTransactions(): Flow<List<Transaction>>
    fun observePendingTransactions(): Flow<List<Transaction>>
    fun observeConfirmedTransactions(): Flow<List<Transaction>>
    fun observeTotalSpentSince(startEpoch: Long): Flow<Double>

    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun saveTransaction(transaction: Transaction): Long
    suspend fun confirmTransaction(id: Long, category: String? = null)
    suspend fun rejectTransaction(id: Long)
    suspend fun updateCategory(id: Long, newCategory: String)
    suspend fun deleteTransaction(id: Long)
    suspend fun clearAllTransactions()
    suspend fun isDuplicate(fingerprint: String, referenceId: String?): Boolean
}

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun observeAllTransactions(): Flow<List<Transaction>> {
        return dao.observeAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun observePendingTransactions(): Flow<List<Transaction>> {
        return dao.observePendingTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun observeConfirmedTransactions(): Flow<List<Transaction>> {
        return dao.observeTransactionsByStatus(TransactionStatus.CONFIRMED.name)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeTotalSpentSince(startEpoch: Long): Flow<Double> {
        return dao.observeTotalSpentSince(startEpoch).map { it ?: 0.0 }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return dao.getTransactionById(id)?.toDomain()
    }

    override suspend fun saveTransaction(transaction: Transaction): Long {
        return dao.insertTransaction(transaction.toEntity())
    }

    override suspend fun confirmTransaction(id: Long, category: String?) {
        val current = dao.getTransactionById(id) ?: return
        val finalCategory = category ?: current.category
        dao.updateStatusAndCategory(id, TransactionStatus.CONFIRMED.name, finalCategory)
    }

    override suspend fun rejectTransaction(id: Long) {
        dao.updateStatus(id, TransactionStatus.REJECTED.name)
    }

    override suspend fun updateCategory(id: Long, newCategory: String) {
        val current = dao.getTransactionById(id) ?: return
        dao.updateStatusAndCategory(id, current.status, newCategory)
    }

    override suspend fun deleteTransaction(id: Long) {
        dao.deleteTransaction(id)
    }

    override suspend fun clearAllTransactions() {
        dao.deleteAllTransactions()
    }

    override suspend fun isDuplicate(fingerprint: String, referenceId: String?): Boolean {
        if (!referenceId.isNullOrBlank()) {
            if (dao.findByReferenceId(referenceId) != null) return true
        }
        return dao.findByFingerprint(fingerprint) != null
    }
}
