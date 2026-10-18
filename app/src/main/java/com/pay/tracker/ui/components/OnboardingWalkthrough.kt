package com.pay.tracker.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.PieChartOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pay.tracker.ui.theme.BotanicalCreamPill
import com.pay.tracker.ui.theme.BotanicalCreamPillText
import com.pay.tracker.ui.theme.BotanicalTextSecondary

const val TOUR_REVIEW = 0
const val TOUR_MONTH = 1
const val TOUR_CASH_FLOW = 2
const val TOUR_SCAN = 3
const val TOUR_BUDGET = 4
const val TOUR_CATEGORIES = 5

private data class WalkthroughStep(
    val target: Int,
    val eyebrow: String,
    val title: String,
    val description: String,
    val tip: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val WALKTHROUGH_STEPS = listOf(
    WalkthroughStep(
        target = TOUR_REVIEW,
        eyebrow = "YOUR REVIEW QUEUE",
        title = "Nothing slips through",
        description = "Tap the bell when transactions need your attention. Review suggestions and correct anything Pay Tracker couldn't confidently categorise.",
        tip = "You stay in control of what becomes part of your records.",
        icon = Icons.Default.NotificationsNone
    ),
    WalkthroughStep(
        target = TOUR_MONTH,
        eyebrow = "BROWSE YOUR TIMELINE",
        title = "Move between months",
        description = "Use the month selector to look back at older spending or return to the current month. Your history stays grouped by transaction date.",
        tip = "Choose a month before comparing your spending.",
        icon = Icons.Default.CalendarMonth
    ),
    WalkthroughStep(
        target = TOUR_CASH_FLOW,
        eyebrow = "YOUR MONTH AT A GLANCE",
        title = "Spending and cash flow",
        description = "Total Spent shows eligible expenses. Received shows incoming money, while Net Cash Flow helps you see the difference.",
        tip = "Refunds and transfers can affect totals differently from ordinary income.",
        icon = Icons.Default.AccountBalanceWallet
    ),
    WalkthroughStep(
        target = TOUR_SCAN,
        eyebrow = "IMPORT TRANSACTIONS",
        title = "Start with your messages",
        description = "Scan supported bank and payment SMS to find transaction candidates. Pay Tracker skips messages it doesn't recognise; imported entries may still need review.",
        tip = "Choose this month for a quick start, or scan older messages to build history.",
        icon = Icons.Default.Sms
    ),
    WalkthroughStep(
        target = TOUR_BUDGET,
        eyebrow = "SET A MONTHLY LIMIT",
        title = "Keep your budget in view",
        description = "Set a budget to compare your spending with your target. The pace and remaining amount help you decide what feels comfortable for the rest of the month.",
        tip = "Past months are for review; update your target for the current month.",
        icon = Icons.Default.AccountBalanceWallet
    ),
    WalkthroughStep(
        target = TOUR_CATEGORIES,
        eyebrow = "SEE WHERE IT GOES",
        title = "Find your spending patterns",
        description = "The category breakdown groups eligible expenses so you can spot the areas taking the biggest share of your budget.",
        tip = "Open History to inspect individual entries and correct their categories.",
        icon = Icons.Default.PieChartOutline
    )
)

/**
 * A contextual walkthrough: the target stays visible through a spotlight cutout,
 * while a concise explanation and explicit controls sit in a bottom card.
 */
@Composable
fun AppWalkthroughSpotlightOverlay(
    stepIndex: Int,
    targetBounds: Rect?,
    onStepChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeStep = stepIndex.coerceIn(WALKTHROUGH_STEPS.indices)
    val step = WALKTHROUGH_STEPS[safeStep]
    val lastStep = safeStep == WALKTHROUGH_STEPS.lastIndex
    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                targetBounds?.takeIf { it.width > 0f && it.height > 0f }?.let { target ->
                    val inset = 8.dp.toPx()
                    val left = (target.left - inset).coerceAtLeast(0f)
                    val top = (target.top - inset).coerceAtLeast(0f)
                    val right = (target.right + inset).coerceAtMost(size.width)
                    val bottom = (target.bottom + inset).coerceAtMost(size.height)
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, right, bottom),
                            cornerRadius = CornerRadius(22.dp.toPx())
                        )
                    )
                }
            }
            drawPath(path = path, color = Color(0xE809120C))
            targetBounds?.takeIf { it.width > 0f && it.height > 0f }?.let { target ->
                val inset = 8.dp.toPx()
                val left = (target.left - inset).coerceAtLeast(0f)
                val top = (target.top - inset).coerceAtLeast(0f)
                val right = (target.right + inset).coerceAtMost(size.width)
                val bottom = (target.bottom + inset).coerceAtMost(size.height)
                drawRoundRect(
                    color = Color(0xFFB9C89B),
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF101F17),
            border = BorderStroke(1.dp, Color(0xFF465B45)),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        WALKTHROUGH_STEPS.forEachIndexed { index, _ ->
                            Box(
                                Modifier
                                    .height(4.dp)
                                    .width(if (index == safeStep) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == safeStep) Color(0xFFB9C89B)
                                        else Color(0xFF405444)
                                    )
                            )
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "${safeStep + 1} / ${WALKTHROUGH_STEPS.size}",
                            color = Color(0xFFB6C2B2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = Color(0xFFB6C2B2), fontSize = 13.sp)
                    }
                }

                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "walkthrough-step"
                ) { current ->
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = current.icon,
                                contentDescription = null,
                                tint = Color(0xFFB9C89B),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = current.eyebrow,
                                color = Color(0xFFB9C89B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.3.sp
                            )
                        }
                        Text(
                            text = current.title,
                            color = Color(0xFFF1EEE5),
                            fontSize = 25.sp,
                            lineHeight = 30.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = current.description,
                            color = Color(0xFFC2CCBF),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = Color(0xFF192B20),
                            border = BorderStroke(1.dp, Color(0xFF304735))
                        ) {
                            Text(
                                text = current.tip,
                                color = Color(0xFFD6DEC9),
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (safeStep > 0) {
                        Surface(
                            onClick = { onStepChange(safeStep - 1) },
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFF4B5F4A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color(0xFFECEFE5), modifier = Modifier.size(17.dp))
                                Text("Back", color = Color(0xFFECEFE5), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            if (lastStep) onDismiss() else onStepChange(safeStep + 1)
                        },
                        shape = CircleShape,
                        color = BotanicalCreamPill,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 17.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lastStep) "Finish tour" else "Next",
                                color = BotanicalCreamPillText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (lastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = BotanicalCreamPillText,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
