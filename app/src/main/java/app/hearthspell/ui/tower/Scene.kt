package app.hearthspell.ui.tower

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind
import app.hearthspell.content.Potion
import app.hearthspell.content.Season
import app.hearthspell.content.Trinket
import app.hearthspell.data.Decor
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Looks
import app.hearthspell.ui.art.Sprite
import app.hearthspell.ui.art.bitmap
import app.hearthspell.ui.art.drawPixels
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** Scene width in art pixels: the tower (120 wide) with a strip of sky on each side. */
internal const val GW = 128

/** The tower's left edge in the scene grid. */
internal const val TX = (GW - SCENE_W) / 2

/** Rows of sky above the roof bitmap. */
private const val SKY_ROWS = 18

/** Rows in the roof and base canvases. */
internal const val ROOF_ROWS = SKY_ROWS + ROOF_H
internal const val BASE_ROWS = BASE_H + 10

/**
 * Animation time in steps of [stepMs], advanced every frame (so 2.5 is halfway between steps 2 and 3); read it inside
 * draw lambdas so only the canvases redraw. Movement uses the fraction, flickers use whole steps.
 */
@Composable
internal fun rememberClock(stepMs: Long = 150): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(stepMs) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> t.floatValue = (now - start) / 1_000_000f / stepMs }
    }
    return t
}

/** One animation step every [stepMs]; read it inside draw lambdas so only the canvases redraw. */
@Composable
internal fun rememberTicker(stepMs: Long = 150): State<Long> {
    val tick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(stepMs) {
        while (true) {
            delay(stepMs)
            tick.longValue = tick.longValue + 1
        }
    }
    return tick
}

/** Draws on a grid of art pixels [px] wide, offset to centre the scene. */
internal class Grid(private val ds: DrawScope, val px: Float, val ox: Float, val oy: Float) {
    fun rect(x: Int, y: Int, w: Int, h: Int, c: Color, alpha: Float = 1f) {
        if (w <= 0 || h <= 0) return
        ds.drawRect(c, Offset(ox + x * px, oy + y * px), Size(w * px + 0.5f, h * px + 0.5f), alpha = alpha)
    }

    fun dot(x: Int, y: Int, c: Color, alpha: Float = 1f) = rect(x, y, 1, 1, c, alpha)

    /** A rectangle at fractional cells, for things in motion. */
    fun rectAt(x: Float, y: Float, w: Float, h: Float, c: Color, alpha: Float = 1f) {
        if (w <= 0f || h <= 0f) return
        ds.drawRect(c, Offset(ox + x * px, oy + y * px), Size(w * px, h * px), alpha = alpha)
    }

    /** [s] repeated over a box of cells (a sky texture), its tiles shifted down by [offY] cells. */
    fun tiled(s: Sprite, x0: Int, y0: Int, w: Int, h: Int, offY: Int) {
        val img = s.bitmap()
        ds.clipRect(ox + x0 * px, oy + y0 * px, ox + (x0 + w) * px, oy + (y0 + h) * px) {
            var ty = y0 - Math.floorMod(y0 + offY, s.h)
            while (ty < y0 + h) {
                var tx = x0 - Math.floorMod(x0, s.w)
                while (tx < x0 + w) {
                    drawPixels(img, ox + tx * px, oy + ty * px, px)
                    tx += s.w
                }
                ty += s.h
            }
        }
    }

    /** A soft round glow at fractional cells: bright in the middle, fading to nothing at radius [r]. */
    fun glow(x: Float, y: Float, r: Float, c: Color, alpha: Float = 1f) {
        if (r <= 0f || alpha <= 0f) return
        val center = Offset(ox + x * px, oy + y * px)
        ds.drawCircle(
            Brush.radialGradient(0f to c.copy(alpha = alpha), 0.55f to c.copy(alpha = alpha * 0.6f), 1f to c.copy(alpha = 0f), center = center, radius = r * px),
            r * px, center,
        )
    }

    /** A smooth line between fractional cells, [width] cells thick. */
    fun lineAt(x0: Float, y0: Float, x1: Float, y1: Float, c: Color, alpha: Float = 1f, width: Float = 0.3f) =
        ds.drawLine(c, Offset(ox + x0 * px, oy + y0 * px), Offset(ox + x1 * px, oy + y1 * px), width * px, StrokeCap.Round, alpha = alpha)

    /** A four-pointed sparkle centred at fractional cells. */
    fun sparkle(x: Float, y: Float, size: Float, c: Color, alpha: Float = 1f) {
        glow(x, y, size * 0.9f, c, alpha * 0.45f)
        lineAt(x - size, y, x + size, y, c, alpha, size * 0.22f)
        lineAt(x, y - size, x, y + size, c, alpha, size * 0.22f)
    }

    /** Cells past the scene grid on each side, out to the screen edges. */
    val margin: Int get() = kotlin.math.ceil(ox / px).toInt() + 1

