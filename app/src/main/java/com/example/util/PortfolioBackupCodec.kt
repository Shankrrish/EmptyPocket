package com.example.util

import com.example.data.model.EpfAccountEntity
import com.example.data.model.EpfEntryEntity
import com.example.data.model.EpfEntryType
import com.example.data.model.HoldingSource
import com.example.data.model.MutualFundCategory
import com.example.data.model.MutualFundEntity
import com.example.data.model.MutualFundOption
import com.example.data.model.MutualFundPlan
import com.example.data.model.MutualFundTxEntity
import com.example.data.model.MutualFundTxType
import com.example.data.model.OtherAssetEntity
import com.example.data.model.OtherAssetKind
import com.example.data.model.PpfAccountEntity
import com.example.data.model.PpfFyRateEntity
import com.example.data.model.PpfTxEntity
import com.example.data.model.PpfTxType
import com.example.data.model.StockEntity
import com.example.data.model.StockExchange
import com.example.data.model.StockTradeEntity
import com.example.data.model.StockTradeType
import org.json.JSONArray
import org.json.JSONObject

data class PortfolioBackupSlice(
    val mutualFunds: List<MutualFundEntity> = emptyList(),
    val mutualFundTxs: List<MutualFundTxEntity> = emptyList(),
    val stocks: List<StockEntity> = emptyList(),
    val stockTrades: List<StockTradeEntity> = emptyList(),
    val ppfAccounts: List<PpfAccountEntity> = emptyList(),
    val ppfTxs: List<PpfTxEntity> = emptyList(),
    val ppfRates: List<PpfFyRateEntity> = emptyList(),
    val epfAccounts: List<EpfAccountEntity> = emptyList(),
    val epfEntries: List<EpfEntryEntity> = emptyList(),
    val otherAssets: List<OtherAssetEntity> = emptyList()
)

