package com.pay.tracker.core.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.core.parser.CategorySuggester
import com.pay.tracker.core.parser.GPayParser
import com.pay.tracker.core.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class SmsImportResult(
    val importedCount: Int = 0,
    val totalAmount: Double = 0.0,
    val skippedDuplicates: Int = 0,
    val totalScanned: Int = 0,
    val errorMessage: String? = null
)

class SmsTransactionImporter(
    private val repository: TransactionRepository,
    private val gPayParser: GPayParser = GPayParser(),
    private val categorySuggester: CategorySuggester = CategorySuggester()
) {

    companion object {
        private const val TAG = "SmsImporter"

        fun getStartOfMonthEpoch(): Long {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }

    suspend fun importMonthSms(
        context: Context,
        sinceEpoch: Long = getStartOfMonthEpoch()
    ): SmsImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceEpoch.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        var imported = 0
        var totalAmount = 0.0
        var duplicates = 0
        var scanned = 0

        try {
            val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    scanned++
                    val address = it.getString(addressIdx).orEmpty()
                    val body = it.getString(bodyIdx).orEmpty()
                    val date = it.getLong(dateIdx)

                    val parsed = gPayParser.parse(
                        title = address,
                        text = body,
                        postTime = date,
                        packageName = address
                    ) ?: continue

                    // Deduplicate
                    if (repository.isDuplicate(parsed.fingerprint, parsed.referenceId)) {
                        duplicates++
                        continue
                    }

                    // Auto categorize
                    val category = categorySuggester.suggestCategory(parsed.merchant, body)

                    val txn = parsed.toTransaction(
                        category = category,
                        status = TransactionStatus.CONFIRMED
                    )

                    val id = repository.saveTransaction(txn)
                    if (id > 0) {
                        imported++
                        totalAmount += parsed.amount
                    }
                }
            }
            SmsImportResult(
                importedCount = imported,
                totalAmount = totalAmount,
                skippedDuplicates = duplicates,
                totalScanned = scanned
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error importing SMS", e)
            SmsImportResult(
                importedCount = imported,
                totalAmount = totalAmount,
                skippedDuplicates = duplicates,
                totalScanned = scanned,
                errorMessage = e.localizedMessage ?: "Failed to read SMS messages"
            )
        }
    }
}
