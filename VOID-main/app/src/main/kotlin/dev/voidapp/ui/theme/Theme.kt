package dev.voidapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.voidapp.R

/** Brand red, lifted from icon/void_icon.svg. */
val VoidRed = Color(0xFFC10520)

/** "Phantom" near-blacks: the app never uses a greyish-blue dark. */
val Phantom = Color(0xFF0A0A0B)
val PhantomSurface = Color(0xFF141416)
val PhantomRaised = Color(0xFF1C1C1F)
val PhantomOutline = Color(0xFF2E2E33)
val PhantomText = Color(0xFFF5F5F5)
val PhantomMuted = Color(0xFF9C9CA3)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private fun poppinsTypography(): Typography {
    val base = Typography()
    fun TextStyle.poppins() = copy(fontFamily = Poppins)
    return Typography(
        displayLarge = base.displayLarge.poppins(),
        displayMedium = base.displayMedium.poppins(),
        displaySmall = base.displaySmall.poppins(),
        headlineLarge = base.headlineLarge.poppins(),
        headlineMedium = base.headlineMedium.poppins(),
        headlineSmall = base.headlineSmall.poppins(),
        titleLarge = base.titleLarge.poppins(),
        titleMedium = base.titleMedium.poppins(),
        titleSmall = base.titleSmall.poppins(),
        bodyLarge = base.bodyLarge.poppins(),
        bodyMedium = base.bodyMedium.poppins(),
        bodySmall = base.bodySmall.poppins(),
        labelLarge = base.labelLarge.poppins(),
        labelMedium = base.labelMedium.poppins(),
        labelSmall = base.labelSmall.poppins(),
    )
}

/**
 * Light theme: plain white page, near-black text, white boxes with a hairline.
 * Dark theme: "phantom" blacks (never grey-blue), white boxes turn black.
 */
private val LightColors = lightColorScheme(
    primary = VoidRed,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Phantom,
    surface = Color.White,
    onSurface = Phantom,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF6B6B70),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFF7F7F8),
    outline = Color(0xFFE2E2E4),
)

private val DarkColors = darkColorScheme(
    primary = VoidRed,
    onPrimary = Color.White,
    background = Phantom,
    onBackground = PhantomText,
    surface = Phantom,
    onSurface = PhantomText,
    surfaceVariant = PhantomRaised,
    onSurfaceVariant = PhantomMuted,
    surfaceContainer = PhantomSurface,
    surfaceContainerHigh = PhantomSurface,
    surfaceContainerHighest = PhantomRaised,
    outline = PhantomOutline,
)

@Composable
fun VoidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = poppinsTypography(),
        content = content,
    )
}
