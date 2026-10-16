package com.pay.tracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.notification.NotificationHelper
import com.pay.tracker.core.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReviewQueueViewModel(
    private val repository: TransactionRepository,
    private val context: Context? = null
) : ViewModel() {

    val pendingTransactions: StateFlow<List<Transaction>> = repository.observePendingTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val availableCategories: List<Category> = Category.DEFAULT_CATEGORIES

    fun confirmTransaction(id: Long, category: String? = null) {
        viewModelScope.launch {
            repository.confirmTransaction(id, category)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }

    fun rejectTransaction(id: Long) {
        viewModelScope.launch {
            repository.rejectTransaction(id)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }

    fun updateCategory(id: Long, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(id, newCategory)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            context?.let { NotificationHelper.cancelNotification(it, id) }
        }
    }
}
