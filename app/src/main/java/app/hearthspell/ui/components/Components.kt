package app.hearthspell.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.hearthspell.R
import app.hearthspell.ui.art.Glyph
import app.hearthspell.ui.art.Glyphs
import app.hearthspell.ui.art.PixelIcon
import app.hearthspell.ui.theme.U
import app.hearthspell.ui.tips.HelpTile
import app.hearthspell.ui.tips.Tip
import app.hearthspell.ui.tips.TipHost
import app.hearthspell.ui.theme.UType
import kotlinx.coroutines.delay

/** A hairline: separators and thin borders. */
val PX: Dp = 1.dp

/**
 * A storybook pane: a rounded card with a soft drop shadow ([outline]), a gentle sheen from the top and a light rim
 * ([bevel]). [lip] deepens the bottom edge. [pressed] flattens it, as if pushed in. Small boxes get rounder corners
 * in proportion, so a checkbox reads as a soft square and a long bar as a pill.
 */
fun Modifier.pixelBox(fill: Color, outline: Color, bevel: Color? = null, lip: Color? = null, pressed: Boolean = false): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val r = minOf(16.dp.toPx(), minOf(w, h) * 0.32f)
    val radius = CornerRadius(r, r)
    if (!pressed) {
        // A soft shadow below: a few faint, widening layers.
        for (i in 1..3) {
            val d = i * 1.5f.dp.toPx()
            drawRoundRect(outline.copy(alpha = 0.16f / i), Offset(0f, d), Size(w, h), CornerRadius(r + d, r + d))
        }
    }
    drawRoundRect(fill, Offset.Zero, size, radius)
    // Sheen: lighter at the top, fading down; a deeper lip at the bottom.
    drawRoundRect(
        Brush.verticalGradient(0f to Color.White.copy(alpha = if (pressed) 0.02f else 0.09f), 0.55f to Color.Transparent),
        Offset.Zero, size, radius,
    )
    lip?.let { drawRoundRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to it.copy(alpha = it.alpha * 0.6f)), Offset.Zero, size, radius) }
    bevel?.let { drawRoundRect(it.copy(alpha = it.alpha * 0.55f), Offset.Zero, size, radius, style = Stroke(1.dp.toPx())) }
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
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pixelBox(fill.copy(alpha = alpha), c.outline, Color.White.copy(alpha = 0.25f * alpha), lip, pressed = pressed)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                PixelIcon(icon, ink.copy(alpha = alpha), size = 20.dp)
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
    ) { PixelIcon(g, if (enabled) tint else tint.copy(alpha = 0.35f), size = size * 0.56f, muted = !enabled) }
}

/** A screen header with a back button. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, tip: Tip? = null, actions: @Composable RowScope.() -> Unit = {}) {
    if (tip != null) TipHost(tip)
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconTile(Glyphs.back, onBack, size = 42.dp)
            Spacer(Modifier.width(14.dp))
        }
        // One line, shrinking a step at a time until the title fits next to the buttons.
        val base = UType.Title
        var size by remember(title) { mutableStateOf(base.fontSize) }
        Text(
            title, style = base.copy(fontSize = size, lineHeight = size * 1.1f), modifier = Modifier.weight(1f),
            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (it.hasVisualOverflow && size.value > 18f) size = (size.value - 2f).sp },
        )
        if (tip != null) {
            HelpTile(tip)
            Spacer(Modifier.width(8.dp))
        }
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = UType.Eyebrow, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = UType.Muted.copy(color = U.c.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), modifier = Modifier.clickable(onClick = onAction).padding(4.dp))
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, style = UType.Body.copy(fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else null), color = if (selected) c.onAccent else c.ink, maxLines = 1)
    }
}

/** A rounded progress bar, [fraction] filled with a glowing gradient; it glides when the value changes. */
@Composable
fun PixelBar(fraction: Float, modifier: Modifier = Modifier, color: Color = U.c.accent, segments: Int = 10, height: Dp = 12.dp) {
    val c = U.c
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(600), label = "bar")
    Box(
        modifier.height(height).drawBehind {
            val r = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(c.outline.copy(alpha = 0.45f), Offset.Zero, size, r)
            drawRoundRect(c.surface2, Offset(1.dp.toPx(), 1.dp.toPx()), Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), r)
            if (f > 0f) {
                val w = maxOf(size.height, size.width * f)
                drawRoundRect(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color), endX = w), Offset.Zero, Size(w, size.height), r)
                drawRoundRect(
                    Brush.verticalGradient(0f to Color.White.copy(alpha = 0.35f), 0.5f to Color.Transparent),
                    Offset.Zero, Size(w, size.height), r,
                )
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
        // A soft switch: the knob glides across, the track warms up when on.
        val pos by animateFloatAsState(if (checked) 1f else 0f, label = "switch")
        val track = androidx.compose.ui.graphics.lerp(c.surface2, c.good, pos)
        Box(
            Modifier.width(52.dp).height(30.dp).drawBehind {
                val r = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(track, Offset.Zero, size, r)
                drawRoundRect(c.bevel.copy(alpha = 0.5f * (1 - pos)), Offset.Zero, size, r, style = Stroke(1.dp.toPx()))
                val k = size.height / 2 - 4.dp.toPx()
                val cx = size.height / 2 + (size.width - size.height) * pos
                drawCircle(c.outline.copy(alpha = 0.25f), k, Offset(cx, size.height / 2 + 1.5f.dp.toPx()))
                drawCircle(if (checked) Color.White else c.muted, k, Offset(cx, size.height / 2))
            }
        )
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.surface,
        shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(24.dp),
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
