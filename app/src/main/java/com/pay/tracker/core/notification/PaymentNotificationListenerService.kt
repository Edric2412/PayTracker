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
            "com.indusind.mobile",                   // IndusInd Bank
            // Online shopping, food delivery, grocery, and ride-hailing apps that send transaction alerts
            "in.swiggy.android",                     // Swiggy
            "com.application.zomato",                // Zomato
            "com.grofers.customerapp",               // Blinkit
            "com.zeptoconsumerapp",                  // Zepto
            "com.ubercab",                           // Uber
            "com.rapido.passenger",                  // Rapido
            "com.olacabs.customer",                  // Ola
            "com.flipkart.android",                  // Flipkart
            "com.myntra.android",                    // Myntra
            "com.bbinstant",                         // BigBasket Instant
            "com.bigbasket.mobileapp"                // BigBasket
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
        val (title, body) = extractNotificationContent(notification)

        val isKnownApp = packageName in KNOWN_FINANCE_PACKAGES ||
            packageName == "com.android.shell" ||
            packageName.contains("pay") ||
            packageName.contains("upi") ||
            packageName.contains("bank") ||
            packageName.contains("sms") ||
            packageName.contains("mms") ||
            packageName.contains("messaging") ||
            packageName.contains("message") ||
            packageName.contains("wallet") ||
            packageName.contains("finance")

        val isTransaction = gPayParser.isCompletedTransaction(title, body)

        if (!isKnownApp && !isTransaction) {
            return
        }

        Log.i(TAG, "Notification received from $packageName: title='$title', body='$body'")

        val parsed = gPayParser.parse(
            title = title,
            text = body,
            postTime = sbn.postTime,
            packageName = packageName
        ) ?: run {
            Log.d(TAG, "Notification from $packageName did not match payment pattern.")
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

                // 2. Suggest category (100% on-device rule-based keyword matching)
                val suggestedCategory = categorySuggester.suggestCategory(
                    merchant = parsed.merchant,
                    rawText = body,
                    classification = parsed.classification
                )

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

    private fun extractNotificationContent(notification: Notification): Pair<String, String> {
        val extras = notification.extras ?: return Pair("", "")

        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: extras.getString("android.title")
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?: ""

        val textParts = LinkedHashSet<String>()

        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }
        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }

        // Handle InboxStyle (array of CharSequence)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            for (line in textLines) {
                line?.toString()?.takeIf { it.isNotBlank() }?.let { textParts.add(it.trim()) }
            }
        }

        // Handle MessagingStyle (array of Message Bundles) - used by Google Messages and Google Pay!
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
            try {
                val parsedMessages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(messages)
                for (msg in parsedMessages) {
                    val msgText = msg.text?.toString()
                    val sender = msg.senderPerson?.name?.toString() ?: msg.sender?.toString()
                    if (title.isBlank() && !sender.isNullOrBlank()) {
                        title = sender
                    }
                    if (!msgText.isNullOrBlank()) {
                        textParts.add(msgText.trim())
                    }
                }
            } catch (_: Throwable) {
                for (msg in messages) {
                    if (msg is android.os.Bundle) {
                        val msgText = msg.getCharSequence("text")?.toString()
                        val sender = msg.getCharSequence("sender")?.toString()
                        if (title.isBlank() && !sender.isNullOrBlank()) {
                            title = sender
                        }
                        if (!msgText.isNullOrBlank()) {
                            textParts.add(msgText.trim())
                        }
                    }
                }
            }
        }

        // Ticker text fallback
        notification.tickerText?.toString()?.takeIf { it.isNotBlank() }?.let {
            textParts.add(it.trim())
        }

        val body = textParts.joinToString("\n")
        return Pair(title.trim(), body.trim())
    }
}