    /** A sprite at fractional cells, for things in motion. */
    fun spriteAt(s: Sprite, x: Float, y: Float, flip: Boolean = false, alpha: Float = 1f) =
        with(ds) { drawPixels(s, ox + x * px, oy + y * px, px, flip, alpha) }

    fun sprite(s: Sprite, x: Int, y: Int, flip: Boolean = false, alpha: Float = 1f) =
        with(ds) { drawPixels(s, ox + x * px, oy + y * px, px, flip, alpha) }

    fun image(img: ImageBitmap, x: Int, y: Int) = with(ds) { drawPixels(img, ox + x * px, oy + y * px, px) }
}

/** Art pixel size for a scene as wide as the screen. */
internal fun cellPx(widthPx: Float): Float = maxOf(1f, floor(widthPx / GW))

internal fun DrawScope.grid(): Grid {
    val px = cellPx(size.width)
    return Grid(this, px, floor((size.width - GW * px) / 2), 0f)
}

/** The sky: night with stars, or a pale day. */
internal class Sky(val day: Boolean) {
    val top = if (day) Color(0xFF7CC4F0) else Color(0xFF0B0920)
    val mid = if (day) Color(0xFF9DD5F6) else Color(0xFF151135)
    val low = if (day) Color(0xFFC2E6FA) else Color(0xFF211A4A)
    val star = Color(0xFFFFF4D6)
    val starDim = Color(0xFFA996E6)
}

/** A cheap deterministic hash for scattering stars and dust. */
internal fun hash(a: Long, b: Long): Int {
    var h = a * 6364136223846793005L + b * 1442695040888963407L
    h = h xor (h ushr 29)
    h *= -0x40a7b892e31b1a47L
    return (h ushr 33).toInt() and 0x7fffffff
}

/** Sky in three bands with dithered seams between them, over [x0, x0+w) and [y0, y0+h). */
private fun Grid.skyBands(x0: Int, y0: Int, w: Int, h: Int, sky: Sky) {
    Art.sky(sky.day)?.let { tiled(it, x0, y0, w, h, x0 * 7 + y0); return }
    val a = h / 3
    val b = h * 2 / 3
    rect(x0, y0, w, a, sky.top)
    rect(x0, y0 + a, w, b - a, sky.mid)
    rect(x0, y0 + b, w, h - b, sky.low)
    for (x in x0 until x0 + w step 2) {
        dot(x + (y0 % 2), y0 + a, sky.top)
        dot(x + 1 - (y0 % 2), y0 + b, sky.mid)
    }
}

/**
 * The sky from [Sky.top] down to [Sky.low] over [rows], in ten flat steps with a dithered row at each seam,
 * so it reads as a gradient but stays pixel art.
 */
internal fun Grid.skyGradient(rows: Int, sky: Sky) {
    Art.sky(sky.day)?.let { tiled(it, -margin, 0, GW + margin * 2, rows, 0); return }
    val steps = 10
    fun shade(i: Int): Color {
        val t = i / (steps - 1f)
        return if (t < 0.5f) lerp(sky.top, sky.mid, t * 2) else lerp(sky.mid, sky.low, (t - 0.5f) * 2)
    }
    val band = rows / steps.toFloat()
    // Reach past the scene's grid to the screen edges.
    val margin = kotlin.math.ceil(ox / px).toInt() + 1
    for (i in 0 until steps) {
        val y = (i * band).toInt()
        val next = ((i + 1) * band).toInt()
        rect(-margin, y, GW + margin * 2, next - y, shade(i))
        if (i > 0) for (x in -margin until GW + margin step 2) dot(x + (i % 2), y, shade(i - 1))
    }
}

/** A soft glow around a cell, for candles, the moon and potions. */
internal fun Grid.disc(cx: Int, cy: Int, r: Int, c: Color, alpha: Float) = glow(cx + 0.5f, cy + 0.5f, r + 0.5f, c, alpha)

/** Twinkling stars scattered over a box. */
internal fun Grid.stars(x0: Int, y0: Int, w: Int, h: Int, n: Int, seed: Long, tick: Long, sky: Sky) {
    if (sky.day || w <= 0 || h <= 0) return
    // Over the painted sky only a few stars twinkle, as soft sparkles.
    if (Art.sky(false) != null) {
        for (i in 0 until n) {
            val hh = hash(seed, i.toLong())
            val phase = (tick + hh) % 40
            if (phase < 6) sparkle(x0 + hh % w + 0.5f, y0 + hh / 7 % h + 0.5f, 1.4f, sky.star, sin(phase / 6f * PI.toFloat()))
        }
        return
    }
    for (i in 0 until n) {
        val hh = hash(seed, i.toLong())
        val x = x0 + hh % w
        val y = y0 + hh / 7 % h
        val phase = (tick / 2 + hh) % 23
        when {
            phase == 0L -> {
                dot(x, y, sky.star)
                dot(x - 1, y, sky.star, 0.5f)
                dot(x + 1, y, sky.star, 0.5f)
                dot(x, y - 1, sky.star, 0.5f)
                dot(x, y + 1, sky.star, 0.5f)
            }
            i % 3 == 0 -> dot(x, y, sky.star, 0.9f)
            else -> dot(x, y, sky.starDim, 0.7f)
        }
    }
}

