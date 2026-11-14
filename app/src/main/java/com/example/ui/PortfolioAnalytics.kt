package com.example.ui

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
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.pow

data class MutualFundHoldingSummary(
    val fund: MutualFundEntity,
    val totalUnits: Double,
    val avgBuyNav: Double,
    val investedAmount: Double,
    val currentValue: Double,
    val unrealizedGain: Double,
    val unrealizedGainPercent: Double,
    val realizedGain: Double,
    val xirrPercent: Double?,
    val transactions: List<MutualFundTxEntity>
)

data class StockHoldingSummary(
    val stock: StockEntity,
    val quantity: Double,
    val avgBuyPrice: Double,
    val investedAmount: Double,
    val currentValue: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPercent: Double,
    val realizedPnl: Double,
    val trades: List<StockTradeEntity>
)

data class PpfSummary(
    val account: PpfAccountEntity,
    val currentBalance: Double,
    val totalDeposits: Double,
    val totalCreditedInterest: Double,
    val estimatedUncreditedInterestForFy: Double,
    val suggestedInterestToCreditForCompletedFy: Double,
    val currentFyDeposited: Double,
    val currentFyLimit: Double,
    val currentFyRemaining: Double,
    val currentFyRatePercent: Double,
    val completedFysCount: Int,
    val maturityFyStart: Int,
    val isLoanEligible: Boolean,
    val isPartialWithdrawalEligible: Boolean,
    val isMatured: Boolean,
    val transactions: List<PpfTxEntity>
)

data class EpfSummary(
    val account: EpfAccountEntity,
    val employeeTotal: Double,
    val employerEpfTotal: Double,
    val epsPensionTotal: Double,
    val vpfTotal: Double,
    val interestTotal: Double,
    val totalBalance: Double,
    val entries: List<EpfEntryEntity>
)

data class OtherAssetSummary(
    val asset: OtherAssetEntity,
    val computedValue: Double,
    val invested: Double,
    val gain: Double,
    val linkedLoanAmount: Double,
    val netEquity: Double
)

data class AllocationSlice(
    val label: String,
    val emoji: String,
    val value: Double,
    val percentage: Float,
    val colorHex: Long
)

data class PortfolioHistoryPoint(
    val label: String,
    val value: Double
)

data class PortfolioSummary(
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val unrealizedGain: Double = 0.0,
    val unrealizedGainPercent: Double = 0.0,
    val realizedGain: Double = 0.0,
    val monthlySipCommitment: Double = 0.0,
    val mfHoldings: List<MutualFundHoldingSummary> = emptyList(),
    val stockHoldings: List<StockHoldingSummary> = emptyList(),
    val ppfSummaries: List<PpfSummary> = emptyList(),
    val epfSummaries: List<EpfSummary> = emptyList(),
    val otherAssetSummaries: List<OtherAssetSummary> = emptyList(),
    val assetAllocation: List<AllocationSlice> = emptyList(),
    val mfCategoryAllocation: List<AllocationSlice> = emptyList(),
    val mfAmcAllocation: List<AllocationSlice> = emptyList(),
    val historyPoints: List<PortfolioHistoryPoint> = emptyList()
)

object PortfolioAnalytics {

    const val PPF_ANNUAL_LIMIT = 150000.0
    private const val MILLIS_PER_DAY = 86_400_000.0

    val defaultHistoricalPpfRates: Map<Int, Double> = mapOf(
        2011 to 8.6,
        2012 to 8.8,
        2013 to 8.7,
        2014 to 8.7,
        2015 to 8.7,
        2016 to 8.0,
        2017 to 7.8,
        2018 to 7.9,
        2019 to 7.9,
        2020 to 7.1,
        2021 to 7.1,
        2022 to 7.1,
        2023 to 7.1,
        2024 to 7.1,
        2025 to 7.1,
        2026 to 7.1
    )

    private data class UnitLot(var units: Double, val unitCost: Double)

    fun financialYearStart(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val year = cal.get(Calendar.YEAR)
        val monthZeroBased = cal.get(Calendar.MONTH)
        return if (monthZeroBased >= Calendar.APRIL) year else year - 1
    }

