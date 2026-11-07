package dev.voidapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import dev.voidapp.R
import dev.voidapp.data.Background
import dev.voidapp.data.BoxEdit
import dev.voidapp.data.SearchProvider
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

// ===========================================================================
// Add shortcut
// ===========================================================================

@Composable
fun AddSiteDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_site_title), fontFamily = Poppins) },
        text = {
            Column {
                DialogField(url, { url = it }, stringResource(R.string.add_site_url))
                Spacer(Modifier.height(8.dp))
                DialogField(name, { name = it }, stringResource(R.string.add_site_name))
            }
        },
        confirmButton = {
            TextButton(enabled = url.isNotBlank(), onClick = { onAdd(name, url) }) {
                Text(stringResource(R.string.add_site_confirm), fontFamily = Poppins)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_site_cancel), fontFamily = Poppins)
            }
        },
    )
}

// ===========================================================================
// Search providers
// ===========================================================================

@Composable
fun ProviderDialog(
    current: SearchProvider,
    onDismiss: () -> Unit,
    onPick: (SearchProvider) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search with", fontFamily = Poppins) },
        text = {
            Column {
                SearchProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(provider) }
                            .padding(horizontal = 8.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = provider.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            fontWeight = if (provider == current) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        if (provider == current) {
                            Spacer(Modifier.weight(1f))
                            Text("•", color = VoidRed, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontFamily = Poppins) }
        },
    )
}

// ===========================================================================
// Notes
// ===========================================================================

@Composable
fun NotesDialog(
    state: VoidAppState,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes), fontFamily = Poppins, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    ) {
                        if (draft.isEmpty()) {
                            Text(
                                stringResource(R.string.notes_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                            )
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                            ),
                            cursorBrush = SolidColor(VoidRed),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(VoidRed)
                            .clickable {
                                state.addNote(draft)
                                draft = ""
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = stringResource(R.string.notes_add),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.notes.isEmpty()) {
                    Text(
                        stringResource(R.string.notes_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(state.notes, key = { it.id }) { note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = note.text,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { state.removeNote(note) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_done), fontFamily = Poppins) }
        },
    )
}

// ===========================================================================
// Box editor: size, position, radius, opacity, tint
// ===========================================================================

@Composable
fun BoxEditorSheet(
    title: String,
    edit: BoxEdit,
    onChange: (BoxEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(edit) }
    val tints = listOf(null, 0xFFFFFFFF, 0xFF141416, 0xFFC10520, 0xFF1C1C22, 0xFFF4F4F5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.box_appearance),
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                SliderRow(stringResource(R.string.edit_size), current.scale, 0.6f, 1.4f) {
                    current = current.copy(scale = it)
                    onChange(current)
                }
                SliderRow(stringResource(R.string.edit_radius), current.radiusDp, 0f, 48f) {
                    current = current.copy(radiusDp = it)
                    onChange(current)
                }
                SliderRow(stringResource(R.string.edit_alpha), current.alpha, 0.2f, 1f) {
                    current = current.copy(alpha = it)
                    onChange(current)
                }
                SliderRow(stringResource(R.string.edit_position), current.y, -80f, 80f) {
                    current = current.copy(y = it)
                    onChange(current)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.edit_color),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tints.forEach { tint ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (tint == null) MaterialTheme.colorScheme.surface
                                    else Color(tint),
                                )
                                .border(
                                    width = if (current.colorArgb == tint) 2.dp else 1.dp,
                                    color = if (current.colorArgb == tint) VoidRed else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable {
                                    current = current.copy(colorArgb = tint)
                                    onChange(current)
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_done), fontFamily = Poppins) }
        },
        dismissButton = {
            TextButton(onClick = {
                current = BoxEdit()
                onChange(current)
            }) { Text(stringResource(R.string.edit_reset), fontFamily = Poppins) }
        },
    )
}

@Composable
private fun SliderRow(label: String, value: Float, from: Float, to: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 11.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = String.format("%.2f", value),
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = Poppins,
                fontSize = 11.sp,
            )
        }
        Slider(
            value = value.coerceIn(from, to),
            onValueChange = onChange,
            valueRange = from..to,
        )
    }
}

// ===========================================================================
// Background editor: gradient / image + blur / dim / brightness
// ===========================================================================

@Composable
fun BackgroundEditorSheet(
    background: Background,
    onChange: (Background) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(background) }
    val context = LocalContext.current

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            current = current.copy(kind = Background.Kind.Image, imageUri = uri.toString())
            onChange(current)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.bg_change),
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BgOption(
                        label = stringResource(R.string.bg_brand),
                        selected = current.kind == Background.Kind.Brand,
                        modifier = Modifier.weight(1f),
                    ) {
                        current = current.copy(kind = Background.Kind.Brand)
                        onChange(current)
                    }
                    BgOption(
                        label = stringResource(R.string.bg_gradient),
                        selected = current.kind == Background.Kind.Gradient,
                        modifier = Modifier.weight(1f),
                    ) {
                        current = current.copy(kind = Background.Kind.Gradient)
                        onChange(current)
                    }
                    BgOption(
                        label = stringResource(R.string.bg_image),
                        selected = current.kind == Background.Kind.Image,
                        modifier = Modifier.weight(1f),
                    ) {
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                }

                if (current.kind == Background.Kind.Gradient) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.bg_gradient),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val presets = listOf(
                            0xFFC10520L to 0xFF0A0A0BL,
                            0xFF1B1B1F to 0xFF3A3A44,
                            0xFF0F2027 to 0xFF2C5364,
                            0xFF42275A to 0xFF734B6D,
                            0xFF000000 to 0xFF434343,
                        )
                        presets.forEach { (start, end) ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(start), Color(end)),
                                        ),
                                    )
                                    .border(
                                        width = if (current.startColor == start) 2.dp else 1.dp,
                                        color = if (current.startColor == start) VoidRed
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        current = current.copy(startColor = start, endColor = end)
                                        onChange(current)
                                    },
                            )
                        }
                    }
                }

                if (current.kind == Background.Kind.Image) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BgOption(label = stringResource(R.string.bg_pick), selected = false, modifier = Modifier.weight(1f)) {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                        BgOption(label = stringResource(R.string.bg_remove), selected = false, modifier = Modifier.weight(1f)) {
                            current = current.copy(kind = Background.Kind.Brand, imageUri = null)
                            onChange(current)
                        }
                    }
                    SliderRow(stringResource(R.string.edit_blur), current.blur, 0f, 1f) {
                        current = current.copy(blur = it)
                        onChange(current)
                    }
                }

                SliderRow(stringResource(R.string.edit_dim), current.dim, 0f, 0.85f) {
                    current = current.copy(dim = it)
                    onChange(current)
                }
                SliderRow(stringResource(R.string.edit_brightness), current.brightness, 0f, 0.6f) {
                    current = current.copy(brightness = it)
                    onChange(current)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.edit_done), fontFamily = Poppins) }
        },
        dismissButton = {
            TextButton(onClick = {
                current = Background()
                onChange(current)
            }) { Text(stringResource(R.string.edit_reset), fontFamily = Poppins) }
        },
    )
}

@Composable
private fun BgOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) VoidRed.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (selected) VoidRed else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) VoidRed else MaterialTheme.colorScheme.onSurface,
            fontFamily = Poppins,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DialogField(value: String, onValueChange: (String) -> Unit, hint: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 13.dp),
    ) {
        if (value.isEmpty()) {
            Text(hint, color = colors.onSurfaceVariant, fontFamily = Poppins, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.onSurface, fontFamily = Poppins, fontSize = 13.sp),
            cursorBrush = SolidColor(VoidRed),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}