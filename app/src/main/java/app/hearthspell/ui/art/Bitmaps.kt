package app.hearthspell.ui.art

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.WeakHashMap
import kotlin.math.roundToInt

private val bitmaps = WeakHashMap<Sprite, ImageBitmap>()

/** The sprite as a bitmap, built once: one image draw instead of a rectangle per pixel. */
fun Sprite.bitmap(): ImageBitmap = synchronized(bitmaps) {
    bitmaps.getOrPut(this) {
        val px = IntArray(w * h) { i -> this[i % w, i / w]?.toArgb() ?: 0 }
        Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

/** Draws a bitmap scaled by whole pixels with no smoothing, its top-left at ([x], [y]). */
fun DrawScope.drawPixels(img: ImageBitmap, x: Float, y: Float, px: Float, flipX: Boolean = false, alpha: Float = 1f) {
    val dw = (img.width * px).roundToInt()
    val dh = (img.height * px).roundToInt()
    val dst = IntOffset(x.roundToInt(), y.roundToInt())
    if (flipX) {
        scale(-1f, 1f, pivot = Offset(dst.x + dw / 2f, dst.y + dh / 2f)) {
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), dst, IntSize(dw, dh), alpha = alpha, filterQuality = FilterQuality.None)
        }
    } else {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), dst, IntSize(dw, dh), alpha = alpha, filterQuality = FilterQuality.None)
    }
}

fun DrawScope.drawPixels(s: Sprite, x: Float, y: Float, px: Float, flipX: Boolean = false, alpha: Float = 1f) =
    drawPixels(s.bitmap(), x, y, px, flipX, alpha)

/** A sprite as a composable at [pixel] dp per art pixel, drawn as one cached bitmap. */
@Composable
fun PixelSprite(s: Sprite, pixel: Dp, modifier: Modifier = Modifier, flipX: Boolean = false, alpha: Float = 1f) {
    Canvas(modifier.size(pixel * s.w, pixel * s.h)) {
        drawPixels(s, 0f, 0f, size.width / s.w, flipX, alpha)
    }
}
