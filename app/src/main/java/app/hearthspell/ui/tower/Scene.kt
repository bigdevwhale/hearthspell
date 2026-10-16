package app.hearthspell.ui.tower

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.data.Decor
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Sprite
import app.hearthspell.ui.art.drawPixels
import kotlinx.coroutines.delay
import kotlin.math.floor
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

/** A filled pixel circle, for soft stepped glows. */
internal fun Grid.disc(cx: Int, cy: Int, r: Int, c: Color, alpha: Float) {
    for (dy in -r..r) {
        val half = kotlin.math.sqrt((r * r - dy * dy).toFloat()).toInt()
        rect(cx - half, cy + dy, half * 2 + 1, 1, c, alpha)
    }
}

/** Twinkling stars scattered over a box. */
private fun Grid.stars(x0: Int, y0: Int, w: Int, h: Int, n: Int, seed: Long, tick: Long, sky: Sky) {
    if (sky.day || w <= 0 || h <= 0) return
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
    stars(0, 0, TX, rows, rows / 12, seed, tick, sky)
    stars(TX + SCENE_W, 0, GW - TX - SCENE_W, rows, rows / 12, seed + 1, tick, sky)
}

private val WALL_DECOR = setOf(DecorKind.PAINTING, DecorKind.CLOCK, DecorKind.BANNER, DecorKind.HERBS, DecorKind.LANTERN)

private val DUST = Color(0xFFD8C8A8)
private val DUST_VEIL = Color(0xFF6E5C4C)
private val SHADOW = Color(0xFF140A1A)
private val FIRE_HOT = Color(0xFFFFFCD6)
private val FIRE_MID = Color(0xFFFF9428)
private val FIRE_GLOW = Color(0xFFFFDA62)

/** Everything that lives on a floor, gathered for drawing. */
internal class FloorLook(
    val id: Long,
    val kind: FloorKind,
    val dust: Float,
    val imps: Int,
    val helpers: List<HelperKind>,
    val decor: List<Decor>,
)

/** Where a helper stands right now: x in scene pixels and whether it faces left. */
internal fun helperX(i: Int, w: Int, tick: Long, id: Long, walk: IntRange, count: Int = 1): Pair<Int, Boolean> {
    // Several helpers share the floor in lanes, so they don't walk through each other.
    val lane = (walk.last - walk.first) / count.coerceAtLeast(1)
    val own = lane >= w + 6
    val lo = if (own) walk.first + lane * i else walk.first
    val hi = (if (own) lo + lane else walk.last) - w
    val span = (hi - lo).coerceAtLeast(1)
    // Each helper paces at its own speed and phase, resting a little at each end.
    val rest = 16
    val period = span * 2 + rest * 2
    val p = ((tick * (2 + hash(id, i.toLong()) % 2) / 3 + hash(id, i + 7L) % period) % period).toInt()
    return when {
        p < span -> (lo + p) to false
        p < span + rest -> hi to false
        p < span * 2 + rest -> (hi - (p - span - rest)) to true
        else -> lo to true
    }
}

/** A soft oval shadow on the floor under something standing there. */
private fun Grid.footShadow(cx: Int, y: Int, w: Int) {
    if (w <= 4) return
    rect(cx - w / 2, y, w, 1, SHADOW, 0.35f)
    rect(cx - w / 2 + 2, y + 1, w - 4, 1, SHADOW, 0.22f)
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
private fun Grid.windowSky(m: SceneMeta, sky: Sky, tick: Long, id: Long) {
    val x = TX + m.glass[0]
    val y = m.glass[1]
    val w = m.glass[2]
    val h = m.glass[3]
    skyBands(x, y, w, h, sky)
    if (sky.day) {
        val c = Art.cloud
        val travel = (w + c.w).coerceAtLeast(1)
        val cx = ((tick / 6 + hash(id, 3) % travel) % travel).toInt() - c.w
        sprite(c, x + cx, y + h / 4)
    } else {
        stars(x, y, w, h, (w * h / 40).coerceIn(3, 30), id * 13 + 5, tick, sky)
    }
}

/** Draws one floor of the tower in full: sky, room, decor, flames, helpers, imps and dust. */
internal fun DrawScope.drawFloor(look: FloorLook, bg: ImageBitmap, tick: Long, sky: Sky) {
    val g = grid()
    val m = SCENES.getValue(look.kind)
    g.sideSky(SCENE_H, sky, look.id, tick)
    g.windowSky(m, sky, tick, look.id)
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
            val y = 6 + ((h / 97 + tick / 5) % (SCENE_FLOOR - 8)).toInt()
            val x = inL + (h % inW + (sin((tick + h) / 9.0) * 1.5).toInt())
            g.dot(x, y, DUST, 0.55f)
        }
        if (look.dust > 0.45f) {
            val web = Art.cobweb
            val a = (look.dust - 0.3f).coerceIn(0f, 1f)
            g.sprite(web, inL, 5, alpha = a)
            g.sprite(web, inL + inW - web.w, 5, flip = true, alpha = a)
        }
    }

    // Helpers pace along the floor, bobbing and blinking.
    look.helpers.forEachIndexed { i, kind ->
        val frame = ((tick / 3 + i * 2) % 4).toInt()
        val s = Art.helper(kind, frame)
        val (x, flip) = helperX(i, s.w, tick, look.id, m.walk, look.helpers.size)
        g.footShadow(TX + x + s.w / 2, SCENE_FLOOR + 1, s.w - 6)
        g.sprite(s, TX + x, SCENE_FLOOR + 2 - s.h, flip)
    }

    // Imps hop about and grin.
    val impSpots = listOf(30, 66, 88)
    for (i in 0 until look.imps.coerceAtMost(3)) {
        val s = Art.imp(((tick / 3 + i) % 2).toInt())
        val hop = intArrayOf(0, 2, 4, 5, 4, 2, 0, 0)[((tick + i * 3) % 8).toInt()]
        val x = impSpots[i] + (sin((tick + i * 40) / 12.0) * 6).toInt()
        g.footShadow(TX + x + s.w / 2, SCENE_FLOOR + 1, s.w - 8 - hop)
        g.sprite(s, TX + x, SCENE_FLOOR + 2 - s.h - hop, flip = (tick / 24 + i) % 2 == 0L)
    }
}

