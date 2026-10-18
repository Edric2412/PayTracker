package com.pay.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.pay.tracker.core.notification.NotificationHelper
import com.pay.tracker.core.security.BiometricAuthManager
import com.pay.tracker.ui.PayApp
import com.pay.tracker.ui.screens.LockScreen
import com.pay.tracker.ui.theme.PayTheme
import com.pay.tracker.ui.viewmodel.PayViewModelFactory
import com.pay.tracker.ui.viewmodel.SettingsViewModel

class MainActivity : FragmentActivity() {

    private lateinit var viewModelFactory: PayViewModelFactory
    private var initialRoute by mutableStateOf<String?>(null)
    private var isAppUnlocked by mutableStateOf(true)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isLockEnabled = SettingsViewModel.isBiometricLockEnabled(applicationContext)
        isAppUnlocked = !isLockEnabled

        // Initialize high-priority notification channel
        NotificationHelper.createNotificationChannel(applicationContext)

        // Request POST_NOTIFICATIONS permission on Android 13+
        checkAndRequestNotificationPermission()

        viewModelFactory = PayViewModelFactory(applicationContext)

        handleNavigationIntent(intent)

        setContent {
            PayTheme {
                if (!isAppUnlocked) {
                    LockScreen(
                        onUnlockRequested = { promptBiometricUnlock() }
                    )
                } else {
                    PayApp(
                        viewModelFactory = viewModelFactory,
                        initialRoute = initialRoute
                    )
                }
            }
        }
    }

    private fun promptBiometricUnlock() {
        BiometricAuthManager.authenticate(
            activity = this,
            onSuccess = {
                isAppUnlocked = true
            },
            onError = { _, _ -> },
            onFailed = { }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent == null) return
        val targetRoute = intent.getStringExtra("navigate_to")
        if (!targetRoute.isNullOrBlank()) {
            initialRoute = targetRoute
        }
        val txnId = intent.getLongExtra(NotificationHelper.EXTRA_TRANSACTION_ID, -1L)
        if (txnId > 0) {
            NotificationHelper.cancelNotification(applicationContext, txnId)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
