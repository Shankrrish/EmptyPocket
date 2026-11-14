package com.example

import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.EpfEntryType
import com.example.data.model.MutualFundCategory
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.MutualFundTxType
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.PpfTxType
import com.example.data.model.StockEntity
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.FinanceAnalytics
import com.example.ui.PortfolioAnalytics
import com.example.util.MonthYear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PortfolioAnalyticsTest {

    private fun calMillis(year: Int, monthZeroBased: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(year, monthZeroBased, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun ledgerInvariants_investmentAndValuationNeverDoubleCountExpenses() {
        val bank = AccountEntity(id = 1L, name = "HDFC", type = AccountType.CHECKING, initialBalance = 100000.0)
        val wallet = AccountEntity(id = 2L, name = "PhonePe Wallet", type = AccountType.E_WALLET, initialBalance = 2000.0)
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(1L, "Groceries", 1000.0, TransactionType.EXPENSE, now, true, 1L, 2L, null),
            TransactionEntity(2L, "Nifty SIP", 15000.0, TransactionType.INVESTMENT_BUY, now, true, null, 1L, null),
            TransactionEntity(3L, "Stock Sale", 5000.0, TransactionType.INVESTMENT_SELL, now, true, null, 1L, null),
            TransactionEntity(4L, "Dividend", 500.0, TransactionType.INVESTMENT_INCOME, now, true, null, 1L, null),
            TransactionEntity(5L, "NAV Gain", 4000.0, TransactionType.VALUATION_ADJUSTMENT, now, true, null, null, null),
            TransactionEntity(6L, "PPF Interest", 12000.0, TransactionType.INTEREST_CREDIT, now, true, null, null, null)
        )

        // Only EXPENSE / CC_EXPENSE count as expenses; only INCOME counts as expense-dashboard income
        assertEquals(1000.0, txs.filter { FinanceAnalytics.isCountedExpense(it) }.sumOf { it.amount }, 0.01)
        assertEquals(0.0, txs.filter { FinanceAnalytics.isCountedIncome(it) }.sumOf { it.amount }, 0.01)

        // Bank balance: 100,000 - 15,000 (invest buy) + 5,000 (sell) + 500 (dividend) = 90,500
        assertEquals(90500.0, FinanceAnalytics.computeAccountBalance(bank, txs), 0.01)
        // Wallet balance: 2,000 - 1,000 = 1,000
        assertEquals(1000.0, FinanceAnalytics.computeAccountBalance(wallet, txs), 0.01)
    }

    @Test
    fun mutualFundFifoAndXirr_computeAccurateReturns() {
        val fund = MutualFundEntity(
            id = 1L,
            amcName = "PPFAS",
            schemeName = "Parag Parikh Flexi Cap",
            category = MutualFundCategory.EQUITY,
            currentNav = 80.0
        )
        val t1 = calMillis(2024, Calendar.JANUARY, 10)
        val t2 = calMillis(2024, Calendar.JULY, 10)
        val t3 = calMillis(2024, Calendar.OCTOBER, 10)
        val now = calMillis(2025, Calendar.JANUARY, 10)

        val mfTxs = listOf(
            // Lot 1: 100 units @ ₹50 = ₹5,000
            MutualFundTxEntity(1L, 1L, MutualFundTxType.PURCHASE, units = 100.0, nav = 50.0, amount = 5000.0, dateMillis = t1),
            // Lot 2: 100 units @ ₹60 = ₹6,000
            MutualFundTxEntity(2L, 1L, MutualFundTxType.SIP, units = 100.0, nav = 60.0, amount = 6000.0, dateMillis = t2),
            // Redeem 50 units @ ₹70 = ₹3,500 (FIFO consumes 50 from Lot 1 @ ₹50 = ₹2,500 cost -> ₹1,000 realized gain)
            MutualFundTxEntity(3L, 1L, MutualFundTxType.REDEMPTION, units = 50.0, nav = 70.0, amount = 3500.0, dateMillis = t3)
        )

        val summary = PortfolioAnalytics.computeMutualFundSummary(fund, mfTxs, nowMillis = now)
        assertEquals(150.0, summary.totalUnits, 0.001)
        // Remaining cost: 50*50 + 100*60 = 8,500
        assertEquals(8500.0, summary.investedAmount, 0.01)
        // Current value: 150 * 80 = 12,000
        assertEquals(12000.0, summary.currentValue, 0.01)
        assertEquals(3500.0, summary.unrealizedGain, 0.01)
        assertEquals(1000.0, summary.realizedGain, 0.01)
        assertNotNull(summary.xirrPercent)
        assertTrue(summary.xirrPercent!! > 20.0)
    }

    @Test
    fun stockFifo_computesRealizedAndUnrealizedPnlWithCharges() {
        val stock = StockEntity(id = 1L, symbol = "TCS", companyName = "TCS Ltd", currentPrice = 4200.0)
        val trades = listOf(
            StockTradeEntity(1L, 1L, StockTradeType.BUY, quantity = 10.0, price = 3500.0, charges = 0.0, dateMillis = calMillis(2024, Calendar.MARCH, 1)),
            StockTradeEntity(2L, 1L, StockTradeType.SELL, quantity = 4.0, price = 4000.0, charges = 100.0, dateMillis = calMillis(2024, Calendar.SEPTEMBER, 1))
        )
        val summary = PortfolioAnalytics.computeStockSummary(stock, trades)
        assertEquals(6.0, summary.quantity, 0.001)
        assertEquals(21000.0, summary.investedAmount, 0.01)
        assertEquals(25200.0, summary.currentValue, 0.01)
        // Realized: (4*4000 - 100) - (4*3500) = 15900 - 14000 = 1900
        assertEquals(1900.0, summary.realizedPnl, 0.01)
        assertEquals(4200.0, summary.unrealizedPnl, 0.01)
    }

    @Test
    fun ppfCalculation_enforces5thOfMonthMinimumBalanceRule() {
        val acc = PpfAccountEntity(id = 1L, accountName = "SBI PPF", startFinancialYear = 2024)
        val rates = listOf(PpfFyRateEntity(2024, 7.1))

        // Deposit ₹1,20,000 on April 4, 2024 -> qualifies for April (full 12 months in FY 2024-25!)
        val on4th = listOf(
            PpfTxEntity(1L, 1L, PpfTxType.DEPOSIT, 120000.0, calMillis(2024, Calendar.APRIL, 4))
        )
        // Deposit ₹1,20,000 on April 10, 2024 -> does NOT qualify for April; qualifies for 11 months (May-Mar)
        val on10th = listOf(
            PpfTxEntity(2L, 1L, PpfTxType.DEPOSIT, 120000.0, calMillis(2024, Calendar.APRIL, 10))
        )

        val endOfFy = calMillis(2025, Calendar.MARCH, 28)
        val s4 = PortfolioAnalytics.computePpfSummary(acc, on4th, rates, nowMillis = endOfFy)
        val s10 = PortfolioAnalytics.computePpfSummary(acc, on10th, rates, nowMillis = endOfFy)

        // 120,000 * 7.1% = 8,520 for 12 months vs 7,810 for 11 months
        assertEquals(8520.0, s4.estimatedUncreditedInterestForFy, 1.0)
        assertEquals(7810.0, s10.estimatedUncreditedInterestForFy, 1.0)
        assertEquals(30000.0, s4.currentFyRemaining, 0.01)
    }

    @Test
    fun epfGoldAndPropertyLinkedLoan_computeNetEquityZeroDoubleCount() {
        val epfAcc = EpfAccountEntity(1L, "Acme", openingEmployeeShare = 100000.0, openingEmployerShare = 80000.0, openingPensionShare = 25000.0)
        val epfEntries = listOf(
            EpfEntryEntity(1L, 1L, EpfEntryType.CONTRIBUTION, employeeAmount = 10000.0, employerEpfAmount = 3670.0, epsPensionAmount = 1250.0, vpfAmount = 5000.0, dateMillis = 1000L),
            EpfEntryEntity(2L, 1L, EpfEntryType.INTEREST_CREDIT, employeeAmount = 8000.0, employerEpfAmount = 6000.0, epsPensionAmount = 0.0, vpfAmount = 500.0, dateMillis = 2000L)
        )
        val epfSum = PortfolioAnalytics.computeEpfSummary(epfAcc, epfEntries)
        // Employee 110k + Employer 83.67k + VPF 5k + Interest 14.5k = 213,170 (EPS pension excluded from withdrawable EPF balance)
        assertEquals(213170.0, epfSum.totalBalance, 0.01)
        assertEquals(26250.0, epfSum.epsPensionTotal, 0.01)

        val homeLoan = NetWorthItemEntity(id = 10L, name = "Home Loan", category = AssetLiabilityCategory.HOME_LOAN, amount = 2000000.0)
        val property = OtherAssetEntity(id = 1L, kind = OtherAssetKind.PROPERTY, name = "Flat", investedAmount = 5000000.0, currentValue = 6500000.0, linkedLoanId = 10L)
        val gold = OtherAssetEntity(id = 2L, kind = OtherAssetKind.GOLD, name = "22K Gold", investedAmount = 120000.0, goldPurityKarat = 22, goldGrams = 20.0, goldRatePerGram = 7500.0)

        val propSum = PortfolioAnalytics.computeOtherAssetSummary(property, listOf(homeLoan))
        val goldSum = PortfolioAnalytics.computeOtherAssetSummary(gold, listOf(homeLoan))

        assertEquals(4500000.0, propSum.netEquity, 0.01)
        assertEquals(150000.0, goldSum.computedValue, 0.01)

        val nw = FinanceAnalytics.computeNetWorth(
            liquidBankAndCash = 100000.0,
            cardOutstanding = 10000.0,
            items = listOf(homeLoan),
            snapshots = emptyList(),
            currentMonth = MonthYear.current(),
            portfolioTotalValue = 6650000.0
        )
        assertEquals(6750000.0, nw.totalAssets, 0.01)
        assertEquals(2010000.0, nw.totalLiabilities, 0.01)
        assertEquals(4740000.0, nw.netWorth, 0.01)
    }
}
