package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.data.model.TransactionType
import com.example.ui.theme.FinFlowTheme

data class EmojiPresetGroup(
    val title: String,
    val emojis: List<String>
)

val CategoryEmojiGroups = listOf(
    EmojiPresetGroup(
        title = "Food & Lifestyle",
        emojis = listOf("🍔", "🍕", "🍜", "☕", "🛒", "🥑", "🛍️", "👕", "🎬", "🎮", "🎧")
    ),
    EmojiPresetGroup(
        title = "Home, Bills & Transit",
        emojis = listOf("🏡", "🛋️", "🪴", "💡", "💧", "📱", "🚗", "🚕", "⛽", "✈️", "🧳", "🛡️")
    ),
    EmojiPresetGroup(
        title = "Health & Learning",
        emojis = listOf("🏥", "💊", "🩺", "🏋️", "📚", "🎓", "✏️", "🐾")
    ),
    EmojiPresetGroup(
        title = "Income & Wealth",
        emojis = listOf("💰", "💵", "🏦", "📈", "💹", "🪙", "🐷", "🎯", "💎", "💳", "🎁")
    )
)

val CategoryAccentColors: List<Long> = listOf(
    0xFF556B2F, // Olive Primary
    0xFF6B8E23, // Olive Drab
    0xFF3B6E22, // Olive Income
    0xFF2F6F6D, // Slate Teal
    0xFF0D9488, // Mint Emerald
    0xFF0284C7, // Cerulean
    0xFFC97A16, // Warm Ochre
    0xFFC23B2B, // Terracotta
    0xFF3E4F22  // Deep Moss
)

fun resolveCategoryEmoji(iconName: String, categoryName: String = ""): String {
    val trimmed = iconName.trim()
    if (trimmed.isNotEmpty() && trimmed.any { it.code > 127 }) {
        return trimmed
    }
    val byIcon = when (trimmed) {
        "Restaurant" -> "🍔"
        "ShoppingCart" -> "🛒"
        "ShoppingBag" -> "🛍️"
        "DirectionsCar" -> "🚗"
        "Bolt" -> "💡"
        "Home" -> "🏡"
        "Subscriptions" -> "🎬"
        "SportsEsports" -> "🎮"
        "Favorite" -> "🏥"
        "School" -> "📚"
        "Flight" -> "✈️"
        "Work" -> "💰"
        "Laptop" -> "📈"
        "Savings" -> "🐷"
        "Shield" -> "🛡️"
        "CreditCard" -> "💳"
        "Payments" -> "💵"
        else -> null
    }
    if (byIcon != null) return byIcon

    val lower = categoryName.lowercase()
    return when {
        "food" in lower || "dining" in lower || "rest" in lower -> "🍔"
        "groc" in lower || "mart" in lower -> "🛒"
        "shop" in lower || "cloth" in lower -> "🛍️"
        "trans" in lower || "commut" in lower || "fuel" in lower || "car" in lower -> "🚗"
        "ent" in lower || "sub" in lower || "movie" in lower || "music" in lower -> "🎬"
        "health" in lower || "med" in lower || "pharm" in lower -> "🏥"
        "edu" in lower || "school" in lower || "book" in lower -> "📚"
        "home" in lower || "rent" in lower || "hous" in lower -> "🏡"
        "travel" in lower || "vac" in lower || "trip" in lower -> "✈️"
        "util" in lower || "bill" in lower || "elect" in lower || "phone" in lower -> "💡"
        "sal" in lower || "pay" in lower -> "💰"
        "free" in lower || "invest" in lower || "stock" in lower || "fund" in lower -> "📈"
        "sav" in lower || "goal" in lower -> "🐷"
        "insur" in lower -> "🛡️"
        "card" in lower -> "💳"
        else -> "✨"
    }
}

fun accountEmoji(type: AccountType, customEmoji: String = ""): String {
    if (customEmoji.isNotBlank()) return customEmoji
    return when (type) {
        AccountType.CHECKING -> "🏦"
        AccountType.CASH -> "💵"
        AccountType.SAVINGS -> "🐷"
        AccountType.E_WALLET -> "📱"
    }
}

fun assetLiabilityEmoji(category: AssetLiabilityCategory): String = when (category) {
    AssetLiabilityCategory.MUTUAL_FUNDS_STOCKS -> "📈"
    AssetLiabilityCategory.EPF_PPF_NPS -> "🏛️"
    AssetLiabilityCategory.GOLD_PROPERTY -> "🪙"
    AssetLiabilityCategory.OTHER_ASSET -> "💎"
    AssetLiabilityCategory.HOME_LOAN -> "🏡"
    AssetLiabilityCategory.PERSONAL_LOAN -> "🧾"
    AssetLiabilityCategory.OTHER_LIABILITY -> "💳"
}

