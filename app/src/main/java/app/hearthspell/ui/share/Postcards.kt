package app.hearthspell.ui.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import app.hearthspell.R
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.Sprite
import app.hearthspell.ui.art.bitmap
import app.hearthspell.ui.tower.BASE_GROUND
import app.hearthspell.ui.tower.BASE_ROWS
import app.hearthspell.ui.tower.FloorLook
import app.hearthspell.ui.tower.GW
import app.hearthspell.ui.tower.ROOF_ROWS
import app.hearthspell.ui.tower.SCENES
import app.hearthspell.ui.tower.SCENE_H
import app.hearthspell.ui.tower.Sky
import app.hearthspell.ui.tower.TowerSky
import app.hearthspell.ui.tower.drawBase
import app.hearthspell.ui.tower.drawFloor
import app.hearthspell.ui.tower.drawRoof
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** What a postcard pictures above its words. */
internal sealed interface Picture {
    /** One floor of the tower, as it looks now. */
    class Room(val look: FloorLook) : Picture

    /** The whole tower, roof to grass. */
    class Tower(val looks: List<FloorLook>, val sky: TowerSky) : Picture

    /** One character on a glowing stage. */
    class Figure(val sprite: Sprite, val glow: Int) : Picture

    /** A shelf of a collection: found pieces in colour, the rest as shadows. */
    class Shelf(val items: List<Pair<Sprite, Boolean>>) : Picture

    /** A week of casts as bars, beside the apprentice. */
    class Week(val counts: List<Int>, val labels: List<String>, val who: Sprite) : Picture
}

/** A card to share: all its words already in the app's language. */
internal class Postcard(
    /** A stable name: the file name, and the key of the card in the gallery. */
    val key: String,
    val eyebrow: String,
    val title: String,
    val sub: String,
    val figures: List<Pair<String, String>>,
    val picture: Picture,
    val tagline: String,
    val store: String,
)

/** Draws postcards as images and hands them to other apps. */
internal object Postcards {
    const val W = 1080
    const val H = 1350
    const val PLAY = "https://play.google.com/store/apps/details?id=app.hearthspell"

    private const val NIGHT_TOP = 0xFF120E2E.toInt()
    private const val NIGHT_LOW = 0xFF2C1F55.toInt()
    private const val GOLD = 0xFFF4C66E.toInt()
    private const val CREAM = 0xFFFFF3DA.toInt()
    private const val LAVENDER = 0xFFC9BDEB.toInt()
    private const val MARGIN = 70f
    private const val FOOTER = 210f

    private class Fonts(ctx: Context) {
        val story: Typeface = ResourcesCompat.getFont(ctx, R.font.kurale) ?: Typeface.SERIF
        val rubric: Typeface = ResourcesCompat.getFont(ctx, R.font.alegreya_sc_extrabold) ?: Typeface.SERIF
        val round: Typeface = ResourcesCompat.getFont(ctx, R.font.philosopher) ?: Typeface.DEFAULT
        val roundBold: Typeface = ResourcesCompat.getFont(ctx, R.font.philosopher_bold) ?: Typeface.DEFAULT_BOLD
    }

