package com.pay.tracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pay.tracker.core.database.PayDatabase
import com.pay.tracker.core.repository.TransactionRepository
import com.pay.tracker.core.repository.TransactionRepositoryImpl

@Suppress("UNCHECKED_CAST")
class PayViewModelFactory(
    private val context: Context,
    private val repository: TransactionRepository = TransactionRepositoryImpl(
        PayDatabase.getInstance(context).transactionDao()
    )
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(repository, context.applicationContext) as T
            }
            modelClass.isAssignableFrom(ReviewQueueViewModel::class.java) -> {
                ReviewQueueViewModel(repository, context.applicationContext) as T
            }
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> {
                HistoryViewModel(repository, context.applicationContext) as T
            }
            modelClass.isAssignableFrom(SimulatorViewModel::class.java) -> {
                SimulatorViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(context.applicationContext, repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
