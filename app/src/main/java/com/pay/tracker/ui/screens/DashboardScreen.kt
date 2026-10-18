package com.pay.tracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.TransactionDirection
import com.pay.tracker.core.model.TransactionClassification
import com.pay.tracker.core.sms.SmsImportResult
import com.pay.tracker.ui.components.AppWalkthroughSpotlightOverlay
import com.pay.tracker.ui.components.TOUR_BUDGET
import com.pay.tracker.ui.components.TOUR_CASH_FLOW
import com.pay.tracker.ui.components.TOUR_CATEGORIES
import com.pay.tracker.ui.components.TOUR_MONTH
import com.pay.tracker.ui.components.TOUR_REVIEW
import com.pay.tracker.ui.components.TOUR_SCAN
import com.pay.tracker.ui.components.BotanicalCreamButton
import com.pay.tracker.ui.components.BotanicalGlassButton
import com.pay.tracker.ui.components.BotanicalGlassCard
import com.pay.tracker.ui.components.BotanicalSquircleContainer
import com.pay.tracker.ui.components.CategoryUiUtils
import com.pay.tracker.ui.components.SmsScanningProgressDialog
import com.pay.tracker.ui.theme.BotanicalCreamPill
import com.pay.tracker.ui.theme.BotanicalCreamPillText
import com.pay.tracker.ui.theme.BotanicalEmerald
import com.pay.tracker.ui.theme.BotanicalGlassBorder
import com.pay.tracker.ui.theme.BotanicalGlassBorderSubtle
import com.pay.tracker.ui.theme.BotanicalGlassButton
import com.pay.tracker.ui.theme.BotanicalGlassButtonBorder
import com.pay.tracker.ui.theme.BotanicalGlassSurface
import com.pay.tracker.ui.theme.BotanicalGlassSurfaceLow
import com.pay.tracker.ui.theme.BotanicalTextPrimary
import com.pay.tracker.ui.theme.BotanicalTextSecondary
import com.pay.tracker.ui.viewmodel.BudgetPacing
import com.pay.tracker.ui.viewmodel.CategoryExpense
import com.pay.tracker.ui.viewmodel.DailySpend
import com.pay.tracker.ui.viewmodel.DashboardTimeframe
import com.pay.tracker.ui.viewmodel.DashboardViewModel
import com.pay.tracker.ui.viewmodel.TimeframeSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReview: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val pendingList by viewModel.pendingTransactions.collectAsState()
    val monthDailyAverage by viewModel.monthDailyAverage.collectAsState()
    val monthDailySpendHistory by viewModel.monthDailySpendHistory.collectAsState()
    val importResult by viewModel.importState.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val showWalkthrough by viewModel.showWalkthrough.collectAsState()
    val budgetPacing by viewModel.budgetPacing.collectAsState()
    val timeframeSummary by viewModel.timeframeSummary.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val previousMonthTotal by viewModel.previousMonthTotal.collectAsState()
    val confirmedCount by viewModel.confirmedTransactionsCount.collectAsState()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val yearlySpendSummaries by viewModel.yearlySpendSummaries.collectAsState()
    val monthlySpendMap by viewModel.monthlySpendMap.collectAsState()
    val cashFlowMetrics by viewModel.cashFlowMetrics.collectAsState()
    val peakExpense by viewModel.peakExpense.collectAsState()

    val dashboardListState = rememberLazyListState()
    var walkthroughStep by remember { mutableIntStateOf(0) }
    val tourTargets = remember { mutableStateMapOf<Int, Rect>() }
    val hasAnnualSpend = yearlySpendSummaries.isNotEmpty()
    val walkthroughTargetItem = when (walkthroughStep) {
        TOUR_REVIEW -> 1
        TOUR_MONTH -> 2
        TOUR_CASH_FLOW -> 3
        TOUR_SCAN -> if (hasAnnualSpend) 5 else 4
        TOUR_BUDGET -> if (hasAnnualSpend) 6 else 5
        else -> if (hasAnnualSpend) 8 else 7
    }

    LaunchedEffect(showWalkthrough, walkthroughStep, walkthroughTargetItem) {
        if (showWalkthrough) {
            // Remove the previous position until the new target has been laid out.
            tourTargets.remove(walkthroughStep)
            dashboardListState.animateScrollToItem(walkthroughTargetItem)
        }
    }

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showScanOptionDialog by remember { mutableStateOf(false) }
    var showYearlyBreakdownSheet by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showScanOptionDialog = true
        }
    }

    val onScanSmsClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            showScanOptionDialog = true
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    val pendingCount = pendingList.size

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
        LazyColumn(
            state = dashboardListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 1. Botanical Header Top Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_REVIEW] = it.boundsInRoot() }
                ) {
                    DashboardHeaderRow(
                        pendingCount = pendingCount,
                        onBellClick = onNavigateToReview
                    )
                }
            }

            // 2. Month Selector Navigation Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_MONTH] = it.boundsInRoot() }
                ) {
                    DashboardMonthSelector(
                        selectedMonth = selectedYearMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onJumpToCurrentMonth = { viewModel.jumpToCurrentMonth() }
                    )
                }
            }

            // 3. Hero Spending Card with Velocity Bar Chart
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_CASH_FLOW] = it.boundsInRoot() }
                ) {
                    HeroSpendingVelocityCard(
                        total = monthlyTotal,
                        dailyAverage = monthDailyAverage,
                        currentDay = budgetPacing.currentDayOfMonth,
                        totalDays = budgetPacing.totalDaysInMonth,
                        dailySpends = monthDailySpendHistory,
                        isCurrentMonth = selectedYearMonth.isCurrentMonth,
                        previousMonthTotal = previousMonthTotal,
                        incomeReceived = cashFlowMetrics.incomeReceived,
                        netCashFlow = cashFlowMetrics.netCashFlow
                    )
                }
            }

            // 3b. Quick Monthly Spend by Year Overview Banner
            if (yearlySpendSummaries.isNotEmpty()) {
                item {
                    AnnualSpendBreakdownBanner(
                        yearlySpendSummaries = yearlySpendSummaries,
                        onViewBreakdownClick = { showYearlyBreakdownSheet = true }
                    )
                }
            }

            // 4. Action Buttons: Cream Pill "+ Add Expense" & Frosted Glass "Scan SMS"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_SCAN] = it.boundsInRoot() }
                ) {
                    ActionButtonsRow(
                        onAddExpenseClick = { showAddExpenseSheet = true },
                        onScanSmsClick = onScanSmsClick,
                        isImporting = isImporting
                    )
                }
            }

            // 5. Monthly Budget Fuel Gauge Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_BUDGET] = it.boundsInRoot() }
                ) {
                    BotanicalBudgetCard(
                        pacing = budgetPacing,
                        isCurrentMonth = selectedYearMonth.isCurrentMonth,
                        onEditBudgetClick = {
                            if (selectedYearMonth.isCurrentMonth) {
                                showEditBudgetDialog = true
                            }
                        }
                    )
                }
            }

            // 6. 3-Stat Metric Cards Row: Transactions, Top Category, Peak Expense
            item {
                ThreeStatCardsRow(
                    confirmedCount = confirmedCount,
                    isCurrentMonth = selectedYearMonth.isCurrentMonth,
                    topCategoryExpense = categoryBreakdown.firstOrNull(),
                    peakExpense = peakExpense
                )
            }

            // 7. Category Allocation Card with Donut Chart and Legend
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { tourTargets[TOUR_CATEGORIES] = it.boundsInRoot() }
                ) {
                    BotanicalCategoryAllocationCard(
                        categoryBreakdown = categoryBreakdown,
                        totalSpend = monthlyTotal,
                        onViewAllClick = onNavigateToHistory
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Clearance for floating bottom nav
            }
        }
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = budgetPacing.budgetTarget,
            onDismiss = { showEditBudgetDialog = false },
            onConfirm = { viewModel.updateBudgetTarget(it) }
        )
    }

    if (showAddExpenseSheet) {
        AddExpenseBottomSheet(
            onDismissRequest = { showAddExpenseSheet = false },
            onSaveTransaction = { amount, merchant, category, direction, classification ->
                viewModel.addManualTransaction(amount, merchant, category, direction, classification)
            }
        )
    }

    importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { viewModel.dismissImportResult() }
        )
    }

    if (showScanOptionDialog) {
        ScanSmsOptionsDialog(
            selectedMonthName = selectedYearMonth.displayName,
            onScanAllHistory = {
                showScanOptionDialog = false
                viewModel.importAllSms(context)
            },
            onScanThisMonth = {
                showScanOptionDialog = false
                viewModel.importMonthSms(context)
            },
            onDismiss = { showScanOptionDialog = false }
        )
    }

    if (showYearlyBreakdownSheet) {
        MonthlySpendByYearBottomSheet(
            yearlySpendSummaries = yearlySpendSummaries,
            selectedMonth = selectedYearMonth,
            onSelectMonth = { viewModel.selectYearMonth(it) },
            onDismissRequest = { showYearlyBreakdownSheet = false }
        )
    }

    if (importProgress.isImporting) {
        SmsScanningProgressDialog(
            scanned = importProgress.scanned,
            total = importProgress.total,
            foundCount = importProgress.foundCount
        )
    }

    if (showWalkthrough) {
        AppWalkthroughSpotlightOverlay(
            stepIndex = walkthroughStep,
            targetBounds = tourTargets[walkthroughStep],
            onStepChange = { nextStep ->
                walkthroughStep = nextStep
                tourTargets.remove(nextStep)
            },
            onDismiss = {
                walkthroughStep = 0
                viewModel.dismissWalkthrough()
            }
        )
    }
    }
}

