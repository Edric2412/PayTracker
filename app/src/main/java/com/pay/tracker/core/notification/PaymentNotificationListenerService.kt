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
        val KNOWN_FINANCE_PACKAGES = setOf(
            GPayParser.GPAY_PACKAGE,                 // Google Pay India ("com.google.android.apps.nbu.paisa.user")
            "com.google.android.apps.walletnfcrel",  // Google Wallet
            "com.google.android.apps.messaging",     // Google Messages
            "com.android.mms",                       // Android SMS
            "com.samsung.android.messaging",         // Samsung SMS
            "com.oneplus.mms",                       // OnePlus SMS
            "com.coloros.mms",                       // Oppo SMS
            "com.phonepe.app",                       // PhonePe
            "net.one97.paytm",                       // Paytm
            "in.org.npci.upiapp",                    // BHIM UPI
            "com.dreamplug.androidapp",              // CRED
            "com.amazon.mShop.android.shopping",     // Amazon Pay
            "in.amazon.mShop.android.shopping",      // Amazon Pay IN
            "com.whatsapp",                          // WhatsApp Pay
            "money.jupiter",                         // Jupiter
            "com.epifi.mobile",                      // Fi Money
            "org.cosmic.slice",                      // Slice
            "com.navi.finance",                      // Navi
            "com.tatadigital.tcp",                   // Tata Neu
            "com.axis.mobile",                       // Axis Bank
            "com.snapwork.sdfc",                     // HDFC Bank MobileBanking
            "com.snapwork.hdfc",                     // HDFC Bank MobileBanking (Alt)
            "com.csam.icici.bank.imobile",           // ICICI iMobile
            "com.sbi.lotusintouch",                  // SBI YONO
            "com.sbi.upi",                           // BHIM SBI Pay
            "com.msf.kbank.mobile",                  // Kotak Bank
            "com.bankofbaroda.mconnect",             // BOB World
            "com.canarabank.mobility",               // Canara Bank
            "com.idfcfirstbank.optimus",             // IDFC FIRST Bank
            "com.indusind.mobile"                    // IndusInd Bank
        )
        val ALLOWED_PACKAGES = KNOWN_FINANCE_PACKAGES
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "✓ PaymentNotificationListenerService successfully connected and actively listening for notifications.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "⚠ PaymentNotificationListenerService disconnected. Requesting rebind...")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            requestRebind(android.content.ComponentName(this, PaymentNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Never process Pay Tracker's own notifications
        if (packageName == applicationContext.packageName) {
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

        val isKnownApp = packageName in KNOWN_FINANCE_PACKAGES ||
            packageName.contains("pay") ||
            packageName.contains("upi") ||
            packageName.contains("bank") ||
            packageName.contains("sms") ||
            packageName.contains("mms") ||
            packageName.contains("messaging") ||
            packageName.contains("wallet")

        val isDebit = gPayParser.isDebitNotification(title, body)

        if (!isKnownApp && !isDebit) {
            return
        }

        Log.i(TAG, "Notification received from $packageName: title='$title', body='$body'")

        val parsed = gPayParser.parse(
            title = title,
            text = body,
            postTime = sbn.postTime,
            packageName = packageName
        ) ?: run {
            Log.d(TAG, "Notification from $packageName did not match debit payment pattern.")
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
