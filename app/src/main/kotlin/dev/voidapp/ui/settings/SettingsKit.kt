package dev.voidapp.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.voidapp.ui.proto.ProtoColors
import dev.voidapp.ui.theme.Poppins

/** Colours offered wherever a colour can be picked. null = follow the theme. */
internal val ColorPalette: List<Color?> = listOf(
    null,
    Color(0xFFFFFFFF), Color(0xFFF3F3F3), Color(0xFFD9D9D9), Color(0xFF8A8A92), Color(0xFF2B2B30), Color(0xFF171717), Color(0xFF000000),
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF00897B), Color(0xFF1E88E5), Color(0xFF3949AB), Color(0xFF8E24AA),
    Color(0xFFEC407A), Color(0xFF6D4C41),
)

@Composable
internal fun SectionTitle(c: ProtoColors, text: String) {
    Text(
        text = text,
        color = c.tabIdle,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.3.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
internal fun Panel(c: ProtoColors, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.soft)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
internal fun Note(c: ProtoColors, text: String) {
    Text(
        text = text,
        color = c.tabIdle,
        fontFamily = Poppins,
        fontSize = 11.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun SliderRow(
    c: ProtoColors,
    label: String,
    shown: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(shown, color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.ink,
                activeTrackColor = c.ink,
                inactiveTrackColor = c.line,
                activeTickColor = c.page,
                inactiveTickColor = c.tabIdle,
            ),
        )
    }
}

@Composable
internal fun ColorRow(c: ProtoColors, label: String, selected: Color?, onPick: (Color?) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ColorPalette.forEach { color ->
                val isSelected = color == selected
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color ?: c.page)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) c.ink else c.line, CircleShape)
                        .clickable { onPick(color) },
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        color == null -> Text(
                            "A",
                            color = c.ink,
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        color != null && isSelected -> Icon(
                            Icons.Filled.Check,
                            null,
                            tint = if (color.red * 0.3f + color.green * 0.6f + color.blue * 0.1f > 0.55f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SwitchRow(
    c: ProtoColors,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (enabled) c.ink else c.tabIdle,
            fontFamily = Poppins,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.ink,
                checkedThumbColor = c.page,
                checkedBorderColor = c.ink,
                uncheckedTrackColor = c.line,
                uncheckedThumbColor = c.page,
                uncheckedBorderColor = c.line,
            ),
        )
    }
}

@Composable
internal fun ValueRow(c: ProtoColors, label: String, value: String = "", onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = c.tabIdle, fontFamily = Poppins, fontSize = 12.sp)
    }
}

/** A row of chips, one selected (theme mode, ...). */
@Composable
internal fun ChoiceRow(c: ProtoColors, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.ink else c.page)
                    .border(1.dp, if (on) c.ink else c.line, RoundedCornerShape(12.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) c.page else c.ink,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun Field(c: ProtoColors, value: String, hint: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.page)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(hint, color = c.placeholder, fontFamily = Poppins, fontSize = 13.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = c.ink, fontFamily = Poppins, fontSize = 13.sp),
            cursorBrush = SolidColor(c.ink),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun TextRow(c: ProtoColors, label: String, value: String, hint: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, color = c.ink, fontFamily = Poppins, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Field(c, value, hint, onChange)
    }
}

@Composable
internal fun ActionButton(c: ProtoColors, text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.ink)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = c.page, fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