/**
 * Top bar matching Botanical Glass design
 */
@Composable
private fun DashboardHeaderRow(
    pendingCount: Int,
    onBellClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Pay Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Track smarter. Spend better.",
                fontSize = 13.sp,
                color = BotanicalTextSecondary
            )
        }

        // Circular Frosted Glass Bell Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x33253B2F))
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(BotanicalGlassBorder, BotanicalGlassBorderSubtle))),
                    CircleShape
                )
                .clickable(onClick = onBellClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = if (pendingCount > 0) BotanicalEmerald else Color.White,
                modifier = Modifier.size(20.dp)
            )
            if (pendingCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape)
                        .background(com.pay.tracker.ui.theme.PendingAmber)
                )
            }
        }
    }
}

/**
 * Month Navigation Bar
 */
@Composable
private fun DashboardMonthSelector(
    selectedMonth: com.pay.tracker.ui.viewmodel.YearMonthSelection,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit
) {
    val canGoNext = !selectedMonth.isCurrentMonth

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Month Selector with Prev & Next arrows
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = selectedMonth.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = onNextMonth,
                enabled = canGoNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = if (canGoNext) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Contextual action: Return to current month when browsing a past month
        if (!selectedMonth.isCurrentMonth) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = onJumpToCurrentMonth,
                shape = CircleShape,
                color = BotanicalCreamPill,
                border = BorderStroke(1.dp, BotanicalCreamPill)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Month ↩",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalCreamPillText
                    )
                }
            }
        }
    }
}

