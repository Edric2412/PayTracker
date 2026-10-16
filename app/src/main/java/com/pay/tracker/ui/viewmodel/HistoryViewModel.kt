package com.pay.tracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.core.notification.NotificationHelper
import com.pay.tracker.core.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: TransactionRepository,
    private val context: Context? = null
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedStatus = MutableStateFlow<TransactionStatus?>(null)

    val availableCategories = Category.DEFAULT_CATEGORIES

    private val allTransactions = repository.observeAllTransactions()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        searchQuery,
        selectedCategory,
        selectedStatus
    ) { transactions, query, category, status ->
        transactions.filter { txn ->
            val matchesQuery = query.isBlank() ||
                    txn.merchant.contains(query, ignoreCase = true) ||
                    txn.category.contains(query, ignoreCase = true) ||
                    (txn.referenceId?.contains(query, ignoreCase = true) == true)

            val matchesCategory = category == null || txn.category.equals(category, ignoreCase = true)
            val matchesStatus = status == null || txn.status == status

            matchesQuery && matchesCategory && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategoryFilterSelect(category: String?) {
        selectedCategory.value = category
    }

    fun onStatusFilterSelect(status: TransactionStatus?) {
        selectedStatus.value = status
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedStatus.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }

    fun updateCategory(id: Long, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(id, newCategory)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }
}
