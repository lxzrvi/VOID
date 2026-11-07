package dev.voidapp.ui.home

import androidx.activity.compose.BackHandler
import coil3.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import dev.voidapp.ui.components.ReorderGrid
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.data.Background
import dev.voidapp.data.OnThisDay
import dev.voidapp.data.Site
import dev.voidapp.ui.IslandPanel
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.components.SearchButton
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

/**
 * Home page.
 *
 * - A full-bleed background (brand gradient, custom gradient or photo) that also
 *   paints behind the status bar - the bars stay transparent, no black strip.
 * - Small bold "void" wordmark, time / date / today's event on the right.
 * - Search bar: the provider's own favicon plus a down arrow on the left, the
 *   app mark + a bare search glyph on the right (no circle behind it).
 * - Frosted shortcut box with an adaptive grid (as many columns as fit the
 *   shortcuts, no empty cells), a notes row and a + tile.
 * - Long-press anything: a box editor (size, position, radius, opacity, tint) or,
 *   on empty background, the background editor (gradient / image, blur, dim,
 *   brightness).
 */
@Composable
fun HomeScreen(
    state: VoidAppState,
    onScrollDirection: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var showAddSite by remember { mutableStateOf(false) }
    var showEvent by remember { mutableStateOf(false) }
    var showProviders by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val shortcutEdit = state.boxEdit("shortcuts")
    val searchEdit = state.boxEdit("search")

    BackHandler(enabled = query.isNotEmpty()) { query = "" }

    val scrollReporter = remember(onScrollDirection) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) onScrollDirection(available.y)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Long press on empty space -> background editor, at that very spot.
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        state.backgroundPickerAt = offset.x / size.width to offset.y / size.height
                    },
                    onDrag = { _, _ -> },
                )
            },
    ) {
        BackgroundLayer(background = state.background)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .nestedScroll(scrollReporter)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Content starts below the status bar, but scrolls UNDER it.
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Text(
                text = "void",
                color = Color.White,
                fontFamily = Poppins,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 5.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = OnThisDay.timeLabel(),
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = OnThisDay.todayLabel(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    EventChip(
                        text = when {
                            state.eventLoading -> stringResource(R.string.event_loading)
                            state.event != null -> "• " + state.event!!.text
                            else -> stringResource(R.string.event_unavailable)
                        },
                        onClick = { if (state.event != null) showEvent = true else state.reloadEvent() },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ---- search box (appearance-editable, long press) --------------
            EditableBox(
                edit = searchEdit,
                onLongPress = { state.editingBox = "search" },
            ) {
                HomeSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    state = state,
                    onProviderClick = { showProviders = true },
                    onSubmit = {
                        focusManager.clearFocus()
                        state.search(query)
                    },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- shortcuts box --------------------------------------------
            EditableBox(
                edit = shortcutEdit,
                onLongPress = { state.editingBox = "shortcuts" },
                // Long press inside the grid reorders; the editor opens from the header.
                longPressEnabled = false,
            ) {
                ShortcutBox(
                    state = state,
                    onAdd = { showAddSite = true },
                    onEdit = { state.editingBox = "shortcuts" },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---- three-dot + notes, right below the shortcuts ---------------
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable { state.showPanel(IslandPanel.Tools) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Ellipsis,
                        contentDescription = stringResource(R.string.browser_tools),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable { showNoteDialog = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.FileText,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.notes),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                        )
                        if (state.notes.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(17.dp)
                                    .clip(CircleShape)
                                    .background(VoidRed),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = state.notes.size.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }

        // Soft scrim so the status bar icons stay readable over scrolled content.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.26f), Color.Transparent),
                    ),
                ),
        )
    }

    if (showAddSite) {
        AddSiteDialog(
            onDismiss = { showAddSite = false },
            onAdd = { name, url ->
                state.addSite(name, url)
                showAddSite = false
            },
        )
    }

    if (showNoteDialog) {
        NotesDialog(
            state = state,
            onDismiss = { showNoteDialog = false },
        )
    }

    if (showProviders) {
        ProviderDialog(
            current = state.provider,
            onDismiss = { showProviders = false },
            onPick = {
                state.selectProvider(it)
                showProviders = false
            },
        )
    }

    if (showEvent) {
        val event = state.event
        AlertDialog(
            onDismissRequest = { showEvent = false },
            title = { Text(event?.year?.toString().orEmpty()) },
            text = { Text(event?.text.orEmpty()) },
            confirmButton = { TextButton(onClick = { showEvent = false }) { Text("OK") } },
            dismissButton = {
                val source = event?.sourceUrl
                if (source != null) {
                    TextButton(onClick = {
                        showEvent = false
                        state.open(source)
                    }) { Text(stringResource(R.string.event_source)) }
                }
            },
        )
    }

    // ---- editors ------------------------------------------------------------
    state.editingBox?.let { key ->
        BoxEditorSheet(
            title = key,
            edit = state.boxEdit(key),
            onChange = { state.setBoxEdit(key, it) },
            onDismiss = { state.editingBox = null },
        )
    }

    state.backgroundPickerAt?.let {
        BackgroundEditorSheet(
            background = state.background,
            onChange = state::updateBackground,
            onDismiss = { state.backgroundPickerAt = null },
        )
    }
}

/** Background: brand gradient, custom gradient or photo, with blur/dim/brightness. */
@Composable
private fun BackgroundLayer(background: Background) {
    val colors = MaterialTheme.colorScheme
    when (background.kind) {
        Background.Kind.Image -> {
            val uri = background.imageUri
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        // Real gaussian blur, scaled with the user's slider (0-25 dp).
                        .blur(radius = (background.blur.coerceIn(0f, 1f) * 25f).dp),
                )
            } else {
                Box(Modifier.fillMaxSize().background(brandGradient(colors)))
            }
            if (background.dim > 0f) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = background.dim.coerceIn(0f, 0.9f))))
            }
            if (background.brightness > 0f) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = background.brightness.coerceIn(0f, 0.6f))))
            }
        }
        Background.Kind.Gradient -> Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(Color(background.startColor), Color(background.endColor)),
                    start = Offset.Zero,
                    end = Offset(
                        kotlin.math.cos(Math.toRadians(background.angle.toDouble())).toFloat() * 1200f,
                        kotlin.math.sin(Math.toRadians(background.angle.toDouble())).toFloat() * 1200f,
                    ),
                ),
            ),
        )
        Background.Kind.Brand -> Box(Modifier.fillMaxSize().background(brandGradient(colors)))
    }
}

