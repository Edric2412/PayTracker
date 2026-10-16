package com.pay.tracker.core.calculator

import com.pay.tracker.core.model.Category
import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionClassification
import com.pay.tracker.core.model.TransactionDirection
import com.pay.tracker.core.model.TransactionStatus
import com.pay.tracker.ui.viewmodel.CategoryExpense

/**
 * Result data holder for calculated cash flow and budget metrics.
 */
data class MonthCashFlowMetrics(
    val grossExpenses: Double = 0.0,
    val incomeReceived: Double = 0.0,
    val refundsReceived: Double = 0.0,
    val cashbackReceived: Double = 0.0,
    val netCashFlow: Double = 0.0, // incomeReceived - grossExpenses
    val totalCategoryOffsets: Double = 0.0, // refunds/cashbacks applied to expense categories
    val netExpenses: Double = 0.0, // (grossExpenses - totalCategoryOffsets).coerceAtLeast(0.0)
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val refundCount: Int = 0,
    val cashbackCount: Int = 0,
    val transferCount: Int = 0,
    val categoryBreakdown: List<CategoryExpense> = emptyList(),
    val categoryNetSpend: Map<String, Double> = emptyMap(),
    val grossBudgetRemaining: Double = 0.0,
    val netBudgetRemaining: Double = 0.0
) {
    val refunds: Double get() = refundsReceived
    val cashbacks: Double get() = cashbackReceived
    val eligibleOffsets: Double get() = totalCategoryOffsets
}

typealias CashFlowMetrics = MonthCashFlowMetrics

/**
 * Shared calculation layer used across Dashboard, History summaries, and Category analytics.
 */
object CashFlowCalculator {

    private val PURE_INCOME_CATEGORY_NAMES = setOf(
        Category.SALARY.name.lowercase(),
        Category.INCOME.name.lowercase()
    )

    fun isEligibleExpense(txn: Transaction): Boolean {
        return txn.direction == TransactionDirection.DEBIT &&
                txn.classification == TransactionClassification.EXPENSE
    }

    fun isEligibleIncome(txn: Transaction): Boolean {
        return txn.direction == TransactionDirection.CREDIT &&
                txn.classification == TransactionClassification.INCOME
    }

    fun calculateGrossExpenses(transactions: List<Transaction>): Double {
        return transactions.filter { it.status == TransactionStatus.CONFIRMED && isEligibleExpense(it) }.sumOf { it.amount }
    }

