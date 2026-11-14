package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HoldingSource(val label: String) {
    MANUAL("Manual"),
    BROKER_IMPORT("Broker Import")
}

enum class MutualFundCategory(val label: String) {
    EQUITY("Equity"),
    DEBT("Debt"),
    HYBRID("Hybrid"),
    ELSS("ELSS Tax Saver"),
    INDEX("Index"),
    LIQUID("Liquid"),
    OTHER("Other")
}

enum class MutualFundPlan(val label: String) {
    DIRECT("Direct"),
    REGULAR("Regular")
}

enum class MutualFundOption(val label: String) {
    GROWTH("Growth"),
    IDCW("IDCW")
}

enum class MutualFundTxType(val label: String) {
    PURCHASE("Purchase"),
    SIP("SIP"),
    REDEMPTION("Redemption"),
    SWITCH_IN("Switch In"),
    SWITCH_OUT("Switch Out")
}

enum class StockExchange(val label: String) {
    NSE("NSE"),
    BSE("BSE")
}

enum class StockTradeType(val label: String) {
    BUY("Buy"),
    SELL("Sell")
}

enum class PpfTxType(val label: String) {
    DEPOSIT("Deposit"),
    INTEREST_CREDIT("Annual Interest"),
    WITHDRAWAL("Partial Withdrawal")
}

enum class EpfEntryType(val label: String) {
    CONTRIBUTION("Monthly Contribution"),
    INTEREST_CREDIT("Interest Credit"),
    WITHDRAWAL("Withdrawal")
}

enum class OtherAssetKind(val label: String, val emoji: String) {
    NPS("NPS", "🏛️"),
    FD("Fixed Deposit", "📜"),
    RD("Recurring Deposit", "🗓️"),
    GOLD("Gold", "🪙"),
    PROPERTY("Property", "🏠"),
    CUSTOM("Custom Asset", "💎")
}

@Entity(tableName = "mutual_funds")
data class MutualFundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amcName: String,
    val schemeName: String,
    val category: MutualFundCategory = MutualFundCategory.EQUITY,
    val plan: MutualFundPlan = MutualFundPlan.DIRECT,
    val option: MutualFundOption = MutualFundOption.GROWTH,
    val folioNumber: String = "",
    val currentNav: Double = 0.0,
    val navUpdatedAtMillis: Long = System.currentTimeMillis(),
    val sipEnabled: Boolean = false,
    val sipAmount: Double = 0.0,
    val sipDay: Int = 5,
    val holdingSource: HoldingSource = HoldingSource.MANUAL,
    val externalRef: String = ""
)

@Entity(tableName = "mutual_fund_transactions")
data class MutualFundTxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fundId: Long,
    val txType: MutualFundTxType,
    val units: Double,
    val nav: Double,
    val amount: Double,
    val dateMillis: Long,
    val linkedBankAccountId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exchange: StockExchange = StockExchange.NSE,
    val symbol: String,
    val companyName: String,
    val currentPrice: Double = 0.0,
    val priceUpdatedAtMillis: Long = System.currentTimeMillis(),
    val holdingSource: HoldingSource = HoldingSource.MANUAL,
    val externalRef: String = ""
)

@Entity(tableName = "stock_trades")
data class StockTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stockId: Long,
    val tradeType: StockTradeType,
    val quantity: Double,
    val price: Double,
    val charges: Double = 0.0,
    val dateMillis: Long,
    val linkedBankAccountId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "ppf_accounts")
data class PpfAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountName: String,
    val bankOrPostOffice: String = "SBI",
    val accountNumberMasked: String = "",
    val startFinancialYear: Int = 2020,
    val isExtended: Boolean = false,
    val extensionBlocks: Int = 0
)

@Entity(tableName = "ppf_transactions")
data class PpfTxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ppfAccountId: Long,
    val txType: PpfTxType,
    val amount: Double,
    val dateMillis: Long,
    val linkedBankAccountId: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "ppf_fy_rates")
data class PpfFyRateEntity(
    @PrimaryKey val financialYearStart: Int,
    val annualRatePercent: Double
)

@Entity(tableName = "epf_accounts")
data class EpfAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employerName: String,
    val uanMasked: String = "",
    val openingEmployeeShare: Double = 0.0,
    val openingEmployerShare: Double = 0.0,
    val openingPensionShare: Double = 0.0,
    val interestRatePercent: Double = 8.25
)

@Entity(tableName = "epf_entries")
data class EpfEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epfAccountId: Long,
    val entryType: EpfEntryType,
    val employeeAmount: Double = 0.0,
    val employerEpfAmount: Double = 0.0,
    val epsPensionAmount: Double = 0.0,
    val vpfAmount: Double = 0.0,
    val dateMillis: Long,
    val financialYearLabel: String = "",
    val notes: String = ""
)

@Entity(tableName = "other_assets")
data class OtherAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: OtherAssetKind,
    val customCategoryName: String = "",
    val name: String,
    val investedAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val interestRatePercent: Double = 0.0,
    val maturityDateMillis: Long? = null,
    val goldPurityKarat: Int = 22,
    val goldGrams: Double = 0.0,
    val goldRatePerGram: Double = 0.0,
    val linkedLoanId: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)