/**
 * Hero Spending Card with Velocity Bar Chart
 */
@Composable
private fun HeroSpendingVelocityCard(
    total: Double,
    dailyAverage: Double,
    currentDay: Int,
    totalDays: Int,
    dailySpends: List<DailySpend>,
    isCurrentMonth: Boolean,
    previousMonthTotal: Double,
    incomeReceived: Double = 0.0,
    netCashFlow: Double = 0.0
) {
    BotanicalGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Amount & Trend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Total Spent",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BotanicalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CategoryUiUtils.formatCurrency(total),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Received: +${CategoryUiUtils.formatCurrency(incomeReceived)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = BotanicalEmerald.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    val netSign = if (netCashFlow >= 0) "+" else "-"
                    val netColor = if (netCashFlow >= 0) BotanicalEmerald else Color(0xFFFF857D)
                    Text(
                        text = "Net Cash Flow: $netSign${CategoryUiUtils.formatCurrency(Math.abs(netCashFlow))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = netColor.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Dynamic Trend badge vs previous month
                if (previousMonthTotal > 0.0) {
                    val diff = total - previousMonthTotal
                    val pct = ((diff / previousMonthTotal) * 100).toInt()
                    val isDown = diff < 0
                    val isUp = diff > 0
                    val badgeColor = if (isDown) com.pay.tracker.ui.theme.BotanicalBarActive else if (isUp) Color(0xFFFF6961) else Color.White
                    val badgeIcon = if (isDown) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp

                    Surface(
                        shape = CircleShape,
                        color = Color(0x33253B2F),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (diff != 0.0) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = when {
                                    isDown -> "${-pct}% vs last month"
                                    isUp -> "+$pct% vs last month"
                                    else -> "0% vs last month"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }
                } else if (dailyAverage > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x33253B2F),
                        border = BorderStroke(1.dp, Color(0x33C4C59E))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Avg ₹${"%,.0f".format(dailyAverage)}/day",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.pay.tracker.ui.theme.BotanicalBarActive
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Velocity Bar Chart (dynamic days)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val maxSpend = dailySpends.maxOfOrNull { it.totalAmount }?.coerceAtLeast(100.0) ?: 1000.0

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val spendList = if (dailySpends.isNotEmpty()) dailySpends else List(totalDays) { DailySpend(it.toString(), it.toString(), 0.0) }

                    spendList.forEachIndexed { index, ds ->
                        val dayNum = index + 1
                        val isElapsed = if (isCurrentMonth) dayNum <= currentDay else true
                        val hasSpend = ds.totalAmount > 0.0
                        val heightFraction = if (hasSpend) {
                            (ds.totalAmount / maxSpend).toFloat().coerceIn(0.12f, 1f)
                        } else if (isElapsed) {
                            0.06f // subtle tick for elapsed days with 0 spend
                        } else {
                            0.04f // subtle tick for future days
                        }

                        val barColor = when {
                            hasSpend && ds.totalAmount >= maxSpend * 0.7 -> com.pay.tracker.ui.theme.BotanicalBarActive
                            hasSpend -> com.pay.tracker.ui.theme.BotanicalBarRegular
                            isElapsed -> com.pay.tracker.ui.theme.BotanicalBarMuted
                            else -> com.pay.tracker.ui.theme.BotanicalBarTrack
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(heightFraction)
                                .padding(horizontal = 1.dp)
                                .clip(CircleShape)
                                .background(barColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day labels along bottom dynamically bounded to month's total days
            val labelDays = listOf(1, 5, 10, 15, 20, 25, totalDays).filter { it <= totalDays }.distinct()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labelDays.forEach { day ->
                    Text(
                        text = day.toString(),
                        fontSize = 10.sp,
                        color = BotanicalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtle horizontal divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x14FFFFFF))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("Avg ₹%,.2f/day", dailyAverage),
                    fontSize = 12.sp,
                    color = BotanicalTextSecondary
                )
                Text(
                    text = if (isCurrentMonth) "$currentDay/$totalDays days" else "$totalDays days in month",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BotanicalTextSecondary
                )
            }
        }
    }
}

/**
 * Action Buttons Row: Cream Pill "+ Add Expense" & Frosted Glass "Scan SMS"
 */
@Composable
private fun ActionButtonsRow(
    onAddExpenseClick: () -> Unit,
    onScanSmsClick: () -> Unit,
    isImporting: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BotanicalCreamButton(
            text = "+ Add Expense",
            onClick = onAddExpenseClick,
            modifier = Modifier.weight(1f)
        )

        BotanicalGlassButton(
            text = if (isImporting) "Scanning..." else "Scan SMS",
            onClick = onScanSmsClick,
            icon = Icons.Default.Download,
            enabled = !isImporting,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Monthly Budget Fuel Gauge Card
 */
@Composable
private fun BotanicalBudgetCard(
    pacing: BudgetPacing,
    isCurrentMonth: Boolean,
    onEditBudgetClick: () -> Unit
) {
    val progressFraction by animateFloatAsState(
        targetValue = (pacing.percentUsed / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "budgetProgress"
    )

    BotanicalGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isCurrentMonth) onEditBudgetClick else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wallet Squircle Icon in warm cream / dark glass
                BotanicalSquircleContainer(
                    icon = Icons.Default.Savings,
                    tint = if (isCurrentMonth) BotanicalCreamPill else BotanicalEmerald,
                    backgroundColor = Color(0x33253B2F),
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentMonth) "Monthly Budget" else "Month Closed",
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (isCurrentMonth) {
                        Text(
                            text = "${CategoryUiUtils.formatCurrency(pacing.remainingBudget)} remaining",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (pacing.eligibleOffsets > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Net left: ${CategoryUiUtils.formatCurrency(pacing.netRemainingBudget)} (after +₹${"%,.0f".format(pacing.eligibleOffsets)} refunds)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = BotanicalEmerald
                            )
                        }
                    } else {
                        if (!pacing.isExceeded) {
                            Text(
                                text = "Saved ${CategoryUiUtils.formatCurrency(pacing.remainingBudget)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            if (pacing.eligibleOffsets > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Net saved: ${CategoryUiUtils.formatCurrency(pacing.netRemainingBudget)} (after +₹${"%,.0f".format(pacing.eligibleOffsets)} refunds)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BotanicalEmerald
                                )
                            }
                        } else {
                            val overSpend = pacing.spentThisMonth - pacing.budgetTarget
                            Text(
                                text = "Over by ${CategoryUiUtils.formatCurrency(overSpend)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                            if (pacing.eligibleOffsets > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                val netOver = pacing.netSpentThisMonth - pacing.budgetTarget
                                val netOverText = if (netOver > 0) "Net over: ₹${"%,.0f".format(netOver)}" else "Net saved: ${CategoryUiUtils.formatCurrency(pacing.netRemainingBudget)}"
                                Text(
                                    text = "$netOverText (after +₹${"%,.0f".format(pacing.eligibleOffsets)} refunds)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BotanicalEmerald
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isCurrentMonth) {
                        Text(
                            text = "${pacing.percentUsed.toInt()}% used",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pacing.isExceeded) Color(0xFFFF5252) else Color(0xFFC4D1BA)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "of ${CategoryUiUtils.formatCurrency(pacing.budgetTarget)}",
                            fontSize = 11.sp,
                            color = BotanicalTextSecondary
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (pacing.isExceeded) Color(0x33FF5252) else Color(0x332E7D32),
                            border = BorderStroke(1.dp, if (pacing.isExceeded) Color(0x4DFF5252) else Color(0x4D81C784))
                        ) {
                            Text(
                                text = if (pacing.isExceeded) "Exceeded" else "Under Budget",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pacing.isExceeded) Color(0xFFFF6961) else Color(0xFF81C784),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${pacing.percentUsed.toInt()}% of budget",
                            fontSize = 11.sp,
                            color = BotanicalTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sleek Horizontal Progress Bar with Botanical Bone to Sage Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(com.pay.tracker.ui.theme.BotanicalBudgetTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (pacing.isExceeded) {
                                Brush.horizontalGradient(listOf(Color(0xFFFF5252), Color(0xFFFF7A7A)))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        com.pay.tracker.ui.theme.BotanicalBudgetBarStart,
                                        com.pay.tracker.ui.theme.BotanicalBudgetBarEnd
                                    )
                                )
                            }
                        )
                )
            }
        }
    }
}


