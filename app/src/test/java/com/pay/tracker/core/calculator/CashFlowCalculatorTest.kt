package com.pay.tracker.core.calculator

import com.pay.tracker.core.model.Transaction
import com.pay.tracker.core.model.TransactionClassification
import com.pay.tracker.core.model.TransactionDirection
import com.pay.tracker.core.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CashFlowCalculatorTest {

    private fun createTxn(
        id: Long = 1,
        amount: Double,
        category: String = "Food",
        direction: TransactionDirection = TransactionDirection.DEBIT,
        classification: TransactionClassification = TransactionClassification.EXPENSE,
        status: TransactionStatus = TransactionStatus.CONFIRMED,
        merchant: String = "Test"
    ): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            merchant = merchant,
            category = category,
            timestamp = System.currentTimeMillis(),
            status = status,
            sourceApp = "SMS",
            rawNotification = "Test txn",
            referenceId = "REF$id",
            fingerprint = "FP$id",
            direction = direction,
            classification = classification
        )
    }

    @Test
    fun calculateMetrics_expensesAndIncome_computesNetCashFlow() {
        val txns = listOf(
            createTxn(id = 1, amount = 1000.0, category = "Food", direction = TransactionDirection.DEBIT, classification = TransactionClassification.EXPENSE),
            createTxn(id = 2, amount = 500.0, category = "Shopping", direction = TransactionDirection.DEBIT, classification = TransactionClassification.EXPENSE),
            createTxn(id = 3, amount = 5000.0, category = "Salary", direction = TransactionDirection.CREDIT, classification = TransactionClassification.INCOME)
        )

        val metrics = CashFlowCalculator.calculateMetrics(txns, budgetTarget = 10000.0)

        assertEquals(1500.0, metrics.grossExpenses, 0.001)
        assertEquals(5000.0, metrics.incomeReceived, 0.001)
        assertEquals(3500.0, metrics.netCashFlow, 0.001) // 5000 - 1500
        assertEquals(1500.0, metrics.netExpenses, 0.001)
        assertEquals(8500.0, metrics.grossBudgetRemaining, 0.001) // 10000 - 1500
        assertEquals(8500.0, metrics.netBudgetRemaining, 0.001)
    }

    @Test
    fun calculateMetrics_withRefundAndCashback_offsetsCategoryAndBudgetRemaining() {
        val txns = listOf(
            createTxn(id = 1, amount = 1200.0, category = "Food", direction = TransactionDirection.DEBIT, classification = TransactionClassification.EXPENSE),
            createTxn(id = 2, amount = 200.0, category = "Food", direction = TransactionDirection.CREDIT, classification = TransactionClassification.REFUND),
            createTxn(id = 3, amount = 50.0, category = "Cashback", direction = TransactionDirection.CREDIT, classification = TransactionClassification.CASHBACK),
            createTxn(id = 4, amount = 10000.0, category = "Salary", direction = TransactionDirection.CREDIT, classification = TransactionClassification.INCOME)
        )

        val metrics = CashFlowCalculator.calculateMetrics(txns, budgetTarget = 5000.0)

        // Gross expenses must not be reduced by refunds
        assertEquals(1200.0, metrics.grossExpenses, 0.001)
        assertEquals(200.0, metrics.refunds, 0.001)
        assertEquals(50.0, metrics.cashbacks, 0.001)
        assertEquals(250.0, metrics.eligibleOffsets, 0.001)

        // Net expenses = 1200 - 250 = 950
        assertEquals(950.0, metrics.netExpenses, 0.001)

        // Food category net spend = 1200 - 200 = 1000
        assertEquals(1000.0, metrics.categoryNetSpend["Food"] ?: 0.0, 0.001)

        // Gross budget remaining = 5000 - 1200 = 3800
        assertEquals(3800.0, metrics.grossBudgetRemaining, 0.001)

        // Net budget remaining = 5000 - 950 = 4050
        assertEquals(4050.0, metrics.netBudgetRemaining, 0.001)

        // Net cash flow = income (10000) - gross expenses (1200) = 8800
        assertEquals(8800.0, metrics.netCashFlow, 0.001)
    }

    @Test
    fun calculateMetrics_transfer_isExcludedFromIncomeAndExpenses() {
        val txns = listOf(
            createTxn(id = 1, amount = 300.0, category = "Food", direction = TransactionDirection.DEBIT, classification = TransactionClassification.EXPENSE),
            createTxn(id = 2, amount = 10000.0, category = "Transfer", direction = TransactionDirection.DEBIT, classification = TransactionClassification.TRANSFER),
            createTxn(id = 3, amount = 10000.0, category = "Transfer", direction = TransactionDirection.CREDIT, classification = TransactionClassification.TRANSFER),
            createTxn(id = 4, amount = 25000.0, category = "Salary", direction = TransactionDirection.CREDIT, classification = TransactionClassification.INCOME)
        )

        val metrics = CashFlowCalculator.calculateMetrics(txns, budgetTarget = 2000.0)

        assertEquals(300.0, metrics.grossExpenses, 0.001)
        assertEquals(25000.0, metrics.incomeReceived, 0.001)
        assertEquals(24700.0, metrics.netCashFlow, 0.001)
    }

    @Test
    fun calculateMetrics_pendingTransactions_areExcludedFromConfirmedMetrics() {
        val txns = listOf(
            createTxn(id = 1, amount = 500.0, category = "Food", status = TransactionStatus.CONFIRMED),
            createTxn(id = 2, amount = 400.0, category = "Food", status = TransactionStatus.PENDING),
            createTxn(id = 3, amount = 2000.0, category = "Salary", direction = TransactionDirection.CREDIT, classification = TransactionClassification.INCOME, status = TransactionStatus.PENDING)
        )

        val metrics = CashFlowCalculator.calculateMetrics(txns, budgetTarget = 1000.0)

        assertEquals(500.0, metrics.grossExpenses, 0.001)
        assertEquals(0.0, metrics.incomeReceived, 0.001)
    }
}
