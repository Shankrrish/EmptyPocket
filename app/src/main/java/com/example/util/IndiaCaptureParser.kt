package com.example.util

import com.example.data.model.CategorizationRuleEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

data class ParsedCaptureCandidate(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val dateMillis: Long,
    val detectedBankOrRail: String,
    val suggestedCategoryId: Long?,
    val suggestedCategoryName: String,
    val isDuplicate: Boolean,
    val tags: String,
    val rawSnippet: String
)

object IndiaCaptureParser {
    private val amountRegex = Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val fallbackNumberRegex = Regex("""\b([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)\b""")
    private val merchantAtRegex = Regex("""(?:to|at|towards|paid to|via|merchant[:\s]+)\s*([A-Za-z0-9 &._-]{2,32}?)(?:\s+(?:on|via|ref|upi|avl|bal|using|from)|[.,;\n]|$)""", RegexOption.IGNORE_CASE)

    private val bankIdentifiers = listOf(
        "HDFC" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "SBI" to "SBI",
        "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak Bank",
        "IDFC" to "IDFC First",
        "GPAY" to "Google Pay (UPI)",
        "GOOGLE PAY" to "Google Pay (UPI)",
        "PHONEPE" to "PhonePe (UPI)",
        "PAYTM" to "Paytm (UPI)",
        "BHIM" to "BHIM UPI",
        "UPI" to "UPI"
    )

    fun isLikelyFinancialMessage(text: String): Boolean {
        val lower = text.lowercase(Locale.ENGLISH)
        val hasAmount = amountRegex.containsMatchIn(text)
        val hasFinanceCue = listOf(
            "debited", "credited", "spent", "paid", "upi", "a/c", "acct", "card",
            "txn", "transaction", "payment", "inr", "rs.", "₹", "imps", "neft"
        ).any { lower.contains(it) }
        return hasAmount && hasFinanceCue
    }

    fun matchCategoryByRules(
        text: String,
        type: TransactionType,
        rules: List<CategorizationRuleEntity>,
        categories: List<CategoryEntity>
    ): CategoryEntity? {
        if (type == TransactionType.TRANSFER || type == TransactionType.CC_PAYMENT) return null
        val targetCatType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        val eligibleCats = categories.filter { it.type == targetCatType }
        val normalized = text.lowercase(Locale.ENGLISH)

        val matchedRule = rules.firstOrNull { rule ->
            rule.isEnabled && normalized.contains(rule.keyword.lowercase(Locale.ENGLISH))
        }
        if (matchedRule != null) {
            eligibleCats.find { it.id == matchedRule.categoryId }?.let { return it }
        }

        // Built-in Indian merchant & keyword fallbacks
        val builtinMap = listOf(
            listOf("swiggy", "zomato", "dominos", "starbucks", "cafe", "restaurant", "dining") to "Food",
            listOf("reliance smart", "bigbasket", "blinkit", "zepto", "dmart", "instamart", "grocery") to "Groceries",
            listOf("amazon", "flipkart", "myntra", "nykaa", "ajio") to "Shopping",
            listOf("uber", "ola", "rapido", "metro", "irctc", "petrol", "fuel", "shell") to "Transport",
            listOf("bescom", "electricity", "water bill", "gas", "broadband", "jio", "airtel") to "Utilities",
            listOf("netflix", "spotify", "youtube", "prime", "hotstar", "apple") to "Subscriptions",
            listOf("rent", "maintenance", "nobroker") to "Housing",
            listOf("apollo", "pharmacy", "1mg", "pharmeasy", "clinic", "hospital") to "Health",
            listOf("salary", "payroll", "stipend") to "Salary"
        )
        for ((keywords, catName) in builtinMap) {
            if (keywords.any { normalized.contains(it) }) {
                eligibleCats.find { it.name.equals(catName, ignoreCase = true) }?.let { return it }
            }
        }
        return eligibleCats.firstOrNull()
    }

    fun isPotentialDuplicate(
        amount: Double,
        dateMillis: Long,
        title: String,
        existing: List<TransactionEntity>,
        excludeId: Long = -1L
    ): Boolean {
        val windowMillis = 36L * 60L * 60L * 1000L // 36h window
        val cleanTitle = title.trim().lowercase(Locale.ENGLISH)
        return existing.any { tx ->
            if (tx.id == excludeId) return@any false
            val sameAmount = abs(tx.amount - amount) < 0.5
            val closeDate = abs(tx.dateMillis - dateMillis) <= windowMillis
            val txTitle = tx.title.trim().lowercase(Locale.ENGLISH)
            val similarTitle = cleanTitle.isNotEmpty() && (
                txTitle == cleanTitle ||
                    txTitle.contains(cleanTitle) ||
                    cleanTitle.contains(txTitle)
                )
            sameAmount && closeDate && (similarTitle || cleanTitle.isBlank())
        }
    }

    fun parseTextMessages(
        rawInput: String,
        rules: List<CategorizationRuleEntity>,
        categories: List<CategoryEntity>,
        existingTransactions: List<TransactionEntity>,
        timestampOverride: Long = System.currentTimeMillis()
    ): List<ParsedCaptureCandidate> {
        val chunks = rawInput
            .split(Regex("""\n{2,}|(?=(?:INR|Rs\.?|₹|Dear|Alert|Your\s+(?:HDFC|ICICI|SBI|Axis|Kotak)|Paid\s+))"""))
            .map { it.trim() }
            .filter { it.length >= 12 }

        val effectiveChunks = if (chunks.isEmpty() && rawInput.isNotBlank()) listOf(rawInput.trim()) else chunks

        return effectiveChunks.mapIndexedNotNull { idx, msg ->
            parseSingleMessage(
                id = "msg_${timestampOverride}_$idx",
                msg = msg,
                dateMillis = timestampOverride,
                rules = rules,
                categories = categories,
                existingTransactions = existingTransactions
            )
        }
    }

