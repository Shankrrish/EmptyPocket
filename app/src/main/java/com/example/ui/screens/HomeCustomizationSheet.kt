package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HomeCardConfig
import com.example.data.model.HomeCardId
import com.example.ui.components.EmojiBadge
import com.example.ui.components.FinSurfaceCard
import com.example.ui.theme.FinFlowTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomizationSheet(
    cards: List<HomeCardConfig>,
    onMove: (Int, Int) -> Unit,
    onToggleVisibility: (HomeCardId) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val semanticColors = FinFlowTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = semanticColors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Customize Homepage", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Reorder cards and choose what appears on your home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = semanticColors.textSecondary
                    )
                }
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetDefault()
                    },
                    modifier = Modifier.testTag("reset_home_layout_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Default")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(cards, key = { _, item -> item.id.name }) { index, item ->
                    val cardEmoji = when (item.id) {
                        HomeCardId.SUMMARY -> "🌿"
                        HomeCardId.ACCOUNTS -> "🏦"
                        HomeCardId.CREDIT_CARDS -> "💳"
                        HomeCardId.BUDGETS -> "🎯"
                        HomeCardId.SAFE_TO_SPEND -> "🛡️"
                        HomeCardId.INSIGHTS -> "💡"
                        HomeCardId.CATEGORY_CHART -> "🍩"
                    }
                    FinSurfaceCard(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 0.dp,
                        containerColor = if (item.visible) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customize_home_row_${item.id.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmojiBadge(
                                emoji = cardEmoji,
                                tintColor = if (item.visible) MaterialTheme.colorScheme.primary else semanticColors.textSecondary,
                                size = 36.dp,
                                emojiSize = 17.sp,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.id.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.id.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semanticColors.textSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMove(index, -1)
                                },
                                enabled = index > 0,
                                modifier = Modifier.testTag("move_up_${item.id.name.lowercase()}")
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move ${item.id.title} up")
                            }
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onMove(index, 1)
                                },
                                enabled = index < cards.lastIndex,
                                modifier = Modifier.testTag("move_down_${item.id.name.lowercase()}")
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move ${item.id.title} down")
                            }
                            Switch(
                                checked = item.visible,
                                onCheckedChange = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleVisibility(item.id)
                                },
                                modifier = Modifier.testTag("toggle_home_${item.id.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }
}
