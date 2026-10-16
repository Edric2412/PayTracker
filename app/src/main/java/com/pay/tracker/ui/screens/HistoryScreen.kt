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
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.platform.LocalContext
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
import com.pay.tracker.core.model.TransactionDirection
import com.pay.tracker.core.model.TransactionClassification
import com.pay.tracker.ui.viewmodel.TransactionTypeFilter
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
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val categories = viewModel.availableCategories

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionDetails by remember { mutableStateOf<Transaction?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
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

            // 4. Compact Controls Row: Type Dropdown, Category Dropdown, Status Dropdown, Month Dropdown, Clear Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Filter Dropdown
                Box {
                    val isTypeFiltered = (selectedTypeFilter != TransactionTypeFilter.ALL)
                    Surface(
                        onClick = { typeMenuExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isTypeFiltered) Color(0x33253B2F) else BotanicalGlassButton,
                        border = BorderStroke(1.dp, if (isTypeFiltered) BotanicalEmerald.copy(alpha = 0.5f) else BotanicalGlassButtonBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Type: ${selectedTypeFilter.label}",
                                fontSize = 12.sp,
                                fontWeight = if (isTypeFiltered) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTypeFiltered) Color.White else BotanicalTextSecondary,
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
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                        modifier = Modifier.background(Color(0xFF14241C))
                    ) {
                        TransactionTypeFilter.values().forEach { type ->
                            val isSelected = (selectedTypeFilter == type)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) BotanicalEmerald else Color.White
                                    )
                                },
                                onClick = {
                                    viewModel.onTypeFilterSelect(type)
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

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
                        customDateRange != null ||
                        selectedTypeFilter != TransactionTypeFilter.ALL

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
            val filteredDebits = remember(transactions) {
                transactions.filter { it.direction == TransactionDirection.DEBIT }.sumOf { it.amount }
            }
            val filteredCredits = remember(transactions) {
                transactions.filter { it.direction == TransactionDirection.CREDIT }.sumOf { it.amount }
            }
            val summaryText = remember(selectedTypeFilter, filteredDebits, filteredCredits) {
                when (selectedTypeFilter) {
                    TransactionTypeFilter.EXPENSES_ONLY -> "Spent: ₹${"%,.2f".format(filteredDebits)}"
                    TransactionTypeFilter.INCOME_ONLY -> "Received: +₹${"%,.2f".format(filteredCredits)}"
                    TransactionTypeFilter.ALL -> {
                        when {
                            filteredDebits > 0 && filteredCredits > 0 ->
                                "Spent: ₹${"%,.0f".format(filteredDebits)}  •  Received: +₹${"%,.0f".format(filteredCredits)}"
                            filteredCredits > 0 ->
                                "Received: +₹${"%,.2f".format(filteredCredits)}"
                            else ->
                                "Total Spent: ₹${"%,.2f".format(filteredDebits)}"
                        }
                    }
                }
            }

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
                        text = summaryText,
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
                            val dayDebits = txnsForDate.filter { it.direction == TransactionDirection.DEBIT }.sumOf { it.amount }
                            val dayCredits = txnsForDate.filter { it.direction == TransactionDirection.CREDIT }.sumOf { it.amount }

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
                                    val dateTotalText = when {
                                        dayDebits > 0 && dayCredits > 0 ->
                                            "- ${CategoryUiUtils.formatCurrency(dayDebits)}  •  + ${CategoryUiUtils.formatCurrency(dayCredits)}"
                                        dayCredits > 0 ->
                                            "+ " + CategoryUiUtils.formatCurrency(dayCredits)
                                        else ->
                                            "- " + CategoryUiUtils.formatCurrency(dayDebits)
                                    }
                                    val dateTotalColor = if (dayDebits == 0.0 && dayCredits > 0.0) BotanicalEmerald else BotanicalTextSecondary
                                    Text(
                                        text = dateTotalText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = dateTotalColor
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
                                                onClick = { selectedTransactionDetails = txn }
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

    // Transaction Details Bottom Sheet
    selectedTransactionDetails?.let { txn ->
        TransactionDetailsBottomSheet(
            transaction = txn,
            categories = categories,
            onCategoryChange = { newCat ->
                viewModel.updateCategory(txn.id, newCat)
                selectedTransactionDetails = txn.copy(category = newCat)
            },
            onDelete = {
                selectedTransactionDetails = null
                transactionToDelete = txn
            },
            onDismissRequest = { selectedTransactionDetails = null }
        )
    }
}

@Composable
private fun BotanicalTransactionRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val categoryColor = CategoryUiUtils.getCategoryColor(transaction.category)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeText = timeFormat.format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
        val isCredit = (transaction.direction == TransactionDirection.CREDIT)
        val amountPrefix = if (isCredit) "+ " else "- "
        val amountColor = if (isCredit) BotanicalEmerald else Color.White

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amountPrefix + CategoryUiUtils.formatCurrency(transaction.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (transaction.classification == TransactionClassification.REFUND) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Refund",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4DD0E1)
                    )
                } else if (transaction.classification == TransactionClassification.CASHBACK) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cashback",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                } else if (transaction.classification == TransactionClassification.TRANSFER) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transfer",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF90CAF9)
                    )
                }

                if (transaction.status == TransactionStatus.PENDING) {
                    if (transaction.classification != TransactionClassification.EXPENSE && transaction.classification != TransactionClassification.INCOME) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
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
}

