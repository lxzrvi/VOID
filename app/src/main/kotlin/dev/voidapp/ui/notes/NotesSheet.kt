package dev.voidapp.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.R
import dev.voidapp.ui.VoidAppState
import dev.voidapp.ui.theme.Poppins
import dev.voidapp.ui.theme.VoidRed

/** Quick notes: a small card above the controls. */
@Composable
fun NotesSheet(state: VoidAppState, modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .width(280.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, colors.outline, shape)
            .padding(14.dp),
    ) {
        Text(
            text = stringResource(R.string.notes),
            color = colors.onSurfaceVariant,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (draft.isEmpty()) {
                    Text(
                        stringResource(R.string.notes_hint),
                        color = colors.onSurfaceVariant,
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = colors.onSurface, fontFamily = Poppins, fontSize = 12.sp),
                    cursorBrush = SolidColor(VoidRed),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { },
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface)
                    .clickable {
                        state.addNote(draft)
                        draft = ""
                        focus.clearFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, null, tint = colors.surface, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.notes.isEmpty()) {
            Text(
                stringResource(R.string.notes_empty),
                color = colors.onSurfaceVariant,
                fontFamily = Poppins,
                fontSize = 11.sp,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                items(state.notes, key = { it.id }) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            note.text,
                            color = colors.onSurface,
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.Close,
                            null,
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { state.removeNote(note) },
                        )
                    }
                }
            }
        }
    }
}
