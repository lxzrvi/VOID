package dev.voidapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import dev.voidapp.R

/**
 * The app mark (icon/void_inner_icon.svg, exported as a VectorDrawable), tinted
 * to [color] - white by default, which is how it reads on the brand red circle
 * and in the launcher icon.
 */
@Composable
fun AppMark(modifier: Modifier = Modifier, color: Color = Color.White) {
    Image(
        painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.void_inner_icon)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier,
    )
}
