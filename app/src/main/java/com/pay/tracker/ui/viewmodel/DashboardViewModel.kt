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

data class YearMonthSelection(
    val year: Int,
    val month: Int // 1..12
) {
    val displayName: String
        get() {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(cal.time)
        }

    val isCurrentMonth: Boolean
        get() {
            val now = Calendar.getInstance()
            return now.get(Calendar.YEAR) == year && (now.get(Calendar.MONTH) + 1) == month
        }

    fun startEpoch(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endEpoch(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, maxDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun totalDays(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun previous(): YearMonthSelection {
        return if (month == 1) YearMonthSelection(year - 1, 12)
        else YearMonthSelection(year, month - 1)
    }

    fun next(): YearMonthSelection {
        return if (month == 12) YearMonthSelection(year + 1, 1)
        else YearMonthSelection(year, month + 1)
    }

    companion object {
        fun current(): YearMonthSelection {
            val now = Calendar.getInstance()
            return YearMonthSelection(
                year = now.get(Calendar.YEAR),
                month = now.get(Calendar.MONTH) + 1
            )
        }
    }
}

data class MonthlySpendSummary(
    val year: Int,
    val month: Int, // 1..12
    val monthName: String,
    val displayName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val isCurrentMonth: Boolean
)

data class YearlySpendSummary(
    val year: Int,
    val totalAmount: Double,
    val transactionCount: Int,
    val months: List<MonthlySpendSummary>
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

    private val _selectedYearMonth = MutableStateFlow(YearMonthSelection.current())
    val selectedYearMonth: StateFlow<YearMonthSelection> = _selectedYearMonth.asStateFlow()

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.previous()
    }

    fun nextMonth() {
        val cur = YearMonthSelection.current()
        val currentSelected = _selectedYearMonth.value
        if (currentSelected.year < cur.year || (currentSelected.year == cur.year && currentSelected.month < cur.month)) {
            _selectedYearMonth.value = currentSelected.next()
        }
    }

    fun selectYearMonth(ym: YearMonthSelection) {
        val cur = YearMonthSelection.current()
        if (ym.year < cur.year || (ym.year == cur.year && ym.month <= cur.month)) {
            _selectedYearMonth.value = ym
        }
    }

    fun jumpToCurrentMonth() {
        _selectedYearMonth.value = YearMonthSelection.current()
    }

    val availableMonths: StateFlow<List<YearMonthSelection>> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { currentYm, txns ->
        val cur = YearMonthSelection.current()
        val set = mutableSetOf<YearMonthSelection>()
        set.add(cur)
        if (currentYm.year < cur.year || (currentYm.year == cur.year && currentYm.month <= cur.month)) {
            set.add(currentYm)
        }
        txns.forEach { txn ->
            val cal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
            val ym = YearMonthSelection(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            if (ym.year < cur.year || (ym.year == cur.year && ym.month <= cur.month)) {
                set.add(ym)
            }
        }
        set.sortedWith(compareByDescending<YearMonthSelection> { it.year }.thenByDescending { it.month })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(YearMonthSelection.current())
    )

    val previousMonthTotal: StateFlow<Double> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, txns ->
        val prev = ym.previous()
        txns.filter { it.timestamp in prev.startEpoch()..prev.endEpoch() }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val yearlySpendSummaries: StateFlow<List<YearlySpendSummary>> = repository.observeConfirmedTransactions()
        .map { txns ->
            val cal = Calendar.getInstance()
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH) + 1
            val monthFormat = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault())

            val yearGroups = txns.groupBy { txn ->
                cal.timeInMillis = txn.timestamp
                cal.get(Calendar.YEAR)
            }

            val allYears = (yearGroups.keys + currentYear).distinct().filter { it <= currentYear }.sortedDescending()

            allYears.map { year ->
                val yearTxns = yearGroups[year] ?: emptyList()
                val monthGroups = yearTxns.groupBy { txn ->
                    cal.timeInMillis = txn.timestamp
                    cal.get(Calendar.MONTH) + 1
                }

                val activeMonths = if (year == currentYear) {
                    (monthGroups.keys + currentMonth).distinct().filter { it <= currentMonth }.sortedDescending()
                } else {
                    monthGroups.keys.sortedDescending()
                }

                val monthSummaries = activeMonths.map { month ->
                    val monthTxns = monthGroups[month] ?: emptyList()
                    val monthCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val mName = monthFormat.format(monthCal.time)

                    MonthlySpendSummary(
                        year = year,
                        month = month,
                        monthName = mName,
                        displayName = "$mName $year",
                        totalAmount = monthTxns.sumOf { it.amount },
                        transactionCount = monthTxns.size,
                        isCurrentMonth = (year == currentYear && month == currentMonth)
                    )
                }

                YearlySpendSummary(
                    year = year,
                    totalAmount = yearTxns.sumOf { it.amount },
                    transactionCount = yearTxns.size,
                    months = monthSummaries
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlySpendMap: StateFlow<Map<YearMonthSelection, Double>> = repository.observeConfirmedTransactions()
        .map { txns ->
            val cal = Calendar.getInstance()
            val map = mutableMapOf<YearMonthSelection, Double>()
            txns.forEach { txn ->
                cal.timeInMillis = txn.timestamp
                val ym = YearMonthSelection(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                map[ym] = (map[ym] ?: 0.0) + txn.amount
            }
            map
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val budgetPacing: StateFlow<BudgetPacing> = combine(
        budgetTarget,
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { target, ym, txns ->
        val now = Calendar.getInstance()
        val totalDaysInMonth = ym.totalDays()
        val isCurrent = ym.isCurrentMonth
        val currentDay = if (isCurrent) now.get(Calendar.DAY_OF_MONTH) else totalDaysInMonth
        val daysRemaining = if (isCurrent) (totalDaysInMonth - currentDay + 1).coerceAtLeast(1) else 1

        val monthTxns = txns.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }
        val spent = monthTxns.sumOf { it.amount }
        val remaining = (target - spent).coerceAtLeast(0.0)
        val safeDaily = if (isCurrent) remaining / daysRemaining else 0.0
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

    val monthlyTotal: StateFlow<Double> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, txns ->
        txns.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }.sumOf { it.amount }
    }.stateIn(
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

    val confirmedTransactionsCount: StateFlow<Int> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, list ->
        list.count { it.timestamp in ym.startEpoch()..ym.endEpoch() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val categoryBreakdown: StateFlow<List<CategoryExpense>> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, transactions ->
        val monthTransactions = transactions.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }
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
    }.stateIn(
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

    val monthDailySpendHistory: StateFlow<List<DailySpend>> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, transactions ->
        val result = mutableListOf<DailySpend>()
        val totalDays = ym.totalDays()
        val now = Calendar.getInstance()
        val isCurrent = ym.isCurrentMonth
        val currentDay = if (isCurrent) now.get(Calendar.DAY_OF_MONTH) else totalDays
        val monthTxns = transactions.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }

        for (day in 1..totalDays) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, ym.year)
                set(Calendar.MONTH, ym.month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 86_400_000L - 1L

            val dayTotal = if (!isCurrent || day <= currentDay) {
                monthTxns.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amount }
            } else 0.0

            result.add(
                DailySpend(
                    dayLabel = day.toString(),
                    dateText = "$day",
                    totalAmount = dayTotal,
                    isToday = isCurrent && (day == currentDay),
                    timestamp = startOfDay
                )
            )
        }
        result
    }.stateIn(
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

    val monthDailyAverage: StateFlow<Double> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, txns ->
        val monthTxns = txns.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }
        val total = monthTxns.sumOf { it.amount }
        val now = Calendar.getInstance()
        val effectiveDays = if (ym.isCurrentMonth) {
            now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        } else {
            ym.totalDays().coerceAtLeast(1)
        }
        total / effectiveDays
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val largestExpenseThisMonth: StateFlow<Transaction?> = combine(
        selectedYearMonth,
        repository.observeConfirmedTransactions()
    ) { ym, txns ->
        txns.filter { it.timestamp in ym.startEpoch()..ym.endEpoch() }.maxByOrNull { it.amount }
    }.stateIn(
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

    fun importAllSms(context: android.content.Context) {
        viewModelScope.launch {
            _isImporting.value = true
            _importState.value = null
            val importer = com.pay.tracker.core.sms.SmsTransactionImporter(repository)
            val result = importer.importAllSms(context)
            _importState.value = result
            _isImporting.value = false
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    fun dismissImportResult() {
        _importState.value = null
    }
}
