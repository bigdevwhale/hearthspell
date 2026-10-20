package app.hearthspell.ui.tower

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import app.hearthspell.content.Constellation
import app.hearthspell.content.Potion
import app.hearthspell.content.Season
import app.hearthspell.ui.art.Art
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.sin

// The little extras drawn over the tower: light, weather and seasons, potion effects, the cat's z's and hearts.

/** What the sky over the whole tower shows today. */
internal class TowerSky(
    val seasons: Set<Season> = emptySet(),
    val potions: Set<Potion> = emptySet(),
    /** No imps anywhere and every floor tidy: a comet by night, a rainbow by day. */
    val allClear: Boolean = false,
    /** Every constellation with how many of its stars are lit. */
    val constellations: List<Pair<Constellation, Int>> = emptyList(),
)

private val IMP_VIOLET = Color(0xFFB062E0)
private val NIGHT_SHADE = Color(0xFF0A0820)
private val WHITE = Color(0xFFFFFCF4)
private val SNOW = Color(0xFFF4F8FF)
private val SNOW_SHADE = Color(0xFFC8D4EC)
private val PINK = Color(0xFFF878B8)
private val RAINBOW = listOf(
    Color(0xFFFF6B5D), Color(0xFFFC9A3C), Color(0xFFFCE070), Color(0xFF7DDB5A), Color(0xFF5FD6FF), Color(0xFF8878E8), Color(0xFFB062E0),
)

/** A tidy floor glows warm at night, a dusty one sinks into the dark, and imps make the room flicker violet. */
internal fun Grid.lighting(look: FloorLook, m: SceneMeta, tick: Long, sky: Sky) {
    val inL = TX + 8
    val inW = SCENE_W - 16
    when {
        look.imps.isNotEmpty() -> {
            val p = ((tick / 3) % 6).let { if (it < 3) it else 6 - it } / 3f
            rect(inL, 5, inW, SCENE_FLOOR - 5, IMP_VIOLET, alpha = 0.04f + 0.06f * p)
        }
        !sky.day && look.clean -> {
            m.flames.forEach { (x, y, _) ->
                disc(TX + x, y - 3, 14, FIRE_GLOW, 0.05f)
                disc(TX + x, y - 3, 8, FIRE_GLOW, 0.05f)
            }
            disc(TX + SCENE_W / 2, SCENE_FLOOR - 18, 34, FIRE_GLOW, 0.035f + 0.008f * (tick % 3))
        }
        !sky.day -> rect(inL, 5, inW, SCENE_FLOOR - 5, NIGHT_SHADE, alpha = 0.10f + look.dust * 0.2f)
    }
}

/** Two hearts floating up from a patted friend; [since] is ticks since the pat. */
internal fun Grid.hearts(x: Int, y: Int, since: Long) {
    val h = Art.heart
    for (k in 0..1) {
        val life = since - k * 5
        if (life !in 0..16) continue
        sprite(h, x - h.w / 2 + (if (k == 0) -4 else 4) + (sin(life / 2.0) * 1.5).toInt(), y - (life / 2).toInt(), alpha = 1f - life / 17f)
    }
}

/** Little z's rising from someone asleep. */
internal fun Grid.zees(x: Int, y: Int, tick: Long, seed: Long) {
    for (k in 0..1) {
        val life = ((tick + seed * 7 + k * 12) % 24).toInt()
        val zx = x + life / 4 + k * 2
        val zy = y - life / 2
        val a = 0.9f - life / 26f
        rect(zx, zy, 3, 1, WHITE, a)
        dot(zx + 1, zy + 1, WHITE, a)
        rect(zx, zy + 2, 3, 1, WHITE, a)
    }
}

/** A four-point glint. */
internal fun Grid.twinkle(x: Int, y: Int) {
    dot(x, y, WHITE)
    dot(x - 1, y, WHITE, 0.6f)
    dot(x + 1, y, WHITE, 0.6f)
    dot(x, y - 1, WHITE, 0.6f)
    dot(x, y + 1, WHITE, 0.6f)
}