    /** The card at [scale] (1 = 1080×1350, the 4:5 most feeds show whole). */
    fun render(ctx: Context, card: Postcard, scale: Float = 1f): Bitmap {
        val out = Bitmap.createBitmap((W * scale).roundToInt(), (H * scale).roundToInt(), Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        c.scale(scale, scale)
        val f = Fonts(ctx)
        background(c)

        val tw = (W - 2 * MARGIN).toInt()
        val eyebrow = layout(card.eyebrow.uppercase(), f.rubric, 32f, GOLD, tw, 1, spacing = 0.1f)
        val title = layout(card.title, f.story, 76f, CREAM, tw, 2)
        val sub = card.sub.takeIf { it.isNotBlank() }?.let { layout(it, f.round, 36f, LAVENDER, tw, 3) }
        val figuresH = if (card.figures.isEmpty()) 0f else 150f

        var y = 84f
        draw(c, eyebrow, MARGIN, y)
        y += eyebrow.height + 26f
        val words = title.height + (sub?.let { 12f + it.height } ?: 0f) + (if (figuresH > 0) 28f + figuresH else 0f)
        val artBottom = H - FOOTER - 60f - words - 34f
        picture(ctx, c, f, card.picture, RectF(MARGIN - 14f, y, W - MARGIN + 14f, artBottom))
        y = artBottom + 34f
        draw(c, title, MARGIN, y)
        y += title.height
        if (sub != null) {
            y += 12f
            draw(c, sub, MARGIN, y)
            y += sub.height
        }
        if (figuresH > 0) figures(c, f, card.figures, RectF(MARGIN, y + 28f, W - MARGIN, y + 28f + figuresH))
        footer(ctx, c, f, card)
        return out
    }

    // ------------------------------------------------------------------ the card

    private fun background(c: android.graphics.Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), NIGHT_TOP, NIGHT_LOW, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        // The night sky texture of the tower, softly over it.
        Art.sky(false)?.let { s ->
            val b = s.bitmap().asAndroidBitmap()
            val sh = BitmapShader(b, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            sh.setLocalMatrix(Matrix().apply { setScale(2.5f, 2.5f) })
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = sh; alpha = 120 })
        }
        // A warm glow at the top, darker corners.
        p.shader = RadialGradient(W / 2f, H * 0.32f, W * 0.75f, intArrayOf(0x30F4C66E, 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = RadialGradient(W / 2f, H / 2f, H * 0.78f, intArrayOf(0x00000000, 0x00000000, 0x99050310.toInt()), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        // A double gold frame with stars in its corners.
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = GOLD }
        line.strokeWidth = 4f; line.alpha = 190
        c.drawRoundRect(RectF(24f, 24f, W - 24f, H - 24f), 44f, 44f, line)
        line.strokeWidth = 2f; line.alpha = 90
        c.drawRoundRect(RectF(38f, 38f, W - 38f, H - 38f), 34f, 34f, line)
        for ((x, y) in listOf(24f to 24f, W - 24f to 24f, 24f to H - 24f, W - 24f to H - 24f)) sparkle(c, x, y, 18f, GOLD, 255)
        // A few stars scattered over the dark.
        var seed = 7L
        repeat(26) {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            val x = 60 + ((seed ushr 33) % (W - 120)).toFloat()
            val y = 60 + ((seed ushr 13) % (H - 120)).toFloat()
            sparkle(c, x, y, 4f + (it % 4) * 2f, CREAM, 60 + (it % 5) * 25)
        }
    }

    private fun figures(c: android.graphics.Canvas, f: Fonts, items: List<Pair<String, String>>, box: RectF) {
        val gap = 18f
        val w = (box.width() - gap * (items.size - 1)) / items.size
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26FFFFFF }
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = GOLD; alpha = 70 }
        items.forEachIndexed { i, (value, label) ->
            val x = box.left + i * (w + gap)
            val r = RectF(x, box.top, x + w, box.bottom)
            c.drawRoundRect(r, 28f, 28f, fill)
            c.drawRoundRect(r, 28f, 28f, edge)
            val v = layout(value, f.story, 62f, GOLD, w.toInt() - 16, 1)
            val l = layout(label, f.round, 28f, LAVENDER, w.toInt() - 16, 2)
            val top = r.top + (r.height() - v.height - l.height) / 2
            draw(c, v, x + 8f, top)
            draw(c, l, x + 8f, top + v.height)
        }
    }

    private fun footer(ctx: Context, c: android.graphics.Canvas, f: Fonts, card: Postcard) {
        val box = RectF(MARGIN - 14f, H - FOOTER - 44f, W - MARGIN + 14f, H - 60f)
        c.drawRoundRect(box, 34f, 34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66060414 })
        c.drawRoundRect(box, 34f, 34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = GOLD; alpha = 80 })
        val pad = 22f
        // The QR code to the store, on white so any camera reads it.
        val qr = BitmapFactory.decodeResource(ctx.resources, R.drawable.hs_qr, BitmapFactory.Options().apply { inScaled = false })
        val qs = box.height() - 2 * pad
        val qbox = RectF(box.right - pad - qs, box.top + pad, box.right - pad, box.bottom - pad)
        c.drawRoundRect(qbox, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        val inner = 14f
        val cell = ((qs - 2 * inner) / qr.width).toInt().toFloat()
        val qd = cell * qr.width
        val qx = qbox.left + (qs - qd) / 2
        val qy = qbox.top + (qs - qd) / 2
        c.drawBitmap(qr, Rect(0, 0, qr.width, qr.height), RectF(qx, qy, qx + qd, qy + qd), Paint())
        // The app's icon and name.
        val icon = runCatching { ctx.packageManager.getApplicationIcon(ctx.packageName).toBitmap(160, 160) }.getOrNull()
        var x = box.left + pad
        val iconS = qs * 0.78f
        if (icon != null) {
            val top = box.centerY() - iconS / 2
            val clip = Path().apply { addRoundRect(RectF(x, top, x + iconS, top + iconS), 30f, 30f, Path.Direction.CW) }
            c.save(); c.clipPath(clip)
            c.drawBitmap(icon, null, RectF(x, top, x + iconS, top + iconS), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
            x += iconS + 22f
        }
        val tw = (qbox.left - 22f - x).toInt()
        val name = layout("Hearthspell", f.story, 54f, CREAM, tw, 1, align = Layout.Alignment.ALIGN_NORMAL)
        val tag = layout(card.tagline, f.round, 27f, LAVENDER, tw, 2, align = Layout.Alignment.ALIGN_NORMAL)
        val store = layout(card.store.uppercase(), f.rubric, 24f, GOLD, tw, 1, align = Layout.Alignment.ALIGN_NORMAL, spacing = 0.08f)
        val th = name.height + tag.height + 6f + store.height
        var ty = box.centerY() - th / 2
        draw(c, name, x, ty); ty += name.height
        draw(c, tag, x, ty); ty += tag.height + 6f
        draw(c, store, x, ty)
    }

    // ------------------------------------------------------------------ pictures

    private fun picture(ctx: Context, c: android.graphics.Canvas, f: Fonts, p: Picture, box: RectF) {
        when (p) {
            is Picture.Room -> framed(c, box, sceneOf(ctx, p.look))
            is Picture.Tower -> framed(c, box, towerOf(ctx, p))
            is Picture.Figure -> figure(c, box, p.sprite, p.glow)
            is Picture.Shelf -> shelf(c, box, p.items)
            is Picture.Week -> week(c, f, box, p)
        }
    }

    /** A picture fitted into [box] with rounded corners and a gold rim. */
    private fun framed(c: android.graphics.Canvas, box: RectF, img: Bitmap) {
        val k = min(box.width() / img.width, box.height() / img.height)
        val w = img.width * k
        val h = img.height * k
        val r = RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2)
        // A soft shadow under it.
        c.drawRoundRect(RectF(r.left + 6f, r.top + 14f, r.right + 6f, r.bottom + 14f), 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 })
        c.save()
        c.clipPath(Path().apply { addRoundRect(r, 32f, 32f, Path.Direction.CW) })
        c.drawBitmap(img, null, r, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
        c.drawRoundRect(r, 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = GOLD; alpha = 200 })
    }

    private fun drawScene(img: ImageBitmap, w: Int, h: Int, top: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
        val canvas = androidx.compose.ui.graphics.Canvas(img)
        canvas.save()
        canvas.translate(0f, top)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat()), block)
        canvas.restore()
    }

    private fun resource(ctx: Context, res: Int): ImageBitmap =
        BitmapFactory.decodeResource(ctx.resources, res, BitmapFactory.Options().apply { inScaled = false }).asImageBitmap()

    /** Art pixels per scene cell for the card's pictures: rendered big, then scaled down smooth. */
    private const val PX = 12

    /** A still moment of the floor's scene: the helpers out, candles lit. */
    private const val MOMENT = 37.5f

    private fun sceneOf(ctx: Context, look: FloorLook): Bitmap {
        val w = GW * PX
        val h = SCENE_H * PX
        val img = ImageBitmap(w, h)
        val bg = resource(ctx, SCENES.getValue(look.kind).res)
        drawScene(img, w, h, 0f) { drawFloor(look, bg, MOMENT, Sky(day = false)) }
        return img.asAndroidBitmap()
    }

    private fun towerOf(ctx: Context, t: Picture.Tower): Bitmap {
        val rows = ROOF_ROWS + t.looks.size * SCENE_H + BASE_ROWS
        // Big enough to stay sharp on the card, small enough to keep the meadow's bitmap modest.
        val px = (2200 / rows).coerceIn(3, PX)
        val w = GW * px
        val img = ImageBitmap(w, rows * px)
        val sky = Sky(day = false)
        androidx.compose.ui.graphics.Canvas(img).drawRect(0f, 0f, w.toFloat(), rows * px.toFloat(), androidx.compose.ui.graphics.Paint().apply { color = sky.top })
        var y = 0f
        val roof = resource(ctx, R.drawable.hs_roof)
        drawScene(img, w, ROOF_ROWS * px, y) { drawRoof(roof, MOMENT, sky, t.sky) }
        y += ROOF_ROWS * px
        for (look in t.looks) {
            val bg = resource(ctx, SCENES.getValue(look.kind).res)
            drawScene(img, w, SCENE_H * px, y) { drawFloor(look, bg, MOMENT, sky) }
            y += SCENE_H * px
        }
        val base = resource(ctx, R.drawable.hs_base)
        drawScene(img, w, BASE_ROWS * px, y) { drawBase(base, MOMENT, sky, t.sky) }
        return meadow(img.asAndroidBitmap(), px, sky)
    }

    /**
     * The tower alone is a tall thin strip; on a card it stands in a wide night meadow instead: the sky goes on to
     * either side and the painted grass runs the whole width, tiled in step with the tower's own.
     */
    private fun meadow(tower: Bitmap, px: Int, sky: Sky): Bitmap {
        val ww = maxOf(tower.width * 2, (tower.height * 1.35f).roundToInt())
        val hh = tower.height + 14 * px
        val out = Bitmap.createBitmap(ww, hh, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        val k = px / app.hearthspell.ui.art.ART_D.toFloat()
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, hh.toFloat(), sky.top.toArgb(), sky.low.toArgb(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, ww.toFloat(), hh.toFloat(), p)
        val left = (ww - tower.width) / 2f
        fun tile(s: Sprite, top: Float, alpha: Int) {
            val sh = BitmapShader(s.bitmap().asAndroidBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            sh.setLocalMatrix(Matrix().apply { setScale(k, k); postTranslate(left, top) })
            c.drawRect(0f, top, ww.toFloat(), hh.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = sh; this.alpha = alpha })
        }
        Art.sky(false)?.let { tile(it, 0f, 255) }
        c.drawBitmap(tower, left, (hh - tower.height).toFloat(), p.apply { shader = null })
        val ground = Art.ground ?: return out
        val top = hh - (BASE_ROWS - (BASE_GROUND - 4)) * px
        tile(ground, top.toFloat(), 255)
        c.drawRect(0f, (hh - (BASE_ROWS - BASE_GROUND + 2) * px).toFloat(), ww.toFloat(), hh.toFloat(), Paint().apply { color = 0x660B0920 })
        return out
    }

    /** A character on a glowing stage, ringed with sparkles. */
    private fun figure(c: android.graphics.Canvas, box: RectF, s: Sprite, glow: Int) {
        val cx = box.centerX()
        val cy = box.centerY()
        val r = min(box.width(), box.height()) * 0.52f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(cx, cy, r, intArrayOf(withAlpha(glow, 150), withAlpha(glow, 60), withAlpha(glow, 0)), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, p)
        val k = min(box.width() * 0.8f / s.rw, box.height() * 0.84f / s.rh)
        val w = s.rw * k
        val h = s.rh * k
        val bottom = cy + h / 2
        // The stage: a soft oval shadow at its feet.
        p.shader = RadialGradient(cx, bottom, w * 0.5f, intArrayOf(0x88000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
        c.save(); c.scale(1f, 0.22f, cx, bottom); c.drawCircle(cx, bottom, w * 0.5f, p); c.restore()
        c.drawBitmap(s.bitmap().asAndroidBitmap(), null, RectF(cx - w / 2, bottom - h, cx + w / 2, bottom), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        for (i in 0 until 9) {
            val a = i * 0.7f + 0.4f
            val d = r * (0.78f + (i % 3) * 0.1f)
            sparkle(c, cx + cos(a) * d, cy + sin(a) * d * 0.8f, 9f + (i % 3) * 5f, CREAM, 170)
        }
    }

    /** A collection as rows of little tiles. */
    private fun shelf(c: android.graphics.Canvas, box: RectF, items: List<Pair<Sprite, Boolean>>) {
        if (items.isEmpty()) return
        val gap = 14f
        // The most columns that still leave square tiles filling the box.
        var cols = 3
        while (cols < 8) {
            val size = (box.width() - gap * (cols - 1)) / cols
            val rows = (items.size + cols - 1) / cols
            if (rows * size + (rows - 1) * gap <= box.height()) break
            cols++
        }
        val size = min((box.width() - gap * (cols - 1)) / cols, (box.height() - gap * (((items.size + cols - 1) / cols) - 1)) / ((items.size + cols - 1) / cols))
        val rows = (items.size + cols - 1) / cols
        val totalH = rows * size + (rows - 1) * gap
        val top0 = box.top + (box.height() - totalH) / 2
        val tile = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = GOLD }
        val shadow = android.graphics.PorterDuffColorFilter(0xFF3A2F6E.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
        items.forEachIndexed { i, (s, found) ->
            val row = i / cols
            val inRow = if (row == rows - 1) items.size - row * cols else cols
            val rowW = inRow * size + (inRow - 1) * gap
            val x = box.centerX() - rowW / 2 + (i % cols) * (size + gap)
            val y = top0 + row * (size + gap)
            val r = RectF(x, y, x + size, y + size)
            tile.color = if (found) 0x33FFFFFF else 0x14FFFFFF
            c.drawRoundRect(r, 22f, 22f, tile)
            edge.alpha = if (found) 110 else 40
            c.drawRoundRect(r, 22f, 22f, edge)
            val k = min(size * 0.78f / s.rw, size * 0.78f / s.rh)
            val w = s.rw * k
            val h = s.rh * k
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply { if (!found) { colorFilter = shadow; alpha = 170 } }
            c.drawBitmap(s.bitmap().asAndroidBitmap(), null, RectF(r.centerX() - w / 2, r.centerY() - h / 2, r.centerX() + w / 2, r.centerY() + h / 2), paint)
        }
    }

    /** The apprentice on the left, the week's casts as bars on the right. */
    private fun week(c: android.graphics.Canvas, f: Fonts, box: RectF, p: Picture.Week) {
        val left = RectF(box.left, box.top, box.left + box.width() * 0.34f, box.bottom)
        figure(c, left, p.who, GOLD)
        val chart = RectF(left.right + 30f, box.top + 30f, box.right - 10f, box.bottom - 60f)
        val panel = RectF(chart.left - 20f, box.top + 6f, box.right, box.bottom - 6f)
        c.drawRoundRect(panel, 30f, 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF })
        val max = maxOf(1, p.counts.maxOrNull() ?: 1)
        val slot = chart.width() / p.counts.size
        val bw = slot * 0.62f
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        val best = p.counts.indexOf(max)
        p.counts.forEachIndexed { i, n ->
            val x = chart.left + i * slot + (slot - bw) / 2
            val h = if (n == 0) 8f else (chart.height() - 50f) * n / max
            bar.shader = LinearGradient(0f, chart.bottom - h, 0f, chart.bottom, GOLD, 0xFFB9792E.toInt(), Shader.TileMode.CLAMP)
            bar.alpha = if (n == 0) 90 else 255
            c.drawRoundRect(RectF(x, chart.bottom - h, x + bw, chart.bottom), 12f, 12f, bar)
            if (n > 0) {
                val v = layout("$n", f.roundBold, 30f, if (i == best) GOLD else CREAM, slot.toInt(), 1)
                draw(c, v, chart.left + i * slot, chart.bottom - h - v.height - 4f)
            }
            val l = layout(p.labels[i], f.round, 28f, LAVENDER, slot.toInt(), 1)
            draw(c, l, chart.left + i * slot, chart.bottom + 12f)
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun sparkle(c: android.graphics.Canvas, x: Float, y: Float, s: Float, color: Int, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.alpha = alpha }
        val path = Path().apply {
            moveTo(x, y - s); quadTo(x, y, x + s, y); quadTo(x, y, x, y + s); quadTo(x, y, x - s, y); quadTo(x, y, x, y - s); close()
        }
        c.drawPath(path, p)
    }

    private fun layout(
        text: String, face: Typeface, size: Float, color: Int, width: Int, maxLines: Int,
        align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER, spacing: Float = 0f,
    ): StaticLayout {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face; textSize = size; this.color = color; letterSpacing = spacing
            setShadowLayer(6f, 0f, 2f, 0x99000000.toInt())
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, p, maxOf(1, width))
            .setAlignment(align).setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.04f).setIncludePad(false).build()
    }

    private fun draw(c: android.graphics.Canvas, l: StaticLayout, x: Float, y: Float) {
        c.save(); c.translate(x, y); l.draw(c); c.restore()
    }

    // ------------------------------------------------------------------ out into the world

    private fun file(ctx: Context, card: Postcard): File {
        val dir = File(ctx.cacheDir, "cards").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val f = File(dir, "hearthspell-${card.key}.png")
        f.outputStream().use { render(ctx, card).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    /** Draws the card and opens the share sheet with it and the store link. Call off the main thread. */
    fun shareIntent(ctx: Context, card: Postcard, text: String): Intent {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".cards", file(ctx, card))
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        return Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    /** Saving into the photo gallery needs no permission from Android 10 on. */
    val canSave: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Puts the card into Pictures/Hearthspell. Call off the main thread. */
    fun save(ctx: Context, card: Postcard): Boolean = runCatching {
        if (!canSave) return false
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "hearthspell-${card.key}-${System.currentTimeMillis() / 1000}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hearthspell")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { render(ctx, card).compress(Bitmap.CompressFormat.PNG, 100, it) } ?: return false
        true
    }.getOrDefault(false)
}
