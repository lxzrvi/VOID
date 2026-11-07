package dev.voidapp.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.data.PageEntry
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

/** Full history and bookmarks: open, delete one, or clear all. */
@Composable
fun LibraryScreen(state: VoidAppState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val entries = if (state.libraryTab == 0) state.history.toList() else state.bookmarks.toList()

    BackHandler(enabled = true) { state.libraryOpen = false }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_title),
                color = colors.onBackground,
                fontFamily = Poppins,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (state.libraryTab == 0 && entries.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.library_clear),
                    color = VoidRed,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { state.clearHistoryOnly() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.5.dp, colors.outline, CircleShape)
                    .clickable { state.libraryOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.X, null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LibraryPill(stringResource(R.string.library_history), state.libraryTab == 0) { state.libraryTab = 0 }
            LibraryPill(stringResource(R.string.library_bookmarks), state.libraryTab == 1) { state.libraryTab = 1 }
        }
        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.library_empty),
                    color = colors.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries, key = { it.url + it.timestamp }) { entry ->
                    LibraryRow(
                        entry = entry,
                        onOpen = {
                            state.libraryOpen = false
                            state.open(entry.url)
                        },
                        onDelete = {
                            if (state.libraryTab == 0) state.removeHistory(entry) else state.removeBookmark(entry)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) VoidRed else colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) androidx.compose.ui.graphics.Color.White else colors.onSurface,
            fontFamily = Poppins,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun LibraryRow(entry: PageEntry, onOpen: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(
            faviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=${entry.host}",
            contentDescription = entry.label,
            size = 34.dp,
            circleColor = colors.surfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                color = colors.onSurface,
                fontFamily = Poppins,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.host,
                color = colors.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.X, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
        }
    }
}