/** The strips of sky either side of a tower segment. */
private fun Grid.sideSky(rows: Int, sky: Sky, seed: Long, tick: Long) {
    Art.sky(sky.day)?.let { t ->
        val off = hash(seed, 1) % t.h
        tiled(t, -margin, 0, TX + margin, rows, off)
        tiled(t, TX + SCENE_W, 0, GW - TX - SCENE_W + margin, rows, off)
        if (sky.day) return
    }
    stars(0, 0, TX, rows, rows / 12, seed, tick, sky)
    stars(TX + SCENE_W, 0, GW - TX - SCENE_W, rows, rows / 12, seed + 1, tick, sky)
}

internal val WALL_DECOR = setOf(
    DecorKind.PAINTING, DecorKind.CLOCK, DecorKind.BANNER, DecorKind.HERBS, DecorKind.LANTERN, DecorKind.OLD_MAP, DecorKind.PAPER_LANTERN,
)

private val DUST = Color(0xFFD8C8A8)
private val DUST_VEIL = Color(0xFF6E5C4C)
private val SHADOW = Color(0xFF140A1A)
private val FIRE_HOT = Color(0xFFFFFCD6)
private val FIRE_MID = Color(0xFFFF9428)
internal val FIRE_GLOW = Color(0xFFFFDA62)

/** Everything that lives on a floor, gathered for drawing. */
internal class FloorLook(
    val id: Long,
    val kind: FloorKind,
    val dust: Float,
    val imps: List<ImpKind>,
    val helpers: List<HelperKind>,
    val decor: List<Decor>,
    /** The tower cat sleeps here. */
    val cat: Boolean = false,
    /** A trinket a helper left here, glinting on the floor. */
    val gift: Trinket? = null,
    /** Late at night on a tidy floor the helpers doze. */
    val asleep: Boolean = false,
    /** The apprentice walking about (on the floor being looked at). */
    val apprentice: ((Int) -> Sprite)? = null,
    val potions: Set<Potion> = emptySet(),
    val seasons: Set<Season> = emptySet(),
) {
    val clean: Boolean get() = imps.isEmpty() && dust < 0.35f
}

/** Taps on a floor scene: when (in ticks) each helper and the cat were last patted. Read while drawing. */
@Stable
internal class Touch {
    val pats = mutableStateMapOf<HelperKind, Long>()
    var cat by mutableLongStateOf(-1000L)
    var apprentice by mutableLongStateOf(-1000L)
}

/** Where things stand on a floor, so taps can find them. */
internal fun FloorLook.helperSpots(tick: Long, m: SceneMeta): List<Pair<HelperKind, IntRange>> {
    val lanes = helpers.size + if (apprentice != null) 1 else 0
    return helpers.mapIndexed { i, kind ->
        val s = Art.helper(kind, 0)
        val (x, _) = if (asleep) restX(i, s.w, id, m.walk, lanes) else helperX(i, s.w, tick, id, m.walk, lanes)
        kind to (x until x + s.w)
    }
}

/** Where a dozing helper rests: the middle of its lane. */
internal fun restX(i: Int, w: Int, id: Long, walk: IntRange, count: Int): Pair<Int, Boolean> {
    val lane = (walk.last - walk.first) / count.coerceAtLeast(1)
    val x = if (lane >= w + 6) walk.first + lane * i + (lane - w) / 2 else walk.first + (hash(id, i.toLong()) % (walk.last - walk.first - w).coerceAtLeast(1))
    return x to (i % 2 == 1)
}

internal const val CAT_X = 14
internal const val GIFT_X = SCENE_W - 34

/** The cat curls up on a cushion if the floor has one, else by the left wall. */
internal fun FloorLook.catX(m: SceneMeta): Int {
    val cushion = decor.indexOfFirst { it.kind == DecorKind.CUSHION.name }
    return if (cushion >= 0) cushionX(m, cushion) + 1 else CAT_X
}

private fun FloorLook.cushionX(m: SceneMeta, index: Int): Int {
    // Mirrors the standing-decor placement in drawFloor.
    val items = decor.mapNotNull { DecorKind.of(it.kind) }.filter { it != DecorKind.RUG }
    var floorSpot = 0
    var wallSpot = 0
    val spots = m.floor.size.coerceAtLeast(1)
    val target = decor[index].kind
    items.forEach { k ->
        val s = Art.decor(k)
        if (k in WALL_DECOR && wallSpot < m.wall.size) { wallSpot++; return@forEach }
        val base = m.floor.getOrNull(floorSpot % spots) ?: 40
        val x = (base - (floorSpot / spots) * 18).coerceIn(10, SCENE_W - 10 - s.w)
        floorSpot++
        if (k.name == target) return x
    }
    return CAT_X
}