/** Sparkles left behind a walking helper (the stardust potion). */
internal fun Grid.sparkleTrail(x: Int, y: Int, facingLeft: Boolean, tick: Long, i: Int) {
    val dir = if (facingLeft) 1 else -1
    for (k in 1..4) {
        val sx = x + dir * (k * 4 + (hash(tick / 2, k + i * 9L) % 3))
        val sy = y - (hash(tick / 3, k + i * 5L) % 10)
        dot(sx, sy, if (k % 2 == 0) FIRE_GLOW else WHITE, 1f - k / 5f)
    }
}

/** Soap bubbles drifting up through a floor (the bubble tonic). */
internal fun Grid.bubbles(seed: Long, tick: Long) {
    val inL = TX + 10
    val inW = SCENE_W - 20
    for (k in 0 until 7) {
        val h = hash(seed + 41, k.toLong())
        val life = ((tick + h) % 56).toInt()
        val x = inL + h % inW + (sin((tick + h) / 7.0) * 2).toInt()
        val y = SCENE_FLOOR - 2 - life
        val r = if (k % 3 == 0) 2 else 1
        val a = 0.75f * (1f - life / 60f)
        val c = Color(0xFF9CE4FF)
        rect(x - r, y - r - 1, r * 2 + 1, 1, c, a)
        rect(x - r, y + r + 1, r * 2 + 1, 1, c, a)
        rect(x - r - 1, y - r, 1, r * 2 + 1, c, a)
        rect(x + r + 1, y - r, 1, r * 2 + 1, c, a)
        dot(x - r, y - r, WHITE, a)
    }
}

/** Vines and flowers climbing the outer walls (the blossom brew). */
internal fun Grid.blossomWalls(seed: Long, rows: Int, tick: Long) {
    val leaf = Color(0xFF62AE58)
    val leafDark = Color(0xFF347E44)
    for (side in 0..1) {
        val x0 = if (side == 0) TX + 2 else TX + SCENE_W - 6
        for (y in 0 until rows) {
            val x = x0 + (sin((y + seed * 3 + side * 5) / 4.0) * 1.6).toInt() + 1
            dot(x, y, leafDark)
            if (y % 4 == 0) dot(x + (if (y % 8 == 0) 1 else -1), y, leaf)
            if (hash(seed + side, y.toLong()) % 11 == 0) {
                val c = if ((y / 11 + side) % 2 == 0) PINK else FIRE_GLOW
                val open = (tick / 6 + y) % 9 != 0L
                dot(x, y - 1, c)
                if (open) {
                    dot(x - 1, y, c)
                    dot(x + 1, y, c)
                    dot(x, y + 1, c)
                }
                dot(x, y, WHITE)
            }
        }
    }
}

/**
 * Seasonal weather (snow, autumn leaves, spring petals) and the rainbow fireflies, in the strips of sky either
 * side of the tower, or over the whole width with [wide] (behind the roof).
 */