/**
 * 3-Stat Metric Cards Row: Transactions, Top Category, Peak Expense
 */
@Composable
private fun ThreeStatCardsRow(
    confirmedCount: Int,
    isCurrentMonth: Boolean,
    topCategoryExpense: CategoryExpense?,
    peakExpense: DashboardViewModel.PeakExpenseInfo
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Transaction Activity Count
        BotanicalGlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = confirmedCount.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isCurrentMonth) "Transactions" else "Total txns",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
                )
            }
        }

        // 2. Top Spending Category
        val topCatName = topCategoryExpense?.category ?: "—"
        val topCatPct = topCategoryExpense?.let { "${(it.percentage * 100).toInt()}%" } ?: ""
        BotanicalGlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = topCatName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (topCategoryExpense != null) CategoryUiUtils.getCategoryColor(topCatName) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (topCatPct.isNotBlank()) "Top: $topCatPct" else "Top category",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Peak / Largest Single Expense
        val peakAmountText = if (peakExpense.amount > 0.0) CategoryUiUtils.formatCurrency(peakExpense.amount) else "—"
        val peakSubText = if (peakExpense.merchant.isNotBlank()) peakExpense.merchant else "Peak spend"
        BotanicalGlassCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = peakAmountText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = peakSubText,
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Category Allocation Card with Donut Chart and Legend
 */
