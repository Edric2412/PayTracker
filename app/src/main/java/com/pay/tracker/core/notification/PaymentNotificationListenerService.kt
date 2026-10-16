package com.pay.tracker.core.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.pay.tracker.core.database.PayDatabase
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.core.parser.CategorySuggester
import com.pay.tracker.core.parser.GPayParser
import com.pay.tracker.core.repository.TransactionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * NotificationListenerService that observes incoming system notifications,
 * filters for Google Pay UPI payments, parses transaction details, saves
 * candidate transactions to the Room database with status PENDING, and triggers
 * an interactive follow-up notification.
 */
class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gPayParser = GPayParser()
    private val categorySuggester = CategorySuggester()

    companion object {
        private const val TAG = "PaymentNotificationSvc"
        val ALLOWED_PACKAGES = setOf(
            GPayParser.GPAY_PACKAGE,                 // Google Pay India ("com.google.android.apps.nbu.paisa.user")
            "com.google.android.apps.messaging",     // Google Messages (Default Nothing Phone SMS)
            "com.android.mms",                       // Standard Android SMS
            "com.axis.mobile",                       // Axis Bank app
            "com.phonepe.app",                       // PhonePe
            "net.one97.paytm",                       // Paytm
            "in.org.npci.upiapp",                    // BHIM UPI
            "com.dreamplug.androidapp",              // CRED
            "com.snapwork.sdfc",                     // HDFC Bank MobileBanking
            "com.snapwork.hdfc",                     // HDFC Bank MobileBanking (Alt)
            "com.csam.icici.bank.imobile",           // ICICI iMobile
            "com.sbi.lotusintouch"                   // SBI YONO
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "PaymentNotificationListenerService successfully connected and active.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "PaymentNotificationListenerService disconnected. Requesting rebind...")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            requestRebind(android.content.ComponentName(this, PaymentNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName !in ALLOWED_PACKAGES) {
            // Ignore notifications from unrelated apps
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val body = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            else -> subText
        }

        Log.d(TAG, "Payment notification received from $packageName: title='$title', body='$body'")

        val parsed = gPayParser.parse(
            title = title,
            text = body,
            postTime = sbn.postTime,
            packageName = packageName
        ) ?: run {
            Log.d(TAG, "Notification did not match debit payment pattern.")
            return
        }

        serviceScope.launch {
            try {
                val db = PayDatabase.getInstance(applicationContext)
                val repository = TransactionRepositoryImpl(db.transactionDao())

                // 1. Deduplication check
                val isDuplicate = repository.isDuplicate(
                    fingerprint = parsed.fingerprint,
                    referenceId = parsed.referenceId
                )
                if (isDuplicate) {
                    Log.d(TAG, "Duplicate transaction detected (fingerprint=${parsed.fingerprint}), skipping.")
                    return@launch
                }

                // 2. Suggest expense category (Local heuristic first, with Gemini AI fallback)
                var suggestedCategory = categorySuggester.suggestCategory(
                    merchant = parsed.merchant,
                    rawText = body
                )

                // If local rules fall back to generic Transfer, query Gemini 3.5 Flash Lite
                if (suggestedCategory == com.pay.tracker.core.model.Category.TRANSFER.name) {
                    val geminiSuggester = com.pay.tracker.core.ai.GeminiCategorySuggester(applicationContext)
                    val aiCategory = geminiSuggester.suggestCategoryWithGemini(
                        merchant = parsed.merchant,
                        description = body
                    )
                    if (aiCategory != null) {
                        suggestedCategory = aiCategory
                    }
                }

                // 3. Persist as PENDING in local Room database
                val transaction = parsed.toTransaction(
                    category = suggestedCategory,
                    status = TransactionStatus.PENDING
                )
                val insertedId = repository.saveTransaction(transaction)
                Log.d(TAG, "Saved pending transaction with ID=$insertedId")

                if (insertedId > 0) {
                    // 4. Trigger actionable follow-up notification
                    NotificationHelper.showExpenseNotification(
                        context = applicationContext,
                        transactionId = insertedId,
                        amount = parsed.amount,
                        merchant = parsed.merchant,
                        category = suggestedCategory
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
