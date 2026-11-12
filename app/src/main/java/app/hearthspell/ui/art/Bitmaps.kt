package app.hearthspell.ui.art

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.WeakHashMap
import kotlin.math.roundToInt

private val bitmaps = WeakHashMap<Sprite, ImageBitmap>()

/** The sprite as a bitmap, built once. */
fun Sprite.bitmap(): ImageBitmap = synchronized(bitmaps) {
    bitmaps.getOrPut(this) { Bitmap.createBitmap(argb, rw, rh, Bitmap.Config.ARGB_8888).asImageBitmap() }
}

/** Draws [img] at [scale] screen pixels per image pixel, its top-left at ([x], [y]). */
fun DrawScope.drawScaled(img: ImageBitmap, x: Float, y: Float, scale: Float, flipX: Boolean = false, alpha: Float = 1f) {
    val dw = (img.width * scale).roundToInt()
    val dh = (img.height * scale).roundToInt()
    val dst = IntOffset(x.roundToInt(), y.roundToInt())
    // Shrinking fine art smooths it; enlarging keeps its pixels crisp.
    val q = if (scale < 1f) FilterQuality.Low else FilterQuality.None
    if (flipX) {
        scale(-1f, 1f, pivot = Offset(dst.x + dw / 2f, dst.y + dh / 2f)) {
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), dst, IntSize(dw, dh), alpha = alpha, filterQuality = q)
        }
    } else {
        drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), dst, IntSize(dw, dh), alpha = alpha, filterQuality = q)
    }
}

/** Draws game art (any [ART_D]-dense bitmap) with each scene cell [px] screen pixels wide. */
fun DrawScope.drawPixels(img: ImageBitmap, x: Float, y: Float, px: Float, flipX: Boolean = false, alpha: Float = 1f) =
    drawScaled(img, x, y, px / ART_D, flipX, alpha)

fun DrawScope.drawPixels(s: Sprite, x: Float, y: Float, px: Float, flipX: Boolean = false, alpha: Float = 1f) =
    drawPixels(s.bitmap(), x, y, px, flipX, alpha)

/** A sprite as a composable at [pixel] dp per scene cell. */
@Composable
fun PixelSprite(s: Sprite, pixel: Dp, modifier: Modifier = Modifier, flipX: Boolean = false, alpha: Float = 1f) {
    Canvas(modifier.size(pixel * s.w, pixel * s.h)) {
        drawPixels(s, 0f, 0f, size.width / s.w, flipX, alpha)
    }
}

/** A sprite as a composable, fitted and centred in a square of [size]. */
@Composable
fun SpriteImage(s: Sprite, size: Dp, modifier: Modifier = Modifier, flipX: Boolean = false, alpha: Float = 1f) {
    Canvas(modifier.size(size)) {
        val k = minOf(this.size.width / s.rw, this.size.height / s.rh)
        drawScaled(s.bitmap(), (this.size.width - s.rw * k) / 2, (this.size.height - s.rh * k) / 2, k, flipX, alpha)
    }
}
