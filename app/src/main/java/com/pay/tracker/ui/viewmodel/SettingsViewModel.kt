package com.pay.tracker.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay.tracker.BuildConfig
import com.pay.tracker.core.repository.TransactionRepository
import com.pay.tracker.core.sms.SmsImportResult
import com.pay.tracker.core.sms.SmsTransactionImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    context: Context,
    private val repository: TransactionRepository? = null
) : ViewModel() {

    private val prefs = context.getSharedPreferences("pay_tracker_prefs", Context.MODE_PRIVATE)

    private val _isNotificationAccessGranted = MutableStateFlow(checkNotificationAccess(context))
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(checkBatteryOptimizations(context))
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    private val _importState = MutableStateFlow<SmsImportResult?>(null)
    val importState: StateFlow<SmsImportResult?> = _importState.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _isResetDone = MutableStateFlow(false)
    val isResetDone: StateFlow<Boolean> = _isResetDone.asStateFlow()

    fun clearAllTransactions() {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.clearAllTransactions()
            _isResetDone.value = true
        }
    }

    fun importThisMonthSms(context: Context) {
        val repo = repository ?: return
        viewModelScope.launch {
            _isImporting.value = true
            _importState.value = null
            val importer = SmsTransactionImporter(repo)
            val result = importer.importMonthSms(context)
            _importState.value = result
            _isImporting.value = false
        }
    }

    fun importAllHistoricalSms(context: Context) {
        val repo = repository ?: return
        viewModelScope.launch {
            _isImporting.value = true
            _importState.value = null
            val importer = SmsTransactionImporter(repo)
            val result = importer.importAllSms(context)
            _importState.value = result
            _isImporting.value = false
        }
    }

    fun dismissImportResult() {
        _importState.value = null
    }

    fun dismissResetStatus() {
        _isResetDone.value = false
    }

    fun refreshPermissionStatuses(context: Context) {
        _isNotificationAccessGranted.value = checkNotificationAccess(context)
        _isIgnoringBatteryOptimizations.value = checkBatteryOptimizations(context)
    }

    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    private fun checkNotificationAccess(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat?.contains(pkgName) == true
    }

    private fun checkBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }
}
