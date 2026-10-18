package com.pay.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pay.tracker.ui.theme.BotanicalEmerald
import com.pay.tracker.ui.theme.BotanicalTextSecondary

/**
 * Polished modal progress dialog showing live SMS scanning count and transactions parsed.
 */
@Composable
fun SmsScanningProgressDialog(
    scanned: Int,
    total: Int,
    foundCount: Int
) {
    val progress = if (total > 0) (scanned.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val percentInt = (progress * 100).toInt()

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss during active scan to prevent partial state */ },
        containerColor = Color(0xF50D1913),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BotanicalSquircleContainer(
                    icon = Icons.Default.Download,
                    tint = BotanicalEmerald,
                    backgroundColor = Color(0x332EE89A),
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Scanning SMS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Parsing bank & UPI alerts locally...",
                        color = BotanicalTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Progress Bar
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BotanicalEmerald,
                        trackColor = Color(0x262EE89A)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BotanicalEmerald,
                        trackColor = Color(0x262EE89A)
                    )
                }

                // Scanned counter & percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (total > 0) "Scanned $scanned of $total SMS" else "Reading message store...",
                        fontSize = 13.sp,
                        color = BotanicalTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (total > 0) {
                        Text(
                            text = "$percentInt%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BotanicalEmerald
                        )
                    }
                }

                // Transactions identified box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x261B422D),
                    border = BorderStroke(1.dp, Color(0x332EE89A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Transactions identified",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$foundCount",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BotanicalEmerald
                        )
                    }
                }

                Text(
                    text = "🔒 100% private • Processing offline on your device",
                    fontSize = 11.sp,
                    color = BotanicalTextSecondary
                )
            }
        },
        confirmButton = {}
    )
}
