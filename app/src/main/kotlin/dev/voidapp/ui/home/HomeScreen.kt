package dev.voidapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import dev.voidapp.data.OnThisDay
import dev.voidapp.data.SearchProvider
import dev.voidapp.data.Site
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.components.AppMark
import dev.voidapp.ui.components.Favicon
import dev.voidapp.ui.icons.Lucide
import dev.voidapp.ui.theme.VoidRed

private val CardShape = RoundedCornerShape(26.dp)
private val FieldShape = RoundedCornerShape(18.dp)

/**
 * Home page: gradient in the app-icon red, a small "void" wordmark, time/date and
 * today's event on the right, a search bar (whose leading icon is the app icon and
 * opens the provider picker) and a frosted box with the website shortcuts.
 *
 * [onScrollDirection] reports the vertical scroll direction so the island can
 * slide away when the page scrolls - this screen owns the scroll state.
 */
@Composable
fun HomeScreen(
    state: VoidAppState,
    onScrollDirection: (delta: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var showAddSite by remember { mutableStateOf(false) }
    var showEvent by remember { mutableStateOf(false) }
    var showProviders by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    BackHandler(enabled = query.isNotEmpty()) { query = "" }

    // Feed scroll deltas upward so the island can hide on scroll-down.
    val scrollReporter = remember(onScrollDirection) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y != 0f) onScrollDirection(available.y)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to VoidRed.copy(alpha = 0.95f),
                    0.35f to VoidRed.copy(alpha = 0.62f),
                    1f to colors.background,
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .nestedScroll(scrollReporter)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- wordmark -------------------------------------------------
            Text(
                text = "void",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 7.sp,
                modifier = Modifier.alpha(0.95f),
            )

            // ---- time / date / event --------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = OnThisDay.timeLabel(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = OnThisDay.todayLabel(),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(22.dp))

            // ---- home search bar ------------------------------------------
            HomeSearchBar(
                query = query,
                onQueryChange = { query = it },
                provider = state.provider,
                onProviderClick = { showProviders = true },
                onSubmit = {
                    focusManager.clearFocus()
                    state.search(query)
                },
            )

            Spacer(Modifier.height(20.dp))

            // ---- websites box ---------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Color.White.copy(alpha = 0.13f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CardShape)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.shortcuts),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = state.sites.size.toString(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))

                // 5 columns; the whole box is inside a vertical scroll, so the grid
                // itself must not scroll (its height is fixed to fit every row).
                val rows = ((state.sites.size + 1) + 4) / 5
                val gridHeight = (rows * 78).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = false,
                ) {
                    items(state.sites, key = { it.url }) { site ->
                        SiteTile(
                            site = site,
                            onClick = { state.open(site.url) },
                            onLongClick = { state.removeSite(site) },
                        )
                    }
                    item(key = "__add__") { AddTile(onClick = { showAddSite = true }) }
                }
            }

            Spacer(Modifier.height(110.dp))
        }
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
}

@Composable
private fun EventChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = Modifier
            .width(224.dp)
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
    provider: SearchProvider,
    onProviderClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(FieldShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), FieldShape)
            .padding(start = 7.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading circle = the app icon; tapping it chooses the search provider.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(VoidRed)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClickLabel = provider.label,
                    onClick = onProviderClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppMark(modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_on, provider.label),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onSubmit,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = stringResource(R.string.search_hint),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SiteTile(
    site: Site,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClickLabel = stringResource(R.string.site_open, site.name),
                onLongClickLabel = stringResource(R.string.site_remove, site.name),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Favicon(
            faviconUrl = site.faviconUrl,
            contentDescription = site.name,
            size = 44.dp,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = site.name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White),
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = stringResource(R.string.action_add_site),
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.action_add_site),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AddSiteDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, url: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_site_title)) },
        text = {
            Column {
                DialogField(
                    value = url,
                    onValueChange = { url = it },
                    hint = stringResource(R.string.add_site_url),
                )
                Spacer(Modifier.height(8.dp))
                DialogField(
                    value = name,
                    onValueChange = { name = it },
                    hint = stringResource(R.string.add_site_name),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = url.isNotBlank(), onClick = { onAdd(name, url) }) {
                Text(stringResource(R.string.add_site_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.add_site_cancel)) }
        },
    )
}

@Composable
internal fun ProviderDialog(
    current: SearchProvider,
    onDismiss: () -> Unit,
    onPick: (SearchProvider) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search with") },
        text = {
            Column {
                SearchProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(provider) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = provider.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
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
            Text(hint, color = colors.onSurfaceVariant, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
            cursorBrush = SolidColor(VoidRed),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
