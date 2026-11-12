package dev.voidapp.ui.proto

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * One place that decides how everything moves. Style 0 is bouncy and wobbly, style 1
 * is smooth with no overshoot, style 2 switches animations off. [speed] scales the
 * spring stiffness (higher = quicker).
 */
@Immutable
class Motion(val style: Int, val speed: Float) {
    val instant: Boolean get() = style == 2

    /** Layout changes: widths, heights, sheets. */
    fun <T> soft(): FiniteAnimationSpec<T> = when (style) {
        2 -> snap()
        1 -> spring(dampingRatio = 0.92f, stiffness = 420f * speed)
        else -> spring(dampingRatio = 0.6f, stiffness = 320f * speed)
    }

    /** Small things: icons, chips, presses. A little livelier. */
    fun <T> pop(): FiniteAnimationSpec<T> = when (style) {
        2 -> snap()
        1 -> spring(dampingRatio = 0.85f, stiffness = 650f * speed)
        else -> spring(dampingRatio = 0.5f, stiffness = 520f * speed)
    }

    /** Whole screens sliding in and out. */
    fun <T> screen(): FiniteAnimationSpec<T> = when (style) {
        2 -> snap()
        1 -> spring(dampingRatio = 0.95f, stiffness = 380f * speed)
        else -> spring(dampingRatio = 0.72f, stiffness = 300f * speed)
    }
}

@Composable
fun rememberMotion(): Motion {
    val ui = LocalUiPrefs.current
    val style = ui?.animStyle ?: 0
    val speed = ui?.animSpeed ?: 1f
    return remember(style, speed) { Motion(style, speed) }
}

/** The live-blur source: the page. Bars blur whatever is under them. */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * A frosted surface: clipped to [shape], live blur of what is behind it (Haze) when
 * blur is on, then [tint] over it. With blur off it is just the tint.
 */
@Composable
fun Modifier.frosted(shape: Shape, tint: Color): Modifier {
    val haze = LocalHaze.current
    val ui = LocalUiPrefs.current
    val radius = ui?.blurRadius ?: 0f
    val noise = ui?.blurNoise ?: 0f
    val enabled = ui?.blurEnabled ?: false
    var result = this.clip(shape)
    if (haze != null && enabled && radius > 0.5f) {
        result = result.hazeEffect(state = haze) {
            blurRadius = radius.dp
            noiseFactor = noise
        }
    }
    return result.background(tint)
}
