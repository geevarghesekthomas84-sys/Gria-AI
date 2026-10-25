package com.nexusai.app.core.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// NEXUS AI — QUIET LUXURY + MODERN MINIMALISM DESIGN TOKENS
// Inspired by Apple native apps, Claude editorial typography, and ChatGPT purity.
// ============================================================================

// --- Light Theme (Default: Sophisticated Warm White & Soft Neutrals) ---
val WarmWhiteBackground = Color(0xFFFAFAF8)
val WarmWhiteSurface = Color(0xFFF6F6F3)
val WarmWhiteSurfaceElevated = Color(0xFFFFFFFF)
val WarmWhiteSurfaceSubtle = Color(0xFFF1F1EE)
val WarmWhiteBorder = Color(0xFFE5E5DF)
val WarmWhiteBorderSubtle = Color(0xFFEBEBE6)

// Text (Light)
val TextDarkPrimary = Color(0xFF202124)       // Near-black, high legibility
val TextDarkSecondary = Color(0xFF5F6368)     // Calm editorial gray
val TextDarkTertiary = Color(0xFF8C9199)      // Subtle metadata / hint
val TextDarkDisabled = Color(0xFFB8BCC2)

// Midnight Blue Accent (Restrained, mature, intentional)
val MidnightBlue = Color(0xFF344B70)
val MidnightBlueHover = Color(0xFF2B3E5C)
val MidnightBlueSubtle = Color(0xFFEEF2F8)
val MidnightBlueBorder = Color(0xFFCBD6E6)

// User Bubble Tint (Warm light neutral bubble)
val UserBubbleLight = Color(0xFFEDECE8)
val UserBubbleLightBorder = Color(0xFFE2E1DC)

// Code Blocks (Light Theme: Clean Warm Paper Code Slate)
val CodeBlockBackgroundLight = Color(0xFFF4F4F0)
val CodeBlockHeaderLight = Color(0xFFEBEBE5)
val CodeBlockBorderLight = Color(0xFFDFDFD8)
val CodeTextLight = Color(0xFF1F2328)

// --- Dark Theme (Deep Graphite & Obsidian Velvet — NEVER Pure Black) ---
val GraphiteBackground = Color(0xFF141517)
val GraphiteSurface = Color(0xFF1A1B1E)
val GraphiteSurfaceElevated = Color(0xFF212327)
val GraphiteSurfaceSubtle = Color(0xFF1E2024)
val GraphiteBorder = Color(0xFF2C2E33)
val GraphiteBorderSubtle = Color(0xFF24262A)

// Text (Dark)
val TextLightPrimary = Color(0xFFEDEDEC)
val TextLightSecondary = Color(0xFF9CA0A6)
val TextLightTertiary = Color(0xFF6B6F76)

// Accent in Dark Mode (Soft Ice Steel Blue)
val MidnightBlueDarkAccent = Color(0xFF8CA5CA)
val MidnightBlueDarkSurface = Color(0xFF1C222E)
val MidnightBlueDarkBorder = Color(0xFF2F3C52)

// User Bubble Tint (Dark Theme)
val UserBubbleDark = Color(0xFF26282D)
val UserBubbleDarkBorder = Color(0xFF32353C)

// Code Blocks (Dark Theme)
val CodeBlockBackgroundDark = Color(0xFF111214)
val CodeBlockHeaderDark = Color(0xFF18191D)
val CodeBlockBorderDark = Color(0xFF26282E)
val CodeTextDark = Color(0xFFE1E4EA)

// Status Colors (Restrained, muted, non-neon)
val CalmSuccess = Color(0xFF3D7A58)
val CalmSuccessDark = Color(0xFF66BB87)
val CalmWarning = Color(0xFF9E6E2E)
val CalmWarningDark = Color(0xFFE5A955)
val CalmError = Color(0xFFA63D3D)
val CalmErrorDark = Color(0xFFE56A6A)
val CalmOffline = Color(0xFF777B80)

// Semantic Theme Mappings
val PrimaryAccentLight = MidnightBlue
val PrimaryAccentDark = MidnightBlueDarkAccent