/** Where a helper stands at step [tick], in whole cells (for taps). */
internal fun helperX(i: Int, w: Int, tick: Long, id: Long, walk: IntRange, count: Int = 1): Pair<Int, Boolean> =
    helperAt(i, w, tick.toFloat(), id, walk, count).let { (x, f) -> x.roundToInt() to f }

/** Where a helper stands at time [t]: x in fractional cells and whether it faces left. */
internal fun helperAt(i: Int, w: Int, t: Float, id: Long, walk: IntRange, count: Int = 1): Pair<Float, Boolean> {
    // Several helpers share the floor in lanes, so they don't walk through each other.
    val lane = (walk.last - walk.first) / count.coerceAtLeast(1)
    val own = lane >= w + 6
    val lo = if (own) walk.first + lane * i else walk.first
    val hi = (if (own) lo + lane else walk.last) - w
    val span = (hi - lo).coerceAtLeast(1).toFloat()
    // Each helper paces at its own speed and phase, easing into a little rest at each end.
    val rest = 16f
    val period = span * 2 + rest * 2
    val speed = (2 + hash(id, i.toLong()) % 2) / 3f
    val p = (t * speed + hash(id, i + 7L) % period.toInt()) % period
    fun ease(f: Float) = (1 - cos(f * PI.toFloat())) / 2
    return when {
        p < span -> (lo + span * ease(p / span)) to false
        p < span + rest -> hi.toFloat() to false
        p < span * 2 + rest -> (hi - span * ease((p - span - rest) / span)) to true
        else -> lo.toFloat() to true
    }
}

/** A happy hop after a pat, [since] steps after it: a big bounce, then a little one. */
internal fun hop(since: Float): Float = when {
    since < 0f -> 0f
    since < 6f -> sin(since / 6f * PI.toFloat()) * 6f
    since < 11f -> sin((since - 6f) / 5f * PI.toFloat()) * 3f
    else -> 0f
}

/** A soft oval shadow on the floor under something standing there. */
private fun Grid.footShadow(cx: Int, y: Int, w: Int) = footShadow(cx.toFloat(), y, w.toFloat())

private fun Grid.footShadow(cx: Float, y: Int, w: Float) {
    if (w <= 4f) return
    rectAt(cx - w / 2, y.toFloat(), w, 1f, SHADOW, 0.35f)
    rectAt(cx - w / 2 + 2, y + 1f, w - 4, 1f, SHADOW, 0.22f)
}

/** A candle flame: the full sprite for fires, a tiny two-tone flicker for little candles. */
private fun Grid.flame(x: Int, y: Int, size: Float, frame: Int) {
    if (size >= 1f) {
        val s = Art.flame(frame)
        sprite(s, x - s.w / 2, y - s.h + 1)
        if (size >= 1.5f) {
            val t = Art.flame(frame + 1)
            sprite(t, x - t.w / 2 + 3, y - t.h + 2)
        }
    } else {
        val sway = if (frame == 1) 1 else 0
        rect(x - 1 + sway, y - 3, 2, 3, FIRE_MID)
        dot(x + sway - (if (frame == 2) 1 else 0), y - 4, FIRE_GLOW)
        dot(x, y - 1, FIRE_HOT)
        rect(x - 3, y - 6, 7, 7, FIRE_GLOW, 0.06f + 0.04f * frame)
    }
}

/** The window's live sky, drawn before the room bitmap so it shows through the glass. */
private fun Grid.windowSky(m: SceneMeta, sky: Sky, t: Float, id: Long) {
    val tick = t.toLong()
    val x = TX + m.glass[0]
    val y = m.glass[1]
    val w = m.glass[2]
    val h = m.glass[3]
    skyBands(x, y, w, h, sky)
    if (sky.day) {
        val c = Art.cloud
        val travel = (w + c.w).coerceAtLeast(1)
        val cx = ((t / 6 + hash(id, 3) % travel) % travel) - c.w
        spriteAt(c, x + cx, (y + h / 4).toFloat())
    } else {
        stars(x, y, w, h, (w * h / 40).coerceIn(3, 30), id * 13 + 5, tick, sky)
    }
}

