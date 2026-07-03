package `in`.firm.consultancy.bayaan.cardfit.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Bayaan "Clear Voice" palette (design-system/tokens/colors.css). Three families: Midnight (indigo,
 * authority), Teal (the one bright signal), Sage (the calm second voice), over paper/white neutrals.
 */

// Neutrals
val Ink = Color(0xFF1A1C2E)
val Paper = Color(0xFFF4F6FB)

// Midnight — indigo, authority
val Midnight50 = Color(0xFFEEF0F8)
val Midnight100 = Color(0xFFD9DCEC)
val Midnight200 = Color(0xFFB9BEDC)
val Midnight300 = Color(0xFF8E96C4)
val Midnight600 = Color(0xFF2A2F6B)
val Midnight700 = Color(0xFF23275B)
val Midnight800 = Color(0xFF1E2150)
val Midnight900 = Color(0xFF16183A)

// Teal — the clear signal (primary accent)
val Teal300 = Color(0xFF5DCAA5)
val Teal400 = Color(0xFF2DC7B5)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0E9384)
val Teal700 = Color(0xFF0B7568)

// Sage — the calm second voice (supporting)
val Sage300 = Color(0xFFBACBBC)
val Sage400 = Color(0xFF93AC99)
val Sage500 = Color(0xFF6B8A74)
val Sage600 = Color(0xFF4F6B59)
val Sage700 = Color(0xFF3E5648)

// Semantic aliases
val TextHeading = Midnight800
val TextBody = Color(0xE62A2F6B) // midnight-600 @ 90%
val TextMuted = Color(0xB32A2F6B) // midnight-600 @ 70%
val AccentSoft = Color(0x1A14B8A6) // teal-500 @ 10%
val SageSoft = Color(0x266B8A74) // sage-500 @ 15%
val BorderSubtle = Midnight100
val BorderHover = Midnight200

// Error (the design system inherits Material's error red, e.g. the dialog bounds hint)
val ErrorRed = Color(0xFFB3261E)
