package com.pay.tracker.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import com.pay.tracker.ui.viewmodel.DatePeriodFilter
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.ui.components.BotanicalCreamButton
import com.pay.tracker.ui.components.BotanicalGlassCard
import com.pay.tracker.ui.components.BotanicalSquircleContainer
import com.pay.tracker.ui.components.CategoryUiUtils
import com.pay.tracker.ui.theme.BotanicalCreamPill
import com.pay.tracker.ui.theme.BotanicalCreamPillText
import com.pay.tracker.ui.theme.BotanicalEmerald
import com.pay.tracker.ui.theme.BotanicalGlassBorder
import com.pay.tracker.ui.theme.BotanicalGlassBorderSubtle
import com.pay.tracker.ui.theme.BotanicalGlassButton
import com.pay.tracker.ui.theme.BotanicalGlassButtonBorder
import com.pay.tracker.ui.theme.BotanicalTextPrimary
import com.pay.tracker.ui.theme.BotanicalTextSecondary
import com.pay.tracker.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val availableMonths by viewModel.availableMonthYears.collectAsState()
    val monthYearTotals by viewModel.monthYearTotals.collectAsState()
    val selectedDatePeriod by viewModel.selectedDatePeriod.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()
    val categories = viewModel.availableCategories

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )

                if (transactions.isNotEmpty()) {
                    Surface(
                        onClick = { showClearAllDialog = true },
                        shape = CircleShape,
                        color = Color(0x26FF453A),
                        border = BorderStroke(1.dp, Color(0x40FF453A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear All Transactions",
                                tint = Color(0xFFFF6961),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF6961)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Botanical Glass Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = Color(0x33253B2F),
                border = BorderStroke(1.dp, Brush.verticalGradient(listOf(BotanicalGlassBorder, BotanicalGlassBorderSubtle)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BotanicalTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("Search transactions, merchants...", fontSize = 14.sp, color = BotanicalTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = BotanicalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Quick Date Period Filter Row (Calendar, This Month, Last Month, 7 Days, Today, All Time)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Calendar Date Range Picker Button
                val isCustomActive = (selectedDatePeriod == DatePeriodFilter.CUSTOM_RANGE && customDateRange != null)
                val customRangeText = remember(customDateRange) {
                    customDateRange?.let { (start, end) ->
                        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                        "${fmt.format(Date(start))} - ${fmt.format(Date(end))}"
                    }
                }

                Surface(
                    onClick = { showDatePickerDialog = true },
                    shape = CircleShape,
                    color = if (isCustomActive) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (isCustomActive) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick Calendar Range",
                            tint = if (isCustomActive) BotanicalCreamPillText else BotanicalEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomActive) customRangeText ?: "Calendar" else "Calendar",
                            fontSize = 12.sp,
                            fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomActive) BotanicalCreamPillText else BotanicalTextPrimary
                        )
                    }
                }

                // Quick Period Options
                val quickPeriods = listOf(
                    DatePeriodFilter.THIS_MONTH,
                    DatePeriodFilter.LAST_MONTH,
                    DatePeriodFilter.LAST_7_DAYS,
                    DatePeriodFilter.TODAY,
                    DatePeriodFilter.ALL_TIME
                )

                quickPeriods.forEach { period ->
                    val isSelected = (selectedDatePeriod == period)
                    Surface(
                        onClick = { viewModel.onDatePeriodSelect(period) },
                        shape = CircleShape,
                        color = if (isSelected) BotanicalCreamPill else BotanicalGlassButton,
                        border = BorderStroke(1.dp, if (isSelected) BotanicalCreamPill else BotanicalGlassButtonBorder)
                    ) {
                        Text(
                            text = period.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BotanicalCreamPillText else BotanicalTextPrimary,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Compact Controls Row: Category Dropdown, Status Dropdown, Month Dropdown, Clear Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Filter Dropdown
                Box {
                    val hasCategory = (selectedCategory != null)
                    val catColor = selectedCategory?.let { CategoryUiUtils.getCategoryColor(it) } ?: Color.Transparent

                    Surface(
                        onClick = { categoryMenuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (hasCategory) Color(0x33253B2F) else BotanicalGlassButton,
                        border = BorderStroke(1.dp, if (hasCategory) BotanicalEmerald.copy(alpha = 0.5f) else BotanicalGlassButtonBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasCategory) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = selectedCategory ?: "Category",
                                fontSize = 12.sp,
                                fontWeight = if (hasCategory) FontWeight.Bold else FontWeight.Medium,
                                color = if (hasCategory) Color.White else BotanicalTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = BotanicalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF14241C))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Categories",
                                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedCategory == null) BotanicalEmerald else Color.White
                                )
                            },
                            onClick = {
                                viewModel.onCategoryFilterSelect(null)
                                categoryMenuExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            val isSelected = (selectedCategory == cat.name)
                            val cColor = CategoryUiUtils.getCategoryColor(cat.name)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(cColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BotanicalEmerald else Color.White
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onCategoryFilterSelect(cat.name)
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status Filter Dropdown
                Box {
                    val hasStatus = (selectedStatus != null)
                    Surface(
                        onClick = { statusMenuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (hasStatus) Color(0x33253B2F) else BotanicalGlassButton,
                        border = BorderStroke(1.dp, if (hasStatus) BotanicalEmerald.copy(alpha = 0.5f) else BotanicalGlassButtonBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (selectedStatus) {
                                    TransactionStatus.CONFIRMED -> "Confirmed"
                                    TransactionStatus.PENDING -> "Pending"
                                    TransactionStatus.REJECTED -> "Rejected"
                                    null -> "Status"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (hasStatus) FontWeight.Bold else FontWeight.Medium,
                                color = if (hasStatus) Color.White else BotanicalTextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = BotanicalTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF14241C))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "All Status",
                                    fontWeight = if (selectedStatus == null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedStatus == null) BotanicalEmerald else Color.White
                                )
                            },
                            onClick = {
                                viewModel.onStatusFilterSelect(null)
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Confirmed",
                                    fontWeight = if (selectedStatus == TransactionStatus.CONFIRMED) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedStatus == TransactionStatus.CONFIRMED) BotanicalEmerald else Color.White
                                )
                            },
                            onClick = {
                                viewModel.onStatusFilterSelect(TransactionStatus.CONFIRMED)
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Pending",
                                    fontWeight = if (selectedStatus == TransactionStatus.PENDING) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedStatus == TransactionStatus.PENDING) BotanicalEmerald else Color.White
                                )
                            },
                            onClick = {
                                viewModel.onStatusFilterSelect(TransactionStatus.PENDING)
                                statusMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Rejected",
                                    fontWeight = if (selectedStatus == TransactionStatus.REJECTED) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedStatus == TransactionStatus.REJECTED) BotanicalEmerald else Color.White
                                )
                            },
                            onClick = {
                                viewModel.onStatusFilterSelect(TransactionStatus.REJECTED)
                                statusMenuExpanded = false
                            }
                        )
                    }
                }

                // Month Filter Dropdown (if transactions span multiple months)
                if (availableMonths.size > 1 || (availableMonths.size == 1 && selectedMonthYear != null)) {
                    Box {
                        val hasMonth = (selectedMonthYear != null)
                        Surface(
                            onClick = { monthMenuExpanded = true },
                            shape = RoundedCornerShape(20.dp),
                            color = if (hasMonth) Color(0x33253B2F) else BotanicalGlassButton,
                            border = BorderStroke(1.dp, if (hasMonth) BotanicalEmerald.copy(alpha = 0.5f) else BotanicalGlassButtonBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedMonthYear ?: "Month",
                                    fontSize = 12.sp,
                                    fontWeight = if (hasMonth) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasMonth) Color.White else BotanicalTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = BotanicalTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF14241C))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "All Months",
                                        fontWeight = if (selectedMonthYear == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedMonthYear == null) BotanicalEmerald else Color.White
                                    )
                                },
                                onClick = {
                                    viewModel.onMonthYearFilterSelect(null)
                                    monthMenuExpanded = false
                                }
                            )
                            availableMonths.forEach { mYear ->
                                val isSelected = (selectedMonthYear == mYear)
                                val spend = monthYearTotals[mYear] ?: 0.0
                                val spendBadge = if (spend > 0) " (₹${"%,.0f".format(spend)})" else ""
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$mYear$spendBadge",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BotanicalEmerald else Color.White
                                        )
                                    },
                                    onClick = {
                                        viewModel.onMonthYearFilterSelect(mYear)
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reset / Clear Filters button
                val isFilterActive = searchQuery.isNotEmpty() ||
                        selectedCategory != null ||
                        selectedStatus != null ||
                        selectedMonthYear != null ||
                        selectedDatePeriod != DatePeriodFilter.ALL_TIME ||
                        customDateRange != null

                if (isFilterActive) {
                    Surface(
                        onClick = { viewModel.clearAllFilters() },
                        shape = CircleShape,
                        color = Color(0x33FF453A),
                        border = BorderStroke(1.dp, Color(0x4DFF453A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Filters",
                                tint = Color(0xFFFF6961),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Clear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF6961)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Active Filter & Spend Summary Banner
            val filteredTotal = remember(transactions) { transactions.sumOf { it.amount } }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x2B104F32),
                border = BorderStroke(1.dp, BotanicalEmerald.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${transactions.size} transactions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Total: ₹${"%,.2f".format(filteredTotal)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Month & Year Grouped Transaction List
            if (transactions.isEmpty()) {
                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = BotanicalTextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search query, month, or category filters.",
                            fontSize = 13.sp,
                            color = BotanicalTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val monthGrouped = remember(transactions) {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val nowStr = todayFormat.format(Date())

                    transactions.groupBy { monthFormat.format(Date(it.timestamp)) }
                        .mapValues { (_, monthTxns) ->
                            monthTxns.groupBy { txn ->
                                val txnStr = todayFormat.format(Date(txn.timestamp))
                                if (txnStr == nowStr) "Today"
                                else dateFormat.format(Date(txn.timestamp))
                            }
                        }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    monthGrouped.forEach { (monthName, daysMap) ->
                        val monthTotal = daysMap.values.flatten().sumOf { it.amount }
                        val monthTxnCount = daysMap.values.flatten().size

                        // Month & Year Header Banner
                        item(key = "month_header_$monthName") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33253B2F),
                                border = BorderStroke(1.dp, Color(0x26FFFFFF))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(BotanicalEmerald)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = monthName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${CategoryUiUtils.formatCurrency(monthTotal)} • $monthTxnCount txns",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BotanicalTextSecondary
                                    )
                                }
                            }
                        }

                        // Days within this month
                        daysMap.forEach { (dateHeader, txnsForDate) ->
                            val dayTotal = txnsForDate.sumOf { it.amount }

                            item(key = "header_${monthName}_$dateHeader") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateHeader,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "- " + CategoryUiUtils.formatCurrency(dayTotal),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BotanicalTextSecondary
                                    )
                                }
                            }

                            item(key = "card_${monthName}_$dateHeader") {
                                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        txnsForDate.forEachIndexed { index, txn ->
                                            if (index > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(Color(0x14FFFFFF))
                                                )
                                            }

                                            BotanicalTransactionRow(
                                                transaction = txn,
                                                onDelete = { transactionToDelete = txn }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = Color(0xF0101D16),
            title = {
                Text(
                    text = "Clear All Transactions?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all transactions from your history. You can scan this month's SMS again anytime from the Dashboard.",
                    fontSize = 13.sp,
                    color = BotanicalTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTransactions()
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = CircleShape
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel", color = BotanicalTextSecondary)
                }
            }
        )
    }

    // Delete Single Transaction Confirmation Dialog
    transactionToDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = Color(0xF0101D16),
            title = {
                Text(
                    text = "Delete Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this ₹${txn.amount} transaction from ${txn.merchant}?",
                    fontSize = 13.sp,
                    color = BotanicalTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txn.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = CircleShape
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel", color = BotanicalTextSecondary)
                }
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showDatePickerDialog) {
        HistoryDateRangePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            onDateRangeSelected = { start, end ->
                viewModel.onCustomDateRangeSelect(start, end)
            },
            initialStartDateMillis = customDateRange?.first,
            initialEndDateMillis = customDateRange?.second
        )
    }
}

