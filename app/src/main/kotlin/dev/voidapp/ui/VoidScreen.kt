package dev.voidapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.voidapp.ui.components.VoidSearchIsland

/** Placeholder result set so the counter has something to count. */
private val Results = listOf("Void", "Vision", "Vector", "Vortex", "Vertex", "Valley", "Vault", "Voyage", "Verve")

/**
 * One page, nothing else: a blank surface with the floating island in the
 * bottom-right corner.
 */
@Composable
fun VoidScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        VoidSearchIsland(results = Results)
    }
}
