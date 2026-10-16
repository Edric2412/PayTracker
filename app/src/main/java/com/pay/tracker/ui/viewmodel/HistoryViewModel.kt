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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DatePeriodFilter(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_7_DAYS("7 Days"),
    TODAY("Today"),
    CUSTOM_RANGE("Calendar Range")
}

class HistoryViewModel(
    private val repository: TransactionRepository,
    private val context: Context? = null
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedStatus = MutableStateFlow<TransactionStatus?>(null)
    val selectedMonthYear = MutableStateFlow<String?>(null)
    val selectedDatePeriod = MutableStateFlow(DatePeriodFilter.ALL_TIME)
    val customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    val availableCategories = Category.DEFAULT_CATEGORIES

    private val allTransactions = repository.observeAllTransactions()

    val availableMonthYears: StateFlow<List<String>> = allTransactions
        .map { list ->
            val now = Calendar.getInstance()
            val curYear = now.get(Calendar.YEAR)
            val curMonth = now.get(Calendar.MONTH) + 1
            val cal = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            list.groupBy { txn ->
                cal.timeInMillis = txn.timestamp
                cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
            }.keys
            .filter { (year, month) ->
                year < curYear || (year == curYear && month <= curMonth)
            }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
            .map { (year, month) ->
                val c = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                monthFormat.format(c.time)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthYearTotals: StateFlow<Map<String, Double>> = allTransactions
        .map { list: List<Transaction> ->
            val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val confirmed = list.filter { it.status == TransactionStatus.CONFIRMED }
            val grouped = confirmed.groupBy { fmt.format(Date(it.timestamp)) }
            grouped.mapValues { entry -> entry.value.sumOf { txn -> txn.amount } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        searchQuery,
        selectedCategory,
        selectedStatus,
        selectedDatePeriod,
        customDateRange,
        selectedMonthYear
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<Transaction>
        val query = args[1] as String
        val category = args[2] as String?
        val status = args[3] as TransactionStatus?
        val period = args[4] as DatePeriodFilter
        @Suppress("UNCHECKED_CAST")
        val customRange = args[5] as Pair<Long, Long>?
        val monthYear = args[6] as String?

        val (rangeStart, rangeEnd) = when (period) {
            DatePeriodFilter.ALL_TIME -> null to null
            DatePeriodFilter.TODAY -> getTodayRange()
            DatePeriodFilter.LAST_7_DAYS -> getLast7DaysRange()
            DatePeriodFilter.THIS_MONTH -> getThisMonthRange()
            DatePeriodFilter.LAST_MONTH -> getLastMonthRange()
            DatePeriodFilter.CUSTOM_RANGE -> customRange?.first to customRange?.second
        }

        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        transactions.filter { txn ->
            val matchesQuery = query.isBlank() ||
                    txn.merchant.contains(query, ignoreCase = true) ||
                    txn.category.contains(query, ignoreCase = true) ||
                    (txn.referenceId?.contains(query, ignoreCase = true) == true)

            val matchesCategory = category == null || txn.category.equals(category, ignoreCase = true)
            val matchesStatus = status == null || txn.status == status
            val matchesDateRange = (rangeStart == null || txn.timestamp >= rangeStart) &&
                    (rangeEnd == null || txn.timestamp <= rangeEnd)
            val matchesMonth = monthYear == null || fmt.format(Date(txn.timestamp)) == monthYear

            matchesQuery && matchesCategory && matchesStatus && matchesDateRange && matchesMonth
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onDatePeriodSelect(period: DatePeriodFilter) {
        selectedDatePeriod.value = period
        if (period != DatePeriodFilter.CUSTOM_RANGE) {
            customDateRange.value = null
        }
        selectedMonthYear.value = null
    }

    fun onCustomDateRangeSelect(startEpoch: Long, endEpoch: Long) {
        selectedDatePeriod.value = DatePeriodFilter.CUSTOM_RANGE
        customDateRange.value = Pair(startEpoch, endEpoch)
        selectedMonthYear.value = null
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onCategoryFilterSelect(category: String?) {
        selectedCategory.value = category
    }

    fun onStatusFilterSelect(status: TransactionStatus?) {
        selectedStatus.value = status
    }

    fun onMonthYearFilterSelect(monthYear: String?) {
        selectedMonthYear.value = monthYear
        if (monthYear != null) {
            selectedDatePeriod.value = DatePeriodFilter.ALL_TIME
            customDateRange.value = null
        }
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedStatus.value = null
        selectedMonthYear.value = null
        selectedDatePeriod.value = DatePeriodFilter.ALL_TIME
        customDateRange.value = null
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }

    private fun getLast7DaysRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val start = cal.timeInMillis
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return Pair(start, end)
    }

    private fun getThisMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }

    private fun getLastMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
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
