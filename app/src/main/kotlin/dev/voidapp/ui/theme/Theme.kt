package dev.voidapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Brand red lifted from icon/void_icon.svg. */
val VoidRed = Color(0xFFC10520)

private val LightColors = lightColorScheme(
    primary = VoidRed,
    onPrimary = Color.White,
    // The page stays blank white; the grown island is the only raised surface,
    // so it gets a light grey container + a hairline border to read against it.
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF5C5C66),
    surfaceContainer = Color(0xFFF6F6F8),
    surfaceContainerHigh = Color(0xFFEDEDF1),
    surfaceContainerHighest = Color(0xFFE4E4EA),
    outline = Color(0xFFD3D3DA),
)

private val DarkColors = darkColorScheme(
    primary = VoidRed,
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1F),
    onSurfaceVariant = Color(0xFF9A9AA5),
    surfaceContainer = Color(0xFF121216),
    surfaceContainerHigh = Color(0xFF1C1C22),
    surfaceContainerHighest = Color(0xFF26262E),
    outline = Color(0xFF3A3A44),
)

/**
 * Follows the system light/dark setting and draws edge to edge: the whole
 * screen is just a plain surface with the island floating on top of it.
 */
@Composable
fun VoidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
