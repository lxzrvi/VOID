package dev.voidapp.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The Lucide glyphs this app draws.
 *
 * Path data is taken 1:1 from the Lucide sources in
 * https://github.com/thelacspace/lucide-compose (v1.16.0), including its
 * `lucidePath` conventions (24x24 viewport, round caps and joins, stroke only).
 *
 * They are inlined here because the published
 * `io.github.thelacspace:lucide-compose:1.16.0` artifact on Maven Central only
 * contains the generator's test icons (Triangle, TriangleAlert, TriangleDashed,
 * Trophy) instead of the generated set, so `LucideIcons.All.ArrowLeft` and
 * friends do not exist at compile time. Once a full release is out, replace the
 * `by lazy` bodies with `lucide-compose`'s composable properties - the call
 * sites (`Lucide.ArrowLeft`, ...) do not change.
 */
object Lucide {
    /**
     * Bold feel at 17-18dp: Lucide's default stroke is 2f, these are drawn at
     * 2.5f so the pill glyphs don't look hairline on a phone screen.
     */
    private const val BoldStrokeWidth = 2.5f

    val ArrowLeft: ImageVector by lazy {
        lucideIcon("ArrowLeft") {
            lucidePath {
                moveToRelative(12f, 19f)
                lineToRelative(-7f, -7f)
                lineToRelative(7f, -7f)
            }
            lucidePath {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
            }
        }
    }

    val ArrowRight: ImageVector by lazy {
        lucideIcon("ArrowRight") {
            lucidePath {
                moveTo(5f, 12f)
                horizontalLineToRelative(14f)
            }
            lucidePath {
                moveToRelative(12f, 5f)
                lineToRelative(7f, 7f)
                lineToRelative(-7f, 7f)
            }
        }
    }

    /** Lucide's three dots (the old "more-horizontal" icon). */
    val Ellipsis: ImageVector by lazy {
        lucideIcon("Ellipsis") {
            dot(11f)
            dot(18f)
            dot(4f)
        }
    }

    val X: ImageVector by lazy {
        lucideIcon("X") {
            lucidePath {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
            lucidePath {
                moveToRelative(6f, 6f)
                lineToRelative(12f, 12f)
            }
        }
    }

    /** One 1r circle at [x], 12 - the shape Lucide uses for each dot. */
    private fun ImageVector.Builder.dot(x: Float) = lucidePath {
        moveTo(x, 12f)
        arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, 2f, 0f)
        arcToRelative(1f, 1f, 0f, isMoreThanHalf = true, isPositiveArc = false, -2f, 0f)
    }

    private fun ImageVector.Builder.lucidePath(pathBuilder: PathBuilder.() -> Unit) = path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = BoldStrokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )

    private fun lucideIcon(
        name: String,
        paths: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(paths).build()
}