    fun formatFyLabel(fyStart: Int): String {
        val nextTwoDigits = ((fyStart + 1) % 100).toString().padStart(2, '0')
        return "FY $fyStart-$nextTwoDigits"
    }

    /**
     * Newton-Raphson XIRR with bounded bisection safety fallback.
     * Cashflows are (dateMillis, signedAmount): investments negative, redemptions & current valuation positive.
     */
    fun calculateXirr(cashflows: List<Pair<Long, Double>>): Double? {
        if (cashflows.size < 2) return null
        val hasPositive = cashflows.any { it.second > 0.0 }
        val hasNegative = cashflows.any { it.second < 0.0 }
        if (!hasPositive || !hasNegative) return null

        val sorted = cashflows.sortedBy { it.first }
        val startMillis = sorted.first().first
        val yearFractions = sorted.map { (ts, amt) ->
            ((ts - startMillis) / MILLIS_PER_DAY) / 365.0 to amt
        }
        val spanYears = yearFractions.maxOf { it.first }
        if (spanYears < (7.0 / 365.0)) return null // Too short for annualized XIRR

        fun npv(rate: Double): Double {
            if (rate <= -0.999) return Double.NaN
            var sum = 0.0
            for ((t, cf) in yearFractions) {
                sum += cf / (1.0 + rate).pow(t)
            }
            return sum
        }

        fun dNpv(rate: Double): Double {
            var sum = 0.0
            for ((t, cf) in yearFractions) {
                if (t != 0.0) {
                    sum -= t * cf / (1.0 + rate).pow(t + 1.0)
                }
            }
            return sum
        }

        var guess = 0.12
        for (i in 0 until 60) {
            val y = npv(guess)
            val dy = dNpv(guess)
            if (y.isNaN() || dy.isNaN() || abs(dy) < 1e-10) break
            val next = guess - y / dy
            if (abs(next - guess) < 1e-7 && next > -0.99 && next < 10.0) {
                return next * 100.0
            }
            guess = next.coerceIn(-0.95, 5.0)
        }

        // Bisection fallback on [-0.90, 3.00]
        var low = -0.90
        var high = 3.00
        var fLow = npv(low)
        val fHigh = npv(high)
        if (fLow.isNaN() || fHigh.isNaN() || fLow * fHigh > 0) return null
        repeat(60) {
            val mid = (low + high) / 2.0
            val fMid = npv(mid)
            if (abs(fMid) < 1e-6) return mid * 100.0
            if (fLow * fMid <= 0) {
                high = mid
            } else {
                low = mid
                fLow = fMid
            }
        }
        return ((low + high) / 2.0) * 100.0
    }

