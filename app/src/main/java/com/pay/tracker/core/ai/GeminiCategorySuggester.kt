package com.pay.tracker.core.ai

import android.content.Context
import android.util.Log
import com.pay.tracker.BuildConfig
import com.pay.tracker.core.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiCategorySuggester(
    private val context: Context
) {

    companion object {
        private const val TAG = "GeminiSuggester"
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
        const val PLACEHOLDER_KEY = "YOUR_GEMINI_API_KEY"
    }

    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("pay_tracker_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", "").orEmpty().trim()
        if (savedKey.isNotBlank()) return savedKey

        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        return if (buildKey.isNotBlank() && buildKey != PLACEHOLDER_KEY) buildKey else ""
    }

    /**
     * Categorizes ambiguous transactions using Gemini 3.5 Flash Lite API.
     * Returns null if API call fails, times out, or returns an unmapped category.
     */
    suspend fun suggestCategoryWithGemini(
        merchant: String,
        description: String = ""
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) return@withContext null

        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent?key=$apiKey"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val validCategoryNames = Category.DEFAULT_CATEGORIES.map { it.name }.joinToString(", ")
            val prompt = """
                Categorize this transaction into exactly one of: [$validCategoryNames]. Reply with ONLY the category name. No punctuation, no preamble.
                Merchant: $merchant
                Details: $description
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawReply = parts.getJSONObject(0).optString("text", "").trim()
                        val matchingCategory = Category.DEFAULT_CATEGORIES.find {
                            it.name.equals(rawReply, ignoreCase = true) || rawReply.contains(it.name, ignoreCase = true)
                        }
                        if (matchingCategory != null) {
                            Log.i(TAG, "Gemini $DEFAULT_MODEL categorized '$merchant' -> ${matchingCategory.name}")
                            return@withContext matchingCategory.name
                        }
                    }
                }
            } else {
                Log.w(TAG, "Gemini API returned response code: $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini categorization failed or timed out: ${e.message}")
        }
        return@withContext null
    }
}
