package com.pay.tracker.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GPayParserTest {

    private lateinit var parser: GPayParser
    private lateinit var suggester: CategorySuggester

    @Before
    fun setUp() {
        parser = GPayParser()
        suggester = CategorySuggester()
    }

    @Test
    fun parse_standardSwiggyPayment_extractsCorrectDetails() {
        val title = "Google Pay"
        val text = "Paid ₹450 to Swiggy using Axis Bank A/C ...1234"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_amountWithCommasAndDecimals_extractsCorrectAmount() {
        val title = "Google Pay"
        val text = "Paid ₹ 2,499.50 to Amazon using HDFC Bank A/C ...9876"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(2499.50, result!!.amount, 0.001)
        assertEquals("Amazon", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Shopping", category)
    }

    @Test
    fun parse_paymentWithUpiReference_extractsRefId() {
        val title = "Google Pay"
        val text = "₹350 paid to Starbucks UPI Ref: 429102910291"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(350.0, result!!.amount, 0.001)
        assertEquals("Starbucks", result.merchant)
        assertEquals("429102910291", result.referenceId)
        assertTrue(result.fingerprint.startsWith("ref:429102910291"))
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_quickGroceryOrder_suggestsGroceries() {
        val title = "Google Pay"
        val text = "Paid ₹285 to Blinkit Commerce"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(285.0, result!!.amount, 0.001)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_p2pTransfer_suggestsTransfer() {
        val title = "Google Pay"
        val text = "Paid ₹1,000 to Rahul Sharma using UPI"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
        assertEquals("Rahul Sharma", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Transfer", category)
    }

    @Test
    fun parse_utilityBill_suggestsBills() {
        val title = "Google Pay"
        val text = "Paid ₹1,850 to BESCOM Electricity using Canara Bank"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(1850.0, result!!.amount, 0.001)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Bills", category)
    }

    @Test
    fun isDebitNotification_rejectsOtpAndCredits() {
        assertFalse(parser.isDebitNotification("Google Pay", "Your OTP is 482910 for transaction"))
        assertFalse(parser.isDebitNotification("Google Pay", "You received ₹500 from John"))
        assertFalse(parser.isDebitNotification("Google Pay", "Rahul Sharma requested ₹300"))
        assertFalse(parser.isDebitNotification("Google Pay", "Payment failed: Bank server down"))
        assertFalse(parser.isDebitNotification("HDFC Bank", "INR 50,000.00 credited to your A/c XX1234 on 01-Nov-26"))
    }

    @Test
    fun isDebitNotification_withSpentAndBankDebit_returnsTrue() {
        assertTrue(parser.isDebitNotification("HDFC Bank", "Spent INR 850.00 on HDFC Bank Card ending 1234"))
        assertTrue(parser.isDebitNotification("Axis Bank", "INR 496.00 debited from A/c XX9270"))
        assertTrue(parser.isDebitNotification("SBI", "debited INR 450.00 on 14-10-26"))
        assertTrue(parser.isDebitNotification("ICICI Bank", "Your Acct XX123 is debited with INR 650.00"))
    }

    @Test
    fun deduplication_sameContentYieldsSameFingerprint() {
        val timestamp = 1715000000000L
        val parsed1 = parser.parse("Google Pay", "Paid ₹150 to Chai Point", timestamp)
        val parsed2 = parser.parse("Google Pay", "Paid ₹150 to Chai Point", timestamp + 5000L) // 5 seconds later

        assertNotNull(parsed1)
        assertNotNull(parsed2)
        assertEquals(parsed1!!.fingerprint, parsed2!!.fingerprint)
    }

    // ==========================================
    // AXIS BANK TEMPLATES (SMS & App Notifications)
    // ==========================================

    @Test
    fun parse_axisBankUpiSms_extractsZeptoAndCorrectAmount() {
        val title = "Axis Bank"
        val text = """
            INR 496.00 debited
            A/c no. XX9270
            13-10-26, 21:18:37
            UPI/P2M/211837387634/ZEPTONOW
            Not you? SMS BLOCKUPI Cust ID to 919951860002
            Axis Bank
        """.trimIndent()

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(496.00, result!!.amount, 0.001)
        assertEquals("Zepto", result.merchant)
        assertEquals("211837387634", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_axisBankUpiSms_extractsSwiggyAndCorrectAmount() {
        val title = "Axis Bank"
        val text = """
            INR 350.00 debited
            A/c no. XX9270
            13-10-26, 20:15:10
            UPI/P2M/211837387635/SWIGGY
            Not you? SMS BLOCKUPI Cust ID to 919951860002
            Axis Bank
        """.trimIndent()

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(350.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("211837387635", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_axisBankUpiSms_extractsZomatoAndCorrectAmount() {
        val title = "Axis Bank"
        val text = """
            INR 280.00 debited
            A/c no. XX9270
            13-10-26, 13:45:22
            UPI/P2M/211837387636/ZOMATO
            Not you? SMS BLOCKUPI Cust ID to 919951860002
            Axis Bank
        """.trimIndent()

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(280.00, result!!.amount, 0.001)
        assertEquals("Zomato", result.merchant)
        assertEquals("211837387636", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_axisBankAppNotification_extractsZeptoAndCorrectAmount() {
        val title = "Axis Mobile"
        val text = "INR 496.00 debited from A/c no. XX9270 on 13-10-26 towards UPI/P2M/211837387634/ZEPTONOW. Avl Bal: INR 15,200.00"

        val result = parser.parse(title, text, packageName = "com.axis.mobile")

        assertNotNull(result)
        assertEquals(496.00, result!!.amount, 0.001)
        assertEquals("Zepto", result.merchant)
        assertEquals("211837387634", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_axisBankAppNotification_extractsSwiggyAndCorrectAmount() {
        val title = "Axis Bank"
        val text = "Your A/c XX9270 is debited for INR 420.00 on 13-10-26 towards SWIGGY. UPI Ref: 211837387637"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(420.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("211837387637", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    // ==========================================
    // HDFC BANK TEMPLATES (SMS & Cards)
    // ==========================================

    @Test
    fun parse_hdfcBankSms_debitedInrWithVpaSwiggy_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "Debited INR 540.00 from HDFC Bank A/c **1234 on 15-OCT-26 to VPA swiggy@icici (UPI Ref No 428918291012). Avl bal: INR 12,345.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(540.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("428918291012", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_hdfcBankSms_debitedInrToZepto_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "debited INR 496.00 from HDFC Bank A/c 1234 on 13-10-26 to ZEPTO (UPI Ref 211837387634). Bal INR 8,900.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(496.00, result!!.amount, 0.001)
        assertEquals("Zepto", result.merchant)
        assertEquals("211837387634", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_hdfcBankSms_spentInrAtSwiggy_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "Spent INR 850.00 on HDFC Bank Card ending 1234 at SWIGGY on 15-OCT-26. Avl limit: INR 45,000.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(850.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_hdfcBankSms_youHaveSpentInrAtAmazon_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "You have spent INR 1,499.00 on your HDFC Bank Card ending 1234 at AMAZON on 15-10-26. Avl limit: INR 50,000.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(1499.00, result!!.amount, 0.001)
        assertEquals("Amazon", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Shopping", category)
    }

    @Test
    fun parse_hdfcBankSms_debitedTowardsZomato_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "Update: INR 1,200.00 debited from HDFC Bank A/c ending 1234 on 15-OCT-26 towards Zomato UPI txn 428918291013"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(1200.00, result!!.amount, 0.001)
        assertEquals("Zomato", result.merchant)
        assertEquals("428918291013", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    // ==========================================
    // SBI (STATE BANK OF INDIA) TEMPLATES
    // ==========================================

    @Test
    fun parse_sbiBankSms_debitedInrTransferToZepto_extractsCorrectDetails() {
        val title = "SBI"
        val text = "debited INR 450.00 on 14-10-26 transfer to ZEPTONOW Ref 428918291017. Bal INR 5,230.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(450.00, result!!.amount, 0.001)
        assertEquals("Zepto", result.merchant)
        assertEquals("428918291017", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_sbiBankSms_debitedByInrTransferToSwiggy_extractsCorrectDetails() {
        val title = "SBI"
        val text = "Your A/c ending 1234 debited by INR 320.00 on 14Oct26 by transfer to SWIGGY Ref No 428918291015. Bal INR 12,450.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(320.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("428918291015", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_sbiBankSms_transferToUpiDrZomato_extractsCorrectDetails() {
        val title = "SBI"
        val text = "Dear SBI User, your A/c XX1234 is debited by INR 500.00 on 14Oct26 transfer to UPI/DR/428918291016/ZOMATO/SBIN. Bal: INR 3,200.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(500.00, result!!.amount, 0.001)
        assertEquals("Zomato", result.merchant)
        assertEquals("428918291016", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_sbiCardSms_spentInrAtSwiggy_extractsCorrectDetails() {
        val title = "SBI Card"
        val text = "Spent INR 650.00 on SBI Credit Card ending 1234 at SWIGGY on 14-Oct-26."

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(650.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    // ==========================================
    // ICICI BANK TEMPLATES
    // ==========================================

    @Test
    fun parse_iciciBankSms_debitedWithInrUpiSwiggy_extractsCorrectDetails() {
        val title = "ICICI Bank"
        val text = "Dear Customer, your Acct XX123 is debited with INR 650.00 on 15-Oct-26. Info: UPI-428918291018-SWIGGY. Available Balance is INR 25,000.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(650.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("428918291018", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_iciciCardSms_spentInrAtAmazonIndia_extractsCorrectDetails() {
        val title = "ICICI Bank"
        val text = "Spent INR 1,499.00 on ICICI Bank Card ending 1234 at AMAZON INDIA on 15-Oct-26. Avl Lmt INR 50,000.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(1499.00, result!!.amount, 0.001)
        assertEquals("Amazon", result.merchant)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Shopping", category)
    }

    @Test
    fun parse_iciciBankSms_debitedInrByUpiZepto_extractsCorrectDetails() {
        val title = "ICICI Bank"
        val text = "debited INR 250.00 from ICICI Bank A/c XX123 on 15-Oct-26 by UPI/428918291019/ZEPTO/Paytm. Avail Bal INR 10,000.00"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(250.00, result!!.amount, 0.001)
        assertEquals("Zepto", result.merchant)
        assertEquals("428918291019", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Groceries", category)
    }

    @Test
    fun parse_iciciBankSms_debitedForInrTowardsSwiggyVpa_extractsCorrectDetails() {
        val title = "ICICI Bank"
        val text = "Your A/c XX123 is debited for INR 300.00 on 15-Oct-26 towards swiggy@icici. UPI Ref: 428918291020."

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(300.00, result!!.amount, 0.001)
        assertEquals("Swiggy", result.merchant)
        assertEquals("428918291020", result.referenceId)
        val category = suggester.suggestCategory(result.merchant, result.rawNotification)
        assertEquals("Food", category)
    }

    @Test
    fun parse_paidToMerchantAmount_extractsCorrectDetails() {
        val title = "PhonePe"
        val text = "Paid to Starbucks ₹150"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.001)
        assertEquals("Starbucks", result.merchant)
    }

    @Test
    fun parse_titleAsMerchant_extractsCorrectDetails() {
        val title = "Chai Point"
        val text = "Paid ₹120 using HDFC Bank account"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(120.0, result!!.amount, 0.001)
        assertEquals("Chai Point", result.merchant)
    }

    @Test
    fun parse_bankDebitWithFallbackMerchant_extractsAmount() {
        val title = "SBI"
        val text = "Your account was debited by Rs.500.00 on 16-10-2026. Avl Bal Rs. 10,230"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.001)
        assertTrue(result.merchant.isNotBlank())
    }

    // ==========================================
    // LEGITIMATE COMPLETED SUBSCRIPTION DEBITS
    // ==========================================

    @Test
    fun parse_axisBankSubscriptionDebit_extractsGoogleCloudAndCorrectAmount() {
        val title = "AD-AXISBK-S"
        val text = "Your A/c has been debited towards Google Cloud for INR 2.00 on 09-10-26. adb9934c76e14b0bbcfa8ea711d5abca@okaxis - Axis Bank"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(2.00, result!!.amount, 0.001)
        assertEquals("Google Cloud", result.merchant)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun parse_completedNetflixAutoPayDebit_extractsCorrectDetails() {
        val title = "HDFC Bank"
        val text = "INR 199.00 debited from A/c XX1234 towards Netflix AutoPay on 15-10-26. UPI Ref 583920194820"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(199.00, result!!.amount, 0.001)
        assertEquals("Netflix", result.merchant)
        assertEquals("583920194820", result.referenceId)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun parse_completedSpotifyAutoDebit_extractsCorrectDetails() {
        val title = "Axis Bank"
        val text = "INR 149.00 auto-debited from A/c XX1234 for Spotify subscription on 15-10-26"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(149.00, result!!.amount, 0.001)
        assertEquals("Spotify", result.merchant)
        assertTrue(result.confidence >= 0.85f)
    }

    @Test
    fun parse_completedCardDebitWithOnMerchant_extractsCorrectDetails() {
        val title = "VM-AXISBK-S"
        val text = "INR 755.40 debited from A/c no. XX109270 on FLY IO 02-10-2026 05:18:50 IST. Avl bal: INR 341.21. Not you? SMS BLOCKCARD XX4738 to +919951860002 - Axis Bank"

        val result = parser.parse(title, text)

        assertNotNull(result)
        assertEquals(755.40, result!!.amount, 0.001)
        assertEquals("FLY IO", result.merchant)
        assertTrue(result.confidence >= 0.85f)
    }

    // ==========================================
    // REJECTION: AUTOPAY MANDATES & SUBSCRIPTION LIFECYCLE
    // ==========================================

    @Test
    fun parse_revokedUpiMandate_isRejected() {
        val title = "AX-AXISBK-S"
        val text = "Your UPI mandate has been successfully revoked towards Google Cloud for INR 15000.00 - Axis Bank"

        val result = parser.parse(title, text)
        assertNull("Mandate revocation must be rejected", result)
    }

    @Test
    fun parse_revokedGooglePlayMandate_isRejected() {
        val title = "AX-AXISBK-S"
        val text = "Your UPI mandate has been successfully revoked towards Google Play for INR 99.00 - Axis Bank"

        val result = parser.parse(title, text)
        assertNull("Mandate revocation must be rejected", result)
    }

    @Test
    fun parse_createdAspresentedMandate_isRejected() {
        val title = "VM-AXISBK-S"
        val text = "Your UPI ASPRESENTED mandate has been successfully created towards Google Cloud from 09-10-26 to 31-12-36 for INR 15000.00 - Axis Bank"

        val result = parser.parse(title, text)
        assertNull("Mandate creation must be rejected", result)
    }

    @Test
    fun parse_autoPayMandateCancelled_isRejected() {
        val title = "HDFC Bank"
        val text = "AutoPay mandate of INR 499.00 for Netflix has been cancelled successfully."

        val result = parser.parse(title, text)
        assertNull("Cancelled mandate must be rejected", result)
    }

    @Test
    fun parse_standingInstructionCreated_isRejected() {
        val title = "ICICI Bank"
        val text = "Standing instruction created for Rs 5000 towards SIP."

        val result = parser.parse(title, text)
        assertNull("Standing instruction creation must be rejected", result)
    }

    @Test
    fun parse_emandateRegistered_isRejected() {
        val title = "SBI"
        val text = "E-mandate of INR 500.00 registered successfully for Hotstar."

        val result = parser.parse(title, text)
        assertNull("E-mandate registration must be rejected", result)
    }

    // ==========================================
    // REJECTION: RENEWAL REMINDERS & UPCOMING PAYMENTS
    // ==========================================

    @Test
    fun parse_subscriptionRenewalReminder_isRejected() {
        val title = "Netflix"
        val text = "Reminder: Your Netflix subscription of Rs. 649 is scheduled for auto-debit on 20-Oct-26."

        val result = parser.parse(title, text)
        assertNull("Subscription reminder must be rejected", result)
    }

    @Test
    fun parse_upcomingAutoPayPaymentAlert_isRejected() {
        val title = "Amazon Pay"
        val text = "Upcoming payment alert: Rs 999 will be auto-debited towards Prime on 25-Oct."

        val result = parser.parse(title, text)
        assertNull("Upcoming auto-debit alert must be rejected", result)
    }

    @Test
    fun parse_renewSubscriptionNotice_isRejected() {
        val title = "Hotstar"
        val text = "Renew your Hotstar subscription for Rs 499 to continue streaming."

        val result = parser.parse(title, text)
        assertNull("Promotional renewal notice must be rejected", result)
    }

    // ==========================================
    // REJECTION: FAILED & REVERSED TRANSACTIONS
    // ==========================================

    @Test
    fun parse_failedTransaction_isRejected() {
        val title = "Google Pay"
        val text = "Payment of Rs. 500 to Uber failed: Bank server timeout."

        val result = parser.parse(title, text)
        assertNull("Failed payment must be rejected", result)
    }

    @Test
    fun parse_declinedTransaction_isRejected() {
        val title = "HDFC Bank"
        val text = "Transaction of INR 200 at Swiggy declined due to insufficient balance."

        val result = parser.parse(title, text)
        assertNull("Declined transaction must be rejected", result)
    }

    @Test
    fun parse_reversedTransaction_isRejected() {
        val title = "PhonePe"
        val text = "UPI payment of Rs. 150 reversed. Amount will be credited back to your account."

        val result = parser.parse(title, text)
        assertNull("Reversed payment must be rejected", result)
    }

    // ==========================================
    // REJECTION: CREDITS, BALANCES, PROMOS & TELCO DUPLICATES
    // ==========================================

    @Test
    fun parse_creditedSms_isRejected() {
        val title = "AX-AXISBK-S"
        val text = """
            INR 100.00 credited
            A/c no. XX9270
            04-10-26, 12:34:30 IST
            UPI/P2A/664382560303/ARYAN R/SIBL/UPI - Axis Bank
        """.trimIndent()

        val result = parser.parse(title, text)
        assertNull("Credited money must be rejected", result)
    }

    @Test
    fun parse_balanceOnlyNotification_doesNotInferExpense() {
        val title = "HDFC Bank"
        val text = "Your account balance is INR 25,000.00. Avl limit: INR 50,000.00."

        val result = parser.parse(title, text)
        assertNull("Balance notification must not infer expense", result)
    }

    @Test
    fun parse_telcoDuplicateRechargeSms_isRejected() {
        val title = "Airtel-S"
        val text = "Recharge of INR 22.00 is successful for your Airtel Mobile on 15-10-2026. Would you like to refer your friends and save Rs100 on next recharge?"

        val result = parser.parse(title, text)
        assertNull("Telco marketing recharge SMS must be rejected", result)
    }

    @Test
    fun parse_billStatementNotice_isRejected() {
        val title = "Axis Bank"
        val text = "Your credit card bill of Rs 15,400 is generated. Minimum due is Rs 1,500 by 25-Oct."

        val result = parser.parse(title, text)
        assertNull("Bill generation notice must be rejected", result)
    }
}

