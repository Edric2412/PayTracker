package com.pay.tracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.core.notification.NotificationHelper
import com.pay.tracker.core.parser.CategorySuggester
import com.pay.tracker.core.parser.GPayParser
import com.pay.tracker.core.parser.ParsedTransaction
import com.pay.tracker.core.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimulatorPreset(
    val title: String,
    val notificationTitle: String,
    val notificationBody: String,
    val expectedCategory: String
)

data class SimulationOutcome(
    val success: Boolean,
    val message: String,
    val parsedTransaction: ParsedTransaction? = null,
    val insertedId: Long? = null,
    val suggestedCategory: String? = null
)

class SimulatorViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val parser = GPayParser()
    private val suggester = CategorySuggester()

    val presets = listOf(
        SimulatorPreset(
            title = "Axis Bank Zepto SMS",
            notificationTitle = "Axis Bank",
            notificationBody = "INR 496.00 debited\nA/c no. XX9270\n13-10-26, 21:18:37\nUPI/P2M/211837387634/ZEPTONOW\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            expectedCategory = "Groceries"
        ),
        SimulatorPreset(
            title = "HDFC Bank Swiggy SMS",
            notificationTitle = "HDFC Bank",
            notificationBody = "Debited INR 540.00 from HDFC Bank A/c **1234 on 15-OCT-26 to VPA swiggy@icici (UPI Ref No 428918291012). Avl bal: INR 12,345.00",
            expectedCategory = "Food"
        ),
        SimulatorPreset(
            title = "SBI Zepto SMS",
            notificationTitle = "SBI",
            notificationBody = "debited INR 450.00 on 14-10-26 transfer to ZEPTONOW Ref 428918291017. Bal INR 5,230.00",
            expectedCategory = "Groceries"
        ),
        SimulatorPreset(
            title = "ICICI Bank Swiggy SMS",
            notificationTitle = "ICICI Bank",
            notificationBody = "Dear Customer, your Acct XX123 is debited with INR 650.00 on 15-Oct-26. Info: UPI-428918291018-SWIGGY. Available Balance is INR 25,000.00",
            expectedCategory = "Food"
        ),
        SimulatorPreset(
            title = "HDFC Card Spent SMS",
            notificationTitle = "HDFC Bank",
            notificationBody = "Spent INR 850.00 on HDFC Bank Card ending 1234 at SWIGGY on 15-OCT-26. Avl limit: INR 45,000.00",
            expectedCategory = "Food"
        ),
        SimulatorPreset(
            title = "Swiggy Food Order",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹450 to Swiggy using Axis Bank A/C ...1234. UPI Ref: 428190281920",
            expectedCategory = "Food"
        ),
        SimulatorPreset(
            title = "Blinkit Quick Grocery",
            notificationTitle = "Google Pay",
            notificationBody = "₹1,200 paid to Blinkit using UPI. UPI Ref: 987654321012",
            expectedCategory = "Groceries"
        ),
        SimulatorPreset(
            title = "Uber Ride",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹320 to Uber using Google Pay. UPI Ref: 112233445566",
            expectedCategory = "Travel"
        ),
        SimulatorPreset(
            title = "Amazon Shopping",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹2,499 to Amazon using HDFC Bank A/C ...5678",
            expectedCategory = "Shopping"
        ),
        SimulatorPreset(
            title = "BESCOM Electricity Bill",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹1,040 to BESCOM Electricity using UPI. UPI Ref: 887766554433",
            expectedCategory = "Bills"
        ),
        SimulatorPreset(
            title = "P2P Friend Transfer",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹500 to Edric using UPI. UPI Ref: 554433221100",
            expectedCategory = "Transfer"
        ),
        SimulatorPreset(
            title = "Apollo Pharmacy",
            notificationTitle = "Google Pay",
            notificationBody = "You paid ₹750 to Apollo Pharmacy using UPI. UPI Ref: 334455667788",
            expectedCategory = "Health"
        ),
        SimulatorPreset(
            title = "BookMyShow Movie",
            notificationTitle = "Google Pay",
            notificationBody = "Paid ₹600 to BookMyShow using Axis Bank A/C ...1234",
            expectedCategory = "Entertainment"
        )
    )

    private val _notificationTitle = MutableStateFlow(presets.first().notificationTitle)
    val notificationTitle: StateFlow<String> = _notificationTitle.asStateFlow()

    private val _notificationBody = MutableStateFlow(presets.first().notificationBody)
    val notificationBody: StateFlow<String> = _notificationBody.asStateFlow()

    private val _outcome = MutableStateFlow<SimulationOutcome?>(null)
    val outcome: StateFlow<SimulationOutcome?> = _outcome.asStateFlow()

    fun onTitleChange(title: String) {
        _notificationTitle.value = title
    }

    fun onBodyChange(body: String) {
        _notificationBody.value = body
    }

    fun applyPreset(preset: SimulatorPreset) {
        _notificationTitle.value = preset.notificationTitle
        _notificationBody.value = preset.notificationBody
        _outcome.value = null
    }

    fun simulate(context: Context) {
        val title = _notificationTitle.value.trim()
        val body = _notificationBody.value.trim()

        val parsed = parser.parse(
            title = title,
            text = body,
            postTime = System.currentTimeMillis()
        )

        if (parsed == null) {
            _outcome.value = SimulationOutcome(
                success = false,
                message = "Parser Error: Text did not match any Google Pay debit patterns. Make sure it describes an outgoing payment (e.g., 'Paid ₹... to ...')."
            )
            return
        }

        viewModelScope.launch {
            try {
                var suggestedCategory = suggester.suggestCategory(parsed.merchant, body)
                if (suggestedCategory == com.pay.tracker.core.model.Category.TRANSFER.name) {
                    val geminiSuggester = com.pay.tracker.core.ai.GeminiCategorySuggester(context)
                    val aiCategory = geminiSuggester.suggestCategoryWithGemini(parsed.merchant, body)
                    if (aiCategory != null) {
                        suggestedCategory = aiCategory
                    }
                }
                val isDuplicate = repository.isDuplicate(parsed.fingerprint, parsed.referenceId)

                val idToUse: Long
                if (isDuplicate) {
                    // When simulating the exact same sample again, generate a unique test fingerprint so the user can test multiple times
                    val newFingerprint = "${parsed.fingerprint}_${System.currentTimeMillis()}"
                    val uniqueTxn = parsed.copy(fingerprint = newFingerprint)
                        .toTransaction(category = suggestedCategory, status = TransactionStatus.PENDING)
                    idToUse = repository.saveTransaction(uniqueTxn)
                } else {
                    val txn = parsed.toTransaction(
                        category = suggestedCategory,
                        status = TransactionStatus.PENDING
                    )
                    idToUse = repository.saveTransaction(txn)
                }

                // Trigger Android system notification
                NotificationHelper.showExpenseNotification(
                    context = context,
                    transactionId = idToUse,
                    amount = parsed.amount,
                    merchant = parsed.merchant,
                    category = suggestedCategory
                )

                _outcome.value = SimulationOutcome(
                    success = true,
                    message = "Success! Transaction parsed & saved as PENDING in Room (ID: $idToUse). Follow-up notification posted with [✓ Confirm] & [Change Category] actions.",
                    parsedTransaction = parsed,
                    insertedId = idToUse,
                    suggestedCategory = suggestedCategory
                )
            } catch (e: Exception) {
                _outcome.value = SimulationOutcome(
                    success = false,
                    message = "Database Error: ${e.message}"
                )
            }
        }
    }
}
