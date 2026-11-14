package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppFontStyle
import com.example.data.model.AppThemeMode
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme
import com.example.ui.theme.fontFamilyFor
import com.example.util.formatCurrency

@Composable
fun MoreScreen(
    state: FinanceUiState,
    onSetThemeMode: (AppThemeMode) -> Unit = {},
    onSetFontStyle: (AppFontStyle) -> Unit = {},
    onResetDemo: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenPlanning: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenNetWorth: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenAutoCapture: () -> Unit,
    onOpenHomeCustomization: () -> Unit,
    onOpenPrivacySettings: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Reset Demo Data?") },
            text = { Text("This will restore all sample accounts, cards, portfolio holdings, goals, budgets, and transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetDemo()
                        showResetConfirm = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = semanticColors.expense),
                    modifier = Modifier.testTag("reset_demo_confirm_button")
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp)
    ) {
        item {
            val activeThemeMode = state.settings.resolvedThemeMode()
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(10.dp))
            FinSurfaceCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appearance_theme_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = when (activeThemeMode) {
                                AppThemeMode.AMOLED_DARK -> "🌙"
                                AppThemeMode.CRISP_LIGHT -> "☀️"
                                AppThemeMode.SYSTEM -> "📱"
                            },
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 40.dp,
                            emojiSize = 18.sp,
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = "Theme Mode"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Display Theme", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = when (activeThemeMode) {
                                    AppThemeMode.AMOLED_DARK -> "True Black AMOLED • S23 battery & contrast optimized"
                                    AppThemeMode.CRISP_LIGHT -> "Crisp Light • Clean daylight financial workspace"
                                    AppThemeMode.SYSTEM -> "System Default • Follows Android display mode"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val selected = activeThemeMode == mode
                            val label = when (mode) {
                                AppThemeMode.AMOLED_DARK -> "🌙 AMOLED"
                                AppThemeMode.CRISP_LIGHT -> "☀️ Light"
                                AppThemeMode.SYSTEM -> "📱 System"
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else semanticColors.surfaceElevated
                                    )
                                    .border(
                                        BorderStroke(
                                            width = if (selected) 1.25.dp else 0.75.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else semanticColors.hairlineBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSetThemeMode(mode)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)

                    val activeFontStyle = state.settings.resolvedFontStyle()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = "✨",
                            tintColor = MaterialTheme.colorScheme.secondary,
                            size = 40.dp,
                            emojiSize = 18.sp,
                            shape = RoundedCornerShape(12.dp),
                            contentDescription = "Typography Font Style"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Font Style", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = activeFontStyle.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = semanticColors.textSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontStyle.entries.forEach { fontStyle ->
                            val selected = activeFontStyle == fontStyle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        else semanticColors.surfaceElevated
                                    )
                                    .border(
                                        BorderStroke(
                                            width = if (selected) 1.25.dp else 0.75.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else semanticColors.hairlineBorder
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSetFontStyle(fontStyle)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .testTag("font_style_${fontStyle.name.lowercase()}")
                            ) {
                                Text(
                                    text = fontStyle.shortLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = fontFamilyFor(fontStyle)
                                    ),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text("Investments & Insights", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(10.dp))
            MoreHubRow(
                emoji = "📈",
                title = "Investment Portfolio",
                subtitle = "Value: ${formatCurrency(state.portfolioSummary.totalCurrentValue, state.isBalanceHidden)} • MF, Stocks, PPF, EPF, Gold",
                tintColor = semanticColors.income,
                testTag = "hub_portfolio",
                onClick = onOpenPortfolio
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "📊",
                title = "Financial Reports & Net Worth",
                subtitle = "Net Worth: ${formatCurrency(state.netWorthSummary.netWorth, state.isBalanceHidden)} • Spending & Account reports",
                tintColor = MaterialTheme.colorScheme.primary,
                testTag = "hub_reports",
                onClick = onOpenReports
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "🎯",
                title = "Budgets, Recurring Bills & Goals",
                subtitle = "Planned: ${formatCurrency(state.totalBudgetPlanned, state.isBalanceHidden)} • ${state.savingsGoals.size} savings goals",
                tintColor = MaterialTheme.colorScheme.secondary,
                testTag = "hub_planning",
                onClick = onOpenPlanning
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "🏦",
                title = "Manage Accounts & Credit Cards",
                subtitle = "${state.accountSummaries.size} active accounts • ${state.cardSummaries.size} cards",
                tintColor = MaterialTheme.colorScheme.primary,
                testTag = "hub_accounts",
                onClick = onOpenAccounts
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text("Personalization & Automation", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(10.dp))
            MoreHubRow(
                emoji = "💎",
                title = "Loans & Manual Net Worth Items",
                subtitle = "${state.netWorthItems.size} tracked loans or manual items",
                tintColor = semanticColors.warning,
                testTag = "hub_net_worth",
                onClick = onOpenNetWorth
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "🎨",
                title = "Customize Homepage Cards",
                subtitle = "${state.homeCards.count { it.visible }} cards shown on Home",
                tintColor = MaterialTheme.colorScheme.primary,
                testTag = "hub_customize_home",
                onClick = onOpenHomeCustomization
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "🔐",
                title = "Privacy, Lock & Backups",
                subtitle = if (state.settings.appLockEnabled) "PIN Lock Active • JSON/CSV Export" else "App Lock Off • JSON/CSV Export",
                tintColor = MaterialTheme.colorScheme.secondary,
                testTag = "hub_privacy_settings",
                onClick = onOpenPrivacySettings
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "✨",
                title = "Smart Categorization Rules",
                subtitle = "${state.rules.count { it.isEnabled }} active merchant rules",
                tintColor = MaterialTheme.colorScheme.primary,
                testTag = "hub_smart_rules",
                onClick = onOpenRules
            )
            Spacer(modifier = Modifier.height(8.dp))
            MoreHubRow(
                emoji = "📲",
                title = "India Auto-Capture (SMS / UPI / CSV)",
                subtitle = "Local-first parsing & duplicate protection",
                tintColor = semanticColors.cardPayment,
                testTag = "hub_auto_capture",
                onClick = onOpenAutoCapture
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_demo_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = semanticColors.expense),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_demo_data))
            }
        }
    }
}

@Composable
private fun MoreHubRow(
    emoji: String,
    title: String,
    subtitle: String,
    tintColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    FinSurfaceCard(
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiBadge(
                emoji = emoji,
                tintColor = tintColor,
                size = 42.dp,
                emojiSize = 19.sp,
                shape = RoundedCornerShape(13.dp),
                contentDescription = title
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FinFlowTheme.colors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = FinFlowTheme.colors.textSecondary)
        }
    }
}
