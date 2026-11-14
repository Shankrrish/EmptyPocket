package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.CategoryType
import com.example.ui.theme.FinFlowTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryCardGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onSelectCategory: (CategoryEntity) -> Unit,
    onRequestCustomCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = FinFlowTheme.colors
    val haptics = LocalHapticFeedback.current
    val grouped = remember(categories) {
        categories.groupBy { categorySectionLabel(it) }
    }
    val orderedSections = listOf("Essentials & Living", "Lifestyle & Growth", "Income Sources", "Custom & Personal")
        .filter { grouped.containsKey(it) } +
        grouped.keys.filter { it !in setOf("Essentials & Living", "Lifestyle & Growth", "Income Sources", "Custom & Personal") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        orderedSections.forEachIndexed { sectionIndex, sectionTitle ->
            val itemsInSection = grouped[sectionTitle].orEmpty()
            val showCustomTile = sectionIndex == orderedSections.lastIndex
            val totalCells = itemsInSection.size + if (showCustomTile) 1 else 0
            val rowCount = (totalCells + 3) / 4

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = semanticColors.textSecondary,
                    modifier = Modifier.padding(start = 2.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (rowIdx in 0 until rowCount) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (colIdx in 0 until 4) {
                                val cellIndex = rowIdx * 4 + colIdx
                                val cat = itemsInSection.getOrNull(cellIndex)
                                if (cat != null) {
                                    val isSelected = selectedCategoryId == cat.id
                                    val catColor = Color(cat.colorHex)
                                    val emoji = resolveCategoryEmoji(cat.iconName, cat.name)
                                    val (pastelBg, pastelBorder) = pastelTileColors(
                                        emoji = emoji,
                                        fallbackTint = catColor,
                                        isAmoled = semanticColors.isAmoledDark
                                    )
                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.03f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "catScale_${cat.id}"
                                    )
                                    val tileBg by animateColorAsState(
                                        targetValue = pastelBg,
                                        label = "catBg_${cat.id}"
                                    )
                                    val borderColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else pastelBorder,
                                        label = "catBorder_${cat.id}"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = tileBg,
                                        tonalElevation = 0.dp,
                                        shadowElevation = 0.dp,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 0.75.dp,
                                            color = borderColor
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onSelectCategory(cat)
                                            }
                                            .testTag("sheet_category_${cat.name.lowercase()}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 12.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = emoji,
                                                    fontSize = 26.sp,
                                                    lineHeight = 28.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = cat.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = Color(0xFF1A1E15),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(16.dp)
                                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (showCustomTile && cellIndex == itemsInSection.size) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onRequestCustomCategory()
                                            }
                                            .testTag("add_custom_category_button")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp, vertical = 12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Create Custom Category",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Custom",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (orderedSections.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onRequestCustomCategory)
                    .testTag("add_custom_category_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCategoryDialog(
    defaultType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: CategoryType, emoji: String, colorHex: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(defaultType) }
    var selectedEmoji by remember { mutableStateOf(if (defaultType == CategoryType.INCOME) "💰" else "☕") }
    var selectedColor by remember { mutableLongStateOf(CategoryAccentColors.first()) }
    var nameError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FinFlowTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create Custom Category", style = MaterialTheme.typography.titleLarge)

            // Live Category Card Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(selectedColor).copy(alpha = 0.12f),
                border = BorderStroke(1.2.dp, Color(selectedColor).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    EmojiBadge(
                        emoji = selectedEmoji.ifBlank { "✨" },
                        tintColor = Color(selectedColor),
                        size = 40.dp,
                        emojiSize = 20.sp,
                        selected = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name.ifBlank { "Category Preview" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (type == CategoryType.INCOME) "Income Category" else "Expense Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinFlowTheme.colors.textSecondary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(if (t == CategoryType.EXPENSE) "Expense" else "Income") },
                        modifier = Modifier.testTag("custom_cat_type_${t.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Category Name (e.g. Coffee, Pet Care)") },
                isError = nameError,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_category_name_input")
            )

            OutlinedTextField(
                value = selectedEmoji,
                onValueChange = { input ->
                    if (input.length <= 6) selectedEmoji = input
                },
                label = { Text("Custom Emoji (pick below or type any)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_category_emoji_input")
            )

            Text("Emoji Palette", style = MaterialTheme.typography.labelLarge)
            CategoryEmojiGroups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = FinFlowTheme.colors.textSecondary
                    )
                    group.emojis.chunked(6).forEach { rowEmojis ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowEmojis.forEach { emoji ->
                                val isPicked = selectedEmoji == emoji
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isPicked) Color(selectedColor).copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        )
                                        .border(
                                            width = if (isPicked) 1.5.dp else 0.dp,
                                            color = if (isPicked) Color(selectedColor) else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedEmoji = emoji
                                        }
                                        .testTag("emoji_pick_$emoji")
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            Text("Accent Color", style = MaterialTheme.typography.labelLarge)
            CategoryAccentColors.chunked(6).forEach { colorRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorRow.forEach { hex ->
                        val picked = selectedColor == hex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex), CircleShape)
                                .border(
                                    width = if (picked) 2.5.dp else 0.dp,
                                    color = if (picked) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedColor = hex
                                }
                        ) {
                            if (picked) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            name.trim(),
                            type,
                            selectedEmoji.trim().ifBlank { "✨" },
                            selectedColor
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_custom_category_button")
                ) {
                    Text("Create")
                }
            }
        }
    }
}