/** Draws one floor of the tower in full: sky, room, decor, flames, helpers, imps and dust. */
internal fun DrawScope.drawFloor(look: FloorLook, bg: ImageBitmap, t: Float, sky: Sky, touch: Touch? = null) {
    val tick = t.toLong()
    val g = grid()
    val m = SCENES.getValue(look.kind)
    g.sideSky(SCENE_H, sky, look.id, tick)
    g.seasonAir(SCENE_H, look.seasons, look.potions, sky, tick, look.id)
    g.windowSky(m, sky, t, look.id)
    g.image(bg, TX, 0)

    // Decor: a rug lies on the floor first; wall pieces hang on free wall spots; the rest stand along the floor.
    val items = look.decor.mapNotNull { d -> DecorKind.of(d.kind) }
    items.filter { it == DecorKind.RUG }.take(1).forEach { k ->
        val s = Art.decor(k)
        g.sprite(s, TX + (m.floor.firstOrNull() ?: 40) - 4, SCENE_FLOOR + 6 - s.h)
    }
    var wallSpot = 0
    var floorSpot = 0
    val spots = m.floor.size.coerceAtLeast(1)
    items.filter { it != DecorKind.RUG }.forEach { k ->
        val s = Art.decor(k, ((tick / 4) % 2).toInt())
        if (k in WALL_DECOR && wallSpot < m.wall.size) {
            val (x, y) = m.wall[wallSpot++]
            g.sprite(s, TX + x, y)
        } else {
            val base = m.floor.getOrNull(floorSpot % spots) ?: 40
            val x = (base - (floorSpot / spots) * 18).coerceIn(10, SCENE_W - 10 - s.w)
            floorSpot++
            g.footShadow(TX + x + s.w / 2, SCENE_FLOOR, s.w)
            g.sprite(s, TX + x, SCENE_FLOOR + 1 - s.h)
        }
    }

    // Fires and candles flicker; steam rises.
    m.flames.forEachIndexed { i, (x, y, size) -> g.flame(TX + x, y, size, ((tick + i) % 3).toInt()) }
    m.steam.forEachIndexed { i, (x, y) ->
        val s = Art.steam(((tick / 2 + i) % 4).toInt())
        g.sprite(s, TX + x - s.w / 2, y - s.h, alpha = 0.85f)
    }

    // Dust: a dim veil, motes and cobwebs once it gets thick.
    if (look.dust > 0.05f) {
        val inL = TX + 8
        val inW = SCENE_W - 16
        g.rect(inL, 5, inW, SCENE_FLOOR - 5, DUST_VEIL, alpha = look.dust * 0.22f)
        val motes = (look.dust * 60).toInt()
        for (i in 0 until motes) {
            val h = hash(look.id + 99, i.toLong())
            // Motes drift slowly downwards and sway.
            val y = 6 + ((h / 97 + t / 5) % (SCENE_FLOOR - 8))
            val x = inL + h % inW + sin((t + h) / 9f) * 1.5f
            g.glow(x + 0.5f, y + 0.5f, 0.9f, DUST, 0.5f)
        }
        if (look.dust > 0.45f) {
            val web = Art.cobweb
            val a = (look.dust - 0.3f).coerceIn(0f, 1f)
            g.sprite(web, inL, 5, alpha = a)
            g.sprite(web, inL + inW - web.w, 5, flip = true, alpha = a)
        }
    }

    // Light: a tidy floor glows warm at night, a dusty one sinks into the dark, imps flicker violet.
    g.lighting(look, m, tick, sky)
    if (Potion.BUBBLES in look.potions) g.bubbles(look.id, tick)

    // A gift glints where a helper left it.
    look.gift?.let { gift ->
        val s = Art.trinket(gift.name)
        val bob = (sin(t / 4f) + 1f) / 2f
        g.disc(TX + GIFT_X + s.w / 2, SCENE_FLOOR - s.h / 2, 9, FIRE_GLOW, 0.10f + 0.05f * (sin(t / 3f) + 1f) / 2f)
        g.spriteAt(s, (TX + GIFT_X).toFloat(), SCENE_FLOOR + 1f - s.h - bob)
        if ((tick + look.id) % 10 < 3) g.twinkle(TX + GIFT_X + s.w - 2, SCENE_FLOOR - s.h - 2)
    }

    // Helpers pace along the floor, bobbing and blinking; late at night on a tidy floor they doze.
    val lanes = look.helpers.size + if (look.apprentice != null) 1 else 0
    look.helpers.forEachIndexed { i, kind ->
        val since = t - (touch?.pats?.get(kind) ?: -1000L)
        val patted = since in 0f..20f
        // Eyes shut asleep, or for a quick blink now and then.
        val blink = (t + i * 7 + look.id % 11) % 26f < 1f
        val s = Art.helper(kind, if ((look.asleep && !patted) || blink) 3 else 0)
        val (x, flip) = if (look.asleep) restX(i, s.w, look.id, m.walk, lanes).let { (a, b) -> a.toFloat() to b } else helperAt(i, s.w, t, look.id, m.walk, lanes)
        // A gentle breathing bob, and a bounce when patted.
        val lift = (if (look.asleep) 0.25f else 0.6f) * (sin(t / 2.2f + i * 1.7f) + 1f) + hop(since)
        if (Potion.STARDUST in look.potions && !look.asleep) g.sparkleTrail(TX + x.roundToInt() + s.w / 2, SCENE_FLOOR - 4, flip, tick, i)
        g.footShadow(TX + x + s.w / 2f, SCENE_FLOOR + 1, s.w - 6 - lift)
        g.spriteAt(s, TX + x, SCENE_FLOOR + 2f - s.h - lift, flip)
        if (patted) g.hearts(TX + x.roundToInt() + s.w / 2, SCENE_FLOOR - s.h - 2, since)
        else if (look.asleep) g.zees(TX + x.roundToInt() + s.w / 2 + 3, SCENE_FLOOR - s.h, t, look.id + i)
    }

    // The apprentice walks the floor too, in the clothes from the wardrobe.
    look.apprentice?.let { dress ->
        val since = t - (touch?.apprentice ?: -1000L)
        val s = dress(0)
        val i = look.helpers.size
        val (x, flip) = if (look.asleep) restX(i, s.w, look.id, m.walk, lanes).let { (a, b) -> a.toFloat() to b } else helperAt(i, s.w, t, look.id + 5, m.walk, lanes)
        // A soft step bob while walking, and a bounce when tapped.
        val lift = (if (look.asleep) 0f else abs(sin(t / 1.6f)) * 0.7f) + hop(since)
        g.footShadow(TX + x + s.w / 2f, SCENE_FLOOR + 1, s.w - 6f)
        g.spriteAt(s, TX + x, SCENE_FLOOR + 2f - s.h - lift, flip)
        if (since in 0f..20f) g.twinkle(TX + x.roundToInt() + (if (flip) 1 else s.w - 2), SCENE_FLOOR - s.h + 2)
    }

    // The tower cat, in front of everyone: curled up asleep, sitting up for a moment when patted.
    if (look.cat) {
        val cx = look.catX(m)
        val since = t - (touch?.cat ?: -1000L)
        if (since in 0f..24f) {
            val s = Art.catAwake(0)
            g.footShadow(TX + cx + s.w / 2, SCENE_FLOOR + 1, s.w - 4)
            g.sprite(s, TX + cx + 3, SCENE_FLOOR + 2 - s.h)
            g.hearts(TX + cx + 12, SCENE_FLOOR - s.h, since)
        } else {
            val s = Art.catAsleep(0)
            // Breathing: a slow, slight rise.
            val breath = (sin(t / 5f) + 1f) * 0.25f
            g.footShadow(TX + cx + s.w / 2, SCENE_FLOOR + 1, s.w - 2)
            g.spriteAt(s, (TX + cx).toFloat(), SCENE_FLOOR + 2f - s.h - breath)
            g.zees(TX + cx + 6, SCENE_FLOOR - s.h - 2, t, look.id)
        }
    }

    // Imps hop about and grin, each in its own colours.
    val impSpots = listOf(30, 66, 88)
    look.imps.take(3).forEachIndexed { i, kind ->
        val s = Looks.imp(kind, 0)
        // Bouncy hops with a short rest on the floor between them, scuttling to and fro.
        val ph = ((t + i * 3) % 10f) / 8f
        val hop = if (ph < 1f) sin(ph * PI.toFloat()) * 5f else 0f
        val drift = (t + i * 40) / 12f
        val x = impSpots[i] + sin(drift) * 6f
        g.footShadow(TX + x + s.w / 2f, SCENE_FLOOR + 1, s.w - 8 - hop)
        g.spriteAt(s, TX + x, SCENE_FLOOR + 2f - s.h - hop, flip = cos(drift) < 0f)
    }

    if (Potion.BLOSSOM in look.potions) g.blossomWalls(look.id, SCENE_H, tick)
}

