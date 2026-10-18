package com.pay.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pay.tracker.ui.components.BotanicalBackground
import com.pay.tracker.ui.components.BotanicalCreamButton
import com.pay.tracker.ui.components.BotanicalGlassCard
import com.pay.tracker.ui.theme.BotanicalEmerald
import com.pay.tracker.ui.theme.BotanicalGlassBorder
import com.pay.tracker.ui.theme.BotanicalGlassBorderSubtle
import com.pay.tracker.ui.theme.BotanicalTextPrimary
import com.pay.tracker.ui.theme.BotanicalTextSecondary

@Composable
fun LockScreen(
    onUnlockRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Automatically trigger the biometric prompt when the lock screen appears
    LaunchedEffect(Unit) {
        onUnlockRequested()
    }

    BotanicalBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            BotanicalGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glowing Icon Frame
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x332EE89A),
                                        Color(0x10141F18),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, BotanicalGlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = BotanicalEmerald,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Pay Tracker Locked",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BotanicalTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Verify your fingerprint, face, or device PIN to view your financial logs.",
                        fontSize = 14.sp,
                        color = BotanicalTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    BotanicalCreamButton(
                        text = "Unlock Pay Tracker",
                        onClick = onUnlockRequested,
                        icon = Icons.Default.Fingerprint,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
