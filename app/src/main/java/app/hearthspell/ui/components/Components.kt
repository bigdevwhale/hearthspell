package app.hearthspell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.hearthspell.R
import app.hearthspell.ui.art.Glyph
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay

/** The size of one "UI pixel": outlines, bevels and stepped corners are drawn in multiples of it. */
val PX: Dp = 3.dp

/**
 * An 8-bit box: a dark outline with stepped (notched) corners, a fill, a light bevel along the top
 * and a darker lip along the bottom. [pressed] drops the bevel, as if pushed in.
 */
fun Modifier.pixelBox(fill: Color, outline: Color, bevel: Color? = null, lip: Color? = null, pressed: Boolean = false): Modifier = drawBehind {
    val p = PX.toPx()
    val w = size.width
    val h = size.height
    // Outline: the rectangle minus its corner pixels.
    drawRect(outline, Offset(p, 0f), Size(w - 2 * p, h))
    drawRect(outline, Offset(0f, p), Size(w, h - 2 * p))
    // Fill, inset by one pixel, again with notched corners.
    drawRect(fill, Offset(2 * p, p), Size(w - 4 * p, h - 2 * p))
    drawRect(fill, Offset(p, 2 * p), Size(w - 2 * p, h - 4 * p))
    if (!pressed) {
        bevel?.let { drawRect(it, Offset(2 * p, p), Size(w - 4 * p, p)) }
        lip?.let { drawRect(it, Offset(2 * p, h - 2 * p), Size(w - 4 * p, p)) }
    }
}

