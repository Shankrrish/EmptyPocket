package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val displayLabel: String) {
    CHECKING("Bank Savings"),
    SAVINGS("Bank Savings / Current"),
    CASH("Cash Account"),
    E_WALLET("Prepaid E-Wallet");

    fun isBank(): Boolean = this == CHECKING || this == SAVINGS
    fun isCash(): Boolean = this == CASH
    fun isWallet(): Boolean = this == E_WALLET
}

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
    CC_EXPENSE("Card Spend"),
    CC_PAYMENT("Pay Card"),
    INVESTMENT_BUY("Invest Buy"),
    INVESTMENT_SELL("Invest Sell"),
    INVESTMENT_INCOME("Invest Income"),
    VALUATION_ADJUSTMENT("Valuation Adj."),
    INTEREST_CREDIT("Interest Credit")
}

enum class CategoryType { EXPENSE, INCOME }
enum class RecurringFrequency { MONTHLY, ANNUAL }

enum class AssetLiabilityCategory(val label: String, val isLiability: Boolean) {
    MUTUAL_FUNDS_STOCKS("Mutual Funds & Stocks", false),
    EPF_PPF_NPS("EPF, PPF & NPS", false),
    GOLD_PROPERTY("Gold & Property", false),
    OTHER_ASSET("Other Investments", false),
    PERSONAL_LOAN("Personal Loan", true),
    HOME_LOAN("Home Loan", true),
    OTHER_LIABILITY("Other Liability", true)
}

data class AccountPreset(
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val defaultType: AccountType,
    val defaultSubType: String
)

object AccountPresets {
    val indianBanks = listOf(
        AccountPreset("ICICI Bank", "🏦", 0xFFE05A2B, AccountType.CHECKING, "Savings"),
        AccountPreset("HDFC Bank", "🏛️", 0xFF1D4ED8, AccountType.CHECKING, "Savings"),
        AccountPreset("SBI", "🏦", 0xFF0284C7, AccountType.CHECKING, "Savings"),
        AccountPreset("Indian Bank", "🏛️", 0xFF0E7490, AccountType.CHECKING, "Savings"),
        AccountPreset("Equitas SFB", "🏦", 0xFFEA580C, AccountType.SAVINGS, "Savings"),
        AccountPreset("Axis Bank", "🏛️", 0xFF9D174D, AccountType.CHECKING, "Savings"),
        AccountPreset("Canara Bank", "🏦", 0xFFCA8A04, AccountType.CHECKING, "Savings"),
        AccountPreset("Post Office Savings", "📮", 0xFFB91C1C, AccountType.SAVINGS, "Savings"),
        AccountPreset("Custom Bank", "🏦", 0xFF556B2F, AccountType.CHECKING, "Savings")
    )

    val cashPresets = listOf(
        AccountPreset("Home Cash", "🏡", 0xFF3B6E22, AccountType.CASH, "Home cash"),
        AccountPreset("Personal Purse", "👛", 0xFF6B8E23, AccountType.CASH, "Personal purse"),
        AccountPreset("Wallet Cash", "💵", 0xFF556B2F, AccountType.CASH, "Wallet cash"),
        AccountPreset("Other Cash", "🪙", 0xFF2F6F6D, AccountType.CASH, "Physical cash")
    )

    val eWalletPresets = listOf(
        AccountPreset("PhonePe Wallet", "📱", 0xFF6D28D9, AccountType.E_WALLET, "Prepaid wallet"),
        AccountPreset("Paytm Wallet", "⚡", 0xFF0284C7, AccountType.E_WALLET, "Prepaid wallet"),
        AccountPreset("Amazon Pay Wallet", "🛒", 0xFFD97706, AccountType.E_WALLET, "Prepaid wallet"),
        AccountPreset("Mobikwik", "💳", 0xFF2563EB, AccountType.E_WALLET, "Prepaid wallet"),
        AccountPreset("Custom E-Wallet", "📲", 0xFF556B2F, AccountType.E_WALLET, "Prepaid wallet")
    )
}