/** The sky, the moon (or sun and clouds), the roof with its chimney smoke and glowing spire. */
internal fun DrawScope.drawRoof(roof: ImageBitmap, t: Float, sky: Sky, ts: TowerSky = TowerSky()) {
    val tick = t.toLong()
    val g = grid()
    drawRect(sky.top, Offset.Zero, size)
    g.skyGradient(ROOF_ROWS, sky)
    g.stars(0, 0, GW, ROOF_ROWS - 10, if (Potion.MOONGLOW in ts.potions) 80 else 46, 7, if (Potion.MOONGLOW in ts.potions) tick * 2 else tick, sky)
    if (!sky.day) g.constellations(ts.constellations, tick)
    g.seasonAir(ROOF_ROWS - 8, ts.seasons, ts.potions, sky, tick, 3, wide = true)
    g.skyShow(ts, sky, tick)
    if (sky.day) {
        val sun = Color(0xFFFFE070)
        g.rect(12, 6, 10, 10, sun)
        g.rect(11, 7, 12, 8, sun)
        g.rect(13, 5, 8, 12, sun)
        g.rect(14, 8, 3, 3, Color(0xFFFFF8D0))
        val c = Art.cloud
        val span = GW + c.w * 2
        listOf(0, 61).forEachIndexed { k, off ->
            val x = ((t / (5 + k * 3) + off) % span) - c.w
            g.spriteAt(c, x, 14f + k * 16)
        }
    } else {
        if (Potion.MOONGLOW in ts.potions) {
            g.disc(19, 15, 16, Color(0xFFB062E0), 0.10f + 0.04f * ((tick / 4) % 2))
            g.disc(19, 15, 12, Color(0xFFD696F0), 0.10f)
        }
        g.sprite(Art.moon, 10, 6)
        // A pumpkin-coloured moon for Hallowe'en.
        if (Season.HALLOWEEN in ts.seasons) g.disc(19, 15, 8, Color(0xFFFC7400), 0.28f)
    }
    val oy = SKY_ROWS
    val rx = (GW - ROOF_W) / 2
    g.image(roof, rx, oy)
    if (Season.WINTER in ts.seasons) g.snowCap(roof, rx, oy)
    if (Season.NEW_YEAR in ts.seasons) g.garland(rx, oy + ROOF_H - 5, ROOF_W, tick)
    // Smoke puffs from the chimney, rising and fading; pink and gold with the embersmoke potion.
    val (cx, cy) = ROOF_CHIMNEY
    val smoke = if (sky.day) Color(0xFFE8ECF4) else Color(0xFF8C86A8)
    val ember = Potion.EMBERSMOKE in ts.potions
    for (k in 0 until 5) {
        val life = ((tick + k * 6) % 30).toInt()
        val r = 1 + life / 8
        val x = rx + cx + life / 3 + (sin((tick + k * 9) / 6.0) * 1.5).toInt()
        val y = oy + cy - life
        if (ember) {
            // Round puffs, pink and gold in turn.
            val c = if (k % 2 == 0) Color(0xFFF878B8) else Color(0xFFFCE070)
            g.disc(x, y, r + 1, c, (1f - life / 30f) * 0.75f)
            g.dot(x - r / 2, y - r / 2, Color(0xFFFFFCF4), (1f - life / 30f) * 0.8f)
        } else {
            // Puffs rise smoothly and swell as they go.
            val lf = (t + k * 6) % 30f
            val rr = 1f + lf / 8f
            val fx = rx + cx + lf / 3f + sin((t + k * 9) / 6f) * 1.5f
            val fy = oy + cy - lf
            g.glow(fx, fy, rr + 1.5f, smoke, alpha = (1f - lf / 30f) * 0.8f)
        }
    }
    // The spire's lantern.
    val (lx, ly) = ROOF_LANTERN
    g.flame(rx + lx + 1, oy + ly + 1, 1f, (tick % 3).toInt())
    val flicker = 0.03f * (tick % 3)
    g.disc(rx + lx + 1, oy + ly - 3, 7, FIRE_GLOW, 0.05f + flicker)
    g.disc(rx + lx + 1, oy + ly - 3, 4, FIRE_GLOW, 0.07f + flicker)
}

