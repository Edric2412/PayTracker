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
}