    fun computeMutualFundSummary(
        fund: MutualFundEntity,
        allFundTxs: List<MutualFundTxEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): MutualFundHoldingSummary {
        val sortedTxs = allFundTxs.filter { it.fundId == fund.id }.sortedBy { it.dateMillis }
        val lots = ArrayDeque<UnitLot>()
        var realizedGain = 0.0
        val xirrCashflows = mutableListOf<Pair<Long, Double>>()

        sortedTxs.forEach { tx ->
            val effectiveUnits = when {
                tx.units > 0.0 -> tx.units
                tx.nav > 0.0 -> tx.amount / tx.nav
                else -> 0.0
            }
            val unitCost = if (effectiveUnits > 0.0) tx.amount / effectiveUnits else tx.nav

            when (tx.txType) {
                MutualFundTxType.PURCHASE,
                MutualFundTxType.SIP,
                MutualFundTxType.SWITCH_IN -> {
                    if (effectiveUnits > 0.0) {
                        lots.addLast(UnitLot(effectiveUnits, unitCost))
                        xirrCashflows.add(tx.dateMillis to -tx.amount)
                    }
                }
                MutualFundTxType.REDEMPTION,
                MutualFundTxType.SWITCH_OUT -> {
                    var toRedeem = effectiveUnits
                    var costBasis = 0.0
                    while (toRedeem > 1e-6 && lots.isNotEmpty()) {
                        val front = lots.first()
                        val consumed = minOf(front.units, toRedeem)
                        costBasis += consumed * front.unitCost
                        front.units -= consumed
                        toRedeem -= consumed
                        if (front.units <= 1e-6) lots.removeFirst()
                    }
                    realizedGain += (tx.amount - costBasis)
                    xirrCashflows.add(tx.dateMillis to tx.amount)
                }
            }
        }

        val remainingUnits = lots.sumOf { it.units }
        val investedAmount = lots.sumOf { it.units * it.unitCost }
        val avgBuyNav = if (remainingUnits > 1e-6) investedAmount / remainingUnits else 0.0
        val currentValue = remainingUnits * fund.currentNav
        val unrealizedGain = currentValue - investedAmount
        val unrealizedPct = if (investedAmount > 0.0) (unrealizedGain / investedAmount) * 100.0 else 0.0

        val xirr = if (currentValue > 0.0) {
            xirrCashflows.add(nowMillis to currentValue)
            calculateXirr(xirrCashflows)
        } else null

        return MutualFundHoldingSummary(
            fund = fund,
            totalUnits = remainingUnits,
            avgBuyNav = avgBuyNav,
            investedAmount = investedAmount,
            currentValue = currentValue,
            unrealizedGain = unrealizedGain,
            unrealizedGainPercent = unrealizedPct,
            realizedGain = realizedGain,
            xirrPercent = xirr,
            transactions = sortedTxs.reversed()
        )
    }

    fun computeStockSummary(
        stock: StockEntity,
        allTrades: List<StockTradeEntity>
    ): StockHoldingSummary {
        val sortedTrades = allTrades.filter { it.stockId == stock.id }.sortedBy { it.dateMillis }
        val lots = ArrayDeque<UnitLot>()
        var realizedPnl = 0.0

        sortedTrades.forEach { trade ->
            when (trade.tradeType) {
                StockTradeType.BUY -> {
                    if (trade.quantity > 0.0) {
                        val unitCost = (trade.quantity * trade.price + trade.charges) / trade.quantity
                        lots.addLast(UnitLot(trade.quantity, unitCost))
                    }
                }
                StockTradeType.SELL -> {
                    var toSell = trade.quantity
                    var costBasis = 0.0
                    while (toSell > 1e-6 && lots.isNotEmpty()) {
                        val front = lots.first()
                        val consumed = minOf(front.units, toSell)
                        costBasis += consumed * front.unitCost
                        front.units -= consumed
                        toSell -= consumed
                        if (front.units <= 1e-6) lots.removeFirst()
                    }
                    val netProceeds = (trade.quantity * trade.price) - trade.charges
                    realizedPnl += (netProceeds - costBasis)
                }
            }
        }

        val openQty = lots.sumOf { it.units }
        val invested = lots.sumOf { it.units * it.unitCost }
        val avgPrice = if (openQty > 1e-6) invested / openQty else 0.0
        val currentValue = openQty * stock.currentPrice
        val unrealized = currentValue - invested
        val unrealizedPct = if (invested > 0.0) (unrealized / invested) * 100.0 else 0.0

        return StockHoldingSummary(
            stock = stock,
            quantity = openQty,
            avgBuyPrice = avgPrice,
            investedAmount = invested,
            currentValue = currentValue,
            unrealizedPnl = unrealized,
            unrealizedPnlPercent = unrealizedPct,
            realizedPnl = realizedPnl,
            trades = sortedTrades.reversed()
        )
    }

