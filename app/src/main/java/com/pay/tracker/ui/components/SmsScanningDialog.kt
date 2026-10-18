package com.pay.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Icon
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SmsScanningProgressDialog(
    scanned: Int,
    total: Int,
    foundCount: Int
) {
    val progress = if (total > 0) (scanned.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).toInt()

    Dialog(
        onDismissRequest = { /* Keep the active import visible until it completes. */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF101D16),
            border = BorderStroke(1.dp, Color(0xFF3D5140)),
            shadowElevation = 28.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = Color(0xFF263B2B),
                        border = BorderStroke(1.dp, Color(0xFF4B6046)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = Color(0xFFD2DDBD),
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scanning messages",
                            color = Color(0xFFF1EEE5),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Looking for supported payment alerts",
                            color = Color(0xFFB5C1B2),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (total > 0) "Messages checked" else "Preparing inbox",
                                color = Color(0xFFB5C1B2),
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (total > 0) "$scanned of $total" else "Reading message list…",
                                color = Color(0xFFF1EEE5),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (total > 0) {
                            Text(
                                text = "$percent%",
                                color = Color(0xFFD2DDBD),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (total > 0) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFB9C89B),
                            trackColor = Color(0xFF2A3B2E)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFFB9C89B),
                            trackColor = Color(0xFF2A3B2E)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF17291E),
                    border = BorderStroke(1.dp, Color(0xFF304534))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Transaction matches",
                                color = Color(0xFFF1EEE5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Recognised so far",
                                color = Color(0xFF9EAE9B),
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = foundCount.toString(),
                            color = Color(0xFFD2DDBD),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB9C89B))
                    )
                    Text(
                        text = "Messages are checked on this device. Imported entries may still need review.",
                        color = Color(0xFF9EAE9B),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