data class SplitAllocation(
    val categoryId: Long,
    val amount: Double
) {
    companion object {
        fun encode(splits: List<SplitAllocation>): String =
            splits.joinToString("|") { "${it.categoryId}:${it.amount}" }

        fun decode(raw: String): List<SplitAllocation> {
            if (raw.isBlank()) return emptyList()
            return raw.split("|").mapNotNull { part ->
                val pieces = part.split(":")
                if (pieces.size == 2) {
                    val cid = pieces[0].toLongOrNull()
                    val amt = pieces[1].toDoubleOrNull()
                    if (cid != null && amt != null && amt > 0.0) SplitAllocation(cid, amt) else null
                } else null
            }
        }
    }
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double,
    val colorHex: Long = 0xFF556B2F,
    val bankName: String = "",
    val accountSubType: String = "",
    val maskedNumber: String = "",
    val ifsc: String = "",
    val emoji: String = "",
    val isArchived: Boolean = false,
    val isPrepaidWallet: Boolean = (type == AccountType.E_WALLET)
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String,
    val limitAmount: Double,
    val closingDay: Int,
    val dueDay: Int,
    val colorHex: Long = 0xFF6B8E23,
    val issuerBank: String = "",
    val statementDay: Int = closingDay,
    val minimumDueAmount: Double = 0.0,
    val isArchived: Boolean = false
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconName: String,
    val colorHex: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val dateMillis: Long,
    val isPaid: Boolean,
    val categoryId: Long?,
    val accountId: Long?,
    val creditCardId: Long?,
    val toAccountId: Long? = null,
    val notes: String = "",
    val isRecurring: Boolean = false,
    val tags: String = "",
    val splitSummary: String = "",
    val linkedAssetType: String = "",
    val linkedAssetId: Long? = null
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthYear: String,
    val limitAmount: Double
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val monthlyContribution: Double,
    val isRecurringContribution: Boolean = true,
    val iconName: String = "Savings",
    val colorHex: Long = 0xFF556B2F
)

@Entity(tableName = "net_worth_items")
data class NetWorthItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: AssetLiabilityCategory,
    val amount: Double
)

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthYear: String,
    val label: String,
    val totalAssets: Double,
    val totalLiabilities: Double
)

@Entity(tableName = "categorization_rules")
data class CategorizationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val categoryId: Long,
    val isAutoLearned: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val previousAmount: Double? = null,
    val dueDay: Int,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val creditCardId: Long? = null,
    val isSubscription: Boolean = false,
    val reminderDaysBefore: Int = 3,
    val isActive: Boolean = true
)

enum class AppThemeMode(val label: String, val shortLabel: String) {
    AMOLED_DARK("AMOLED Dark", "AMOLED"),
    CRISP_LIGHT("Crisp Light", "Light"),
    SYSTEM("System Default", "Auto");

    companion object {
        fun fromRaw(raw: String?): AppThemeMode =
            entries.firstOrNull { it.name == raw } ?: AMOLED_DARK
    }
}

enum class AppFontStyle(
    val label: String,
    val shortLabel: String,
    val tagline: String
) {
    JAKARTA_SANS("Plus Jakarta Sans", "Jakarta", "Crisp geometric fintech • Default"),
    MANROPE("Manrope", "Manrope", "Modern editorial balance"),
    OUTFIT("Outfit", "Outfit", "Clean rounded contemporary");

    companion object {
        fun fromRaw(raw: String?): AppFontStyle =
            entries.firstOrNull { it.name == raw } ?: JAKARTA_SANS
    }
}

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monthlySavingsTarget: Double = 20000.0,
    val salaryDay: Int = 1,
    val smsAutoScanConsent: Boolean = false,
    val homeCardsConfig: String = HomeCardConfig.encode(HomeCardConfig.defaultList()),
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val themeMode: String = AppThemeMode.AMOLED_DARK.name,
    val fontStyle: String = AppFontStyle.JAKARTA_SANS.name
) {
    fun resolvedThemeMode(): AppThemeMode = AppThemeMode.fromRaw(themeMode)
    fun resolvedFontStyle(): AppFontStyle = AppFontStyle.fromRaw(fontStyle)
}

enum class HomeCardId(
    val title: String,
    val subtitle: String,
    val defaultVisible: Boolean
) {
    SUMMARY("Balance & Cashflow", "Total balance, monthly income & expenses", true),
    ACCOUNTS("Accounts & Wallets", "Checking, cash & savings balances", true),
    CREDIT_CARDS("Credit Cards", "Outstanding balance, limit & due dates", true),
    BUDGETS("Monthly Budget", "Overall spending limit & progress", true),
    SAFE_TO_SPEND("Safe to Spend", "Daily allowance after bills & savings goal", false),
    INSIGHTS("Monthly Insights", "Data-driven trends & spending alerts", false),
    CATEGORY_CHART("Category Breakdown", "Interactive expense donut chart", false)
}

data class HomeCardConfig(
    val id: HomeCardId,
    val visible: Boolean
) {
    companion object {
        fun defaultList(): List<HomeCardConfig> =
            HomeCardId.entries.map { HomeCardConfig(it, it.defaultVisible) }

        fun encode(items: List<HomeCardConfig>): String =
            items.joinToString(";") { "${it.id.name}:${it.visible}" }

        fun decode(raw: String): List<HomeCardConfig> {
            if (raw.isBlank()) return defaultList()
            val parsed = raw.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val id = runCatching { HomeCardId.valueOf(parts[0]) }.getOrNull()
                    val visible = parts[1].toBooleanStrictOrNull()
                    if (id != null && visible != null) HomeCardConfig(id, visible) else null
                } else null
            }.distinctBy { it.id }

            if (parsed.isEmpty()) return defaultList()
            val missing = HomeCardId.entries
                .filter { candidate -> parsed.none { it.id == candidate } }
                .map { HomeCardConfig(it, it.defaultVisible) }
            return parsed + missing
        }
    }
}