private fun brandGradient(colors: androidx.compose.material3.ColorScheme) = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to VoidRed,
        0.38f to VoidRed.copy(alpha = 0.55f),
        1f to colors.background,
    ),
)

/** A box that can be scaled, moved, rounded, faded and tinted from a long press. */
@Composable
private fun EditableBox(
    edit: dev.voidapp.data.BoxEdit,
    onLongPress: () -> Unit,
    longPressEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = edit.scale
                scaleY = edit.scale
                alpha = edit.alpha
                translationX = edit.x
                translationY = edit.y
            }
            .clip(RoundedCornerShape(edit.radiusDp.dp))
            .then(
                if (edit.colorArgb != null) {
                    Modifier.background(Color(edit.colorArgb))
                } else {
                    Modifier
                },
            )
            .then(
                if (longPressEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onLongPress() },
                            onDrag = { _, _ -> },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

@Composable
private fun EventChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 10.sp,
        fontFamily = Poppins,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    state: VoidAppState,
    onProviderClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(18.dp))
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Provider: its own favicon + down arrow (no coloured chip behind it).
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClickLabel = state.provider.label,
                    onClick = onProviderClick,
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Favicon(
                faviconUrl = state.provider.faviconUrl,
                contentDescription = state.provider.label,
                size = 26.dp,
                circleColor = Color.Transparent,
            )
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_on, state.provider.label),
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = Poppins,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(VoidRed),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Bare search glyph with the void mark inside - no circle, same as the island.
        SearchButton(
            onClick = onSubmit,
            contentDescription = stringResource(R.string.search_hint),
            size = 22.dp,
            tint = colors.onSurface,
        )
        Spacer(Modifier.width(4.dp))
    }
}

private sealed interface ShortcutItem {
    data class Entry(val site: Site) : ShortcutItem
    data object Add : ShortcutItem
}

@Composable
private fun ShortcutBox(
    state: VoidAppState,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val sites = state.visibleSites
    val items: List<ShortcutItem> = sites.map { ShortcutItem.Entry(it) } + ShortcutItem.Add
    val total = items.size

    // At most 5 columns; take the count that leaves the shortest gap in the last
    // row. The last row is then stretched over the full width, so the box never
    // has an empty slot.
    val columns = if (total <= 4) total else listOf(5, 4, 3).minByOrNull { (it - total % it) % it } ?: 5

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onEdit() },
                        onDrag = { _, _ -> },
                    )
                }
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.shortcuts),
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.3.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = sites.size.toString(),
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = Poppins,
            )
        }
        Spacer(Modifier.height(10.dp))

        ReorderGrid(
            items = items,
            itemKey = { item ->
                when (item) {
                    is ShortcutItem.Entry -> item.site.key
                    ShortcutItem.Add -> "__add__"
                }
            },
            columns = columns,
            cellHeight = 78.dp,
            gap = 2.dp,
            fixedTail = 1,
            onMove = { from, to -> state.moveSite(from, to) },
        ) { item, _, lifted ->
            when (item) {
                is ShortcutItem.Entry -> SiteTile(
                    site = item.site,
                    lifted = lifted,
                    onClick = { state.open(item.site.url) },
                )
                ShortcutItem.Add -> AddTile(onClick = onAdd)
            }
        }
    }
}

@Composable
private fun SiteTile(
    site: Site,
    lifted: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(if (lifted) colors.surfaceVariant else Color.Transparent)
            .clickable(enabled = !lifted, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Favicon(
            faviconUrl = site.faviconUrl,
            contentDescription = site.name,
            size = 44.dp,
            circleColor = colors.surfaceVariant,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = site.name,
            color = colors.onSurface.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontFamily = Poppins,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant)
                .border(1.dp, colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = stringResource(R.string.action_add_site),
                tint = colors.onSurface,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.action_add_site),
            color = colors.onSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = Poppins,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