@Composable
private fun BotanicalCategoryAllocationCard(
    categoryBreakdown: List<CategoryExpense>,
    totalSpend: Double,
    onViewAllClick: () -> Unit
) {
    BotanicalGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Allocation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "View all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalEmerald,
                    modifier = Modifier.clickable(onClick = onViewAllClick)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryBreakdown.isEmpty() || totalSpend <= 0.0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No category expenses recorded for this month",
                        fontSize = 13.sp,
                        color = BotanicalTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(126.dp)) {
                            val strokeWidth = 14.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                            var startAngle = -90f
                            val gapAngle = if (categoryBreakdown.size > 1) 3f else 0f
                            val availableAngle = 360f - (gapAngle * categoryBreakdown.size)

                            categoryBreakdown.forEach { item ->
                                val sweep = (item.percentage / 100f * availableAngle).coerceAtLeast(2f)
                                val color = CategoryUiUtils.getCategoryColor(item.category)

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep + gapAngle
                            }
                        }

                        // Center text of Donut
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total",
                                fontSize = 10.sp,
                                color = BotanicalTextSecondary
                            )
                            Text(
                                text = CategoryUiUtils.formatCurrency(totalSpend).substringBefore("."),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Legend List on the right
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displayItems = categoryBreakdown.take(5)

                        displayItems.forEach { item ->
                            val color = CategoryUiUtils.getCategoryColor(item.category)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.category,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${item.percentage.toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BotanicalTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Edit Budget Dialog styled for Botanical Glass
 */
@Composable
private fun EditBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var textValue by remember {
        mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "10000")
    }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0101D16),
        title = {
            Text(
                text = "Set Monthly Budget",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your monthly spending limit to track daily pace and remaining balance.",
                    fontSize = 13.sp,
                    color = BotanicalTextSecondary
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it.filter { ch -> ch.isDigit() }
                        isError = false
                    },
                    label = { Text("Budget (₹)", color = BotanicalTextSecondary) },
                    prefix = { Text("₹ ", color = Color.White) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BotanicalEmerald,
                        unfocusedBorderColor = BotanicalGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onConfirm(parsed)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotanicalCreamPill,
                    contentColor = BotanicalCreamPillText
                ),
                shape = CircleShape
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BotanicalTextSecondary)
            }
        }
    )
}

