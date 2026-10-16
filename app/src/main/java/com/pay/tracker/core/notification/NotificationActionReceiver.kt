package com.pay.tracker.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pay.tracker.core.database.PayDatabase
import com.pay.tracker.core.repository.TransactionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver handling direct notification action: '[✓ Confirm]'.
 * Marks the pending transaction as CONFIRMED in Room database in the background
 * without opening the main app interface, and dismisses the notification.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionRx"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_CONFIRM_EXPENSE) {
            val transactionId = intent.getLongExtra(NotificationHelper.EXTRA_TRANSACTION_ID, -1L)
            val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY)

            android.util.Log.i(TAG, "Received ACTION_CONFIRM_EXPENSE for txnId=$transactionId, category=$category")

            if (transactionId <= 0) {
                android.util.Log.w(TAG, "Invalid transactionId=$transactionId, ignoring.")
                return
            }

            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    val appContext = context.applicationContext
                    val db = PayDatabase.getInstance(appContext)
                    val repository = TransactionRepositoryImpl(db.transactionDao())

                    // Confirm the transaction in the database
                    repository.confirmTransaction(transactionId, category)
                    android.util.Log.i(TAG, "Successfully confirmed transaction ID $transactionId in Room DB.")

                    // Clear the notification from the tray
                    NotificationHelper.cancelNotification(appContext, transactionId)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to confirm transaction ID $transactionId", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
