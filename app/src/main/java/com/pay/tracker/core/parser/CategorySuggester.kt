package com.pay.tracker.core.parser

import com.pay.tracker.core.model.Category

/**
 * Suggests expense categories based on keywords matching merchant names
 * or transaction notification descriptions.
 */
class CategorySuggester(
    private val categories: List<Category> = Category.DEFAULT_CATEGORIES
) {

    /**
     * Suggests the best matching category name for a given merchant and optional notification text.
     * Defaults to "Transfer" when no specific merchant category matches.
     */
    fun suggestCategory(merchant: String, rawText: String = ""): String {
        val normalizedMerchant = merchant.lowercase().trim()
        val normalizedText = rawText.lowercase().trim()
        val combinedText = "$normalizedMerchant $normalizedText"

        // Priority 1: Exact or substring match in merchant name
        for (category in categories) {
            for (keyword in category.defaultKeywords) {
                val kw = keyword.lowercase()
                if (normalizedMerchant.contains(kw) || kw.contains(normalizedMerchant)) {
                    return category.name
                }
            }
        }

        // Priority 2: Keyword match in full text
        for (category in categories) {
            for (keyword in category.defaultKeywords) {
                val kw = keyword.lowercase()
                val regex = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(combinedText)) {
                    return category.name
                }
            }
        }

        // Priority 3: Fallback based on typical peer-to-peer or general payments
        return Category.TRANSFER.name
    }

    /**
     * Finds Category object by name, or returns null.
     */
    fun getCategoryByName(name: String): Category? {
        return categories.find { it.name.equals(name, ignoreCase = true) }
    }
}