/**
 * Calendar Date Range Picker Dialog:
 * Month navigation via side arrows (< and >),
 * Year selection via center Month-Year pill opening a year matrix popup.
 */
@Composable
private fun HistoryDateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startLocalEpoch: Long, endLocalEpoch: Long) -> Unit,
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null
) {
    val initialCal = remember(initialStartDateMillis) {
        Calendar.getInstance().apply {
            if (initialStartDateMillis != null) timeInMillis = initialStartDateMillis
        }
    }

    var displayedYear by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) } // 0..11
    var selectedStartDate by remember { mutableStateOf(initialStartDateMillis) }
    var selectedEndDate by remember { mutableStateOf(initialEndDateMillis) }
    var showYearMatrixDialog by remember { mutableStateOf(false) }

    val todayCal = remember { Calendar.getInstance() }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) } // 0..11
    val currentDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    val monthNames = remember {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }

    val canGoNextMonth = (displayedYear < currentYear) || (displayedYear == currentYear && displayedMonth < currentMonth)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF14241C),
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title & Month/Year Navigation Row with Side Arrows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Tappable Month Year pill that opens the Year Matrix
                    Surface(
                        onClick = { showYearMatrixDialog = true },
                        shape = CircleShape,
                        color = Color(0x33253B2F),
                        border = BorderStroke(1.dp, Color(0x33C4C59E))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${monthNames[displayedMonth]} $displayedYear",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Year",
                                tint = BotanicalEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (canGoNextMonth) {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            }
                        },
                        enabled = canGoNextMonth,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = if (canGoNextMonth) Color.White else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week header (S M T W T F S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BotanicalTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid for displayedMonth & displayedYear
                val monthCal = remember(displayedYear, displayedMonth) {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, displayedYear)
                        set(Calendar.MONTH, displayedMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
                val totalDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val totalSlots = ((firstDayOfWeek + totalDays + 6) / 7) * 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (week in 0 until (totalSlots / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dayOfWeek in 0..6) {
                                val slotIndex = week * 7 + dayOfWeek
                                val dayNum = slotIndex - firstDayOfWeek + 1

                                if (dayNum in 1..totalDays) {
                                    val isFuture = (displayedYear > currentYear) ||
                                            (displayedYear == currentYear && displayedMonth > currentMonth) ||
                                            (displayedYear == currentYear && displayedMonth == currentMonth && dayNum > currentDay)

                                    val dayStart = remember(displayedYear, displayedMonth, dayNum) {
                                        Calendar.getInstance().apply {
                                            set(Calendar.YEAR, displayedYear)
                                            set(Calendar.MONTH, displayedMonth)
                                            set(Calendar.DAY_OF_MONTH, dayNum)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }

                                    val dayEnd = remember(dayStart) { dayStart + 86_400_000L - 1L }

                                    val isStart = selectedStartDate != null && (dayStart == selectedStartDate || (selectedStartDate!! in dayStart..dayEnd))
                                    val isEnd = selectedEndDate != null && (dayEnd == selectedEndDate || (selectedEndDate!! in dayStart..dayEnd))
                                    val isInRange = selectedStartDate != null && selectedEndDate != null &&
                                            dayStart >= selectedStartDate!! && dayEnd <= selectedEndDate!!

                                    val cellBg = when {
                                        isStart || isEnd -> BotanicalEmerald
                                        isInRange -> BotanicalEmerald.copy(alpha = 0.25f)
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        isFuture -> Color.White.copy(alpha = 0.22f)
                                        isStart || isEnd -> Color(0xFF112117)
                                        isInRange -> Color.White
                                        else -> Color.White
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(34.dp)
                                            .clip(if (isStart || isEnd) CircleShape else RoundedCornerShape(4.dp))
                                            .background(cellBg)
                                            .clickable(enabled = !isFuture) {
                                                if (selectedStartDate == null || (selectedStartDate != null && selectedEndDate != null)) {
                                                    selectedStartDate = dayStart
                                                    selectedEndDate = null
                                                } else if (dayStart < selectedStartDate!!) {
                                                    selectedStartDate = dayStart
                                                    selectedEndDate = null
                                                } else {
                                                    selectedEndDate = dayEnd
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Readout of currently picked range
                val rangeFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
                val rangeLabel = when {
                    selectedStartDate != null && selectedEndDate != null ->
                        "${rangeFmt.format(Date(selectedStartDate!!))} – ${rangeFmt.format(Date(selectedEndDate!!))}"
                    selectedStartDate != null ->
                        "From ${rangeFmt.format(Date(selectedStartDate!!))} (select end date)"
                    else -> "Select start date"
                }

                Text(
                    text = rangeLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BotanicalEmerald,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = selectedStartDate
                    val end = selectedEndDate ?: start
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                    onDismissRequest()
                },
                enabled = selectedStartDate != null
            ) {
                Text("Apply", color = BotanicalEmerald, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = BotanicalTextSecondary)
            }
        }
    )

    // Year Matrix Popup Dialog
    if (showYearMatrixDialog) {
        AlertDialog(
            onDismissRequest = { showYearMatrixDialog = false },
            containerColor = Color(0xFF1B2E24),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Select Year",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                val years = ((currentYear - 8)..currentYear).toList()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.chunked(3).forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowYears.forEach { yr ->
                                val isSelected = yr == displayedYear
                                Surface(
                                    onClick = {
                                        displayedYear = yr
                                        if (yr == currentYear && displayedMonth > currentMonth) {
                                            displayedMonth = currentMonth
                                        }
                                        showYearMatrixDialog = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) BotanicalEmerald else Color(0x33253B2F),
                                    border = BorderStroke(1.dp, if (isSelected) BotanicalEmerald else Color(0x2EFFFFFF)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = yr.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF112117) else Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showYearMatrixDialog = false }) {
                    Text("Close", color = BotanicalTextSecondary)
                }
            }
        )
    }
}

/**
 * Transaction Details Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDetailsBottomSheet(
    transaction: Transaction,
    categories: List<Category>,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val categoryColor = CategoryUiUtils.getCategoryColor(transaction.category)
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = dateFormat.format(Date(transaction.timestamp))
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF14241C),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x4DFFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Category Icon, Merchant Name, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotanicalSquircleContainer(
                    icon = CategoryUiUtils.getCategoryIcon(transaction.category),
                    tint = categoryColor,
                    backgroundColor = categoryColor.copy(alpha = 0.2f),
                    size = 52.dp,
                    iconSize = 26.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Amount & Status Card
            BotanicalGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Amount",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val isCredit = (transaction.direction == TransactionDirection.CREDIT)
                        val amountPrefix = if (isCredit) "+ " else "- "
                        val amountColor = if (isCredit) BotanicalEmerald else Color.White
                        Text(
                            text = amountPrefix + CategoryUiUtils.formatCurrency(transaction.amount),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (transaction.classification != TransactionClassification.EXPENSE && transaction.classification != TransactionClassification.INCOME) {
                            val (badgeBg, badgeBorder, badgeTextColor) = when (transaction.classification) {
                                TransactionClassification.REFUND -> Triple(Color(0x334DD0E1), Color(0x664DD0E1), Color(0xFF4DD0E1))
                                TransactionClassification.CASHBACK -> Triple(Color(0x33FFD54F), Color(0x66FFD54F), Color(0xFFFFD54F))
                                TransactionClassification.TRANSFER -> Triple(Color(0x3390CAF9), Color(0x6690CAF9), Color(0xFF90CAF9))
                                else -> Triple(Color(0x3381C784), Color(0x6681C784), Color(0xFF81C784))
                            }
                            Surface(
                                shape = CircleShape,
                                color = badgeBg,
                                border = BorderStroke(1.dp, badgeBorder)
                            ) {
                                Text(
                                    text = transaction.classification.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Status Badge
                        Surface(
                            shape = CircleShape,
                            color = when (transaction.status) {
                                TransactionStatus.CONFIRMED -> Color(0x2B81C784)
                                TransactionStatus.PENDING -> Color(0x33FFB74D)
                                TransactionStatus.REJECTED -> Color(0x33FF5252)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (transaction.status) {
                                    TransactionStatus.CONFIRMED -> Color(0x4D81C784)
                                    TransactionStatus.PENDING -> Color(0x4DFFB74D)
                                    TransactionStatus.REJECTED -> Color(0x4DFF5252)
                                }
                            )
                        ) {
                            Text(
                                text = when (transaction.status) {
                                    TransactionStatus.CONFIRMED -> "Confirmed"
                                    TransactionStatus.PENDING -> "Pending"
                                    TransactionStatus.REJECTED -> "Rejected"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (transaction.status) {
                                    TransactionStatus.CONFIRMED -> Color(0xFF81C784)
                                    TransactionStatus.PENDING -> Color(0xFFFFB74D)
                                    TransactionStatus.REJECTED -> Color(0xFFFF6961)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Change Section
            Text(
                text = "Category",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BotanicalTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Category selector horizontal scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.name.equals(transaction.category, ignoreCase = true)
                    val cColor = CategoryUiUtils.getCategoryColor(cat.name)
                    Surface(
                        onClick = { onCategoryChange(cat.name) },
                        shape = CircleShape,
                        color = if (isSelected) BotanicalCreamPill else Color(0x26253B2F),
                        border = BorderStroke(1.dp, if (isSelected) BotanicalCreamPill else Color(0x26FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BotanicalCreamPillText else cColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BotanicalCreamPillText else Color.White
                            )
                        }
                    }
                }
            }

            // Reference ID (if present)
            if (!transaction.referenceId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                BotanicalGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reference / UPI ID",
                                fontSize = 11.sp,
                                color = BotanicalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = transaction.referenceId,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Reference ID", transaction.referenceId))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Reference ID",
                                tint = BotanicalEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Raw SMS Message Card (if available)
            if (transaction.rawNotification.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                BotanicalGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Original SMS Text",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BotanicalTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transaction.rawNotification,
                            fontSize = 12.sp,
                            color = Color(0xFFD4E0D6),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Transaction Button
            Surface(
                onClick = onDelete,
                shape = CircleShape,
                color = Color(0x26FF453A),
                border = BorderStroke(1.dp, Color(0x40FF453A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Transaction",
                        tint = Color(0xFFFF6961),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Delete Transaction",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF6961)
                    )
                }
            }
        }
    }
}
