package com.nexusai.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// NEXUS AI — QUIET LUXURY THEME SPECIFICATION
// Default: Sophisticated Warm White (#FAFAF8)
// Dark Mode: Deep Velvet Graphite (#141517)
// ============================================================================

data class NexusColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSubtle: Color,
    val userBubble: Color,
    val userBubbleBorder: Color,
    val codeBackground: Color,
    val codeHeader: Color,
    val codeBorder: Color,
    val codeText: Color,
    val statusSuccess: Color,
    val statusError: Color,
    val isDark: Boolean
)

val LocalNexusColors = staticCompositionLocalOf {
    NexusColorTokens(
        background = WarmWhiteBackground,
        surface = WarmWhiteSurface,
        surfaceElevated = WarmWhiteSurfaceElevated,
        surfaceSubtle = WarmWhiteSurfaceSubtle,
        border = WarmWhiteBorder,
        borderSubtle = WarmWhiteBorderSubtle,
        textPrimary = TextDarkPrimary,
        textSecondary = TextDarkSecondary,
        textTertiary = TextDarkTertiary,
        accent = MidnightBlue,
        accentSubtle = MidnightBlueSubtle,
        userBubble = UserBubbleLight,
        userBubbleBorder = UserBubbleLightBorder,
        codeBackground = CodeBlockBackgroundLight,
        codeHeader = CodeBlockHeaderLight,
        codeBorder = CodeBlockBorderLight,
        codeText = CodeTextLight,
        statusSuccess = CalmSuccess,
        statusError = CalmError,
        isDark = false
    )
}

private val LightTokens = NexusColorTokens(
    background = WarmWhiteBackground,
    surface = WarmWhiteSurface,
    surfaceElevated = WarmWhiteSurfaceElevated,
    surfaceSubtle = WarmWhiteSurfaceSubtle,
    border = WarmWhiteBorder,
    borderSubtle = WarmWhiteBorderSubtle,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textTertiary = TextDarkTertiary,
    accent = MidnightBlue,
    accentSubtle = MidnightBlueSubtle,
    userBubble = UserBubbleLight,
    userBubbleBorder = UserBubbleLightBorder,
    codeBackground = CodeBlockBackgroundLight,
    codeHeader = CodeBlockHeaderLight,
    codeBorder = CodeBlockBorderLight,
    codeText = CodeTextLight,
    statusSuccess = CalmSuccess,
    statusError = CalmError,
    isDark = false
)

private val DarkTokens = NexusColorTokens(
    background = GraphiteBackground,
    surface = GraphiteSurface,
    surfaceElevated = GraphiteSurfaceElevated,
    surfaceSubtle = GraphiteSurfaceSubtle,
    border = GraphiteBorder,
    borderSubtle = GraphiteBorderSubtle,
    textPrimary = TextLightPrimary,
    textSecondary = TextLightSecondary,
    textTertiary = TextLightTertiary,
    accent = MidnightBlueDarkAccent,
    accentSubtle = MidnightBlueDarkSurface,
    userBubble = UserBubbleDark,
    userBubbleBorder = UserBubbleDarkBorder,
    codeBackground = CodeBlockBackgroundDark,
    codeHeader = CodeBlockHeaderDark,
    codeBorder = CodeBlockBorderDark,
    codeText = CodeTextDark,
    statusSuccess = CalmSuccessDark,
    statusError = CalmErrorDark,
    isDark = true
)

private val MaterialLightScheme = lightColorScheme(
    primary = MidnightBlue,
    onPrimary = Color.White,
    primaryContainer = MidnightBlueSubtle,
    onPrimaryContainer = MidnightBlue,
    background = WarmWhiteBackground,
    onBackground = TextDarkPrimary,
    surface = WarmWhiteSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = WarmWhiteSurfaceSubtle,
    onSurfaceVariant = TextDarkSecondary,
    outline = WarmWhiteBorder,
    error = CalmError,
    onError = Color.White
)

private val MaterialDarkScheme = darkColorScheme(
    primary = MidnightBlueDarkAccent,
    onPrimary = GraphiteBackground,
    primaryContainer = MidnightBlueDarkSurface,
    onPrimaryContainer = TextLightPrimary,
    background = GraphiteBackground,
    onBackground = TextLightPrimary,
    surface = GraphiteSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = GraphiteSurfaceSubtle,
    onSurfaceVariant = TextLightSecondary,
    outline = GraphiteBorder,
    error = CalmErrorDark,
    onError = Color.White
)

@Composable
fun NexusAITheme(
    darkTheme: Boolean = false, // Warm white is the flagship default
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    val colorScheme = if (darkTheme) MaterialDarkScheme else MaterialLightScheme

    CompositionLocalProvider(LocalNexusColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexusTypography,
            content = content
        )
    }
}
