package dev.voidapp.ui.downloads

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.protoColors
import dev.voidapp.ui.proto.rememberMotion
import dev.voidapp.ui.theme.Poppins
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** The file-format icon for a name: a document, an archive, a picture... */
fun fileIconFor(name: String): ImageVector = when (extensionOf(name)) {
    "pdf" -> Icons.Filled.PictureAsPdf
    "zip", "rar", "7z", "tar", "gz", "xz", "bz2" -> Icons.Filled.FolderZip
    "apk", "aab", "xapk" -> Icons.Filled.Android
    "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "avif" -> Icons.Filled.Image
    "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv" -> Icons.Filled.Movie
    "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus" -> Icons.Filled.MusicNote
    "xls", "xlsx", "csv", "ods" -> Icons.Filled.TableChart
    "ppt", "pptx", "odp" -> Icons.Filled.Slideshow
    else -> Icons.Filled.Description
}

fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

/**
 * After a download is confirmed: a tile with the file's format icon pops up where the
 * box was, then arcs down and merges into the bottom circle, which pulses. The circle
 * then shows the progress ring.
 */
@Composable
fun FlyingFileOverlay(state: VoidAppState) {
    val file = state.flyingFile ?: return
    val c = protoColors()
    val motion = rememberMotion()
    val density = LocalDensity.current
    val pop = remember(file.id) { Animatable(0f) }
    val flight = remember(file.id) { Animatable(0f) }
    val half = with(density) { 32.dp.toPx() }
    val arc = with(density) { 90.dp.toPx() }

    LaunchedEffect(file.id) {
        pop.animateTo(1f, motion.pop())
        delay(if (motion.instant) 0 else 220)
        flight.animateTo(1f, if (motion.instant) tween(1) else tween(640, easing = FastOutSlowInEasing))
        state.downloadPulse++
        state.flyingFile = null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Box(
            modifier = Modifier
                .offset {
                    val start = Offset(w / 2f, h * 0.46f)
                    val end = state.downloadTarget ?: Offset(w * 0.88f, h * 0.94f)
                    val p = flight.value
                    val x = lerp(start.x, end.x, p)
                    val y = lerp(start.y, end.y, p) - (sin(PI * p) * arc).toFloat()
                    IntOffset((x - half).roundToInt(), (y - half).roundToInt())
                }
                .size(64.dp)
                .graphicsLayer {
                    val s = lerp(1f, 0.42f, flight.value) * pop.value
                    scaleX = s
                    scaleY = s
                    alpha = 1f - 0.55f * flight.value
                }
                .clip(RoundedCornerShape(20.dp))
                .background(c.ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(fileIconFor(file.name), null, tint = c.page, modifier = Modifier.size(30.dp))
            Text(
                text = extensionOf(file.name).take(4).uppercase(),
                color = c.page.copy(alpha = 0.8f),
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.sp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-5).dp),
            )
        }
    }
}