    /**
     * Authentic Indian PPF calculation:
     * - For each month, interest is calculated on the lowest balance between the close of the 5th day and end of that month.
     * - Deposits on/before day 5 count for that month; deposits on day 6+ start earning from the next month.
     * - Interest is compounded and credited annually on March 31.
     */
    fun computePpfSummary(
        account: PpfAccountEntity,
        allPpfTxs: List<PpfTxEntity>,
        configuredRates: List<PpfFyRateEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): PpfSummary {
        val txs = allPpfTxs.filter { it.ppfAccountId == account.id }.sortedBy { it.dateMillis }
        val ratesByFy = defaultHistoricalPpfRates.toMutableMap()
        configuredRates.forEach { ratesByFy[it.financialYearStart] = it.annualRatePercent }

        val currentFyStart = financialYearStart(nowMillis)
        val firstTxFy = txs.minOfOrNull { financialYearStart(it.dateMillis) } ?: account.startFinancialYear
        val earliestFy = minOf(account.startFinancialYear, firstTxFy)

        var runningBalance = 0.0
        var estimatedCurrentFyInterest = 0.0
        var lastCompletedFyCalculatedInterest = 0.0

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH)

        for (fy in earliestFy..currentFyStart) {
            val ratePct = ratesByFy[fy] ?: 7.1
            val fyMonths = (0 until 12).map { offset ->
                val month = (Calendar.APRIL + offset) % 12
                val year = if (month >= Calendar.APRIL) fy else fy + 1
                year to month
            }

            var fyAccruedInterest = 0.0
            for ((y, m) in fyMonths) {
                if (y > nowYear || (y == nowYear && m > nowMonth)) break

                val monthTxs = txs.filter { tx ->
                    val c = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                }

                // Balance at close of 5th day of month
                var balOn5th = runningBalance
                monthTxs.forEach { tx ->
                    val day = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }.get(Calendar.DAY_OF_MONTH)
                    if (day <= 5) {
                        balOn5th += when (tx.txType) {
                            PpfTxType.DEPOSIT, PpfTxType.INTEREST_CREDIT -> tx.amount
                            PpfTxType.WITHDRAWAL -> -tx.amount
                        }
                    }
                }

                // Track lowest closing balance between 5th and end of month
                var minQualifying = balOn5th
                var rollingMonthEnd = balOn5th
                monthTxs.forEach { tx ->
                    val day = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }.get(Calendar.DAY_OF_MONTH)
                    if (day > 5) {
                        rollingMonthEnd += when (tx.txType) {
                            PpfTxType.DEPOSIT, PpfTxType.INTEREST_CREDIT -> tx.amount
                            PpfTxType.WITHDRAWAL -> -tx.amount
                        }
                        if (rollingMonthEnd < minQualifying) {
                            minQualifying = rollingMonthEnd
                        }
                    }
                }

