package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TransactionType
import com.example.ui.CategorySlice
import com.example.ui.theme.FinFlowTheme
import com.example.util.MonthYear
import com.example.util.formatCurrency
import kotlin.math.atan2

fun categoryIcon(name: String): ImageVector = when (name) {
    "Restaurant" -> Icons.Default.Restaurant
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "ShoppingBag" -> Icons.Default.ShoppingBag
    "DirectionsCar" -> Icons.Default.DirectionsCar
    "Bolt" -> Icons.Default.Bolt
    "Home" -> Icons.Default.Home
    "Subscriptions" -> Icons.Default.Subscriptions
    "SportsEsports" -> Icons.Default.SportsEsports
    "Favorite" -> Icons.Default.Favorite
    "Work" -> Icons.Default.Work
    "Laptop" -> Icons.Default.Laptop
    "CreditCard" -> Icons.Default.CreditCard
    "Payments" -> Icons.Default.Payments
    "Savings" -> Icons.Default.Savings
    "Flight" -> Icons.Default.Flight
    "School" -> Icons.Default.School
    else -> Icons.Default.SwapHoriz
}

@Composable
fun MonthSelectorBar(
    selectedMonth: MonthYear,
    isBalanceHidden: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = selectedMonth.displayLabel()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.size(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_month),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        IconButton(
            onClick = onToggleVisibility,
            modifier = Modifier.testTag("toggle_visibility_button")
        ) {
            Icon(
                imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = stringResource(R.string.cd_toggle_visibility),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveDonutChart(
    slices: List<CategorySlice>,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(slices) { mutableStateOf<Int?>(null) }
    val total = slices.sumOf { it.amount }
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val sweepScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "donutReveal"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(152.dp)
        ) {
            val strokeWidths = slices.indices.map { idx ->
                val target = if (selectedIndex == idx) 26f else 18f
                val animated by animateFloatAsState(
                    targetValue = target,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "sliceStroke$idx"
                )
                animated
            }

            Canvas(
                modifier = Modifier
                    .size(140.dp)
                    .testTag("category_donut_chart")
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            var angle = Math.toDegrees(
                                atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                            ).toFloat() + 90f
                            if (angle < 0f) angle += 360f
                            var acc = 0f
                            slices.forEachIndexed { idx, slice ->
                                val sweep = slice.percentage * 360f
                                if (angle in acc..(acc + sweep)) {
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                }
                                acc += sweep
                            }
                        }
                    }
            ) {
                var startAngle = -90f
                slices.forEachIndexed { idx, slice ->
                    val sweep = slice.percentage * 360f * sweepScale
                    val isDimmed = selectedIndex != null && selectedIndex != idx
                    val sliceColor = Color(slice.category.colorHex).let {
                        if (isDimmed) it.copy(alpha = 0.28f) else it
                    }
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 3f).coerceAtLeast(1.5f),
                        useCenter = false,
                        topLeft = Offset(18f, 18f),
                        size = Size(size.width - 36f, size.height - 36f),
                        style = Stroke(width = strokeWidths[idx], cap = StrokeCap.Round)
                    )
                    startAngle += slice.percentage * 360f
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                val active = selectedIndex?.let { slices.getOrNull(it) }
                if (active != null) {
                    Text(
                        text = resolveCategoryEmoji(active.category.iconName, active.category.name),
                        fontSize = 18.sp
                    )
                }
                Text(
                    text = active?.category?.name ?: "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(active?.amount ?: total, isBalanceHidden),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            slices.forEachIndexed { idx, slice ->
                val isSelected = selectedIndex == idx
                val emoji = resolveCategoryEmoji(slice.category.iconName, slice.category.name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(slice.category.colorHex).copy(alpha = 0.14f)
                            else Color.Transparent
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("donut_legend_${slice.category.name.lowercase()}")
                ) {
                    Text(text = emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(slice.category.colorHex), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = slice.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(slice.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color(slice.category.colorHex) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableGradientFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectType: (TransactionType) -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabRotation"
    )
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FabOptionPill(
                    label = "Expense",
                    color = semanticColors.expense,
                    icon = Icons.Default.ArrowDownward,
                    testTag = "fab_expense_button",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectType(TransactionType.EXPENSE)
                    }
                )
                FabOptionPill(
                    label = "Card Spend",
                    color = semanticColors.warning,
                    icon = Icons.Default.CreditCard,
                    testTag = "fab_cc_expense_button",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectType(TransactionType.CC_EXPENSE)
                    }
                )
                FabOptionPill(
                    label = "Pay Card",
                    color = semanticColors.cardPayment,
                    icon = Icons.Default.Payments,
                    testTag = "fab_cc_payment_button",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectType(TransactionType.CC_PAYMENT)
                    }
                )
                FabOptionPill(
                    label = "Income",
                    color = semanticColors.income,
                    icon = Icons.Default.ArrowUpward,
                    testTag = "fab_income_button",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectType(TransactionType.INCOME)
                    }
                )
                FabOptionPill(
                    label = "Transfer",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.SwapHoriz,
                    testTag = "fab_transfer_button",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectType(TransactionType.TRANSFER)
                    }
                )
            }
        }
        FloatingActionButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            },
            shape = CircleShape,
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(2.dp, 4.dp),
            modifier = Modifier.testTag("main_add_fab")
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(semanticColors.accentCtaStart, semanticColors.accentCtaEnd)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_entry),
                    tint = Color.White,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