/** The sky, the moon (or sun and clouds), the roof with its chimney smoke and glowing spire. */
internal fun DrawScope.drawRoof(roof: ImageBitmap, tick: Long, sky: Sky) {
    val g = grid()
    drawRect(sky.top, Offset.Zero, size)
    g.skyGradient(ROOF_ROWS, sky)
    g.stars(0, 0, GW, ROOF_ROWS - 10, 46, 7, tick, sky)
    if (sky.day) {
        val sun = Color(0xFFFFE070)
        g.rect(12, 6, 10, 10, sun)
        g.rect(11, 7, 12, 8, sun)
        g.rect(13, 5, 8, 12, sun)
        g.rect(14, 8, 3, 3, Color(0xFFFFF8D0))
        val c = Art.cloud
        val span = GW + c.w * 2
        listOf(0, 61).forEachIndexed { k, off ->
            val x = ((tick / (5 + k * 3) + off) % span).toInt() - c.w
            g.sprite(c, x, 14 + k * 16)
        }
    } else {
        g.sprite(Art.moon, 10, 6)
    }
    val oy = SKY_ROWS
    val rx = (GW - ROOF_W) / 2
    g.image(roof, rx, oy)
    // Smoke puffs from the chimney, rising and fading.
    val (cx, cy) = ROOF_CHIMNEY
    val smoke = if (sky.day) Color(0xFFE8ECF4) else Color(0xFF8C86A8)
    for (k in 0 until 5) {
        val life = ((tick + k * 6) % 30).toInt()
        val r = 1 + life / 8
        val x = rx + cx + life / 3 + (sin((tick + k * 9) / 6.0) * 1.5).toInt()
        val y = oy + cy - life
        g.rect(x - r, y - r, r * 2 + 1, r * 2 + 1, smoke, alpha = (1f - life / 30f) * 0.7f)
    }
    // The spire's lantern.
    val (lx, ly) = ROOF_LANTERN
    g.flame(rx + lx + 1, oy + ly + 1, 1f, (tick % 3).toInt())
    val flicker = 0.03f * (tick % 3)
    g.disc(rx + lx + 1, oy + ly - 3, 7, FIRE_GLOW, 0.05f + flicker)
    g.disc(rx + lx + 1, oy + ly - 3, 4, FIRE_GLOW, 0.07f + flicker)
}

/** The ground floor: the front door between flickering torches, grass, and fireflies at night. */
internal fun DrawScope.drawBase(base: ImageBitmap, tick: Long, sky: Sky) {
    val g = grid()
    g.sideSky(BASE_GROUND, sky, 11, tick)
    val grass = if (sky.day) listOf(Color(0xFF62B04A), Color(0xFF4A9640), Color(0xFF357A38))
    else listOf(Color(0xFF2E6A44), Color(0xFF235438), Color(0xFF183E2C))
    drawRect(grass[1], Offset(0f, g.px * BASE_GROUND), Size(size.width, size.height - g.px * BASE_GROUND))
    drawRect(grass[0], Offset(0f, g.px * BASE_GROUND), Size(size.width, g.px))
    for (i in 0 until 70) {
        val h = hash(5, i.toLong())
        val x = h % GW
        val y = BASE_GROUND + 1 + h / 13 % (BASE_ROWS - BASE_GROUND - 1)
        g.dot(x, y, if (i % 2 == 0) grass[2] else grass[0])
        if (i % 9 == 0) g.dot(x, y - 1, if (i % 18 == 0) Color(0xFFF890B0) else Color(0xFFFFE070))
    }
    // A path to the door.
    g.rect(GW / 2 - 9, BASE_GROUND, 18, BASE_ROWS - BASE_GROUND, Color(0xFFC8A878))
    g.rect(GW / 2 - 9, BASE_GROUND, 18, 1, Color(0xFFE8C88A))
    g.image(base, TX, 0)
    BASE_TORCHES.forEachIndexed { i, (x, y) -> g.flame(TX + x, y + 1, 1f, ((tick + i) % 3).toInt()) }
    if (!sky.day) {
        for (k in 0 until 6) {
            val h = hash(77, k.toLong())
            val x = h % GW + (sin((tick + h) / 14.0) * 10).toInt()
            val y = BASE_GROUND - 6 + (sin((tick + h * 3) / 10.0) * 5).toInt() + h / 11 % 10
            if ((tick / 3 + h) % 5 != 0L) {
                g.dot(x, y, FIRE_GLOW)
                g.rect(x - 1, y - 1, 3, 3, FIRE_GLOW, alpha = 0.25f)
            }
        }
    }
}

/** An empty floor waiting to be built: scaffolding over a dark, unfinished room. */
internal fun DrawScope.drawScaffold(tick: Long, sky: Sky) {
    val g = grid()
    val rows = SCENE_H / 2
    g.sideSky(rows, sky, 21, tick)
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
