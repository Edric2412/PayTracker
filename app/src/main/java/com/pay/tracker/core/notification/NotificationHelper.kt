package com.pay.tracker.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pay.tracker.MainActivity
import com.pay.tracker.R

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    const val CHANNEL_ID = "pay_expense_alerts"
    const val CHANNEL_NAME = "Expense Alerts"
    const val CHANNEL_DESC = "Notifications for detected UPI and payment transactions requiring review"

    const val ACTION_CONFIRM_EXPENSE = "com.pay.tracker.ACTION_CONFIRM_EXPENSE"
    const val ACTION_EDIT_CATEGORY = "com.pay.tracker.ACTION_EDIT_CATEGORY"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_CATEGORY = "extra_category"

    private val VIBRATION_PATTERN = longArrayOf(0, 80)

    /**
     * Creates high-priority notification channel on Android 8.0+ (Oreo through Android 16).
     * Configures IMPORTANCE_HIGH, explicit audio attributes, vibration pattern, and lockscreen visibility.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = android.graphics.Color.WHITE
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates and posts a high-priority actionable heads-up notification for a detected expense.
     * Actions:
     *  1. [✓ Confirm] - triggers background BroadcastReceiver to mark CONFIRMED without opening app
     *  2. [Change Category] - opens app edit sheet in Review Queue
     */
    fun showExpenseNotification(
        context: Context,
        transactionId: Long,
        amount: Double,
        merchant: String,
        category: String
    ) {
        createNotificationChannel(context)

        val formattedAmount = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", amount)
        }

        // Tap notification body -> Open app Review Queue
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra("navigate_to", "review_queue")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            (transactionId * 3).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: [✓ Confirm] via BroadcastReceiver
        val confirmIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_EXPENSE
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_CATEGORY, category)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            (transactionId * 3 + 1).toInt(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: [Change Category] -> Open app dialog/sheet
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT_CATEGORY
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra("open_category_picker", true)
            putExtra("navigate_to", "review_queue")
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            (transactionId * 3 + 2).toInt(),
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("₹$formattedAmount • $merchant")
            .setContentText(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(VIBRATION_PATTERN)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                0,
                "Confirm",
                confirmPendingIntent
            )
            .addAction(
                0,
                "Change Category",
                editPendingIntent
            )
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) {
                android.util.Log.e(TAG, "❌ CANNOT POST NOTIFICATION: App notifications are turned OFF in system settings!")
            }
            notificationManager.notify(transactionId.toInt(), notification)
            android.util.Log.i(TAG, "✓ Successfully posted expense heads-up notification for transaction ID: $transactionId (₹$formattedAmount at $merchant)")
        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "❌ SecurityException: POST_NOTIFICATIONS permission not granted.", e)
        }
    }

    /**
     * Dismisses the notification by transaction ID.
     */
    fun cancelNotification(context: Context, transactionId: Long) {
        NotificationManagerCompat.from(context).cancel(transactionId.toInt())
        android.util.Log.d(TAG, "Cancelled notification for transaction ID: $transactionId")
    }
}