    fun parseSingleMessage(
        id: String,
        msg: String,
        dateMillis: Long,
        rules: List<CategorizationRuleEntity>,
        categories: List<CategoryEntity>,
        existingTransactions: List<TransactionEntity>
    ): ParsedCaptureCandidate? {
        val amtMatch = amountRegex.find(msg)
        val rawAmt = amtMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
            ?: return null
        if (rawAmt <= 0.0) return null

        val lower = msg.lowercase(Locale.ENGLISH)
        val isCreditCard = lower.contains("credit card") || lower.contains("card xx") || lower.contains("cc ")
        val isCreditIncome = (lower.contains("credited") || lower.contains("received") || lower.contains("refund")) &&
            !lower.contains("debited") && !lower.contains("spent")

        val txType = when {
            isCreditIncome -> TransactionType.INCOME
            isCreditCard -> TransactionType.CC_EXPENSE
            else -> TransactionType.EXPENSE
        }

        val bankOrRail = bankIdentifiers.firstOrNull { (key, _) ->
            msg.uppercase(Locale.ENGLISH).contains(key)
        }?.second ?: "Bank / UPI"

        val merchant = merchantAtRegex.find(msg)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?: inferMerchantFromKnownKeywords(msg)
            ?: bankOrRail

        val matchedCategory = matchCategoryByRules("$merchant $msg", txType, rules, categories)
        val dup = isPotentialDuplicate(rawAmt, dateMillis, merchant, existingTransactions)

        val tags = buildList {
            if (lower.contains("upi")) add("upi")
            if (isCreditCard) add("card")
            add("auto-capture")
        }.joinToString(",")

        return ParsedCaptureCandidate(
            id = id,
            title = merchant,
            amount = rawAmt,
            type = txType,
            dateMillis = dateMillis,
            detectedBankOrRail = bankOrRail,
            suggestedCategoryId = matchedCategory?.id,
            suggestedCategoryName = matchedCategory?.name ?: "Uncategorized",
            isDuplicate = dup,
            tags = tags,
            rawSnippet = msg.take(120)
        )
    }

    fun parseCsvStatement(
        csvContent: String,
        rules: List<CategorizationRuleEntity>,
        categories: List<CategoryEntity>,
        existingTransactions: List<TransactionEntity>
    ): List<ParsedCaptureCandidate> {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        )

        val candidates = mutableListOf<ParsedCaptureCandidate>()
        lines.forEachIndexed { idx, line ->
            if (idx == 0 && (line.lowercase(Locale.ENGLISH).contains("description") || line.lowercase(Locale.ENGLISH).contains("narration") || line.lowercase(Locale.ENGLISH).contains("amount"))) {
                return@forEachIndexed
            }
            val cols = line.split(",").map { it.trim().trim('"') }
            if (cols.size < 3) return@forEachIndexed

            val parsedDate = dateFormats.firstNotNullOfOrNull { fmt ->
                runCatching { fmt.parse(cols[0])?.time }.getOrNull()
            } ?: System.currentTimeMillis()

            val description = cols.getOrNull(1)?.ifBlank { "Bank Entry" } ?: "Bank Entry"
            val amountCol = cols.drop(2).firstNotNullOfOrNull { col ->
                col.replace("₹", "").replace(",", "").replace("Rs.", "", ignoreCase = true).trim().toDoubleOrNull()
            } ?: return@forEachIndexed

            if (amountCol == 0.0) return@forEachIndexed
            val isCredit = amountCol > 0 && cols.any { it.equals("CR", ignoreCase = true) || it.equals("CREDIT", ignoreCase = true) }
            val absAmt = abs(amountCol)
            val txType = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
            val bank = bankIdentifiers.firstOrNull { (k, _) -> description.uppercase(Locale.ENGLISH).contains(k) }?.second ?: "CSV Import"
            val cat = matchCategoryByRules(description, txType, rules, categories)
            val dup = isPotentialDuplicate(absAmt, parsedDate, description, existingTransactions)

            candidates.add(
                ParsedCaptureCandidate(
                    id = "csv_$idx",
                    title = description.take(40),
                    amount = absAmt,
                    type = txType,
                    dateMillis = parsedDate,
                    detectedBankOrRail = bank,
                    suggestedCategoryId = cat?.id,
                    suggestedCategoryName = cat?.name ?: "Uncategorized",
                    isDuplicate = dup,
                    tags = "csv-import",
                    rawSnippet = line.take(120)
                )
            )
        }
        return candidates
    }

    private fun inferMerchantFromKnownKeywords(msg: String): String? {
        val known = listOf(
            "Swiggy", "Zomato", "Reliance Smart", "BigBasket", "Blinkit", "Zepto",
            "Amazon", "Flipkart", "Uber", "Ola", "Netflix", "Spotify", "YouTube Premium",
            "BESCOM", "Airtel", "Jio", "Apollo Pharmacy"
        )
        return known.firstOrNull { msg.contains(it, ignoreCase = true) }
    }
}