/** The ground floor: the front door between flickering torches, grass, and fireflies at night. */
internal fun DrawScope.drawBase(base: ImageBitmap, t: Float, sky: Sky, ts: TowerSky = TowerSky()) {
    val tick = t.toLong()
    val g = grid()
    g.sideSky(BASE_GROUND, sky, 11, tick)
    // The sky shows above the wall's crenellations too.
    Art.sky(sky.day)?.let { g.tiled(it, TX, 0, SCENE_W, BASE_GROUND, hash(11, 1) % it.h) }
    g.seasonAir(BASE_GROUND, ts.seasons, ts.potions, sky, tick, 11)
    val snow = Season.WINTER in ts.seasons
    val grass = when {
        snow && sky.day -> listOf(Color(0xFFF4F8FF), Color(0xFFDCE6F4), Color(0xFFB8C6DC))
        snow -> listOf(Color(0xFFB8C2E0), Color(0xFF8C98BC), Color(0xFF6A7498))
        sky.day -> listOf(Color(0xFF62B04A), Color(0xFF4A9640), Color(0xFF357A38))
        else -> listOf(Color(0xFF2E6A44), Color(0xFF235438), Color(0xFF183E2C))
    }
    val ground = Art.ground
    if (ground == null) drawOldGrass(g, grass)
    g.image(base, TX, 0)
    if (ground != null) {
        // One painted strip of grass and soil across the whole width, in front of the wall's foot, so the tower
        // stands in the meadow and the grass is the same everywhere.
        val top = BASE_GROUND - 4
        g.tiled(ground, -g.margin, top, GW + g.margin * 2, BASE_ROWS - top, -top)
        if (!sky.day) g.rect(-g.margin, BASE_GROUND - 2, GW + g.margin * 2, BASE_ROWS, Color(0xFF0B0920), 0.4f)
        if (snow) g.rect(-g.margin, BASE_GROUND - 1, GW + g.margin * 2, BASE_ROWS, if (sky.day) Color(0xFFF4F8FF) else Color(0xFFB8C2E0), 0.6f)
    }
    BASE_TORCHES.forEachIndexed { i, (x, y) -> g.flame(TX + x, y + 1, 1f, ((tick + i) % 3).toInt()) }
    if (Potion.BLOSSOM in ts.potions) g.blossomWalls(5, BASE_GROUND, tick)
    // Jack-o'-lanterns on the steps.
    if (Season.HALLOWEEN in ts.seasons) {
        val p = Art.decor(DecorKind.PUMPKIN, ((tick / 3) % 2).toInt())
        g.sprite(p, GW / 2 - 24 - p.w, BASE_ROWS - p.h - 1)
        g.sprite(p, GW / 2 + 24, BASE_ROWS - p.h - 1)
    }
    if (Season.NEW_YEAR in ts.seasons) {
        val f = Art.decor(DecorKind.FIR, ((tick / 4) % 2).toInt())
        g.sprite(f, 4, BASE_ROWS - f.h)
    }
    if (Season.SUMMER in ts.seasons && sky.day) g.butterflies(tick)
    if (Potion.FIREFLIES in ts.potions) g.rainbowFlies(tick, BASE_GROUND)
    if (!sky.day) {
        for (k in 0 until 6) {
            val h = hash(77, k.toLong())
            val x = h % GW + sin((t + h) / 14f) * 10f
            val y = BASE_GROUND - 6 + sin((t + h * 3) / 10f) * 5f + h / 11 % 10
            // Fireflies glow up and fade rather than blink.
            val glow = (sin((t + h) / 3f) + 1f) / 2f
            g.glow(x + 0.5f, y + 0.5f, 2.2f, FIRE_GLOW, alpha = 0.35f * glow)
            g.glow(x + 0.5f, y + 0.5f, 0.8f, Color(0xFFFFF6C8), alpha = 0.3f + 0.7f * glow)
        }
    }
}