                fyAccruedInterest += minQualifying.coerceAtLeast(0.0) * (ratePct / 100.0) / 12.0
                runningBalance = rollingMonthEnd.coerceAtLeast(0.0)
            }

            if (fy == currentFyStart) {
                estimatedCurrentFyInterest = fyAccruedInterest
            } else {
                lastCompletedFyCalculatedInterest = fyAccruedInterest
            }
        }

        val totalDeposits = txs.filter { it.txType == PpfTxType.DEPOSIT }.sumOf { it.amount }
        val totalWithdrawals = txs.filter { it.txType == PpfTxType.WITHDRAWAL }.sumOf { it.amount }
        val totalCreditedInterest = txs.filter { it.txType == PpfTxType.INTEREST_CREDIT }.sumOf { it.amount }
        val actualLedgerBalance = (totalDeposits + totalCreditedInterest - totalWithdrawals).coerceAtLeast(0.0)

        val currentFyDeposited = txs.filter {
            it.txType == PpfTxType.DEPOSIT && financialYearStart(it.dateMillis) == currentFyStart
        }.sumOf { it.amount }
        val currentFyRemaining = (PPF_ANNUAL_LIMIT - currentFyDeposited).coerceAtLeast(0.0)

        val completedFysCount = (currentFyStart - account.startFinancialYear).coerceAtLeast(0)
        val totalBlockEndFy = account.startFinancialYear + 15 + (account.extensionBlocks.coerceAtLeast(0) * 5)

        return PpfSummary(
            account = account,
            currentBalance = actualLedgerBalance,
            totalDeposits = totalDeposits,
            totalCreditedInterest = totalCreditedInterest,
            estimatedUncreditedInterestForFy = estimatedCurrentFyInterest,
            suggestedInterestToCreditForCompletedFy = lastCompletedFyCalculatedInterest,
            currentFyDeposited = currentFyDeposited,
            currentFyLimit = PPF_ANNUAL_LIMIT,
            currentFyRemaining = currentFyRemaining,
            currentFyRatePercent = ratesByFy[currentFyStart] ?: 7.1,
            completedFysCount = completedFysCount,
            maturityFyStart = totalBlockEndFy,
            isLoanEligible = completedFysCount in 2..5,
            isPartialWithdrawalEligible = completedFysCount >= 6,
            isMatured = currentFyStart >= (account.startFinancialYear + 15),
            transactions = txs.reversed()
        )
    }

    fun computeEpfSummary(
        account: EpfAccountEntity,
        allEntries: List<EpfEntryEntity>
    ): EpfSummary {
        val entries = allEntries.filter { it.epfAccountId == account.id }.sortedByDescending { it.dateMillis }
        val emp = account.openingEmployeeShare + entries.sumOf { entry ->
            when (entry.entryType) {
                EpfEntryType.CONTRIBUTION -> entry.employeeAmount
                EpfEntryType.WITHDRAWAL -> -entry.employeeAmount
                EpfEntryType.INTEREST_CREDIT -> 0.0
            }
        }
        val erEpf = account.openingEmployerShare + entries.sumOf { entry ->
            when (entry.entryType) {
                EpfEntryType.CONTRIBUTION -> entry.employerEpfAmount
                EpfEntryType.WITHDRAWAL -> -entry.employerEpfAmount
                EpfEntryType.INTEREST_CREDIT -> 0.0
            }
        }
        val eps = account.openingPensionShare + entries.sumOf { entry ->
            if (entry.entryType == EpfEntryType.CONTRIBUTION) entry.epsPensionAmount else 0.0
        }
        val vpf = entries.sumOf { entry ->
            when (entry.entryType) {
                EpfEntryType.CONTRIBUTION -> entry.vpfAmount
                EpfEntryType.WITHDRAWAL -> -entry.vpfAmount
                EpfEntryType.INTEREST_CREDIT -> 0.0
            }
        }
        val interest = entries.filter { it.entryType == EpfEntryType.INTEREST_CREDIT }.sumOf {
            it.employeeAmount + it.employerEpfAmount + it.vpfAmount
        }
        val balance = (emp + erEpf + vpf + interest).coerceAtLeast(0.0)
        return EpfSummary(
            account = account,
            employeeTotal = emp.coerceAtLeast(0.0),
            employerEpfTotal = erEpf.coerceAtLeast(0.0),
            epsPensionTotal = eps.coerceAtLeast(0.0),
            vpfTotal = vpf.coerceAtLeast(0.0),
            interestTotal = interest.coerceAtLeast(0.0),
            totalBalance = balance,
            entries = entries
        )
    }

    fun computeOtherAssetSummary(
        asset: OtherAssetEntity,
        liabilities: List<NetWorthItemEntity>
    ): OtherAssetSummary {
        val value = if (asset.kind == OtherAssetKind.GOLD && asset.goldGrams > 0.0 && asset.goldRatePerGram > 0.0) {
            asset.goldGrams * asset.goldRatePerGram
        } else {
            asset.currentValue
        }
        val invested = if (asset.investedAmount > 0.0) asset.investedAmount else value
        val linkedLoan = asset.linkedLoanId?.let { id ->
            liabilities.find { it.id == id && it.category.isLiability }?.amount
        } ?: 0.0
        return OtherAssetSummary(
            asset = asset,
            computedValue = value,
            invested = invested,
            gain = value - invested,
            linkedLoanAmount = linkedLoan,
            netEquity = (value - linkedLoan).coerceAtLeast(0.0)
        )
    }

    fun computePortfolioSummary(
        mutualFunds: List<MutualFundEntity>,
        mfTxs: List<MutualFundTxEntity>,
        stocks: List<StockEntity>,
        stockTrades: List<StockTradeEntity>,
        ppfAccounts: List<PpfAccountEntity>,
        ppfTxs: List<PpfTxEntity>,
        ppfRates: List<PpfFyRateEntity>,
        epfAccounts: List<EpfAccountEntity>,
        epfEntries: List<EpfEntryEntity>,
        otherAssets: List<OtherAssetEntity>,
        netWorthItems: List<NetWorthItemEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): PortfolioSummary {
        val mfSummaries = mutualFunds.map { computeMutualFundSummary(it, mfTxs, nowMillis) }
        val stockSummaries = stocks.map { computeStockSummary(it, stockTrades) }
        val ppfSummaries = ppfAccounts.map { computePpfSummary(it, ppfTxs, ppfRates, nowMillis) }
        val epfSummaries = epfAccounts.map { computeEpfSummary(it, epfEntries) }
        val otherSummaries = otherAssets.map { computeOtherAssetSummary(it, netWorthItems) }

        val mfVal = mfSummaries.sumOf { it.currentValue }
        val mfInv = mfSummaries.sumOf { it.investedAmount }
        val mfRealized = mfSummaries.sumOf { it.realizedGain }

        val stockVal = stockSummaries.sumOf { it.currentValue }
        val stockInv = stockSummaries.sumOf { it.investedAmount }
        val stockRealized = stockSummaries.sumOf { it.realizedPnl }

        val ppfVal = ppfSummaries.sumOf { it.currentBalance }
        val ppfInv = ppfSummaries.sumOf { it.totalDeposits }

        val epfVal = epfSummaries.sumOf { it.totalBalance }
        val epfInv = epfSummaries.sumOf { it.employeeTotal + it.employerEpfTotal + it.vpfTotal }

        val otherVal = otherSummaries.sumOf { it.computedValue }
        val otherInv = otherSummaries.sumOf { it.invested }

        val totalCurrent = mfVal + stockVal + ppfVal + epfVal + otherVal
        val totalInvested = mfInv + stockInv + ppfInv + epfInv + otherInv
        val unrealized = totalCurrent - totalInvested
        val unrealizedPct = if (totalInvested > 0.0) (unrealized / totalInvested) * 100.0 else 0.0
        val realized = mfRealized + stockRealized
        val monthlySip = mutualFunds.filter { it.sipEnabled }.sumOf { it.sipAmount }

        val rawBuckets = listOf(
            Triple("Mutual Funds", "📊", mfVal to 0xFF556B2F),
            Triple("Stocks", "📈", stockVal to 0xFF0284C7),
            Triple("PPF", "🏛️", ppfVal to 0xFF3B6E22),
            Triple("EPF", "🛡️", epfVal to 0xFF2F6F6D),
            Triple("Other Assets", "🪙", otherVal to 0xFFC97A16)
        ).filter { it.third.first > 0.0 }

        val allocation = rawBuckets.map { (name, emoji, pair) ->
            val (valAmt, colorHex) = pair
            AllocationSlice(
                label = name,
                emoji = emoji,
                value = valAmt,
                percentage = if (totalCurrent > 0.0) (valAmt / totalCurrent).toFloat() else 0f,
                colorHex = colorHex
            )
        }

        val mfCatColors = mapOf(
            MutualFundCategory.EQUITY to 0xFF556B2F,
            MutualFundCategory.INDEX to 0xFF6B8E23,
            MutualFundCategory.ELSS to 0xFF3B6E22,
            MutualFundCategory.HYBRID to 0xFF2F6F6D,
            MutualFundCategory.DEBT to 0xFF0284C7,
            MutualFundCategory.LIQUID to 0xFF0D9488,
            MutualFundCategory.OTHER to 0xFFC97A16
        )
        val mfCatSlices = mfSummaries
            .groupBy { it.fund.category }
            .mapNotNull { (cat, list) ->
                val sum = list.sumOf { it.currentValue }
                if (sum <= 0.0) null else AllocationSlice(
                    label = cat.label,
                    emoji = "📊",
                    value = sum,
                    percentage = if (mfVal > 0.0) (sum / mfVal).toFloat() else 0f,
                    colorHex = mfCatColors[cat] ?: 0xFF556B2F
                )
            }.sortedByDescending { it.value }

        val amcColors = listOf(0xFF556B2F, 0xFF0284C7, 0xFF2F6F6D, 0xFFC97A16, 0xFF6B8E23, 0xFF0D9488)
        val mfAmcSlices = mfSummaries
            .groupBy { it.fund.amcName.ifBlank { "Other AMC" } }
            .entries
            .mapIndexedNotNull { idx, (amc, list) ->
                val sum = list.sumOf { it.currentValue }
                if (sum <= 0.0) null else AllocationSlice(
                    label = amc,
                    emoji = "🏢",
                    value = sum,
                    percentage = if (mfVal > 0.0) (sum / mfVal).toFloat() else 0f,
                    colorHex = amcColors[idx % amcColors.size]
                )
            }.sortedByDescending { it.value }

        // Build 6-point monthly historical portfolio curve based on cumulative invested + current valuation
        val cal = Calendar.getInstance()
        val history = (-5..0).map { offset ->
            val pointCal = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, offset)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }
            val cutoff = pointCal.timeInMillis
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val label = monthNames[pointCal.get(Calendar.MONTH)]
            val valueAtMonth = if (offset == 0) {
                totalCurrent
            } else {
                val mfAt = mutualFunds.sumOf { f ->
                    val units = mfTxs.filter { it.fundId == f.id && it.dateMillis <= cutoff }.sumOf { tx ->
                        val u = if (tx.units > 0) tx.units else if (tx.nav > 0) tx.amount / tx.nav else 0.0
                        when (tx.txType) {
                            MutualFundTxType.PURCHASE, MutualFundTxType.SIP, MutualFundTxType.SWITCH_IN -> u
                            MutualFundTxType.REDEMPTION, MutualFundTxType.SWITCH_OUT -> -u
                        }
                    }.coerceAtLeast(0.0)
                    units * f.currentNav
                }
                val stAt = stocks.sumOf { st ->
                    val q = stockTrades.filter { it.stockId == st.id && it.dateMillis <= cutoff }.sumOf { t ->
                        if (t.tradeType == StockTradeType.BUY) t.quantity else -t.quantity
                    }.coerceAtLeast(0.0)
                    q * st.currentPrice
                }
                val ppfAt = ppfTxs.filter { it.dateMillis <= cutoff }.sumOf { tx ->
                    when (tx.txType) {
                        PpfTxType.DEPOSIT, PpfTxType.INTEREST_CREDIT -> tx.amount
                        PpfTxType.WITHDRAWAL -> -tx.amount
                    }
                }.coerceAtLeast(0.0)
                val epfAt = epfSummaries.sumOf { epf ->
                    val open = epf.account.openingEmployeeShare + epf.account.openingEmployerShare
                    val delta = epf.entries.filter { it.dateMillis <= cutoff }.sumOf { e ->
                        val amt = e.employeeAmount + e.employerEpfAmount + e.vpfAmount
                        if (e.entryType == EpfEntryType.WITHDRAWAL) -amt else amt
                    }
                    (open + delta).coerceAtLeast(0.0)
                }
                val otherAt = otherSummaries.sumOf { it.computedValue }
                mfAt + stAt + ppfAt + epfAt + otherAt
            }
            PortfolioHistoryPoint(label, valueAtMonth)
        }

        return PortfolioSummary(
            totalInvested = totalInvested,
            totalCurrentValue = totalCurrent,
            unrealizedGain = unrealized,
            unrealizedGainPercent = unrealizedPct,
            realizedGain = realized,
            monthlySipCommitment = monthlySip,
            mfHoldings = mfSummaries,
            stockHoldings = stockSummaries,
            ppfSummaries = ppfSummaries,
            epfSummaries = epfSummaries,
            otherAssetSummaries = otherSummaries,
            assetAllocation = allocation,
            mfCategoryAllocation = mfCatSlices,
            mfAmcAllocation = mfAmcSlices,
            historyPoints = history
        )
    }
}