internal fun Grid.seasonAir(rows: Int, seasons: Set<Season>, potions: Set<Potion>, sky: Sky, tick: Long, seed: Long, wide: Boolean = false) {
    if (rows <= 0) return
    val margin = kotlin.math.ceil(ox / px).toInt() + 1
    val strips = if (wide) listOf(-margin to GW + margin * 2) else listOf(-margin to TX + margin, TX + SCENE_W to GW - TX - SCENE_W + margin)
    fun particles(n: Int, salt: Long, speed: Int, drift: Double, draw: (Int, Int, Int) -> Unit) {
        strips.forEachIndexed { si, (x0, w) ->
            val count = if (wide) n * 2 else n
            for (k in 0 until count) {
                val h = hash(seed * 31 + salt + si, k.toLong())
                val y = ((h / 13 + tick * speed / 4) % rows).toInt()
                val x = x0 + (h % w.coerceAtLeast(1)) + (sin((tick + h) / 9.0) * drift).toInt()
                draw(x, y, k)
            }
        }
    }
    when {
        Season.WINTER in seasons -> particles(rows / 10 + 1, 1, 2, 1.5) { x, y, k ->
            dot(x, y, if (k % 3 == 0) SNOW_SHADE else SNOW, 0.9f)
            if (k % 5 == 0) dot(x + 1, y, SNOW, 0.5f)
        }
        Season.AUTUMN in seasons -> particles(rows / 36 + 1, 2, 2, 3.0) { x, y, k ->
            val c = when (k % 3) { 0 -> Color(0xFFE2782C); 1 -> Color(0xFFD82800); else -> Color(0xFFFCC040) }
            dot(x, y, c)
            dot(x + (if ((tick / 4 + k) % 2 == 0L) 1 else -1), y, c, 0.7f)
        }
        Season.SPRING in seasons -> particles(rows / 16 + 1, 3, 1, 3.5) { x, y, _ ->
            dot(x, y, PINK, 0.9f)
            dot(x + 1, y + 1, Color(0xFFFCC0D6), 0.7f)
        }
    }
    if (Potion.FIREFLIES in potions && !sky.day) particles(rows / 12 + 1, 4, 0, 6.0) { x, y, k ->
        if ((tick / 3 + k) % 4 != 0L) {
            val c = RAINBOW[((tick / 6 + k) % RAINBOW.size).toInt()]
            dot(x, (y + (sin((tick + k * 5) / 8.0) * 4).toInt()), c)
        }
    }
}

/** A comet or a rainbow when every floor is tidy; fireworks for the New Year; bats for Hallowe'en. Behind the roof. */
internal fun Grid.skyShow(ts: TowerSky, sky: Sky, tick: Long) {
    if (ts.allClear) {
        if (sky.day) {
            val cx = GW / 2
            val cy = 18 + 44
            RAINBOW.forEachIndexed { i, c ->
                val r = 50 - i
                for (x in -r..r) {
                    val y = cy - kotlin.math.sqrt((r * r - x * x).toDouble()).toInt()
                    dot(cx + x, y, c, 0.45f)
                }
            }
        } else {
            val t = (tick % 90).toInt()
            if (t < 22) {
                val hx = GW - 6 - t * 5
                val hy = 4 + t
                for (k in 0 until 10) dot(hx + k * 2, hy - k / 2, if (k < 2) WHITE else FIRE_GLOW, 1f - k / 10f)
                disc(hx, hy, 2, WHITE, 0.4f)
            }
        }
    }
    if (Season.NEW_YEAR in ts.seasons && !sky.day) {
        for (b in 0..1) {
            val t = ((tick + b * 23) % 46).toInt()
            if (t >= 16) continue
            val h = hash(tick / 46 + b * 101, 7)
            val fx = 12 + h % (GW - 24)
            val fy = 6 + h / 7 % 22
            val c = RAINBOW[(h / 3 % RAINBOW.size)]
            for (k in 0 until 10) {
                val a = k * Math.PI * 2 / 10
                dot(fx + (Math.cos(a) * t * 0.9).toInt(), fy + (Math.sin(a) * t * 0.9).toInt() + t / 5, c, 1f - t / 16f)
            }
        }
    }
    if (Season.HALLOWEEN in ts.seasons) {
        for (k in 0..1) {
            val span = GW + 30
            val x = ((tick * 3 / 2 + k * 70) % span).toInt() - 15
            val y = 8 + k * 10 + (sin((tick + k * 20) / 5.0) * 3).toInt()
            sprite(Art.bat(((tick / 2 + k) % 2).toInt()), x, y, flip = k == 1)
        }
    }
}

/** The constellations lit so far, faint behind the roof, squeezed into the top of the sky. */
internal fun Grid.constellations(list: List<Pair<Constellation, Int>>, tick: Long) {
    fun y(v: Int) = (v * 0.55).toInt() + 2
    list.forEachIndexed { ci, (c, lit) ->
        if (lit == 0) return@forEachIndexed
        c.lines.forEach { (a, b) ->
            if (a < lit && b < lit) line(c.stars[a].first, y(c.stars[a].second), c.stars[b].first, y(c.stars[b].second), Color(0xFF9B8AE0), 0.22f)
        }
        c.stars.take(lit).forEachIndexed { i, (x, yy) ->
            val bright = (tick / 3 + i * 7 + ci * 3) % 29 == 0L
            if (bright) twinkle(x, y(yy)) else dot(x, y(yy), Color(0xFFFFF4D6), 0.95f)
        }
    }
}