fun transactionEmoji(type: TransactionType, category: CategoryEntity?): String = when (type) {
    TransactionType.CC_PAYMENT -> "💳"
    TransactionType.TRANSFER -> "🔄"
    TransactionType.INVESTMENT_BUY -> "📈"
    TransactionType.INVESTMENT_SELL -> "💹"
    TransactionType.INVESTMENT_INCOME -> "🪙"
    TransactionType.VALUATION_ADJUSTMENT -> "⚖️"
    TransactionType.INTEREST_CREDIT -> "🏛️"
    else -> resolveCategoryEmoji(category?.iconName.orEmpty(), category?.name.orEmpty())
}

fun categorySectionLabel(category: CategoryEntity): String {
    if (category.type == CategoryType.INCOME) return "Income Sources"
    return when (category.name.lowercase()) {
        "housing", "utilities", "groceries", "transport", "health", "insurance" -> "Essentials & Living"
        "food", "shopping", "subscriptions", "entertainment", "travel", "education" -> "Lifestyle & Growth"
        else -> "Custom & Personal"
    }
}

/**
 * Calibrated soft pastel tile fills & borders so multi-color system emojis stay vibrant
 * and legible inside dark AMOLED charcoal cards as well as crisp light cards.
 */
fun pastelTileColors(
    emoji: String,
    fallbackTint: Color,
    isAmoled: Boolean
): Pair<Color, Color> {
    val baseFill = when (emoji.trim()) {
        "🍔", "🍕", "🍜", "☕", "🛒", "🥑", "💸" ->
            if (isAmoled) Color(0xFFF6E3CE) else Color(0xFFFCEBD8) // Warm peach
        "🚗", "🚕", "⛽", "✈️", "🧳", "🔄" ->
            if (isAmoled) Color(0xFFDFEDF7) else Color(0xFFE5F2FC) // Soft sky blue
        "🛍️", "👕", "🎬", "🎮", "🎧", "🎁", "📊" ->
            if (isAmoled) Color(0xFFF3E2EC) else Color(0xFFF9EAF3) // Soft blush pink
        "💰", "💵", "🏦", "📈", "💹", "🐷", "🎯", "🌿", "✨" ->
            if (isAmoled) Color(0xFFE1EEDC) else Color(0xFFE7F3E2) // Soft sage mint
        "🏡", "🛋️", "🪴", "💡", "💧", "🪙", "💎", "🛡️", "⚠️" ->
            if (isAmoled) Color(0xFFF5EAD0) else Color(0xFFFDF3DA) // Warm buttercream
        "🏥", "💊", "🩺", "🏋️", "📱", "💳", "🧾", "🏛️", "🔐" ->
            if (isAmoled) Color(0xFFE5E6F6) else Color(0xFFECECFD) // Soft periwinkle
        "📚", "🎓", "✏️", "🐾", "🗓️" ->
            if (isAmoled) Color(0xFFE2F1EF) else Color(0xFFE7F6F4) // Soft seafoam
        else -> {
            val base = if (isAmoled) Color(0xFFEFECE6) else Color(0xFFF6F4EE)
            lerp(base, fallbackTint, 0.16f)
        }
    }
    val border = if (isAmoled) {
        lerp(baseFill, Color.White, 0.18f).copy(alpha = 0.55f)
    } else {
        lerp(baseFill, fallbackTint, 0.28f).copy(alpha = 0.45f)
    }
    return baseFill to border
}

@Composable
fun EmojiBadge(
    emoji: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    emojiSize: TextUnit = 20.sp,
    shape: Shape = RoundedCornerShape(13.dp),
    selected: Boolean = false,
    contentDescription: String? = null
) {
    val isAmoled = FinFlowTheme.colors.isAmoledDark
    val (pastelBg, pastelBorder) = pastelTileColors(emoji, tintColor, isAmoled)
    val bgColor = if (selected) lerp(pastelBg, MaterialTheme.colorScheme.primary, 0.12f) else pastelBg
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else pastelBorder

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(bgColor, shape)
            .border(
                width = if (selected) 1.5.dp else 0.75.dp,
                color = borderColor,
                shape = shape
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            )
    ) {
        Text(
            text = emoji,
            fontSize = emojiSize,
            lineHeight = emojiSize
        )
    }
}

@Composable
fun AnimatedFinProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "finProgress"
    )
    val animatedColor by animateColorAsState(
        targetValue = color,
        label = "finProgressColor"
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape),
        color = animatedColor,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round
    )
}

@Composable
fun FinSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    backgroundBrush: Brush? = null,
    elevation: Dp = 0.dp,
    borderColor: Color = FinFlowTheme.colors.hairlineBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val effectiveBrush = backgroundBrush ?: if (containerColor == MaterialTheme.colorScheme.surface) {
        Brush.verticalGradient(listOf(semanticColors.cardSheenStart, semanticColors.cardSheenEnd))
    } else null

    Card(
        modifier = modifier,
        shape = shape,
        border = BorderStroke(0.75.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (effectiveBrush != null) Color.Transparent else containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        if (effectiveBrush != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(effectiveBrush),
                content = content
            )
        } else {
            content()
        }
    }
}