@Composable
private fun BotanicalTransactionRow(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val categoryColor = CategoryUiUtils.getCategoryColor(transaction.category)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeText = timeFormat.format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Category Icon
        BotanicalSquircleContainer(
            icon = CategoryUiUtils.getCategoryIcon(transaction.category),
            tint = categoryColor,
            backgroundColor = categoryColor.copy(alpha = 0.18f),
            size = 40.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${transaction.category} • $timeText",
                fontSize = 12.sp,
                color = BotanicalTextSecondary
            )
        }

        // Amount & Status
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "- " + CategoryUiUtils.formatCurrency(transaction.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (transaction.status == TransactionStatus.PENDING) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pending",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = com.pay.tracker.ui.theme.PendingAmber
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startLocalEpoch: Long, endLocalEpoch: Long) -> Unit,
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMillis,
        initialSelectedEndDateMillis = initialEndDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        val (localStart, localEnd) = convertUtcMidnightToLocalRange(start, end)
                        onDateRangeSelected(localStart, localEnd)
                    }
                    onDismissRequest()
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null
            ) {
                Text("Apply", color = BotanicalEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = BotanicalTextSecondary)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color(0xFF14241C)
        )
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            headline = null,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF14241C),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = BotanicalTextSecondary,
                subheadContentColor = BotanicalTextSecondary,
                yearContentColor = Color.White,
                currentYearContentColor = BotanicalEmerald,
                selectedYearContentColor = Color(0xFF112117),
                selectedYearContainerColor = BotanicalEmerald,
                dayContentColor = Color.White,
                selectedDayContainerColor = BotanicalEmerald,
                selectedDayContentColor = Color(0xFF112117),
                dayInSelectionRangeContainerColor = BotanicalEmerald.copy(alpha = 0.35f),
                dayInSelectionRangeContentColor = Color.White
            )
        )
    }
}

private fun convertUtcMidnightToLocalRange(startUtc: Long, endUtc: Long): Pair<Long, Long> {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = startUtc
    }
    val localCalStart = Calendar.getInstance().apply {
        set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val utcCalEnd = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = endUtc
    }
    val localCalEnd = Calendar.getInstance().apply {
        set(utcCalEnd.get(Calendar.YEAR), utcCalEnd.get(Calendar.MONTH), utcCalEnd.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return Pair(localCalStart.timeInMillis, localCalEnd.timeInMillis)
}
