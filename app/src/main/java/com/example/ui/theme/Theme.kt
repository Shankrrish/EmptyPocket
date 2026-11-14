package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppFontStyle
import com.example.data.model.AppThemeMode

@Immutable
data class FinFlowSemanticColors(
    val isAmoledDark: Boolean,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val cardPayment: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val heroBorder: Color,
    val cardSheenStart: Color,
    val cardSheenEnd: Color,
    val bevelTopHighlight: Color,
    val bevelSideBorder: Color,
    val bevelBottomShadow: Color,
    val popupSurfaceStart: Color,
    val popupSurfaceEnd: Color,
    val tileRaisedStart: Color,
    val tileRaisedEnd: Color,
    val scrimColor: Color,
    val accentCtaStart: Color,
    val accentCtaEnd: Color,
    val textSecondary: Color,
    val navInactive: Color,
    val divider: Color,
    val hairlineBorder: Color,
    val surfaceElevated: Color,
    val sageGlow: Color
)

private val LightSemanticColors = FinFlowSemanticColors(
    isAmoledDark = false,
    income = OliveIncome,
    incomeContainer = OliveIncomeSoft,
    expense = TerracottaExpense,
    expenseContainer = TerracottaExpenseSoft,
    warning = OchreWarning,
    warningContainer = OchreWarningSoft,
    cardPayment = CardSlateTeal,
    heroStart = LightSurface,
    heroEnd = LightSurfaceElevated,
    heroGradientStart = LightHeroGradientStart,
    heroGradientEnd = LightHeroGradientEnd,
    heroBorder = LightHeroBorder,
    cardSheenStart = LightCardSheenStart,
    cardSheenEnd = LightCardSheenEnd,
    bevelTopHighlight = LightBevelTopHighlight,
    bevelSideBorder = LightBevelSideBorder,
    bevelBottomShadow = LightBevelBottomShadow,
    popupSurfaceStart = LightPopupSurfaceStart,
    popupSurfaceEnd = LightPopupSurfaceEnd,
    tileRaisedStart = LightTileRaisedStart,
    tileRaisedEnd = LightTileRaisedEnd,
    scrimColor = LightScrim,
    accentCtaStart = OlivePrimary,
    accentCtaEnd = OliveDrab,
    textSecondary = LightTextSecondary,
    navInactive = LightNavInactive,
    divider = LightOutline,
    hairlineBorder = LightOutline,
    surfaceElevated = LightSurfaceElevated,
    sageGlow = SageAccent.copy(alpha = 0.16f)
)

private val AmoledDarkSemanticColors = FinFlowSemanticColors(
    isAmoledDark = true,
    income = AmoledIncome,
    incomeContainer = AmoledIncome.copy(alpha = 0.15f),
    expense = AmoledExpense,
    expenseContainer = AmoledExpense.copy(alpha = 0.15f),
    warning = AmoledWarning,
    warningContainer = AmoledWarning.copy(alpha = 0.16f),
    cardPayment = AmoledCardTeal,
    heroStart = AmoledSurface,
    heroEnd = AmoledSurfaceElevated,
    heroGradientStart = AmoledHeroGradientStart,
    heroGradientEnd = AmoledHeroGradientEnd,
    heroBorder = AmoledHeroBorder,
    cardSheenStart = AmoledCardSheenStart,
    cardSheenEnd = AmoledCardSheenEnd,
    bevelTopHighlight = AmoledBevelTopHighlight,
    bevelSideBorder = AmoledBevelSideBorder,
    bevelBottomShadow = AmoledBevelBottomShadow,
    popupSurfaceStart = AmoledPopupSurfaceStart,
    popupSurfaceEnd = AmoledPopupSurfaceEnd,
    tileRaisedStart = AmoledTileRaisedStart,
    tileRaisedEnd = AmoledTileRaisedEnd,
    scrimColor = AmoledScrim,
    accentCtaStart = OlivePrimary,
    accentCtaEnd = OliveDrab,
    textSecondary = AmoledTextSecondary,
    navInactive = AmoledNavInactive,
    divider = AmoledOutline,
    hairlineBorder = AmoledOutline,
    surfaceElevated = AmoledSurfaceElevated,
    sageGlow = AmoledOlivePrimary.copy(alpha = 0.12f)
)

val LocalFinFlowColors = staticCompositionLocalOf { AmoledDarkSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = OlivePrimary,
    onPrimary = Color.White,
    primaryContainer = OliveSoft,
    onPrimaryContainer = OliveDeep,
    secondary = OliveDrab,
    onSecondary = Color.White,
    secondaryContainer = SageSoft,
    onSecondaryContainer = OliveDeep,
    tertiary = SageAccent,
    onTertiary = OliveDeep,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = TerracottaExpense,
    onError = Color.White
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = AmoledOlivePrimary,
    onPrimary = Color(0xFF10160A),
    primaryContainer = AmoledOliveContainer,
    onPrimaryContainer = Color(0xFFD5E6B8),
    secondary = AmoledSageSecondary,
    onSecondary = Color(0xFF0E150C),
    secondaryContainer = AmoledSurfaceVariant,
    onSecondaryContainer = AmoledTextPrimary,
    tertiary = SageAccent,
    onTertiary = OliveDeep,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledOutline,
    error = AmoledExpense,
    onError = Color(0xFF1A0908)
)

object FinFlowTheme {
    val colors: FinFlowSemanticColors
        @Composable get() = LocalFinFlowColors.current
}

@Composable
fun FinFlowTheme(
    themeMode: AppThemeMode = AppThemeMode.AMOLED_DARK,
    fontStyle: AppFontStyle = AppFontStyle.JAKARTA_SANS,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.AMOLED_DARK -> true
        AppThemeMode.CRISP_LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmoledDarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) AmoledDarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalFinFlowColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(fontStyle),
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