@Composable
private fun FabOptionPill(
    label: String,
    color: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FinFlowTheme.colors.surfaceElevated,
            border = BorderStroke(0.75.dp, FinFlowTheme.colors.hairlineBorder),
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    emoji: String = "🌿"
) {
    FinSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            EmojiBadge(
                emoji = emoji,
                tintColor = MaterialTheme.colorScheme.primary,
                size = 64.dp,
                emojiSize = 30.sp,
                shape = RoundedCornerShape(20.dp),
                contentDescription = title
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = FinFlowTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class CenteredNavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun CenteredQuickAddTray(
    visible: Boolean,
    onSelectType: (TransactionType) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dimmed backdrop scrim dismissible on outside tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(semanticColors.scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
                    .testTag("quick_add_scrim")
            )

            // Floating 3D beveled card with raised tactile action tiles
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.25.dp,
                    brush = topLitBevelBrush(semanticColors, semanticColors.heroBorder)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = if (semanticColors.isAmoledDark) Color(0xFF4D6639).copy(alpha = 0.36f) else Color.Black.copy(alpha = 0.14f),
                        spotColor = Color.Black.copy(alpha = if (semanticColors.isAmoledDark) 0.90f else 0.24f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("quick_add_popup_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(semanticColors.popupSurfaceStart, semanticColors.popupSurfaceEnd)
                            )
                        )
                ) {
                    // Crisp top specular rim inside floating 3D popup shell
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(1.25.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = if (semanticColors.isAmoledDark) 0.34f else 0.95f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Quick Transaction",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pick an entry type or tap outside to close",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            SculptedActionIcon(
                                icon = Icons.Default.Close,
                                contentDescription = "Close quick add",
                                tint = semanticColors.textSecondary,
                                onClick = onDismiss,
                                testTag = "quick_add_close_button"
                            )
                        }

                        // Primary Tier: 2 Wide Raised 3D Tactile Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Raised3DPrimaryTile(
                                emoji = "💸",
                                title = "Expense",
                                subtitle = "Bank & Cash",
                                color = semanticColors.expense,
                                testTag = "fab_expense_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType(TransactionType.EXPENSE)
                                }
                            )
                            Raised3DPrimaryTile(
                                emoji = "💰",
                                title = "Income",
                                subtitle = "Salary & Inflow",
                                color = semanticColors.income,
                                testTag = "fab_income_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType(TransactionType.INCOME)
                                }
                            )
                        }

                        // Secondary Tier: 3 Symmetrical Raised 3D Tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Raised3DSecondaryTile(
                                emoji = "💳",
                                title = "Card Spend",
                                subtitle = "Credit limit",
                                color = semanticColors.warning,
                                testTag = "fab_cc_expense_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType(TransactionType.CC_EXPENSE)
                                }
                            )
                            Raised3DSecondaryTile(
                                emoji = "🧾",
                                title = "Pay Card",
                                subtitle = "Settle bill",
                                color = semanticColors.cardPayment,
                                testTag = "fab_cc_payment_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType(TransactionType.CC_PAYMENT)
                                }
                            )
                            Raised3DSecondaryTile(
                                emoji = "🔄",
                                title = "Transfer",
                                subtitle = "Move funds",
                                color = MaterialTheme.colorScheme.primary,
                                testTag = "fab_transfer_button",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectType(TransactionType.TRANSFER)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Raised3DPrimaryTile(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tilePress_$testTag"
    )
    val tileShape = RoundedCornerShape(18.dp)
    val bgTop = lerp(semanticColors.tileRaisedStart, color, if (semanticColors.isAmoledDark) 0.08f else 0.04f)
    val bgBottom = semanticColors.tileRaisedEnd

    Surface(
        shape = tileShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, topLitBevelBrush(semanticColors, color.copy(alpha = 0.48f))),
        modifier = modifier
            .heightIn(min = 66.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 6.dp,
                shape = tileShape,
                ambientColor = color.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = if (semanticColors.isAmoledDark) 0.78f else 0.16f)
            )
            .clip(tileShape)
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (semanticColors.isAmoledDark) 0.28f else 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                EmojiBadge(
                    emoji = emoji,
                    tintColor = color,
                    size = 40.dp,
                    emojiSize = 20.sp,
                    shape = RoundedCornerShape(13.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = semanticColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun Raised3DSecondaryTile(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "secTilePress_$testTag"
    )
    val tileShape = RoundedCornerShape(18.dp)
    val bgTop = lerp(semanticColors.tileRaisedStart, color, if (semanticColors.isAmoledDark) 0.06f else 0.03f)
    val bgBottom = semanticColors.tileRaisedEnd

    Surface(
        shape = tileShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(0.9.dp, topLitBevelBrush(semanticColors, color.copy(alpha = 0.42f))),
        modifier = modifier
            .heightIn(min = 84.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 5.dp,
                shape = tileShape,
                ambientColor = color.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = if (semanticColors.isAmoledDark) 0.72f else 0.14f)
            )
            .clip(tileShape)
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (semanticColors.isAmoledDark) 0.24f else 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 11.dp)
            ) {
                EmojiBadge(
                    emoji = emoji,
                    tintColor = color,
                    size = 34.dp,
                    emojiSize = 17.sp,
                    shape = RoundedCornerShape(11.dp)
                )
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CenteredBottomNavBar(
    tabs: List<CenteredNavTabItem>,
    currentRoute: String,
    fabExpanded: Boolean,
    onSelectTab: (String) -> Unit,
    onToggleCenterAdd: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        targetValue = if (fabExpanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "centerAddRotation"
    )
    val dockedContainer = if (semanticColors.isAmoledDark) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = dockedContainer,
        tonalElevation = 0.dp,
        shadowElevation = if (semanticColors.isAmoledDark) 0.dp else 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.take(2).forEach { tab ->
                    BottomBarNavSlot(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = semanticColors.accentCtaEnd.copy(alpha = 0.35f),
                                spotColor = Color.Black.copy(alpha = if (semanticColors.isAmoledDark) 0.85f else 0.25f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(semanticColors.accentCtaEnd, semanticColors.accentCtaStart)),
                                CircleShape
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleCenterAdd()
                            }
                            .testTag("main_add_fab")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_entry),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }

                tabs.drop(2).take(2).forEach { tab ->
                    BottomBarNavSlot(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onSelectTab(tab.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarNavSlot(
    tab: CenteredNavTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = FinFlowTheme.colors
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = semanticColors.navInactive
    val tint by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "navTint_${tab.route}"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("nav_tab_${tab.route}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selected) activeColor.copy(alpha = if (semanticColors.isAmoledDark) 0.14f else 0.11f)
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