/**
 * Add Expense Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseBottomSheet(
    onDismissRequest: () -> Unit,
    onSaveTransaction: (Double, String, String, TransactionDirection, TransactionClassification) -> Unit
) {
    var isExpenseType by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var hasError by remember { mutableStateOf(false) }

    val categories = remember(isExpenseType) {
        if (isExpenseType) Category.EXPENSE_CATEGORIES else Category.INCOME_CATEGORIES
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF50D1912),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
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
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isExpenseType) "Add Expense" else "Add Income",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Segmented Toggle: Expense (Money Out) vs Income (Money In)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x33253B2F))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = {
                        isExpenseType = true
                        selectedCategory = "Food"
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isExpenseType) BotanicalCreamPill else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense (Money Out)",
                            fontSize = 12.sp,
                            fontWeight = if (isExpenseType) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExpenseType) BotanicalCreamPillText else BotanicalTextSecondary
                        )
                    }
                }

                Surface(
                    onClick = {
                        isExpenseType = false
                        selectedCategory = "Salary"
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isExpenseType) BotanicalCreamPill else Color.Transparent,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income (Money In)",
                            fontSize = 12.sp,
                            fontWeight = if (!isExpenseType) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isExpenseType) BotanicalCreamPillText else BotanicalTextSecondary
                        )
                    }
                }
            }

            // Amount field
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; hasError = false },
                label = { Text("Amount (₹)", color = BotanicalTextSecondary) },
                prefix = { Text("₹ ", color = Color.White) },
                singleLine = true,
                isError = hasError && (amountText.toDoubleOrNull() ?: 0.0) <= 0.0,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BotanicalEmerald,
                    unfocusedBorderColor = BotanicalGlassBorder
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Merchant / Source field
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it; hasError = false },
                label = {
                    Text(
                        text = if (isExpenseType) "Paid to / Merchant" else "Received from / Source",
                        color = BotanicalTextSecondary
                    )
                },
                placeholder = {
                    Text(
                        text = if (isExpenseType) "e.g. Swiggy, Starbucks, Grocery Store" else "e.g. Employer, Client, Friend, Bank",
                        color = Color(0x66FFFFFF)
                    )
                },
                singleLine = true,
                isError = hasError && merchantText.trim().isEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BotanicalEmerald,
                    unfocusedBorderColor = BotanicalGlassBorder
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // Category selector
            Text(
                text = "Select Category",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BotanicalTextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat.name == selectedCategory
                    val catColor = CategoryUiUtils.getCategoryColor(cat.name)

                    Surface(
                        onClick = { selectedCategory = cat.name },
                        shape = CircleShape,
                        color = if (isSelected) BotanicalCreamPill else Color(0x26253B2F),
                        border = BorderStroke(1.dp, if (isSelected) BotanicalCreamPill else Color(0x26FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CategoryUiUtils.getCategoryIcon(cat.name),
                                contentDescription = null,
                                tint = if (isSelected) BotanicalCreamPillText else catColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BotanicalCreamPillText else Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            BotanicalCreamButton(
                text = if (isExpenseType) "Save Expense" else "Save Income",
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    val merchant = merchantText.trim()
                    if (parsedAmount == null || parsedAmount <= 0.0 || merchant.isEmpty()) {
                        hasError = true
                    } else {
                        val direction = if (isExpenseType) TransactionDirection.DEBIT else TransactionDirection.CREDIT
                        val classification = if (isExpenseType) {
                            TransactionClassification.EXPENSE
                        } else {
                            when (selectedCategory) {
                                "Refund" -> TransactionClassification.REFUND
                                "Cashback" -> TransactionClassification.CASHBACK
                                else -> TransactionClassification.INCOME
                            }
                        }
                        onSaveTransaction(parsedAmount, merchant, selectedCategory, direction, classification)
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Import Result Dialog
 */
