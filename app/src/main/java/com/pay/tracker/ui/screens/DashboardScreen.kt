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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pay.tracker.core.model.Category
import com.pay.tracker.core.sms.SmsImportResult
import com.pay.tracker.ui.components.BotanicalCreamButton
import com.pay.tracker.ui.components.BotanicalGlassButton
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
    val budgetPacing by viewModel.budgetPacing.collectAsState()
    val timeframeSummary by viewModel.timeframeSummary.collectAsState()

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.importMonthSms(context)
        }
    }

    val onScanSmsClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.importMonthSms(context)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    val pendingCount = pendingList.size

    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
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
                DashboardHeaderRow(
                    pendingCount = pendingCount,
                    onBellClick = onNavigateToReview
                )
            }

            // 2. Month Selector Navigation Bar
            item {
                DashboardMonthSelector(
                    monthText = currentMonthYear
                )
            }

            // 3. Hero Spending Card with Velocity Bar Chart
            item {
                HeroSpendingVelocityCard(
                    total = monthlyTotal,
                    dailyAverage = monthDailyAverage,
                    currentDay = budgetPacing.currentDayOfMonth,
                    totalDays = budgetPacing.totalDaysInMonth,
                    dailySpends = monthDailySpendHistory
                )
            }

            // 4. Action Buttons: Cream Pill "+ Add Expense" & Frosted Glass "Scan SMS"
            item {
                ActionButtonsRow(
                    onAddExpenseClick = { showAddExpenseSheet = true },
                    onScanSmsClick = onScanSmsClick,
                    isImporting = isImporting
                )
            }

            // 5. Monthly Budget Fuel Gauge Card
            item {
                BotanicalBudgetCard(
                    pacing = budgetPacing,
                    onEditBudgetClick = { showEditBudgetDialog = true }
                )
            }

            // 6. 3-Stat Metric Cards Row: Daily Pace, Days Elapsed, Daily Average
            item {
                ThreeStatCardsRow(
                    dailyPace = budgetPacing.safeDailyPace,
                    daysElapsed = budgetPacing.currentDayOfMonth,
                    dailyAverage = monthDailyAverage
                )
            }

            // 7. Category Allocation Card with Donut Chart and Legend
            item {
                BotanicalCategoryAllocationCard(
                    summary = timeframeSummary,
                    onViewAllClick = onNavigateToHistory
                )
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
            onSaveExpense = { amount, merchant, category ->
                viewModel.addManualExpense(amount, merchant, category)
            }
        )
    }

    importResult?.let { result ->
        ImportResultDialog(
            result = result,
            onDismiss = { viewModel.dismissImportResult() }
        )
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
    monthText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { /* Prev month if multi-month supported */ }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                tint = BotanicalTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = monthText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Pill Dropdown Button "This Month ∨"
        Surface(
            shape = CircleShape,
            color = Color(0x33253B2F),
            border = BorderStroke(1.dp, Color(0x2EFFFFFF))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Month",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BotanicalTextPrimary
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
    dailySpends: List<DailySpend>
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
                Column {
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
                }

                // Trend badge "↓ 12% vs last month"
                Surface(
                    shape = CircleShape,
                    color = Color(0x33253B2F),
                    border = BorderStroke(1.dp, Color(0x33C4C59E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = com.pay.tracker.ui.theme.BotanicalBarActive,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "12% vs last month",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.pay.tracker.ui.theme.BotanicalBarActive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Velocity Bar Chart (31 bars)
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
                    val daysCount = if (dailySpends.isNotEmpty()) dailySpends.size else totalDays
                    val spendList = if (dailySpends.isNotEmpty()) dailySpends else List(totalDays) { DailySpend(it.toString(), it.toString(), 0.0) }

                    spendList.forEachIndexed { index, ds ->
                        val dayNum = index + 1
                        val isElapsed = dayNum <= currentDay
                        val hasSpend = ds.totalAmount > 0.0
                        val heightFraction = if (hasSpend) {
                            (ds.totalAmount / maxSpend).toFloat().coerceIn(0.12f, 1f)
                        } else if (isElapsed) {
                            0.06f // subtle tick for past days with 0 spend
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

            // Day labels along bottom: 1, 5, 10, 15, 20, 25, 31
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1, 5, 10, 15, 20, 25, 31).forEach { day ->
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
                    text = "$currentDay/$totalDays days",
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
    onEditBudgetClick: () -> Unit
) {
    val progressFraction by animateFloatAsState(
        targetValue = (pacing.percentUsed / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "budgetProgress"
    )

    val progressColor = if (pacing.isExceeded) Color(0xFFFF5252) else com.pay.tracker.ui.theme.BotanicalBudgetBarStart

    BotanicalGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEditBudgetClick
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
                    tint = BotanicalCreamPill,
                    backgroundColor = Color(0x33253B2F),
                    size = 40.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Budget",
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${CategoryUiUtils.formatCurrency(pacing.remainingBudget)} remaining",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
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
 * 3-Stat Metric Cards Row
 */
@Composable
private fun ThreeStatCardsRow(
    dailyPace: Double,
    daysElapsed: Int,
    dailyAverage: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily Pace
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
                    text = CategoryUiUtils.formatCurrency(dailyPace),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Daily pace",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
                )
            }
        }

        // Days Elapsed
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
                    text = daysElapsed.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Days elapsed",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
                )
            }
        }

        // Daily Average
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
                    text = CategoryUiUtils.formatCurrency(dailyAverage),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Daily average",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
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
    summary: TimeframeSummary,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val effectiveBreakdown = if (summary.breakdown.isNotEmpty() && summary.totalSpend > 0.0) {
                    summary.breakdown
                } else {
                    listOf(
                        CategoryExpense("Food & Dining", 0.0, 38f),
                        CategoryExpense("Shopping", 0.0, 24f),
                        CategoryExpense("Bills", 0.0, 18f),
                        CategoryExpense("Transport", 0.0, 12f),
                        CategoryExpense("Others", 0.0, 8f)
                    )
                }

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
                        val gapAngle = if (effectiveBreakdown.size > 1) 3f else 0f
                        val availableAngle = 360f - (gapAngle * effectiveBreakdown.size)

                        effectiveBreakdown.forEach { item ->
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
                            text = if (summary.totalSpend > 0.0) "Total" else "Sample",
                            fontSize = 10.sp,
                            color = BotanicalTextSecondary
                        )
                        Text(
                            text = if (summary.totalSpend > 0.0) {
                                CategoryUiUtils.formatCurrency(summary.totalSpend).substringBefore(".")
                            } else {
                                "—"
                            },
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
                    val displayItems = effectiveBreakdown.take(5)

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
    onSaveExpense: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var hasError by remember { mutableStateOf(false) }

    val categories = remember {
        Category.DEFAULT_CATEGORIES
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
                text = "Add Expense",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

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

            // Merchant field
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it; hasError = false },
                label = { Text("Paid to / Merchant", color = BotanicalTextSecondary) },
                placeholder = { Text("e.g. Swiggy, Starbucks, Grocery Store", color = Color(0x66FFFFFF)) },
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
                text = "Save Expense",
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    val merchant = merchantText.trim()
                    if (parsedAmount == null || parsedAmount <= 0.0 || merchant.isEmpty()) {
                        hasError = true
                    } else {
                        onSaveExpense(parsedAmount, merchant, selectedCategory)
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
                        text = "Imported ${result.importedCount} transactions from this month.",
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
