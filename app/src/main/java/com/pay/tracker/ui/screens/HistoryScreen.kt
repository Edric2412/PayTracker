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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val categories = viewModel.availableCategories

    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

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
            Text(
                text = "History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )

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

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Status Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAllStatus = (selectedStatus == null)
                Surface(
                    onClick = { viewModel.onStatusFilterSelect(null) },
                    shape = CircleShape,
                    color = if (isAllStatus) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (isAllStatus) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "All Status",
                        fontSize = 12.sp,
                        fontWeight = if (isAllStatus) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllStatus) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                val isConfirmed = (selectedStatus == TransactionStatus.CONFIRMED)
                Surface(
                    onClick = { viewModel.onStatusFilterSelect(if (isConfirmed) null else TransactionStatus.CONFIRMED) },
                    shape = CircleShape,
                    color = if (isConfirmed) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (isConfirmed) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "Confirmed",
                        fontSize = 12.sp,
                        fontWeight = if (isConfirmed) FontWeight.Bold else FontWeight.Medium,
                        color = if (isConfirmed) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                val isPending = (selectedStatus == TransactionStatus.PENDING)
                Surface(
                    onClick = { viewModel.onStatusFilterSelect(if (isPending) null else TransactionStatus.PENDING) },
                    shape = CircleShape,
                    color = if (isPending) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (isPending) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "Pending",
                        fontSize = 12.sp,
                        fontWeight = if (isPending) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPending) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Category Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isAllCats = (selectedCategory == null)
                Surface(
                    onClick = { viewModel.onCategoryFilterSelect(null) },
                    shape = CircleShape,
                    color = if (isAllCats) BotanicalCreamPill else Color(0x26253B2F),
                    border = BorderStroke(1.dp, if (isAllCats) BotanicalCreamPill else Color(0x26FFFFFF))
                ) {
                    Text(
                        text = "All Categories",
                        fontSize = 12.sp,
                        fontWeight = if (isAllCats) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllCats) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                categories.forEach { cat ->
                    val isSelected = (selectedCategory == cat.name)
                    val catColor = CategoryUiUtils.getCategoryColor(cat.name)

                    Surface(
                        onClick = { viewModel.onCategoryFilterSelect(if (isSelected) null else cat.name) },
                        shape = CircleShape,
                        color = if (isSelected) BotanicalCreamPill else Color(0x26253B2F),
                        border = BorderStroke(1.dp, if (isSelected) BotanicalCreamPill else Color(0x26FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BotanicalCreamPillText else catColor)
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

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Date-Grouped Transaction List
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
                            text = "Try adjusting your search query or category filters.",
                            fontSize = 13.sp,
                            color = BotanicalTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val grouped = remember(transactions) {
                    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    val nowStr = todayFormat.format(Date())

                    transactions.groupBy { txn ->
                        val txnStr = todayFormat.format(Date(txn.timestamp))
                        if (txnStr == nowStr) "Today"
                        else dateFormat.format(Date(txn.timestamp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    grouped.forEach { (dateHeader, txnsForDate) ->
                        val dayTotal = txnsForDate.sumOf { it.amount }

                        item(key = "header_$dateHeader") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "- " + CategoryUiUtils.formatCurrency(dayTotal),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BotanicalTextSecondary
                                )
                            }
                        }

                        item(key = "card_$dateHeader") {
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

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
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
