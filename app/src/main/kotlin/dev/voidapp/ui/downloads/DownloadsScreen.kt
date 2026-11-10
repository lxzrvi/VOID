package dev.voidapp.ui.downloads

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.data.DownloadItem
import dev.voidapp.data.DownloadStatus
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.proto.protoColors
import dev.voidapp.ui.theme.Poppins
import kotlin.math.roundToInt

private fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "?"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${(bytes / 102.4).roundToInt() / 10.0} KB"
    bytes < 1024L * 1024 * 1024 -> "${(bytes / (1024 * 102.4)).roundToInt() / 10.0} MB"
    else -> "${(bytes / (1024L * 1024 * 102.4)).roundToInt() / 10.0} GB"
}

/** Every download: progress, speed, pause / resume / cancel / retry / open / delete. */
@Composable
fun DownloadsScreen(state: VoidAppState, modifier: Modifier = Modifier) {
    val c = protoColors()
    val center = state.downloads
    BackHandler(enabled = true) { state.downloadsOpen = false }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.soft)
                    .clickable { state.downloadsOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.ink, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Downloads", color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${center.activeCount} active",
                color = c.tabIdle,
                fontFamily = Poppins,
                fontSize = 12.sp,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(c, "Pause all") { center.pauseAll() }
            Chip(c, "Resume all") { center.resumeAll() }
            Chip(c, "Clear finished") { center.clearFinished() }
            Chip(c, "Settings") { state.openSettingsPage("downloads") }
        }
        Spacer(Modifier.height(8.dp))

        if (center.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloads yet.", color = c.tabIdle, fontFamily = Poppins, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(center.items.toList(), key = { it.id }) { item ->
                    DownloadRow(state, c, item)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun Chip(c: ProtoColors, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, color = c.ink, fontFamily = Poppins, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DownloadRow(state: VoidAppState, c: ProtoColors, item: DownloadItem) {
    val center = state.downloads
    val shape = RoundedCornerShape(18.dp)
    val status = when (item.status) {
        DownloadStatus.Running -> buildString {
            if (item.total > 0) append("${(item.progress * 100).roundToInt()}%  ·  ")
            append(formatBytes(item.done))
            if (item.total > 0) append(" of ${formatBytes(item.total)}")
            if (item.speed > 0) append("  ·  ${formatBytes(item.speed)}/s")
        }
        DownloadStatus.Paused -> "Paused  ·  ${formatBytes(item.done)}" + if (item.total > 0) " of ${formatBytes(item.total)}" else ""
        DownloadStatus.Completed -> "Completed  ·  ${formatBytes(if (item.total > 0) item.total else item.done)}"
        DownloadStatus.Failed -> "Failed  ·  ${item.error ?: "unknown error"}"
        DownloadStatus.Canceled -> "Canceled"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .padding(14.dp),
    ) {
        Text(item.fileName, color = c.ink, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(status, color = c.tabIdle, fontFamily = Poppins, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            text = center.displayPath(item.folder),
            color = c.tabIdle.copy(alpha = 0.7f),
            fontFamily = Poppins,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.status == DownloadStatus.Running || item.status == DownloadStatus.Paused) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(c.line)) {
                val fraction = if (item.total > 0) item.progress else 0f
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.accent),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            when (item.status) {
                DownloadStatus.Running -> {
                    Action(Icons.Filled.Pause, "Pause", c) { center.pause(item) }
                    Action(Icons.Filled.Close, "Cancel", c) { center.cancel(item) }
                }
                DownloadStatus.Paused -> {
                    Action(Icons.Filled.PlayArrow, "Resume", c) { center.resume(item) }
                    Action(Icons.Filled.Close, "Cancel", c) { center.cancel(item) }
                }
                DownloadStatus.Failed, DownloadStatus.Canceled -> {
                    Action(Icons.Filled.Refresh, "Retry", c) { center.retry(item) }
                    Action(Icons.Filled.Delete, "Remove", c) { center.remove(item, deleteFile = false) }
                }
                DownloadStatus.Completed -> {
                    Action(Icons.AutoMirrored.Filled.OpenInNew, "Open", c) { center.open(item) }
                    Action(Icons.Filled.Close, "Remove from list", c) { center.remove(item, deleteFile = false) }
                    Action(Icons.Filled.Delete, "Delete file", c) { center.remove(item, deleteFile = true) }
                }
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, c: ProtoColors, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = c.ink, modifier = Modifier.size(23.dp))
    }
}

/** The box that appears when a page starts a download: rename it, see where it is saved. */
@Composable
fun DownloadConfirmDialog(state: VoidAppState) {
    val request = state.pendingDownload ?: return
    val folder = state.ui.downloadFolder.ifBlank { "VOID" }
    var name by remember(request) { mutableStateOf(request.fileName) }
    val host = request.url.substringAfter("://").substringBefore("/")

    AlertDialog(
        onDismissRequest = { state.pendingDownload = null },
        title = { Text("Download file?", fontFamily = Poppins, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("File name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("Saves to", fontFamily = Poppins, fontSize = 11.sp)
                Text(
                    state.downloads.displayPath(folder),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "From $host" + if (request.length > 0) "  ·  ${formatBytes(request.length)}" else "",
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { state.startDownload(request, name.trim()) }) {
                Text("Download", fontFamily = Poppins)
            }
        },
        dismissButton = {
            TextButton(onClick = { state.pendingDownload = null }) { Text("Cancel", fontFamily = Poppins) }
        },
    )
}