@Composable
private fun ImportResultDialog(
    result: SmsImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0101D16),
        title = {
            Text(
                text = if (result.errorMessage != null) "Scan Error" else "SMS Scan Complete",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            if (result.errorMessage != null) {
                Text(result.errorMessage, color = Color(0xFFFF5252))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Imported ${result.importedCount} transactions.",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Total spend: ${CategoryUiUtils.formatCurrency(result.totalAmount)}",
                        color = BotanicalEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    if (result.skippedDuplicates > 0) {
                        Text(
                            text = "Skipped ${result.skippedDuplicates} duplicate or already imported records.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary
                        )
                    }
                    Text(
                        text = "Scanned ${result.totalScanned} SMS messages total.",
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotanicalCreamPill,
                    contentColor = BotanicalCreamPillText
                ),
                shape = CircleShape
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Quick banner showing overview of annual & monthly spend
 */
@Composable
private fun AnnualSpendBreakdownBanner(
    yearlySpendSummaries: List<com.pay.tracker.ui.viewmodel.YearlySpendSummary>,
    onViewBreakdownClick: () -> Unit
) {
    Surface(
        onClick = onViewBreakdownClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x33253B2F),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(BotanicalGlassBorder, BotanicalGlassBorderSubtle))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33104F32)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = BotanicalEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Monthly Spend Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val totalMonths = yearlySpendSummaries.sumOf { it.months.size }
                    Text(
                        text = "Calculated across $totalMonths active months • Tap to view all",
                        fontSize = 11.sp,
                        color = BotanicalTextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Breakdown",
                tint = BotanicalEmerald,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Scan SMS Options Dialog
 */
@Composable
private fun ScanSmsOptionsDialog(
    selectedMonthName: String,
    onScanAllHistory: () -> Unit,
    onScanThisMonth: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF0101D16),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = BotanicalEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan SMS Transactions",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Extract completed debit transactions from your bank and UPI SMS alerts.",
                    fontSize = 13.sp,
                    color = BotanicalTextSecondary
                )

                // Option 1: Scan All History (Recommended)
                Surface(
                    onClick = onScanAllHistory,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33104F32),
                    border = BorderStroke(1.5.dp, BotanicalEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scan All Historical SMS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = CircleShape,
                                color = BotanicalEmerald
                            ) {
                                Text(
                                    text = "Recommended",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0A150F),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scans all previous transactions across all months and years. Automatically calculates money spent for each month in each year.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Option 2: Current Month Only
                Surface(
                    onClick = onScanThisMonth,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x33253B2F),
                    border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Scan This Month Only",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Only scans SMS received in $selectedMonthName.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BotanicalTextSecondary)
            }
        }
    )
}

