package com.pay.tracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pay.tracker.ui.theme.BotanicalCreamPill
import com.pay.tracker.ui.theme.BotanicalCreamPillText
import com.pay.tracker.ui.theme.BotanicalEmerald
import com.pay.tracker.ui.theme.BotanicalGlassBorder
import com.pay.tracker.ui.theme.BotanicalGlassBorderSubtle
import com.pay.tracker.ui.theme.BotanicalTextSecondary

private data class WalkthroughStep(
    val stepIndex: Int,
    val badge: String,
    val title: String,
    val description: String,
    val callout: String,
    val icon: ImageVector
)

private val WALKTHROUGH_STEPS = listOf(
    WalkthroughStep(
        stepIndex = 1,
        badge = "Step 1 of 4 • CASH FLOW",
        title = "Real-Time Net Cash Flow",
        description = "Your financial command center. Automatically tallies your total spending against income received to show your true net cash velocity.",
        callout = "Tap on the month header to travel back to previous months and examine historical trends.",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    ),
    WalkthroughStep(
        stepIndex = 2,
        badge = "Step 2 of 4 • ZERO TYPING",
        title = "Instant SMS Auto-Scan",
        description = "Never input expenses manually again. With one tap, Pay Tracker reads bank and UPI debit alerts from your SMS inbox and populates your ledger.",
        callout = "100% on-device & offline. Your bank alerts never leave your smartphone.",
        icon = Icons.Default.Download
    ),
    WalkthroughStep(
        stepIndex = 3,
        badge = "Step 3 of 4 • PACING",
        title = "Dynamic Budget Fuel Gauge",
        description = "Set your monthly spend limit. Pay Tracker calculates a live Safe Daily Pace based on days remaining so you never run out of money before month end.",
        callout = "Tap 'Set Budget' anytime on the dashboard to adjust your targets.",
        icon = Icons.Default.Savings
    ),
    WalkthroughStep(
        stepIndex = 4,
        badge = "Step 4 of 4 • INSTANT REVIEW",
        title = "1-Tap Verification Queue",
        description = "Incoming UPI and card notifications land in the Review Queue. Easily confirm, recategorize, or mark items as reimbursable or refunds.",
        callout = "Check the bell icon on the top right or tap the Review tab at the bottom.",
        icon = Icons.Default.Notifications
    )
)

/**
 * Full-screen spotlight onboarding overlay that dims all other UI and walks users
 * step-by-step through the core features of Pay Tracker.
 */
@Composable
fun AppWalkthroughSpotlightOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val stepData = WALKTHROUGH_STEPS[currentStep]
    val isLastStep = currentStep == WALKTHROUGH_STEPS.size - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6050C08)) // Deep dimmed obsidian scrim
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume taps to avoid clicking underlying UI */ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Glass Spotlight Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xF70C1812),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(BotanicalGlassBorder, BotanicalGlassBorderSubtle)
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Row: Step indicators & Skip button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pill Indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WALKTHROUGH_STEPS.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .height(4.dp)
                                        .width(if (index == currentStep) 24.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == currentStep) BotanicalEmerald
                                            else Color(0x332EE89A)
                                        )
                                )
                            }
                        }

                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Skip tour",
                                fontSize = 13.sp,
                                color = BotanicalTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = stepData,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "WalkthroughContent"
                    ) { targetStep ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Icon container
                            BotanicalSquircleContainer(
                                icon = targetStep.icon,
                                tint = BotanicalEmerald,
                                backgroundColor = Color(0x332EE89A),
                                size = 56.dp,
                                iconSize = 28.dp,
                                shape = RoundedCornerShape(16.dp)
                            )

                            // Badge
                            Surface(
                                shape = CircleShape,
                                color = Color(0x1F2EE89A),
                                border = BorderStroke(1.dp, Color(0x3D2EE89A))
                            ) {
                                Text(
                                    text = targetStep.badge,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = BotanicalEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            // Title
                            Text(
                                text = targetStep.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            // Description
                            Text(
                                text = targetStep.description,
                                fontSize = 14.sp,
                                color = Color(0xFFC7D3CD),
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )

                            // Pro-tip Callout Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x261B422D),
                                border = BorderStroke(1.dp, Color(0x332EE89A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 ${targetStep.callout}",
                                    fontSize = 12.sp,
                                    color = BotanicalEmerald,
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons Row: Back and Next/Finish
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep > 0) {
                            BotanicalGlassButton(
                                text = "Back",
                                icon = Icons.Default.ChevronLeft,
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        BotanicalCreamButton(
                            text = if (isLastStep) "Get Started" else "Next",
                            icon = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = {
                                if (isLastStep) {
                                    onDismiss()
                                } else {
                                    currentStep++
                                }
                            },
                            modifier = Modifier.weight(if (currentStep > 0) 1.5f else 1f)
                        )
                    }
                }
            }
        }
    }
}
