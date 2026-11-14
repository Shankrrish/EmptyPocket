package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.EmojiBadge
import com.example.ui.theme.FinFlowTheme
import com.example.util.SecurityUtils

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canBiometric = remember(context) { SecurityUtils.canUseBiometrics(context) }
    val haptics = LocalHapticFeedback.current
    val semanticColors = FinFlowTheme.colors

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val triggerBiometrics: () -> Unit = {
        if (activity != null && biometricEnabled && canBiometric) {
            SecurityUtils.launchBiometricPrompt(
                activity = activity,
                onSuccess = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBiometricSuccess()
                },
                onUsePin = {},
                onError = { msg -> errorMessage = msg }
            )
        }
    }

    LaunchedEffect(biometricEnabled, canBiometric) {
        if (biometricEnabled && canBiometric) {
            triggerBiometrics()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 32.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmojiBadge(
                emoji = "🔐",
                tintColor = MaterialTheme.colorScheme.primary,
                size = 68.dp,
                emojiSize = 30.sp,
                shape = RoundedCornerShape(22.dp),
                contentDescription = "App Locked"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("MoneyTracker is Locked", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter your 4-digit PIN to access your balances",
                style = MaterialTheme.typography.bodyMedium,
                color = semanticColors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = semanticColors.expense,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = biometricEnabled && canBiometric) {
                                                triggerBiometrics()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (biometricEnabled && canBiometric) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Unlock with Biometrics",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                            .testTag("pin_backspace"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.75.dp, semanticColors.hairlineBorder),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (enteredPin.length < 4) {
                                                    val next = enteredPin + key
                                                    enteredPin = next
                                                    errorMessage = null
                                                    if (next.length == 4) {
                                                        val ok = onVerifyPin(next)
                                                        if (!ok) {
                                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            errorMessage = "Incorrect PIN. Try again."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                            .testTag("pin_key_$key")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (biometricEnabled && canBiometric) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = triggerBiometrics,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("biometric_prompt_button")
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometrics")
                }
            }
        }
    }
}