/** A thin pixel line. */
internal fun Grid.line(x0: Int, y0: Int, x1: Int, y1: Int, c: Color, alpha: Float) {
    var x = x0
    var y = y0
    val dx = abs(x1 - x0)
    val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1
    var err = dx + dy
    while (true) {
        dot(x, y, c, alpha)
        if (x == x1 && y == y1) break
        val e2 = 2 * err
        if (e2 >= dy) { err += dy; x += sx }
        if (e2 <= dx) { err += dx; y += sy }
    }
}

private val tops = WeakHashMap<ImageBitmap, IntArray>()

/** The top opaque row of every column of [img], -1 for empty columns; read once per bitmap. */
private fun topRows(img: ImageBitmap): IntArray = synchronized(tops) {
    tops.getOrPut(img) {
        val map = img.toPixelMap()
        IntArray(img.width) { x -> (0 until img.height).firstOrNull { y -> map[x, y].alpha > 0.5f } ?: -1 }
    }
}

/** Snow lying on the roof and the chimney. */
internal fun Grid.snowCap(roof: ImageBitmap, rx: Int, oy: Int) {
    val top = topRows(roof)
    top.forEachIndexed { x, y ->
        if (y < 0 || y < 4) return@forEachIndexed
        dot(rx + x, oy + y - 1, SNOW)
        dot(rx + x, oy + y, if (x % 3 == 0) SNOW_SHADE else SNOW)
    }
    // The eave too.
    for (x in 0 until ROOF_W) if (top[x] >= 0 && top[x] < ROOF_H - 8) dot(rx + x, oy + ROOF_H - 9, if (x % 4 == 0) SNOW_SHADE else SNOW)
}

/** Blinking coloured bulbs strung along the eave. */
internal fun Grid.garland(x0: Int, y: Int, w: Int, tick: Long) {
    for (i in 0 until w / 4) {
        val x = x0 + 2 + i * 4
        val sag = if (i % 2 == 0) 0 else 1
        dot(x, y + sag - 1, Color(0xFF1E5034))
        val on = (tick / 4 + i) % 3 != 0L
        val c = RAINBOW[(i + (tick / 8).toInt()) % RAINBOW.size]
        dot(x, y + sag, c, if (on) 1f else 0.35f)
        if (on) disc(x, y + sag, 2, c, 0.18f)
    }
}

/** Two butterflies over the summer grass. */
internal fun Grid.butterflies(tick: Long) {
    for (k in 0..1) {
        val x = ((tick + k * 50) % (GW + 20)).toInt() - 10
        val y = BASE_GROUND - 6 + (sin((tick + k * 17) / 6.0) * 5).toInt()
        val c = if (k == 0) Color(0xFFFCE070) else Color(0xFF9CE4FF)
        val open = (tick / 2 + k) % 2 == 0L
        if (open) {
            dot(x - 1, y, c); dot(x + 1, y, c); dot(x - 1, y - 1, c); dot(x + 1, y - 1, c)
        } else {
            dot(x - 1, y, c); dot(x + 1, y, c)
        }
        dot(x, y, Color(0xFF2B1D12))
    }
}

/** Rainbow fireflies around the door (the firefly draught). */
internal fun Grid.rainbowFlies(tick: Long, ground: Int) {
    for (k in 0 until 9) {
        val h = hash(91, k.toLong())
        val x = h % GW + (sin((tick + h) / 11.0) * 9).toInt()
        val y = ground - 8 + (sin((tick + h * 3) / 8.0) * 6).toInt() + h / 11 % 12
        if ((tick / 3 + h) % 5 != 0L) {
            val c = RAINBOW[((tick / 5 + k) % RAINBOW.size).toInt()]
            dot(x, y, c)
            rect(x - 1, y - 1, 3, 3, c, alpha = 0.3f)
        }
    }
}
