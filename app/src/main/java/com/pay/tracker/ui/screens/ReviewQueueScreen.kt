package com.pay.tracker.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.pay.tracker.ui.theme.BotanicalTextPrimary
import com.pay.tracker.ui.theme.BotanicalTextSecondary
import com.pay.tracker.ui.viewmodel.ReviewQueueViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(
    viewModel: ReviewQueueViewModel,
    modifier: Modifier = Modifier
) {
    val pendingList by viewModel.pendingTransactions.collectAsState()
    val categories = viewModel.availableCategories
    var selectedTab by remember { mutableStateOf(0) }

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

            // 1. Top Bar with Settings/Filter Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review Queue",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )

                // Frosted Glass Filter Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x33253B2F))
                        .border(
                            BorderStroke(1.dp, Brush.verticalGradient(listOf(BotanicalGlassBorder, BotanicalGlassBorderSubtle))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Filter Tabs: Pending (Cream Pill), Confirmed, Ignored
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pending Tab
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = CircleShape,
                    color = if (selectedTab == 0) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (selectedTab == 0) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "Pending (${pendingList.size})",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }

                // Confirmed Tab
                Surface(
                    onClick = { selectedTab = 1 },
                    shape = CircleShape,
                    color = if (selectedTab == 1) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (selectedTab == 1) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "Confirmed",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }

                // Ignored Tab
                Surface(
                    onClick = { selectedTab = 2 },
                    shape = CircleShape,
                    color = if (selectedTab == 2) BotanicalCreamPill else BotanicalGlassButton,
                    border = BorderStroke(1.dp, if (selectedTab == 2) BotanicalCreamPill else BotanicalGlassButtonBorder)
                ) {
                    Text(
                        text = "Ignored",
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 2) BotanicalCreamPillText else BotanicalTextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Queue Content
            if (pendingList.isEmpty() || selectedTab != 0) {
                BotanicalEmptyQueueCard(
                    message = if (selectedTab == 0) "All caught up!" else "No transactions in this filter"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = pendingList,
                        key = { it.id }
                    ) { transaction ->
                        BotanicalReviewCard(
                            transaction = transaction,
                            categories = categories,
                            onCategorySelected = { newCategory ->
                                viewModel.updateCategory(transaction.id, newCategory)
                            },
                            onConfirm = {
                                viewModel.confirmTransaction(transaction.id)
                            },
                            onReject = {
                                viewModel.rejectTransaction(transaction.id)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * Botanical Review Card for pending transactions
 */
@Composable
private fun BotanicalReviewCard(
    transaction: Transaction,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    var showCategorySelector by remember { mutableStateOf(false) }
    val formattedSourceApp = CategoryUiUtils.formatSourceApp(transaction.sourceApp)
    val timeText = CategoryUiUtils.formatTimestamp(transaction.timestamp)
    val categoryColor = CategoryUiUtils.getCategoryColor(transaction.category)

    BotanicalGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: App Squircle + Merchant/Source + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BotanicalSquircleContainer(
                    icon = Icons.Default.Payment,
                    tint = BotanicalEmerald,
                    backgroundColor = Color(0x264EBA74),
                    size = 44.dp,
                    iconSize = 22.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedSourceApp • $timeText",
                        fontSize = 12.sp,
                        color = BotanicalTextSecondary
                    )
                }

                // Amount
                Text(
                    text = "- " + CategoryUiUtils.formatCurrency(transaction.amount),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Suggested Category Pill & Confidence Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CategoryUiUtils.getCategoryIcon(transaction.category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = transaction.category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor
                        )
                    }
                }

                // High Confidence tag
                Surface(
                    shape = CircleShape,
                    color = Color(0x264EBA74),
                    border = BorderStroke(1.dp, Color(0x404EBA74))
                ) {
                    Text(
                        text = "92% match",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BotanicalEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Inline Category Selector when "Change" is tapped
            if (showCategorySelector) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = (cat.name == transaction.category)
                        Surface(
                            onClick = {
                                onCategorySelected(cat.name)
                                showCategorySelector = false
                            },
                            shape = CircleShape,
                            color = if (isSelected) BotanicalCreamPill else Color(0x26253B2F),
                            border = BorderStroke(1.dp, if (isSelected) BotanicalCreamPill else Color(0x33FFFFFF))
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BotanicalCreamPillText else Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Confirm & Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cream Confirm Button
                Button(
                    onClick = onConfirm,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotanicalCreamPill,
                        contentColor = BotanicalCreamPillText
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Frosted Glass Change Button
                Surface(
                    onClick = { showCategorySelector = !showCategorySelector },
                    shape = CircleShape,
                    color = BotanicalGlassButton,
                    border = BorderStroke(1.dp, BotanicalGlassButtonBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = BotanicalTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCategorySelector) "Done" else "Change",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = BotanicalTextPrimary
                        )
                    }
                }

                // Ignore cross icon button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BotanicalGlassButton)
                        .border(BorderStroke(1.dp, BotanicalGlassButtonBorder), CircleShape)
                        .clickable(onClick = onReject),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ignore",
                        tint = BotanicalTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty queue view
 */
@Composable
private fun BotanicalEmptyQueueCard(
    message: String
) {
    BotanicalGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BotanicalSquircleContainer(
                icon = Icons.Default.CheckCircle,
                tint = BotanicalEmerald,
                backgroundColor = Color(0x264EBA74),
                size = 56.dp,
                iconSize = 28.dp,
                shape = CircleShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "All new SMS expenses have been verified and added to your monthly cockpit.",
                fontSize = 13.sp,
                color = BotanicalTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
