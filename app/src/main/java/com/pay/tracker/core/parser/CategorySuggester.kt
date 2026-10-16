package com.pay.tracker.core.parser

import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.TransactionClassification

/**
 * Suggests expense and income categories based on keywords matching merchant names
 * or transaction notification descriptions.
 */
class CategorySuggester(
    private val categories: List<Category> = Category.DEFAULT_CATEGORIES
) {

    /**
     * Suggests the best matching category name for a given merchant, optional notification text,
     * and transaction classification.
     */
    fun suggestCategory(
        merchant: String,
        rawText: String = "",
        classification: TransactionClassification = TransactionClassification.EXPENSE
    ): String {
        val normalizedMerchant = merchant.lowercase().trim()
        val normalizedText = rawText.lowercase().trim()
        val combinedText = "$normalizedMerchant $normalizedText"

        when (classification) {
            TransactionClassification.REFUND -> {
                // If refund is for a known merchant/category (e.g. Amazon -> Shopping, Swiggy -> Food),
                // categorize it to that expense category so it can offset category spending.
                val expenseCat = findMatchingExpenseCategory(normalizedMerchant, combinedText)
                if (expenseCat != null) return expenseCat
                return Category.REFUND.name
            }
            TransactionClassification.CASHBACK -> {
                val expenseCat = findMatchingExpenseCategory(normalizedMerchant, combinedText)
                if (expenseCat != null) return expenseCat
                return Category.CASHBACK.name
            }
            TransactionClassification.INCOME -> {
                // Check if salary-related
                val salaryKeywords = listOf("salary", "payroll", "stipend", "wages", "remuneration", "bonus", "corp salary")
                if (salaryKeywords.any { combinedText.contains(it) }) {
                    return Category.SALARY.name
                }
                return Category.INCOME.name
            }
            TransactionClassification.TRANSFER -> {
                return Category.TRANSFER.name
            }
            TransactionClassification.EXPENSE -> {
                val expenseCat = findMatchingExpenseCategory(normalizedMerchant, combinedText)
                if (expenseCat != null) return expenseCat
                return Category.TRANSFER.name
            }
        }
    }

    private fun findMatchingExpenseCategory(normalizedMerchant: String, combinedText: String): String? {
        val expenseCategories = Category.EXPENSE_CATEGORIES

        // Priority 1: Exact or substring match in merchant name
        for (category in expenseCategories) {
            for (keyword in category.defaultKeywords) {
                val kw = keyword.lowercase()
                if (normalizedMerchant.contains(kw) || kw.contains(normalizedMerchant)) {
                    return category.name
                }
            }
        }

        // Priority 2: Keyword match in full text
        for (category in expenseCategories) {
            for (keyword in category.defaultKeywords) {
                val kw = keyword.lowercase()
                val regex = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(combinedText)) {
                    return category.name
                }
            }
        }

        return null
    }

    /**
     * Finds Category object by name, or returns null.
     */
    fun getCategoryByName(name: String): Category? {
        return categories.find { it.name.equals(name, ignoreCase = true) }
    }
}
