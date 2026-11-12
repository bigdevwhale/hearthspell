package app.hearthspell.notify

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import app.hearthspell.MainActivity
import app.hearthspell.R
import app.hearthspell.container
import app.hearthspell.domain.World
import app.hearthspell.ui.art.ART_D
import app.hearthspell.ui.art.Art
import app.hearthspell.ui.art.bitmap
import app.hearthspell.ui.tower.WALL_GLASS
import app.hearthspell.ui.tower.WALL_H
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The tower on the home screen, seen from outside: one lit window per floor. Warm where it's tidy, dim where dust
 * gathers, violet (with two yellow eyes) where imps moved in. Redrawn whenever anything changes in the app.
 */
class TowerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = refresh(context)

    private fun refresh(context: Context) {
        val pending = goAsync()
        scope.launch {
            try {
                update(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Most floors drawn; a taller tower shows its top ones. */
        private const val MAX_FLOORS = 9
        private const val W = 128
        private const val SKY = 14

        suspend fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TowerWidget::class.java))
            if (ids.isEmpty()) return
            if (!context.container.settings.now().onboarded) return
            val w = Alarms.world(context)
            val views = RemoteViews(context.packageName, R.layout.widget_tower)
            views.setImageViewBitmap(R.id.widget_tower, paint(context, w))
            val label = if (w.totalImps == 0) context.getString(R.string.widget_quiet)
            else context.resources.getQuantityString(R.plurals.widget_imps, w.totalImps, w.totalImps)
            views.setTextViewText(R.id.widget_label, label)
            val open = PendingIntent.getActivity(
                context, 200,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(Alarms.EXTRA_ROUTE, "tower"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(ids, views)
        }

        private fun res(context: Context, id: Int): Bitmap =
            BitmapFactory.decodeResource(context.resources, id, BitmapFactory.Options().apply { inScaled = false })

        /** Draws the tower in art pixels, then scales it up by whole pixels so it stays crisp. */
        internal fun paint(context: Context, w: World): Bitmap {
            val roof = res(context, R.drawable.hs_roof)
            val wall = res(context, R.drawable.hs_wall)
            val base = res(context, R.drawable.hs_base)
            val floors = w.floors.take(MAX_FLOORS)
            val roofH = roof.height / ART_D
            val baseH = base.height / ART_D
            val h = SKY + roofH + floors.size * WALL_H + baseH
            // Drawn in scene cells on a canvas ART_D times finer, so the art keeps its own pixels.
            val big = Bitmap.createBitmap(W * ART_D, h * ART_D, Bitmap.Config.ARGB_8888)
            val c = Canvas(big)
            c.scale(ART_D.toFloat(), ART_D.toFloat())
            val p = Paint()
            val art = Paint(Paint.FILTER_BITMAP_FLAG)
            fun put(b: Bitmap, x: Float, y: Float) = c.drawBitmap(b, null, RectF(x, y, x + b.width / ART_D.toFloat(), y + b.height / ART_D.toFloat()), art)
            fun rect(x: Int, y: Int, ww: Int, hh: Int, color: Int) {
                p.color = color
                c.drawRect(x.toFloat(), y.toFloat(), (x + ww).toFloat(), (y + hh).toFloat(), p)
            }
            // Night sky with a few stars and the moon.
            val top = SKY + roofH + floors.size * WALL_H
            for (y in 0 until top) rect(0, y, W, 1, if (y < top / 3) 0xFF0B0920.toInt() else if (y < top * 2 / 3) 0xFF151135.toInt() else 0xFF211A4A.toInt())
            for (k in 0 until 40) {
                val hh = app.hearthspell.ui.tower.hash(5, k.toLong())
                rect(hh % W, hh / 7 % top, 1, 1, if (k % 3 == 0) 0xFFFFF4D6.toInt() else 0xFFA996E6.toInt())
            }
            put(Art.moon.bitmap().asAndroidBitmap(), 6f, 4f)
            put(roof, 0f, SKY.toFloat())
            val wx = (W - wall.width / ART_D) / 2
            floors.forEachIndexed { i, f ->
                val y = SKY + roofH + i * WALL_H
                val (gx, gy, gw, gh) = WALL_GLASS.toList()
                val imps = w.imps(f.id) > 0
                val dust = w.dust(f.id)
                val ox = wx + gx
                val oy = y + gy
                when {
                    imps -> {
                        rect(ox, oy, gw, gh, 0xFF4A1A64.toInt())
                        rect(ox, oy + gh / 2, gw, gh - gh / 2, 0xFF7A309E.toInt())
                        // Two yellow eyes peering out.
                        rect(ox + gw / 2 - 3, oy + gh / 2, 1, 2, 0xFFFCE070.toInt())
                        rect(ox + gw / 2 + 2, oy + gh / 2, 1, 2, 0xFFFCE070.toInt())
                    }
                    dust < 0.35f -> {
                        rect(ox, oy, gw, gh, 0xFFFFDA62.toInt())
                        rect(ox, oy + gh / 2, gw, gh - gh / 2, 0xFFFF9428.toInt())
                        rect(ox + 1, oy + 1, 2, 2, 0xFFFFFCD6.toInt())
                    }
                    else -> {
                        rect(ox, oy, gw, gh, 0xFF5C3A2A.toInt())
                        rect(ox, oy + gh / 2, gw, gh - gh / 2, 0xFF3A2420.toInt())
                    }
                }
                put(wall, wx.toFloat(), y.toFloat())
                // A warm halo around a lit window.
                if (!imps && dust < 0.35f) {
                    p.color = 0x22FFDA62
                    c.drawRect((ox - 4).toFloat(), (oy - 3).toFloat(), (ox + gw + 4).toFloat(), (oy + gh + 4).toFloat(), p)
                }
            }
            put(base, wx.toFloat(), (SKY + roofH + floors.size * WALL_H).toFloat())
            return big
        }
    }
}