/**
 * Bottom Sheet displaying money spent for each month in each year
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlySpendByYearBottomSheet(
    yearlySpendSummaries: List<com.pay.tracker.ui.viewmodel.YearlySpendSummary>,
    selectedMonth: com.pay.tracker.ui.viewmodel.YearMonthSelection,
    onSelectMonth: (com.pay.tracker.ui.viewmodel.YearMonthSelection) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xF50E1A14),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Monthly Spend by Year",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Calculated spending for each month across all years",
                fontSize = 12.sp,
                color = BotanicalTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (yearlySpendSummaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transaction data yet. Tap 'Scan SMS' to import.",
                        color = BotanicalTextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(yearlySpendSummaries) { yearSummary ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x33253B2F),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Year Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${yearSummary.year}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${"%,.2f".format(yearSummary.totalAmount)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BotanicalEmerald
                                        )
                                        Text(
                                            text = "${yearSummary.transactionCount} transactions",
                                            fontSize = 11.sp,
                                            color = BotanicalTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0x1AFFFFFF))
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Month Rows
                                yearSummary.months.forEach { monthItem ->
                                    val isCurrentViewing = (selectedMonth.year == yearSummary.year && selectedMonth.month == monthItem.month)
                                    Surface(
                                        onClick = {
                                            onSelectMonth(com.pay.tracker.ui.viewmodel.YearMonthSelection(yearSummary.year, monthItem.month))
                                            onDismissRequest()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isCurrentViewing) Color(0x33104F32) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = monthItem.monthName,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrentViewing) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrentViewing) BotanicalEmerald else Color.White
                                                )
                                                if (monthItem.isCurrentMonth) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = BotanicalEmerald.copy(alpha = 0.2f),
                                                        border = BorderStroke(0.5.dp, BotanicalEmerald)
                                                    ) {
                                                        Text(
                                                            text = "Current",
                                                            fontSize = 9.sp,
                                                            color = BotanicalEmerald,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                if (isCurrentViewing) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• Viewing",
                                                        fontSize = 11.sp,
                                                        color = BotanicalEmerald
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "₹${"%,.2f".format(monthItem.totalAmount)}",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${monthItem.transactionCount} txns",
                                                        fontSize = 10.sp,
                                                        color = BotanicalTextSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = "View month",
                                                    tint = BotanicalTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