/** The procedural grass, used until the painted strip exists. */
private fun DrawScope.drawOldGrass(g: Grid, grass: List<Color>) {
    drawRect(grass[1], Offset(0f, g.px * BASE_GROUND), Size(size.width, size.height - g.px * BASE_GROUND))
    drawRect(grass[0], Offset(0f, g.px * BASE_GROUND), Size(size.width, g.px))
    for (i in 0 until 70) {
        val h = hash(5, i.toLong())
        val x = h % GW
        val y = BASE_GROUND + 1 + h / 13 % (BASE_ROWS - BASE_GROUND - 1)
        g.dot(x, y, if (i % 2 == 0) grass[2] else grass[0])
        if (i % 9 == 0) g.dot(x, y - 1, if (i % 18 == 0) Color(0xFFF890B0) else Color(0xFFFFE070))
    }
    g.rect(GW / 2 - 9, BASE_GROUND, 18, BASE_ROWS - BASE_GROUND, Color(0xFFC8A878))
    g.rect(GW / 2 - 9, BASE_GROUND, 18, 1, Color(0xFFE8C88A))
}

/** An empty floor waiting to be built: scaffolding over a dark, unfinished room. */
internal fun DrawScope.drawScaffold(t: Float, sky: Sky) {
    val tick = t.toLong()
    val g = grid()
    val rows = SCENE_H / 2
    g.sideSky(rows, sky, 21, tick)
    Art.scaffold?.let { s ->
        g.sprite(s, TX, 0)
        // The work lantern on the right post flickers softly.
        g.glow(TX + 94.5f, 12f, 9f, FIRE_GLOW, 0.10f + 0.05f * sin(t / 2.3f) + 0.03f * sin(t * 1.7f))
        return
    }
    g.rect(TX, 0, SCENE_W, rows, Color(0xFF1A1430))
    for (y in 0 until rows step 6) for (x in TX until TX + SCENE_W step 2) g.dot(x + (y / 6) % 2, y, Color(0xFF241C40))
    val wood = Color(0xFF9C6236)
    val woodDark = Color(0xFF52282A)
    for (x in TX until TX + SCENE_W step 15) {
        g.rect(x, 0, 3, rows, woodDark)
        g.rect(x, 0, 2, rows, wood)
    }
    g.rect(TX, 0, SCENE_W, 3, woodDark)
    g.rect(TX, 0, SCENE_W, 2, wood)
    g.rect(TX, rows - 3, SCENE_W, 3, woodDark)
    g.rect(TX, rows - 3, SCENE_W, 2, wood)
    for (k in 0 until 7) {
        val x = TX + k * 17
        for (j in 0 until 15) g.dot(x + j, rows - 4 - j * (rows - 6) / 15, woodDark)
    }
}

/** A canvas sized for [rows] art rows at the screen width. */
@Composable
internal fun SceneCanvas(rows: Int, modifier: Modifier = Modifier, draw: DrawScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val px = cellPx(constraints.maxWidth.toFloat())
        val h: Dp = with(density) { (px * rows).toDp() }
        Canvas(Modifier.fillMaxWidth().height(h)) { draw() }
    }
}
