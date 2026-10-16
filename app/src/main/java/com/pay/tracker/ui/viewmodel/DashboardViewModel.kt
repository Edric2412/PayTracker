package com.pay.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.repository.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

enum class DashboardTimeframe(val label: String) {
    LAST_7_DAYS("7 Days"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month")
}

data class CategoryExpense(
    val category: String,
    val totalAmount: Double,
    val percentage: Float
)

data class DailySpend(
    val dayLabel: String,
    val dateText: String,
    val totalAmount: Double,
    val isToday: Boolean = false,
    val timestamp: Long = 0L
)

data class BudgetPacing(
    val budgetTarget: Double = 10000.0,
    val spentThisMonth: Double = 0.0,
    val remainingBudget: Double = 10000.0,
    val safeDailyPace: Double = 0.0,
    val daysRemaining: Int = 1,
    val totalDaysInMonth: Int = 30,
    val currentDayOfMonth: Int = 1,
    val percentUsed: Float = 0f,
    val isExceeded: Boolean = false
)

data class TimeframeSummary(
    val timeframe: DashboardTimeframe = DashboardTimeframe.THIS_MONTH,
    val totalSpend: Double = 0.0,
    val transactionCount: Int = 0,
    val dailyAverage: Double = 0.0,
    val breakdown: List<CategoryExpense> = emptyList()
)

data class DashboardUiState(
    val monthlyTotal: Double = 0.0,
    val pendingCount: Int = 0,
    val categoryBreakdown: List<CategoryExpense> = emptyList(),
    val dailySpendHistory: List<DailySpend> = emptyList()
)

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val context: Context? = null
) : ViewModel() {

    private val prefs = context?.getSharedPreferences("pay_tracker_prefs", Context.MODE_PRIVATE)

    private val _budgetTarget = MutableStateFlow(
        prefs?.getFloat("monthly_budget_target", 10000f)?.toDouble() ?: 10000.0
    )
    val budgetTarget: StateFlow<Double> = _budgetTarget.asStateFlow()

    fun updateBudgetTarget(newTarget: Double) {
        val sanitized = if (newTarget < 1.0) 10000.0 else newTarget
        _budgetTarget.value = sanitized
        prefs?.edit()?.putFloat("monthly_budget_target", sanitized.toFloat())?.apply()
    }

    private val _selectedTimeframe = MutableStateFlow(DashboardTimeframe.THIS_MONTH)
    val selectedTimeframe: StateFlow<DashboardTimeframe> = _selectedTimeframe.asStateFlow()

    fun setTimeframe(timeframe: DashboardTimeframe) {
        _selectedTimeframe.value = timeframe
    }

    private val startOfMonthEpoch: Long
        get() {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

    val budgetPacing: StateFlow<BudgetPacing> = combine(
        budgetTarget,
        repository.observeConfirmedTransactions()
    ) { target, txns ->
        val cal = Calendar.getInstance()
        val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = (totalDaysInMonth - currentDay + 1).coerceAtLeast(1)

        val monthTxns = txns.filter { it.timestamp >= startOfMonthEpoch }
        val spent = monthTxns.sumOf { it.amount }
        val remaining = (target - spent).coerceAtLeast(0.0)
        val safeDaily = remaining / daysRemaining
        val percent = if (target > 0) ((spent / target) * 100).toFloat() else 0f
        val isExceeded = spent > target

        BudgetPacing(
            budgetTarget = target,
            spentThisMonth = spent,
            remainingBudget = remaining,
            safeDailyPace = safeDaily,
            daysRemaining = daysRemaining,
            totalDaysInMonth = totalDaysInMonth,
            currentDayOfMonth = currentDay,
            percentUsed = percent,
            isExceeded = isExceeded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetPacing()
    )

    private fun getTimeframeRange(tf: DashboardTimeframe): Triple<Long, Long, Int> {
        return when (tf) {
            DashboardTimeframe.LAST_7_DAYS -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startEpoch = cal.timeInMillis - (6 * 86_400_000L)
                val endEpoch = System.currentTimeMillis()
                Triple(startEpoch, endEpoch, 7)
            }
            DashboardTimeframe.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startEpoch = cal.timeInMillis
                val endEpoch = System.currentTimeMillis()
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                Triple(startEpoch, endEpoch, currentDay)
            }
            DashboardTimeframe.LAST_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startEpoch = cal.timeInMillis
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDays)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endEpoch = cal.timeInMillis
                Triple(startEpoch, endEpoch, maxDays)
            }
        }
    }

    val timeframeSummary: StateFlow<TimeframeSummary> = combine(
        selectedTimeframe,
        repository.observeConfirmedTransactions()
    ) { tf, txns ->
        val (startEpoch, endEpoch, daysCount) = getTimeframeRange(tf)
        val filteredTxns = txns.filter { it.timestamp in startEpoch..endEpoch }
        val total = filteredTxns.sumOf { it.amount }
        val count = filteredTxns.size
        val dailyAvg = if (daysCount > 0) total / daysCount else 0.0

        val breakdown = if (total == 0.0) {
            emptyList()
        } else {
            filteredTxns.groupBy { it.category }
                .map { (cat, list) ->
                    val sum = list.sumOf { it.amount }
                    CategoryExpense(
                        category = cat,
                        totalAmount = sum,
                        percentage = ((sum / total) * 100).toFloat()
                    )
                }
                .sortedByDescending { it.totalAmount }
        }

        TimeframeSummary(
            timeframe = tf,
            totalSpend = total,
            transactionCount = count,
            dailyAverage = dailyAvg,
            breakdown = breakdown
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimeframeSummary()
    )

    val monthlyTotal: StateFlow<Double> = repository.observeTotalSpentSince(startOfMonthEpoch)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val pendingTransactions: StateFlow<List<Transaction>> = repository.observePendingTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val confirmedTransactionsCount: StateFlow<Int> = repository.observeConfirmedTransactions()
        .map { list -> list.count { it.timestamp >= startOfMonthEpoch } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val categoryBreakdown: StateFlow<List<CategoryExpense>> = repository.observeConfirmedTransactions()
        .map { transactions ->
            val monthTransactions = transactions.filter { it.timestamp >= startOfMonthEpoch }
            val total = monthTransactions.sumOf { it.amount }
            if (total == 0.0) {
                emptyList()
            } else {
                monthTransactions.groupBy { it.category }
                    .map { (cat, list) ->
                        val sum = list.sumOf { it.amount }
                        CategoryExpense(
                            category = cat,
                            totalAmount = sum,
                            percentage = ((sum / total) * 100).toFloat()
                        )
                    }
                    .sortedByDescending { it.totalAmount }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailySpendHistory: StateFlow<List<DailySpend>> = repository.observeConfirmedTransactions()
        .map { transactions ->
            val result = mutableListOf<DailySpend>()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())

            for (i in 6 downTo 0) {
                val startOfDay = cal.timeInMillis - (i * 86_400_000L)
                val endOfDay = startOfDay + 86_400_000L - 1L

                val dayTotal = transactions
                    .filter { it.timestamp in startOfDay..endOfDay }
                    .sumOf { it.amount }

                result.add(
                    DailySpend(
                        dayLabel = if (i == 0) "Today" else dayFormat.format(java.util.Date(startOfDay)),
                        dateText = dateFormat.format(java.util.Date(startOfDay)),
                        totalAmount = dayTotal,
                        isToday = (i == 0),
                        timestamp = startOfDay
                    )
                )
            }
            result
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthDailySpendHistory: StateFlow<List<DailySpend>> = repository.observeConfirmedTransactions()
        .map { transactions ->
            val result = mutableListOf<DailySpend>()
            val cal = Calendar.getInstance()
            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val monthTxns = transactions.filter { it.timestamp >= startOfMonthEpoch }

            for (day in 1..totalDays) {
                val dayCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = dayCal.timeInMillis
                val endOfDay = startOfDay + 86_400_000L - 1L

                val dayTotal = if (day <= currentDay) {
                    monthTxns.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amount }
                } else 0.0

                result.add(
                    DailySpend(
                        dayLabel = day.toString(),
                        dateText = "$day",
                        totalAmount = dayTotal,
                        isToday = (day == currentDay),
                        timestamp = startOfDay
                    )
                )
            }
            result
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    val sevenDayTotal: StateFlow<Double> = dailySpendHistory
        .map { list -> list.sumOf { it.totalAmount } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val monthDailyAverage: StateFlow<Double> = repository.observeConfirmedTransactions()
        .map { txns ->
            val monthTxns = txns.filter { it.timestamp >= startOfMonthEpoch }
            val total = monthTxns.sumOf { it.amount }
            val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            total / currentDayOfMonth
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val largestExpenseThisMonth: StateFlow<Transaction?> = repository.observeConfirmedTransactions()
        .map { txns ->
            txns.filter { it.timestamp >= startOfMonthEpoch }.maxByOrNull { it.amount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val recentTransactions: StateFlow<List<Transaction>> = repository.observeAllTransactions()
        .map { list -> list.take(6) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _importState = kotlinx.coroutines.flow.MutableStateFlow<com.pay.tracker.core.sms.SmsImportResult?>(null)
    val importState: StateFlow<com.pay.tracker.core.sms.SmsImportResult?> = _importState

    private val _isImporting = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    fun addManualExpense(
        amount: Double,
        merchant: String,
        category: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val fingerprint = "${amount}_${merchant}_${timestamp}_manual"
            val txn = Transaction(
                id = 0,
                amount = amount,
                merchant = merchant,
                category = category,
                timestamp = timestamp,
                status = com.pay.tracker.core.model.TransactionStatus.CONFIRMED,
                sourceApp = "Manual",
                rawNotification = "Manual expense entry",
                referenceId = null,
                fingerprint = fingerprint
            )
            repository.saveTransaction(txn)
        }
    }

    fun importMonthSms(context: android.content.Context) {
        viewModelScope.launch {
            _isImporting.value = true
            _importState.value = null
            val importer = com.pay.tracker.core.sms.SmsTransactionImporter(repository)
            val result = importer.importMonthSms(context)
            _importState.value = result
            _isImporting.value = false
        }
    }

    fun dismissImportResult() {
        _importState.value = null
    }
}
