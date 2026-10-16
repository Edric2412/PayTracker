package com.pay.tracker.core.notification

import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.core.parser.GPayParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFlowTest {

    @Test
    fun notificationConstants_verifyChannelAndActions() {
        assertEquals("pay_expense_alerts", NotificationHelper.CHANNEL_ID)
        assertEquals("Expense Alerts", NotificationHelper.CHANNEL_NAME)
        assertEquals("com.pay.tracker.ACTION_CONFIRM_EXPENSE", NotificationHelper.ACTION_CONFIRM_EXPENSE)
        assertEquals("com.pay.tracker.ACTION_EDIT_CATEGORY", NotificationHelper.ACTION_EDIT_CATEGORY)
        assertEquals("extra_transaction_id", NotificationHelper.EXTRA_TRANSACTION_ID)
        assertEquals("extra_category", NotificationHelper.EXTRA_CATEGORY)
    }

    @Test
    fun allowedPackages_containsAllEssentialIndianPaymentApps() {
        val packages = PaymentNotificationListenerService.ALLOWED_PACKAGES

        // Google Pay India
        assertTrue(packages.contains(GPayParser.GPAY_PACKAGE))
        assertTrue(packages.contains("com.google.android.apps.nbu.paisa.user"))

        // Default Nothing Phone (2a) SMS app (Google Messages) and standard SMS
        assertTrue(packages.contains("com.google.android.apps.messaging"))
        assertTrue(packages.contains("com.android.mms"))

        // Key Indian banks and UPI apps
        assertTrue(packages.contains("com.axis.mobile"))
        assertTrue(packages.contains("com.phonepe.app"))
        assertTrue(packages.contains("net.one97.paytm"))
        assertTrue(packages.contains("in.org.npci.upiapp"))
        assertTrue(packages.contains("com.dreamplug.androidapp"))
        assertTrue(packages.contains("com.snapwork.sdfc"))
        assertTrue(packages.contains("com.csam.icici.bank.imobile"))
        assertTrue(packages.contains("com.sbi.lotusintouch"))
    }

    @Test
    fun transactionConfirmation_statusTransitionFromPendingToConfirmed() {
        val pendingTxn = Transaction(
            id = 42L,
            amount = 496.00,
            merchant = "Zepto",
            category = Category.GROCERIES.name,
            timestamp = System.currentTimeMillis(),
            sourceApp = "com.google.android.apps.messaging",
            rawNotification = "INR 496.00 debited UPI/P2M/211837387634/ZEPTONOW",
            fingerprint = "ref:211837387634",
            referenceId = "211837387634",
            status = TransactionStatus.PENDING
        )

        assertEquals(TransactionStatus.PENDING, pendingTxn.status)

        // Simulating the effect of confirmTransaction
        val confirmedTxn = pendingTxn.copy(status = TransactionStatus.CONFIRMED)
        assertEquals(TransactionStatus.CONFIRMED, confirmedTxn.status)
        assertEquals(496.00, confirmedTxn.amount, 0.001)
        assertEquals("Zepto", confirmedTxn.merchant)
        assertEquals(Category.GROCERIES.name, confirmedTxn.category)
    }

    @Test
    fun gPayNotification_parsesAndPreparesPendingTransaction() {
        val parser = GPayParser()
        val parsed = parser.parse(
            title = "Google Pay",
            text = "Paid ₹320 to Uber using Google Pay. UPI Ref: 112233445566"
        )

        assertNotNull(parsed)
        val transaction = parsed!!.toTransaction(
            category = Category.TRAVEL.name,
            status = TransactionStatus.PENDING
        )

        assertEquals(TransactionStatus.PENDING, transaction.status)
        assertEquals(320.0, transaction.amount, 0.001)
        assertEquals("Uber", transaction.merchant)
        assertEquals("112233445566", transaction.referenceId)
        assertEquals("Travel", transaction.category)
    }
}
