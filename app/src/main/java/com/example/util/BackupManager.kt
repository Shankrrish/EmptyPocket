package com.example.util

import com.example.data.model.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.AppFontStyle
import com.example.data.model.AppThemeMode
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.BudgetEntity
import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.CreditCardEntity
import com.example.data.model.HomeCardConfig
import com.example.data.model.NetWorthItemEntity
import com.example.data.model.NetWorthSnapshotEntity
import com.example.data.model.RecurringBillEntity
import com.example.data.model.RecurringFrequency
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.UserSettingsEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FinFlowBackupData(
    val accounts: List<AccountEntity>,
    val creditCards: List<CreditCardEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val savingsGoals: List<SavingsGoalEntity>,
    val netWorthItems: List<NetWorthItemEntity>,
    val netWorthSnapshots: List<NetWorthSnapshotEntity>,
    val rules: List<CategorizationRuleEntity>,
    val recurringBills: List<RecurringBillEntity>,
    val settings: UserSettingsEntity?,
    val portfolio: PortfolioBackupSlice = PortfolioBackupSlice()
)

object BackupManager {
    fun encodeBackupJson(data: FinFlowBackupData): String {
        val root = JSONObject()
        root.put("schemaVersion", 2)
        root.put("exportedAtMillis", System.currentTimeMillis())

        root.put("accounts", JSONArray().apply {
            data.accounts.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("type", a.type.name)
                    put("initialBalance", a.initialBalance)
                    put("colorHex", a.colorHex)
                    put("bankName", a.bankName)
                    put("accountSubType", a.accountSubType)
                    put("maskedNumber", a.maskedNumber)
                    put("ifsc", a.ifsc)
                    put("emoji", a.emoji)
                    put("isArchived", a.isArchived)
                    put("isPrepaidWallet", a.isPrepaidWallet)
                })
            }
        })

        root.put("creditCards", JSONArray().apply {
            data.creditCards.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("brand", c.brand)
                    put("limitAmount", c.limitAmount)
                    put("closingDay", c.closingDay)
                    put("dueDay", c.dueDay)
                    put("colorHex", c.colorHex)
                    put("issuerBank", c.issuerBank)
                    put("statementDay", c.statementDay)
                    put("minimumDueAmount", c.minimumDueAmount)
                    put("isArchived", c.isArchived)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            data.categories.forEach { cat ->
                put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("type", cat.type.name)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            data.transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("amount", t.amount)
                    put("type", t.type.name)
                    put("dateMillis", t.dateMillis)
                    put("isPaid", t.isPaid)
                    put("categoryId", t.categoryId ?: JSONObject.NULL)
                    put("accountId", t.accountId ?: JSONObject.NULL)
                    put("creditCardId", t.creditCardId ?: JSONObject.NULL)
                    put("toAccountId", t.toAccountId ?: JSONObject.NULL)
                    put("notes", t.notes)
                    put("isRecurring", t.isRecurring)
                    put("tags", t.tags)
                    put("splitSummary", t.splitSummary)
                    put("linkedAssetType", t.linkedAssetType)
                    put("linkedAssetId", t.linkedAssetId ?: JSONObject.NULL)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            data.budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("categoryId", b.categoryId)
                    put("monthYear", b.monthYear)
                    put("limitAmount", b.limitAmount)
                })
            }
        })

        root.put("savingsGoals", JSONArray().apply {
            data.savingsGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("monthlyContribution", g.monthlyContribution)
                    put("isRecurringContribution", g.isRecurringContribution)
                    put("iconName", g.iconName)
                    put("colorHex", g.colorHex)
                })
            }
        })

        root.put("netWorthItems", JSONArray().apply {
            data.netWorthItems.forEach { n ->
                put(JSONObject().apply {
                    put("id", n.id)
                    put("name", n.name)
                    put("category", n.category.name)
                    put("amount", n.amount)
                })
            }
        })

        root.put("netWorthSnapshots", JSONArray().apply {
            data.netWorthSnapshots.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("monthYear", s.monthYear)
                    put("label", s.label)
                    put("totalAssets", s.totalAssets)
                    put("totalLiabilities", s.totalLiabilities)
                })
            }
        })

        root.put("rules", JSONArray().apply {
            data.rules.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("keyword", r.keyword)
                    put("categoryId", r.categoryId)
                    put("isAutoLearned", r.isAutoLearned)
                    put("isEnabled", r.isEnabled)
                })
            }
        })

        root.put("recurringBills", JSONArray().apply {
            data.recurringBills.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("amount", b.amount)
                    put("previousAmount", b.previousAmount ?: JSONObject.NULL)
                    put("dueDay", b.dueDay)
                    put("frequency", b.frequency.name)
                    put("type", b.type.name)
                    put("categoryId", b.categoryId ?: JSONObject.NULL)
                    put("accountId", b.accountId ?: JSONObject.NULL)
                    put("creditCardId", b.creditCardId ?: JSONObject.NULL)
                    put("isSubscription", b.isSubscription)
                    put("reminderDaysBefore", b.reminderDaysBefore)
                    put("isActive", b.isActive)
                })
            }
        })

        data.settings?.let { s ->
            root.put("settings", JSONObject().apply {
                put("monthlySavingsTarget", s.monthlySavingsTarget)
                put("salaryDay", s.salaryDay)
                put("smsAutoScanConsent", s.smsAutoScanConsent)
                put("homeCardsConfig", s.homeCardsConfig)
                put("themeMode", s.themeMode)
                put("fontStyle", s.fontStyle)
            })
        }

        PortfolioBackupCodec.writeInto(root, data.portfolio)
        return root.toString(2)
    }

    fun decodeBackupJson(rawJson: String): FinFlowBackupData {
        val root = JSONObject(rawJson)

        val accounts = mutableListOf<AccountEntity>()
        root.optJSONArray("accounts")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val accType = AccountType.valueOf(o.getString("type"))
                accounts.add(
                    AccountEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        type = accType,
                        initialBalance = o.getDouble("initialBalance"),
                        colorHex = o.optLong("colorHex", 0xFF556B2F),
                        bankName = o.optString("bankName", ""),
                        accountSubType = o.optString("accountSubType", ""),
                        maskedNumber = o.optString("maskedNumber", ""),
                        ifsc = o.optString("ifsc", ""),
                        emoji = o.optString("emoji", ""),
                        isArchived = o.optBoolean("isArchived", false),
                        isPrepaidWallet = o.optBoolean("isPrepaidWallet", accType == AccountType.E_WALLET)
                    )
                )
            }
        }

        val creditCards = mutableListOf<CreditCardEntity>()
        root.optJSONArray("creditCards")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val closing = o.optInt("closingDay", 20)
                creditCards.add(
                    CreditCardEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        brand = o.optString("brand", "Visa"),
                        limitAmount = o.getDouble("limitAmount"),
                        closingDay = closing,
                        dueDay = o.optInt("dueDay", 5),
                        colorHex = o.optLong("colorHex", 0xFF6B8E23),
                        issuerBank = o.optString("issuerBank", ""),
                        statementDay = o.optInt("statementDay", closing),
                        minimumDueAmount = o.optDouble("minimumDueAmount", 0.0),
                        isArchived = o.optBoolean("isArchived", false)
                    )
                )
            }
        }

        val categories = mutableListOf<CategoryEntity>()
        root.optJSONArray("categories")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        type = CategoryType.valueOf(o.getString("type")),
                        iconName = o.optString("iconName", "SwapHoriz"),
                        colorHex = o.optLong("colorHex", 0xFF556B2F)
                    )
                )
            }
        }

        val transactions = mutableListOf<TransactionEntity>()
        root.optJSONArray("transactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        amount = o.getDouble("amount"),
                        type = TransactionType.valueOf(o.getString("type")),
                        dateMillis = o.getLong("dateMillis"),
                        isPaid = o.optBoolean("isPaid", true),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        accountId = if (o.isNull("accountId")) null else o.getLong("accountId"),
                        creditCardId = if (o.isNull("creditCardId")) null else o.getLong("creditCardId"),
                        toAccountId = if (o.isNull("toAccountId")) null else o.getLong("toAccountId"),
                        notes = o.optString("notes", ""),
                        isRecurring = o.optBoolean("isRecurring", false),
                        tags = o.optString("tags", ""),
                        splitSummary = o.optString("splitSummary", ""),
                        linkedAssetType = o.optString("linkedAssetType", ""),
                        linkedAssetId = if (o.isNull("linkedAssetId")) null else o.getLong("linkedAssetId")
                    )
                )
            }
        }

        val budgets = mutableListOf<BudgetEntity>()
        root.optJSONArray("budgets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                budgets.add(
                    BudgetEntity(
                        id = o.optLong("id", 0L),
                        categoryId = o.getLong("categoryId"),
                        monthYear = o.getString("monthYear"),
                        limitAmount = o.getDouble("limitAmount")
                    )
                )
            }
        }

        val goals = mutableListOf<SavingsGoalEntity>()
        root.optJSONArray("savingsGoals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                goals.add(
                    SavingsGoalEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        targetAmount = o.getDouble("targetAmount"),
                        savedAmount = o.getDouble("savedAmount"),
                        monthlyContribution = o.optDouble("monthlyContribution", 0.0),
                        isRecurringContribution = o.optBoolean("isRecurringContribution", true),
                        iconName = o.optString("iconName", "Savings"),
                        colorHex = o.optLong("colorHex", 0xFF556B2F)
                    )
                )
            }
        }

        val nwItems = mutableListOf<NetWorthItemEntity>()
        root.optJSONArray("netWorthItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                nwItems.add(
                    NetWorthItemEntity(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        category = AssetLiabilityCategory.valueOf(o.getString("category")),
                        amount = o.getDouble("amount")
                    )
                )
            }
        }

        val nwSnaps = mutableListOf<NetWorthSnapshotEntity>()
        root.optJSONArray("netWorthSnapshots")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                nwSnaps.add(
                    NetWorthSnapshotEntity(
                        id = o.optLong("id", 0L),
                        monthYear = o.getString("monthYear"),
                        label = o.getString("label"),
                        totalAssets = o.getDouble("totalAssets"),
                        totalLiabilities = o.getDouble("totalLiabilities")
                    )
                )
            }
        }

        val rules = mutableListOf<CategorizationRuleEntity>()
        root.optJSONArray("rules")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                rules.add(
                    CategorizationRuleEntity(
                        id = o.optLong("id", 0L),
                        keyword = o.getString("keyword"),
                        categoryId = o.getLong("categoryId"),
                        isAutoLearned = o.optBoolean("isAutoLearned", false),
                        isEnabled = o.optBoolean("isEnabled", true)
                    )
                )
            }
        }

        val bills = mutableListOf<RecurringBillEntity>()
        root.optJSONArray("recurringBills")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                bills.add(
                    RecurringBillEntity(
                        id = o.optLong("id", 0L),
                        title = o.getString("title"),
                        amount = o.getDouble("amount"),
                        previousAmount = if (o.isNull("previousAmount")) null else o.getDouble("previousAmount"),
                        dueDay = o.getInt("dueDay"),
                        frequency = RecurringFrequency.valueOf(o.optString("frequency", "MONTHLY")),
                        type = TransactionType.valueOf(o.optString("type", "EXPENSE")),
                        categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                        accountId = if (o.isNull("accountId")) null else o.getLong("accountId"),
                        creditCardId = if (o.isNull("creditCardId")) null else o.getLong("creditCardId"),
                        isSubscription = o.optBoolean("isSubscription", false),
                        reminderDaysBefore = o.optInt("reminderDaysBefore", 3),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }
        }

        val settingsObj = root.optJSONObject("settings")
        val settings = settingsObj?.let { s ->
            UserSettingsEntity(
                id = 1,
                monthlySavingsTarget = s.optDouble("monthlySavingsTarget", 20000.0),
                salaryDay = s.optInt("salaryDay", 1),
                smsAutoScanConsent = s.optBoolean("smsAutoScanConsent", false),
                homeCardsConfig = s.optString("homeCardsConfig", HomeCardConfig.encode(HomeCardConfig.defaultList())),
                themeMode = AppThemeMode.fromRaw(s.optString("themeMode", AppThemeMode.AMOLED_DARK.name)).name,
                fontStyle = AppFontStyle.fromRaw(s.optString("fontStyle", AppFontStyle.JAKARTA_SANS.name)).name
            )
        }

        return FinFlowBackupData(
            accounts = accounts,
            creditCards = creditCards,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            savingsGoals = goals,
            netWorthItems = nwItems,
            netWorthSnapshots = nwSnaps,
            rules = rules,
            recurringBills = bills,
            settings = settings,
            portfolio = PortfolioBackupCodec.readFrom(root)
        )
    }

    fun exportTransactionsCsv(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        cards: List<CreditCardEntity>
    ): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Type,Amount,Status,Category,AccountOrCard,Tags,Notes")
        transactions.forEach { tx ->
            val date = dateFmt.format(Date(tx.dateMillis))
            val cat = categories.find { it.id == tx.categoryId }?.name ?: ""
            val accountOrCard = when (tx.type) {
                TransactionType.CC_EXPENSE -> cards.find { it.id == tx.creditCardId }?.name ?: ""
                TransactionType.CC_PAYMENT -> {
                    val acc = accounts.find { it.id == tx.accountId }?.name ?: ""
                    val card = cards.find { it.id == tx.creditCardId }?.name ?: ""
                    "$acc -> $card"
                }
                TransactionType.TRANSFER -> {
                    val from = accounts.find { it.id == tx.accountId }?.name ?: ""
                    val to = accounts.find { it.id == tx.toAccountId }?.name ?: ""
                    "$from -> $to"
                }
                else -> accounts.find { it.id == tx.accountId }?.name ?: ""
            }
            val status = if (tx.isPaid) "Paid" else "Pending"
            sb.appendLine(
                listOf(
                    date,
                    csvEscape(tx.title),
                    tx.type.name,
                    String.format(Locale.ENGLISH, "%.2f", tx.amount),
                    status,
                    csvEscape(cat),
                    csvEscape(accountOrCard),
                    csvEscape(tx.tags),
                    csvEscape(tx.notes)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\n", " ").replace("\r", " ")
        return if (clean.contains(",") || clean.contains("\"")) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }
}
