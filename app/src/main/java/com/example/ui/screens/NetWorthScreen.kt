package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssetLiabilityCategory
import com.example.data.model.NetWorthItemEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.assetLiabilityEmoji
import com.example.ui.theme.FinFlowTheme
import com.example.util.formatCurrency

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onSaveItem: (String, AssetLiabilityCategory, Double) -> Unit,
    onDeleteItem: (NetWorthItemEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val nw = state.netWorthSummary
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(AssetLiabilityCategory.MUTUAL_FUNDS_STOCKS) }
        var catExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Add Asset or Liability") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(
                            value = "${assetLiabilityEmoji(selectedCat)} ${selectedCat.label} (${if (selectedCat.isLiability) "Liability" else "Asset"})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            AssetLiabilityCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${assetLiabilityEmoji(cat)} ${cat.label} (${if (cat.isLiability) "Liability" else "Asset"})") },
                                    onClick = { selectedCat = cat; catExpanded = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (e.g. PPF, HDFC Home Loan)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("nw_name_input")
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Current Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("nw_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && amt > 0.0) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveItem(name.trim(), selectedCat, amt)
                            showAddDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("nw_save_button")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Net Worth & Assets") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nw_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("nw_add_button")) {
                        Icon(Icons.Default.Add, contentDescription = "Add Asset or Liability")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("net_worth_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FinSurfaceCard(
                    shape = RoundedCornerShape(22.dp),
                    backgroundBrush = Brush.linearGradient(
                        listOf(semanticColors.heroGradientStart, semanticColors.heroGradientEnd)
                    ),
                    borderColor = semanticColors.heroBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Total Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = semanticColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatCurrency(nw.netWorth, state.isBalanceHidden),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("net_worth_total_text")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(thickness = 0.75.dp, color = semanticColors.hairlineBorder)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Assets", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                Text(
                                    text = formatCurrency(nw.totalAssets, state.isBalanceHidden),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = semanticColors.income
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Liabilities", style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                Text(
                                    text = formatCurrency(nw.totalLiabilities, state.isBalanceHidden),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = semanticColors.expense
                                )
                            }
                        }
                    }
                }
            }

            // Historical Net Worth Chart
            item {
                FinSurfaceCard(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(
                                emoji = "📈",
                                tintColor = MaterialTheme.colorScheme.primary,
                                size = 34.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Net Worth History", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val points = nw.historyPoints.map { it.totalAssets - it.totalLiabilities }
                        val lineColor = MaterialTheme.colorScheme.primary
                        val dotSurfaceColor = MaterialTheme.colorScheme.surface
                        if (points.size >= 2) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(116.dp)
                            ) {
                                val minVal = points.minOrNull() ?: 0.0
                                val maxVal = points.maxOrNull() ?: 1.0
                                val span = (maxVal - minVal).coerceAtLeast(1.0)
                                val strokePath = Path()
                                val fillPath = Path()
                                val coords = points.mapIndexed { idx, v ->
                                    val x = size.width * (idx.toFloat() / (points.size - 1))
                                    val y = size.height - (((v - minVal) / span).toFloat() * (size.height - 24f)) - 12f
                                    Offset(x, y)
                                }
                                coords.forEachIndexed { idx, pt ->
                                    if (idx == 0) {
                                        strokePath.moveTo(pt.x, pt.y)
                                        fillPath.moveTo(pt.x, size.height)
                                        fillPath.lineTo(pt.x, pt.y)
                                    } else {
                                        strokePath.lineTo(pt.x, pt.y)
                                        fillPath.lineTo(pt.x, pt.y)
                                    }
                                }
                                fillPath.lineTo(coords.last().x, size.height)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(lineColor.copy(alpha = 0.24f), Color.Transparent)
                                    )
                                )
                                drawPath(path = strokePath, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                                coords.forEach { pt ->
                                    drawCircle(color = dotSurfaceColor, radius = 8f, center = pt)
                                    drawCircle(color = lineColor, radius = 5.5f, center = pt)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                nw.historyPoints.forEach { snap ->
                                    Text(snap.label, style = MaterialTheme.typography.labelSmall, color = semanticColors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            item { Text("Assets", style = MaterialTheme.typography.titleMedium) }
            item {
                FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = "🏦",
                            tintColor = semanticColors.income,
                            size = 40.dp,
                            emojiSize = 18.sp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bank Accounts & Cash", style = MaterialTheme.typography.titleMedium)
                            Text("Auto-synced from Accounts", style = MaterialTheme.typography.bodyMedium, color = semanticColors.textSecondary)
                        }
                        Text(formatCurrency(nw.liquidBankAndCash, state.isBalanceHidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = semanticColors.income)
                    }
                }
            }

            items(state.netWorthItems.filter { !it.category.isLiability }, key = { "asset_${it.id}" }) { item ->
                NetWorthItemRow(item = item, isHidden = state.isBalanceHidden, onDelete = { onDeleteItem(item) })
            }

            item { Spacer(modifier = Modifier.height(4.dp)); Text("Liabilities", style = MaterialTheme.typography.titleMedium) }
            item {
                FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(
                            emoji = "💳",
                            tintColor = semanticColors.expense,
                            size = 40.dp,
                            emojiSize = 18.sp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Credit Card Outstanding", style = MaterialTheme.typography.titleMedium)
                            Text("Auto-synced from Cards", style = MaterialTheme.typography.bodyMedium, color = semanticColors.textSecondary)
                        }
                        Text(formatCurrency(nw.cardOutstandingLiabilities, state.isBalanceHidden), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = semanticColors.expense)
                    }
                }
            }

            items(state.netWorthItems.filter { it.category.isLiability }, key = { "liab_${it.id}" }) { item ->
                NetWorthItemRow(item = item, isHidden = state.isBalanceHidden, onDelete = { onDeleteItem(item) })
            }
        }
    }
}

@Composable
private fun NetWorthItemRow(
    item: NetWorthItemEntity,
    isHidden: Boolean,
    onDelete: () -> Unit
) {
    val semanticColors = FinFlowTheme.colors
    val tint = if (item.category.isLiability) semanticColors.expense else semanticColors.income
    FinSurfaceCard(shape = RoundedCornerShape(16.dp), elevation = 0.dp) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(
                emoji = assetLiabilityEmoji(item.category),
                tintColor = tint,
                size = 40.dp,
                emojiSize = 18.sp,
                shape = RoundedCornerShape(12.dp),
                contentDescription = item.name
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.category.label, style = MaterialTheme.typography.bodyMedium, color = semanticColors.textSecondary)
            }
            Text(
                text = formatCurrency(item.amount, isHidden),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${item.name}", tint = semanticColors.textSecondary)
            }
        }
    }
}
