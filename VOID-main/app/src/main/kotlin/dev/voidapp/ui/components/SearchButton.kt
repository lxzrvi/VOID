package dev.voidapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.VoidRed

/**
 * The search button, in one place so home and the island always look the same:
 * a bare magnifier glyph with **no circle behind it**, whose tint is the theme's
 * on-surface colour, and with the void mark sitting inside the lens.
 */
@Composable
fun SearchButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Lucide.Search,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.fillMaxSize(),
        )
        // The mark inside the lens, offset a touch up-left like a real glass.
        AppMark(
            color = VoidRed,
            modifier = Modifier
                .size(size * 0.30f)
                .offset(x = (-size * 0.045f), y = (-size * 0.045f)),
        )
    }
}
