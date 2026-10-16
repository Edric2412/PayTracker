package com.pay.tracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.pay.tracker.ui.theme.BotanicalTextPrimary
import com.pay.tracker.ui.theme.BotanicalTextSecondary
import com.pay.tracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToSimulator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsState()
    val savedApiKey by viewModel.geminiApiKey.collectAsState()
    val importResult by viewModel.importState.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val isResetDone by viewModel.isResetDone.collectAsState()

    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var apiKeySavedNotification by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.importAllHistoricalSms(context)
        }
    }

    val onScanSmsClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.importThisMonthSms(context)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    val onScanAllSmsClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.importAllHistoricalSms(context)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatuses(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
            }

            // 1. App & Permissions Section
            item {
                Text(
                    text = "App & Permissions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Notification Listener Access Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalSquircleContainer(
                                icon = if (isNotificationAccessGranted) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                tint = if (isNotificationAccessGranted) BotanicalEmerald else Color(0xFFFFB74D),
                                backgroundColor = if (isNotificationAccessGranted) Color(0x264EBA74) else Color(0x26FFB74D),
                                size = 42.dp,
                                iconSize = 20.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Access",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isNotificationAccessGranted) "Active & Listening" else "Permission Required",
                                    fontSize = 12.sp,
                                    color = if (isNotificationAccessGranted) BotanicalEmerald else Color(0xFFFFB74D)
                                )
                            }
                        }

                        Text(
                            text = "Enables real-time payment detection from apps like Google Pay, Paytm, and PhonePe. All parsing happens locally.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 18.sp
                        )

                        BotanicalGlassButton(
                            text = if (isNotificationAccessGranted) "Notification Settings" else "Grant Access",
                            onClick = { viewModel.openNotificationAccessSettings(context) },
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))

                        // Battery Optimization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalSquircleContainer(
                                icon = if (isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                                tint = if (isIgnoringBatteryOptimizations) BotanicalEmerald else Color(0xFF38B2AC),
                                backgroundColor = if (isIgnoringBatteryOptimizations) Color(0x264EBA74) else Color(0x2638B2AC),
                                size = 42.dp,
                                iconSize = 20.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Execution",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isIgnoringBatteryOptimizations) "Unrestricted" else "Recommended",
                                    fontSize = 12.sp,
                                    color = if (isIgnoringBatteryOptimizations) BotanicalEmerald else BotanicalTextSecondary
                                )
                            }
                        }

                        if (!isIgnoringBatteryOptimizations) {
                            BotanicalGlassButton(
                                text = "Disable Battery Restrictions",
                                onClick = { viewModel.requestIgnoreBatteryOptimizations(context) },
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 2. AI & Categorization Section
            item {
                Text(
                    text = "AI & Categorization",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BotanicalSquircleContainer(
                                icon = Icons.Default.Key,
                                tint = Color(0xFFF0B429),
                                backgroundColor = Color(0x26F0B429),
                                size = 42.dp,
                                iconSize = 20.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini AI Key (Optional)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (savedApiKey.isNotBlank()) "AI Model Connected" else "Using Local Rule Engine",
                                    fontSize = 12.sp,
                                    color = if (savedApiKey.isNotBlank()) BotanicalEmerald else BotanicalTextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Add a free Google AI Studio key to intelligently auto-classify ambiguous merchants like obscure cafes and local vendors.",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                apiKeySavedNotification = false
                            },
                            label = { Text("Gemini API Key", color = BotanicalTextSecondary) },
                            placeholder = { Text("YOUR_GEMINI_API_KEY", color = Color(0x66FFFFFF)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BotanicalEmerald,
                                unfocusedBorderColor = BotanicalGlassBorder
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (savedApiKey.isNotBlank()) {
                                BotanicalGlassButton(
                                    text = "Clear Key",
                                    onClick = {
                                        viewModel.updateGeminiApiKey("")
                                        apiKeyInput = ""
                                        apiKeySavedNotification = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            BotanicalCreamButton(
                                text = if (apiKeySavedNotification) "Saved!" else "Save Key",
                                onClick = {
                                    viewModel.updateGeminiApiKey(apiKeyInput)
                                    apiKeySavedNotification = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Data & Tools Section
            item {
                Text(
                    text = "Data & Tools",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Scan SMS Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalSquircleContainer(
                                icon = Icons.Default.Download,
                                tint = BotanicalEmerald,
                                backgroundColor = Color(0x264EBA74),
                                size = 42.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMS Transaction Import",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Scan bank & UPI debit SMS to populate monthly spending across all years.",
                                    fontSize = 12.sp,
                                    color = BotanicalTextSecondary
                                )
                            }
                        }

                        BotanicalCreamButton(
                            text = if (isImporting) "Scanning All Historical SMS..." else "Scan All History (All Months & Years)",
                            onClick = onScanAllSmsClick,
                            icon = Icons.Default.History,
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth()
                        )

                        BotanicalGlassButton(
                            text = if (isImporting) "Scanning..." else "Scan Current Month Only",
                            onClick = onScanSmsClick,
                            icon = Icons.Default.Download,
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))

                        // Simulator Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToSimulator),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalSquircleContainer(
                                icon = Icons.Default.BugReport,
                                tint = Color(0xFF38B2AC),
                                backgroundColor = Color(0x2638B2AC),
                                size = 42.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Transaction Simulator",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Test notifications and bank SMS parsers in a safe sandbox.",
                                    fontSize = 12.sp,
                                    color = BotanicalTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = BotanicalTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 4. Data Management (Clear Data)
            item {
                Text(
                    text = "Data Management",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BotanicalTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalSquircleContainer(
                                icon = Icons.Default.DeleteSweep,
                                tint = Color(0xFFFF5252),
                                backgroundColor = Color(0x26FF5252),
                                size = 42.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear All Transactions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Erase all transactions from local database to start fresh.",
                                    fontSize = 12.sp,
                                    color = BotanicalTextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x33FF5252),
                                contentColor = Color(0xFFFF7B7B)
                            ),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0x4DFF5252)),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text("Start Fresh (Clear Data)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. About Card
            item {
                BotanicalGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BotanicalSquircleContainer(
                                icon = Icons.Default.Info,
                                tint = BotanicalEmerald,
                                backgroundColor = Color(0x264EBA74),
                                size = 36.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pay Tracker v1.0",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Powered by Botanical Glass design language\n• 100% private: SQLite Room database stored strictly on-device\n• Zero third-party telemetry or ad SDKs",
                            fontSize = 12.sp,
                            color = BotanicalTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            containerColor = Color(0xF0101D16),
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252)) },
            title = { Text("Start Fresh?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "This will delete all transactions from your local storage. You can immediately re-import this month's bank expenses by scanning SMS.",
                    fontSize = 13.sp,
                    color = BotanicalTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.clearAllTransactions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = CircleShape
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel", color = BotanicalTextSecondary)
                }
            }
        )
    }

    if (isResetDone) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetStatus() },
            containerColor = Color(0xF0101D16),
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BotanicalEmerald) },
            title = { Text("Database Reset", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("All transaction records have been erased. You now have a fresh start.", fontSize = 13.sp, color = BotanicalTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissResetStatus() },
                    colors = ButtonDefaults.buttonColors(containerColor = BotanicalCreamPill, contentColor = BotanicalCreamPillText),
                    shape = CircleShape
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportResult() },
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
                    onClick = { viewModel.dismissImportResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = BotanicalCreamPill, contentColor = BotanicalCreamPillText),
                    shape = CircleShape
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
