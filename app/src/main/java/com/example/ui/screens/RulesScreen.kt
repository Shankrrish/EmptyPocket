package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorizationRuleEntity
import com.example.ui.FinanceUiState
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.components.resolveCategoryEmoji
import com.example.ui.theme.FinFlowTheme

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    state: FinanceUiState,
    onBack: () -> Unit,
    onAddRule: (String, Long) -> Unit,
    onToggleRule: (CategorizationRuleEntity) -> Unit,
    onDeleteRule: (CategorizationRuleEntity) -> Unit
) {
    BackHandler(onBack = onBack)
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var keyword by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf(state.categories.firstOrNull()?.id) }
        var catExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = semanticColors.surfaceElevated,
            shape = RoundedCornerShape(24.dp),
            title = { Text("New Categorization Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Merchant / Keyword (e.g. Swiggy)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("rule_keyword_input")
                    )
                    val selectedCat = state.categories.find { it.id == selectedCatId }
                    val catDisplay = selectedCat?.let {
                        "${resolveCategoryEmoji(it.iconName, it.name)} ${it.name}"
                    } ?: "Select Category"
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                        OutlinedTextField(
                            value = catDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Always Categorize As") },
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            state.categories.forEach { cat ->
                                val emoji = resolveCategoryEmoji(cat.iconName, cat.name)
                                DropdownMenuItem(
                                    text = { Text("$emoji ${cat.name} (${cat.type.name.lowercase()})") },
                                    onClick = { selectedCatId = cat.id; catExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cid = selectedCatId
                        if (keyword.isNotBlank() && cid != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddRule(keyword.trim(), cid)
                            showAddDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("rule_save_button")
                ) { Text("Save Rule") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Categorization Rules") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("rules_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("rules_add_button")) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule")
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
                .testTag("rules_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FinSurfaceCard(
                    shape = RoundedCornerShape(16.dp),
                    containerColor = semanticColors.surfaceElevated,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Rules auto-categorize new entries, SMS/UPI alerts, and CSV imports. FinFlow also learns automatically when you correct a merchant's category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(state.rules, key = { it.id }) { rule ->
                val cat = state.categories.find { it.id == rule.categoryId }
                val catColor = cat?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary
                val emoji = resolveCategoryEmoji(cat?.iconName.orEmpty(), cat?.name.orEmpty())
                FinSurfaceCard(
                    shape = RoundedCornerShape(16.dp),
                    elevation = 0.dp,
                    modifier = Modifier.testTag("rule_card_${rule.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmojiBadge(
                            emoji = emoji,
                            tintColor = catColor,
                            size = 40.dp,
                            emojiSize = 19.sp,
                            shape = RoundedCornerShape(13.dp),
                            contentDescription = cat?.name
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("\"${rule.keyword}\" → ${cat?.name ?: "Category"}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (rule.isAutoLearned) "Learned from your correction" else "Custom merchant rule",
                                style = MaterialTheme.typography.labelSmall,
                                color = semanticColors.textSecondary
                            )
                        }
                        Switch(checked = rule.isEnabled, onCheckedChange = { onToggleRule(rule) })
                        IconButton(onClick = { onDeleteRule(rule) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete rule", tint = semanticColors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
