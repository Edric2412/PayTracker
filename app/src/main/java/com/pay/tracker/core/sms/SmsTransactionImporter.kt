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

        fun getStartOfMonthEpoch(cal: Calendar = Calendar.getInstance()): Long {
            val c = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis
        }

        fun getEndOfMonthEpoch(cal: Calendar = Calendar.getInstance()): Long {
            val c = (cal.clone() as Calendar).apply {
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, maxDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return c.timeInMillis
        }
    }

    /**
     * Strictly imports SMS from the current calendar month only (startOfMonth to endOfMonth).
     */
    suspend fun importMonthSms(
        context: Context,
        startEpoch: Long = getStartOfMonthEpoch(),
        endEpoch: Long = getEndOfMonthEpoch(),
        onProgress: (scanned: Int, total: Int, found: Int) -> Unit = { _, _, _ -> }
    ): SmsImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?"
        val selectionArgs = arrayOf(startEpoch.toString(), endEpoch.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        var imported = 0
        var totalAmount = 0.0
        var duplicates = 0
        var scanned = 0

        // Clean up any previously imported false positive transactions
        repository.purgeInvalidTransactions()

        try {
            val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
            cursor?.use {
                val totalCount = it.count
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    scanned++
                    if (scanned % 5 == 0 || scanned == totalCount) {
                        onProgress(scanned, totalCount, imported)
                    }
                    val address = it.getString(addressIdx).orEmpty()
                    val body = it.getString(bodyIdx).orEmpty()
                    val date = it.getLong(dateIdx)

                    // Strict boundary check: ensure SMS is within the requested month range
                    if (date < startEpoch || date > endEpoch) {
                        continue
                    }

                    // Skip promotional / telemarketing / personal sender headers
                    val addressLower = address.lowercase().trim()
                    if (addressLower.endsWith("-p") || Regex("""^[a-z]{2}-\d{5,6}$|^\d{5,6}$|^(?:\+?91)?[6-9]\d{9}$""").containsMatchIn(addressLower)) {
                        continue
                    }

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
                    val category = categorySuggester.suggestCategory(
                        merchant = parsed.merchant,
                        rawText = body,
                        classification = parsed.classification
                    )

                    // Assign status: High confidence -> CONFIRMED, Uncertain/Ambiguous -> PENDING for review
                    val status = if (parsed.confidence >= 0.85f) {
                        TransactionStatus.CONFIRMED
                    } else {
                        TransactionStatus.PENDING
                    }

                    val txn = parsed.toTransaction(
                        category = category,
                        status = status
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

    /**
     * Imports all historical SMS messages, ensuring each transaction retains its
     * original timestamp so it is categorized under its specific month and year.
     */
    suspend fun importAllSms(
        context: Context,
        onProgress: (scanned: Int, total: Int, found: Int) -> Unit = { _, _, _ -> }
    ): SmsImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        var imported = 0
        var totalAmount = 0.0
        var duplicates = 0
        var scanned = 0

        // Clean up any previously imported false positive transactions
        repository.purgeInvalidTransactions()

        try {
            val cursor = resolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} ASC")
            cursor?.use {
                val totalCount = it.count
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    scanned++
                    if (scanned % 5 == 0 || scanned == totalCount) {
                        onProgress(scanned, totalCount, imported)
                    }
                    val address = it.getString(addressIdx).orEmpty()
                    val body = it.getString(bodyIdx).orEmpty()
                    val date = it.getLong(dateIdx)

                    // Skip promotional / telemarketing / personal sender headers
                    val addressLower = address.lowercase().trim()
                    if (addressLower.endsWith("-p") || Regex("""^[a-z]{2}-\d{5,6}$|^\d{5,6}$|^(?:\+?91)?[6-9]\d{9}$""").containsMatchIn(addressLower)) {
                        continue
                    }

                    val parsed = gPayParser.parse(
                        title = address,
                        text = body,
                        postTime = date,
                        packageName = address
                    ) ?: continue

                    if (repository.isDuplicate(parsed.fingerprint, parsed.referenceId)) {
                        duplicates++
                        continue
                    }

                    val category = categorySuggester.suggestCategory(
                        merchant = parsed.merchant,
                        rawText = body,
                        classification = parsed.classification
                    )
                    val status = if (parsed.confidence >= 0.85f) {
                        TransactionStatus.CONFIRMED
                    } else {
                        TransactionStatus.PENDING
                    }

                    val txn = parsed.toTransaction(
                        category = category,
                        status = status
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
            Log.e(TAG, "Error importing historical SMS", e)
            SmsImportResult(
                importedCount = imported,
                totalAmount = totalAmount,
                skippedDuplicates = duplicates,
                totalScanned = scanned,
                errorMessage = e.localizedMessage ?: "Failed to read SMS messages"
            )
        }
    }

    /**
     * Purges previously imported transactions that fail the current parser rules.
     */
    suspend fun purgeInvalidTransactions(): Int = withContext(Dispatchers.IO) {
        repository.purgeInvalidTransactions()
    }
}