/** A panel. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    fill: Color = U.c.surface,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier
            .pixelBox(fill, U.c.outline, U.c.bevel, U.c.outline.copy(alpha = 0.35f), pressed = pressed && onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

enum class ButtonKind { PRIMARY, SECONDARY, DANGER, MANA }

/** A chunky 8-bit button: it sinks by a pixel while pressed. */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    icon: Glyph? = null,
) {
    val c = U.c
    val (fill, ink, lip) = when (kind) {
        ButtonKind.PRIMARY -> Triple(c.accent, c.onAccent, c.accentDeep)
        ButtonKind.SECONDARY -> Triple(c.surface2, c.ink, c.outline.copy(alpha = 0.4f))
        ButtonKind.DANGER -> Triple(c.danger, Color(0xFF1A0505), c.outline.copy(alpha = 0.4f))
        ButtonKind.MANA -> Triple(c.mana, Color(0xFF04222E), c.outline.copy(alpha = 0.4f))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha = if (enabled) 1f else 0.45f
    Box(
        modifier
            .offset(y = if (pressed) PX else 0.dp)
            .pixelBox(fill.copy(alpha = alpha), c.outline, Color.White.copy(alpha = 0.35f * alpha), lip, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                PixelIcon(icon, ink.copy(alpha = alpha), size = 16.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = UType.Button, color = ink.copy(alpha = alpha), textAlign = TextAlign.Center)
        }
    }
}

/** A small square icon button. */
@Composable
fun IconTile(g: Glyph, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = U.c.ink, fill: Color = U.c.surface2, size: Dp = 44.dp, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(size)
            .pixelBox(fill, U.c.outline, U.c.bevel, null, pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { PixelIcon(g, if (enabled) tint else tint.copy(alpha = 0.35f), size = size * 0.42f) }
}

/** A screen header with a back button. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconTile(Glyphs.back, onBack, size = 42.dp)
            Spacer(Modifier.width(14.dp))
        }
        Text(title, style = UType.Title, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = UType.Eyebrow, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = UType.Muted.copy(color = U.c.accent), modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
        }
    }
}

/** A toggle chip. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.accent) {
    val c = U.c
    Box(
        modifier
            .pixelBox(if (selected) color else c.surface2, c.outline, if (selected) Color.White.copy(alpha = 0.3f) else c.bevel)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, style = UType.Body, color = if (selected) c.onAccent else c.ink, maxLines = 1)
    }
}

/** A segmented bar of [segments] blocks, [fraction] of them lit: the 8-bit progress bar. */
@Composable
fun PixelBar(fraction: Float, modifier: Modifier = Modifier, color: Color = U.c.accent, segments: Int = 10, height: Dp = 12.dp) {
    val c = U.c
    val lit = (fraction.coerceIn(0f, 1f) * segments).let { if (it > 0f && it < 1f) 1 else it.toInt() }
    Box(
        modifier.height(height).drawBehind {
            val p = PX.toPx()
            drawRect(c.outline, Offset(0f, 0f), size)
            val inner = Size(size.width - 2 * p, size.height - 2 * p)
            drawRect(c.surface2, Offset(p, p), inner)
            val gap = p * 0.67f
            val segW = (inner.width - gap * (segments + 1)) / segments
            for (i in 0 until lit) {
                val x = p + gap + i * (segW + gap)
                drawRect(color, Offset(x, p + gap), Size(segW, inner.height - 2 * gap))
                drawRect(Color.White.copy(alpha = 0.35f), Offset(x, p + gap), Size(segW, gap))
            }
        }
    )
}

/** A text field in a pixel box. */
@Composable
fun PixelField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
) {
    val c = U.c
    Box(modifier.pixelBox(c.surface2, c.outline).padding(horizontal = 14.dp, vertical = 12.dp)) {
        if (value.isEmpty()) Text(placeholder, style = UType.Body.copy(color = c.faint))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = UType.Body,
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = keyboard,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A number of days (or anything) with − and + buttons. */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, label: String, modifier: Modifier = Modifier, min: Int = 1, max: Int = 365) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconTile(Glyphs.down, { onChange((value - 1).coerceAtLeast(min)) }, size = 40.dp, enabled = value > min)
        Text(label, style = UType.Strong, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        IconTile(Glyphs.up, { onChange((value + 1).coerceAtMost(max)) }, size = 40.dp, enabled = value < max)
    }
}

/** A row with a label and an 8-bit on/off switch. */
@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, sub: String? = null) {
    val c = U.c
    Row(modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = UType.Body)
            if (sub != null) Text(sub, style = UType.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(54.dp).height(30.dp).pixelBox(if (checked) c.good else c.surface2, c.outline)) {
            Box(
                Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 5.dp)
                    .size(18.dp)
                    .background(if (checked) c.ink else c.faint)
            )
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = androidx.compose.ui.graphics.RectangleShape,
        title = { Text(title, style = UType.Heading) },
        text = { Text(text, style = UType.Body) },
        confirmButton = {
            PixelButton(confirm, { onConfirm(); onDismiss() }, kind = if (danger) ButtonKind.DANGER else ButtonKind.PRIMARY)
        },
        dismissButton = { PixelButton(stringResource(R.string.cancel), onDismiss, kind = ButtonKind.SECONDARY) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minutes: Int, onDismiss: () -> Unit, onPicked: (Int) -> Unit) {
    val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = androidx.compose.ui.graphics.RectangleShape,
        text = { TimePicker(state) },
        confirmButton = { PixelButton(stringResource(R.string.ok), { onPicked(state.hour * 60 + state.minute); onDismiss() }) },
        dismissButton = { PixelButton(stringResource(R.string.cancel), onDismiss, kind = ButtonKind.SECONDARY) },
    )
}

/** Mana with its drop icon. */
@Composable
fun ManaTag(amount: Int, modifier: Modifier = Modifier, prefix: String = "") {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(Glyphs.mana, U.c.mana, size = 14.dp)
        Spacer(Modifier.width(5.dp))
        Text("$prefix$amount", style = UType.Number.copy(fontSize = UType.Eyebrow.fontSize * 1.25f, color = U.c.mana))
    }
}

/** The current time, ticking every [stepMs] while the screen is resumed. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { now = System.currentTimeMillis() }
    LaunchedEffect(stepMs) {
        while (true) {
            delay(stepMs - System.currentTimeMillis() % stepMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** A vertical gap. */
@Composable
fun Gap(h: Dp) = Spacer(Modifier.height(h))

/** Converts dp to px in composition. */
@Composable
fun Dp.toPxNow(): Float = with(LocalDensity.current) { this@toPxNow.toPx() }