    /**
     * Calculates comprehensive cash flow metrics for a given list of transactions.
     * By default, calculates for CONFIRMED transactions.
     */
    fun calculateMetrics(
        transactions: List<Transaction>,
        budgetTarget: Double = 0.0,
        requireConfirmed: Boolean = true
    ): MonthCashFlowMetrics {
        val eligible = if (requireConfirmed) {
            transactions.filter { it.status == TransactionStatus.CONFIRMED }
        } else {
            transactions
        }

        var grossExpenses = 0.0
        var incomeReceived = 0.0
        var refundsReceived = 0.0
        var cashbackReceived = 0.0

        var expenseCount = 0
        var incomeCount = 0
        var refundCount = 0
        var cashbackCount = 0
        var transferCount = 0

        // Maps to aggregate per category for net category spending
        val categoryGrossDebits = mutableMapOf<String, Double>()
        val categoryOffsets = mutableMapOf<String, Double>()

        eligible.forEach { txn ->
            when (txn.classification) {
                TransactionClassification.TRANSFER -> {
                    // Own-account transfers are excluded from both income and expenses
                    transferCount++
                }
                TransactionClassification.EXPENSE -> {
                    if (txn.direction == TransactionDirection.DEBIT) {
                        grossExpenses += txn.amount
                        expenseCount++
                        val cat = txn.category.trim()
                        categoryGrossDebits[cat] = (categoryGrossDebits[cat] ?: 0.0) + txn.amount
                    }
                }
                TransactionClassification.INCOME -> {
                    if (txn.direction == TransactionDirection.CREDIT) {
                        incomeReceived += txn.amount
                        incomeCount++
                    }
                }
                TransactionClassification.REFUND -> {
                    if (txn.direction == TransactionDirection.CREDIT) {
                        refundsReceived += txn.amount
                        refundCount++
                        val cat = txn.category.trim()
                        // If refund is assigned to an expense category (not pure income/salary), it offsets that category
                        if (cat.lowercase() !in PURE_INCOME_CATEGORY_NAMES && !cat.equals("refund", ignoreCase = true)) {
                            categoryOffsets[cat] = (categoryOffsets[cat] ?: 0.0) + txn.amount
                        } else {
                            // General refund offsets overall
                            categoryOffsets["Other"] = (categoryOffsets["Other"] ?: 0.0) + txn.amount
                        }
                    }
                }
                TransactionClassification.CASHBACK -> {
                    if (txn.direction == TransactionDirection.CREDIT) {
                        cashbackReceived += txn.amount
                        cashbackCount++
                        val cat = txn.category.trim()
                        if (cat.lowercase() !in PURE_INCOME_CATEGORY_NAMES && !cat.equals("cashback", ignoreCase = true)) {
                            categoryOffsets[cat] = (categoryOffsets[cat] ?: 0.0) + txn.amount
                        } else {
                            categoryOffsets["Other"] = (categoryOffsets["Other"] ?: 0.0) + txn.amount
                        }
                    }
                }
            }
        }

        // Category-specific offsets capped to each category's gross spend
        var categorySpecificOffsets = 0.0
        val netCategorySpends = mutableMapOf<String, Double>()

        categoryGrossDebits.forEach { (cat, gross) ->
            val offset = categoryOffsets[cat] ?: 0.0
            val effectiveOffset = offset.coerceAtMost(gross)
            categorySpecificOffsets += effectiveOffset
            netCategorySpends[cat] = (gross - effectiveOffset).coerceAtLeast(0.0)
        }

        // General offsets from refunds/cashback not assigned to an active expense category
        var generalOffsets = 0.0
        categoryOffsets.forEach { (cat, offset) ->
            if (!categoryGrossDebits.containsKey(cat)) {
                generalOffsets += offset
            }
        }
        val remainingGross = (grossExpenses - categorySpecificOffsets).coerceAtLeast(0.0)
        val effectiveGeneralOffset = generalOffsets.coerceAtMost(remainingGross)
        val totalCategoryOffsets = categorySpecificOffsets + effectiveGeneralOffset

        val netExpenses = (grossExpenses - totalCategoryOffsets).coerceAtLeast(0.0)
        val netCashFlow = incomeReceived - grossExpenses

        // Build net category breakdown
        val breakdown = if (netExpenses <= 0.0) {
            emptyList()
        } else {
            netCategorySpends
                .filter { it.value > 0.0 }
                .map { (cat, netAmount) ->
                    CategoryExpense(
                        category = cat,
                        totalAmount = netAmount,
                        percentage = ((netAmount / netExpenses) * 100).toFloat()
                    )
                }
                .sortedByDescending { it.totalAmount }
        }

        val grossRemaining = (budgetTarget - grossExpenses).coerceAtLeast(0.0)
        val netRemaining = (budgetTarget - netExpenses).coerceAtLeast(0.0)

        return MonthCashFlowMetrics(
            grossExpenses = grossExpenses,
            incomeReceived = incomeReceived,
            refundsReceived = refundsReceived,
            cashbackReceived = cashbackReceived,
            netCashFlow = netCashFlow,
            totalCategoryOffsets = totalCategoryOffsets,
            netExpenses = netExpenses,
            expenseCount = expenseCount,
            incomeCount = incomeCount,
            refundCount = refundCount,
            cashbackCount = cashbackCount,
            transferCount = transferCount,
            categoryBreakdown = breakdown,
            categoryNetSpend = netCategorySpends,
            grossBudgetRemaining = grossRemaining,
            netBudgetRemaining = netRemaining
        )
    }

    /**
     * Calculates gross and net budget remaining.
     */
    fun calculateBudgetRemaining(
        budgetTarget: Double,
        grossExpenses: Double,
        totalCategoryOffsets: Double
    ): Pair<Double, Double> {
        val grossRemaining = (budgetTarget - grossExpenses).coerceAtLeast(0.0)
        val netRemaining = (budgetTarget - grossExpenses + totalCategoryOffsets).coerceAtLeast(0.0)
        return grossRemaining to netRemaining
    }
}