object PortfolioBackupCodec {
    fun writeInto(root: JSONObject, slice: PortfolioBackupSlice) {
        root.put("mutualFunds", JSONArray().apply {
            slice.mutualFunds.forEach { f ->
                put(JSONObject().apply {
                    put("id", f.id)
                    put("amcName", f.amcName)
                    put("schemeName", f.schemeName)
                    put("category", f.category.name)
                    put("plan", f.plan.name)
                    put("option", f.option.name)
                    put("folioNumber", f.folioNumber)
                    put("currentNav", f.currentNav)
                    put("navUpdatedAtMillis", f.navUpdatedAtMillis)
                    put("sipEnabled", f.sipEnabled)
                    put("sipAmount", f.sipAmount)
                    put("sipDay", f.sipDay)
                    put("holdingSource", f.holdingSource.name)
                    put("externalRef", f.externalRef)
                })
            }
        })
        root.put("mutualFundTxs", JSONArray().apply {
            slice.mutualFundTxs.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("fundId", t.fundId)
                    put("txType", t.txType.name)
                    put("units", t.units)
                    put("nav", t.nav)
                    put("amount", t.amount)
                    put("dateMillis", t.dateMillis)
                    put("linkedBankAccountId", t.linkedBankAccountId ?: JSONObject.NULL)
                    put("notes", t.notes)
                })
            }
        })
        root.put("stocks", JSONArray().apply {
            slice.stocks.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("exchange", s.exchange.name)
                    put("symbol", s.symbol)
                    put("companyName", s.companyName)
                    put("currentPrice", s.currentPrice)
                    put("priceUpdatedAtMillis", s.priceUpdatedAtMillis)
                    put("holdingSource", s.holdingSource.name)
                    put("externalRef", s.externalRef)
                })
            }
        })
        root.put("stockTrades", JSONArray().apply {
            slice.stockTrades.forEach { st ->
                put(JSONObject().apply {
                    put("id", st.id)
                    put("stockId", st.stockId)
                    put("tradeType", st.tradeType.name)
                    put("quantity", st.quantity)
                    put("price", st.price)
                    put("charges", st.charges)
                    put("dateMillis", st.dateMillis)
                    put("linkedBankAccountId", st.linkedBankAccountId ?: JSONObject.NULL)
                    put("notes", st.notes)
                })
            }
        })
        root.put("ppfAccounts", JSONArray().apply {
            slice.ppfAccounts.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("accountName", p.accountName)
                    put("bankOrPostOffice", p.bankOrPostOffice)
                    put("accountNumberMasked", p.accountNumberMasked)
                    put("startFinancialYear", p.startFinancialYear)
                    put("isExtended", p.isExtended)
                    put("extensionBlocks", p.extensionBlocks)
                })
            }
        })
        root.put("ppfTxs", JSONArray().apply {
            slice.ppfTxs.forEach { pt ->
                put(JSONObject().apply {
                    put("id", pt.id)
                    put("ppfAccountId", pt.ppfAccountId)
                    put("txType", pt.txType.name)
                    put("amount", pt.amount)
                    put("dateMillis", pt.dateMillis)
                    put("linkedBankAccountId", pt.linkedBankAccountId ?: JSONObject.NULL)
                    put("notes", pt.notes)
                })
            }
        })
        root.put("ppfRates", JSONArray().apply {
            slice.ppfRates.forEach { r ->
                put(JSONObject().apply {
                    put("financialYearStart", r.financialYearStart)
                    put("annualRatePercent", r.annualRatePercent)
                })
            }
        })
        root.put("epfAccounts", JSONArray().apply {
            slice.epfAccounts.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("employerName", e.employerName)
                    put("uanMasked", e.uanMasked)
                    put("openingEmployeeShare", e.openingEmployeeShare)
                    put("openingEmployerShare", e.openingEmployerShare)
                    put("openingPensionShare", e.openingPensionShare)
                    put("interestRatePercent", e.interestRatePercent)
                })
            }
        })
        root.put("epfEntries", JSONArray().apply {
            slice.epfEntries.forEach { ee ->
                put(JSONObject().apply {
                    put("id", ee.id)
                    put("epfAccountId", ee.epfAccountId)
                    put("entryType", ee.entryType.name)
                    put("employeeAmount", ee.employeeAmount)
                    put("employerEpfAmount", ee.employerEpfAmount)
                    put("epsPensionAmount", ee.epsPensionAmount)
                    put("vpfAmount", ee.vpfAmount)
                    put("dateMillis", ee.dateMillis)
                    put("financialYearLabel", ee.financialYearLabel)
                    put("notes", ee.notes)
                })
            }
        })
        root.put("otherAssets", JSONArray().apply {
            slice.otherAssets.forEach { oa ->
                put(JSONObject().apply {
                    put("id", oa.id)
                    put("kind", oa.kind.name)
                    put("customCategoryName", oa.customCategoryName)
                    put("name", oa.name)
                    put("investedAmount", oa.investedAmount)
                    put("currentValue", oa.currentValue)
                    put("interestRatePercent", oa.interestRatePercent)
                    put("maturityDateMillis", oa.maturityDateMillis ?: JSONObject.NULL)
                    put("goldPurityKarat", oa.goldPurityKarat)
                    put("goldGrams", oa.goldGrams)
                    put("goldRatePerGram", oa.goldRatePerGram)
                    put("linkedLoanId", oa.linkedLoanId ?: JSONObject.NULL)
                    put("updatedAtMillis", oa.updatedAtMillis)
                    put("notes", oa.notes)
                })
            }
        })
    }

    fun readFrom(root: JSONObject): PortfolioBackupSlice {
        val mfs = mutableListOf<MutualFundEntity>()
        root.optJSONArray("mutualFunds")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                mfs.add(
                    MutualFundEntity(
                        id = o.optLong("id", 0L),
                        amcName = o.optString("amcName", ""),
                        schemeName = o.getString("schemeName"),
                        category = MutualFundCategory.valueOf(o.optString("category", "EQUITY")),
                        plan = MutualFundPlan.valueOf(o.optString("plan", "DIRECT")),
                        option = MutualFundOption.valueOf(o.optString("option", "GROWTH")),
                        folioNumber = o.optString("folioNumber", ""),
                        currentNav = o.optDouble("currentNav", 0.0),
                        navUpdatedAtMillis = o.optLong("navUpdatedAtMillis", System.currentTimeMillis()),
                        sipEnabled = o.optBoolean("sipEnabled", false),
                        sipAmount = o.optDouble("sipAmount", 0.0),
                        sipDay = o.optInt("sipDay", 5),
                        holdingSource = HoldingSource.valueOf(o.optString("holdingSource", "MANUAL")),
                        externalRef = o.optString("externalRef", "")
                    )
                )
            }
        }
        val mfTxs = mutableListOf<MutualFundTxEntity>()
        root.optJSONArray("mutualFundTxs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                mfTxs.add(
                    MutualFundTxEntity(
                        id = o.optLong("id", 0L),
                        fundId = o.getLong("fundId"),
                        txType = MutualFundTxType.valueOf(o.optString("txType", "PURCHASE")),
                        units = o.optDouble("units", 0.0),
                        nav = o.optDouble("nav", 0.0),
                        amount = o.optDouble("amount", 0.0),
                        dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                        linkedBankAccountId = if (o.isNull("linkedBankAccountId")) null else o.getLong("linkedBankAccountId"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
        val stocks = mutableListOf<StockEntity>()
        root.optJSONArray("stocks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                stocks.add(
                    StockEntity(
                        id = o.optLong("id", 0L),
                        exchange = StockExchange.valueOf(o.optString("exchange", "NSE")),
                        symbol = o.getString("symbol"),
                        companyName = o.optString("companyName", ""),
                        currentPrice = o.optDouble("currentPrice", 0.0),
                        priceUpdatedAtMillis = o.optLong("priceUpdatedAtMillis", System.currentTimeMillis()),
                        holdingSource = HoldingSource.valueOf(o.optString("holdingSource", "MANUAL")),
                        externalRef = o.optString("externalRef", "")
                    )
                )
            }
        }
        val trades = mutableListOf<StockTradeEntity>()
        root.optJSONArray("stockTrades")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                trades.add(
                    StockTradeEntity(
                        id = o.optLong("id", 0L),
                        stockId = o.getLong("stockId"),
                        tradeType = StockTradeType.valueOf(o.optString("tradeType", "BUY")),
                        quantity = o.optDouble("quantity", 0.0),
                        price = o.optDouble("price", 0.0),
                        charges = o.optDouble("charges", 0.0),
                        dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                        linkedBankAccountId = if (o.isNull("linkedBankAccountId")) null else o.getLong("linkedBankAccountId"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
        val ppfs = mutableListOf<PpfAccountEntity>()
        root.optJSONArray("ppfAccounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                ppfs.add(
                    PpfAccountEntity(
                        id = o.optLong("id", 0L),
                        accountName = o.getString("accountName"),
                        bankOrPostOffice = o.optString("bankOrPostOffice", "SBI"),
                        accountNumberMasked = o.optString("accountNumberMasked", ""),
                        startFinancialYear = o.optInt("startFinancialYear", 2020),
                        isExtended = o.optBoolean("isExtended", false),
                        extensionBlocks = o.optInt("extensionBlocks", 0)
                    )
                )
            }
        }
        val ppfTxs = mutableListOf<PpfTxEntity>()
        root.optJSONArray("ppfTxs")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                ppfTxs.add(
                    PpfTxEntity(
                        id = o.optLong("id", 0L),
                        ppfAccountId = o.getLong("ppfAccountId"),
                        txType = PpfTxType.valueOf(o.optString("txType", "DEPOSIT")),
                        amount = o.optDouble("amount", 0.0),
                        dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                        linkedBankAccountId = if (o.isNull("linkedBankAccountId")) null else o.getLong("linkedBankAccountId"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
        val ppfRates = mutableListOf<PpfFyRateEntity>()
        root.optJSONArray("ppfRates")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                ppfRates.add(
                    PpfFyRateEntity(
                        financialYearStart = o.getInt("financialYearStart"),
                        annualRatePercent = o.getDouble("annualRatePercent")
                    )
                )
            }
        }
        val epfs = mutableListOf<EpfAccountEntity>()
        root.optJSONArray("epfAccounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                epfs.add(
                    EpfAccountEntity(
                        id = o.optLong("id", 0L),
                        employerName = o.getString("employerName"),
                        uanMasked = o.optString("uanMasked", ""),
                        openingEmployeeShare = o.optDouble("openingEmployeeShare", 0.0),
                        openingEmployerShare = o.optDouble("openingEmployerShare", 0.0),
                        openingPensionShare = o.optDouble("openingPensionShare", 0.0),
                        interestRatePercent = o.optDouble("interestRatePercent", 8.25)
                    )
                )
            }
        }
        val epfEntries = mutableListOf<EpfEntryEntity>()
        root.optJSONArray("epfEntries")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                epfEntries.add(
                    EpfEntryEntity(
                        id = o.optLong("id", 0L),
                        epfAccountId = o.getLong("epfAccountId"),
                        entryType = EpfEntryType.valueOf(o.optString("entryType", "CONTRIBUTION")),
                        employeeAmount = o.optDouble("employeeAmount", 0.0),
                        employerEpfAmount = o.optDouble("employerEpfAmount", 0.0),
                        epsPensionAmount = o.optDouble("epsPensionAmount", 0.0),
                        vpfAmount = o.optDouble("vpfAmount", 0.0),
                        dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
                        financialYearLabel = o.optString("financialYearLabel", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
        val others = mutableListOf<OtherAssetEntity>()
        root.optJSONArray("otherAssets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                others.add(
                    OtherAssetEntity(
                        id = o.optLong("id", 0L),
                        kind = OtherAssetKind.valueOf(o.optString("kind", "CUSTOM")),
                        customCategoryName = o.optString("customCategoryName", ""),
                        name = o.getString("name"),
                        investedAmount = o.optDouble("investedAmount", 0.0),
                        currentValue = o.optDouble("currentValue", 0.0),
                        interestRatePercent = o.optDouble("interestRatePercent", 0.0),
                        maturityDateMillis = if (o.isNull("maturityDateMillis")) null else o.getLong("maturityDateMillis"),
                        goldPurityKarat = o.optInt("goldPurityKarat", 22),
                        goldGrams = o.optDouble("goldGrams", 0.0),
                        goldRatePerGram = o.optDouble("goldRatePerGram", 0.0),
                        linkedLoanId = if (o.isNull("linkedLoanId")) null else o.getLong("linkedLoanId"),
                        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis()),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }
        return PortfolioBackupSlice(
            mutualFunds = mfs,
            mutualFundTxs = mfTxs,
            stocks = stocks,
            stockTrades = trades,
            ppfAccounts = ppfs,
            ppfTxs = ppfTxs,
            ppfRates = ppfRates,
            epfAccounts = epfs,
            epfEntries = epfEntries,
            otherAssets = others
        )
    }
}
