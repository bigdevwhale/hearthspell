package app.hearthspell.ui.art

import androidx.compose.ui.graphics.Color
import app.hearthspell.content.Garment
import app.hearthspell.content.ImpKind

/** Sprites dressed up at runtime: the apprentice in the chosen robe and hat. */
object Looks {
    private val apprentices = HashMap<Triple<Garment, Garment, Int>, Sprite>()

    /** The red of the drawn robe (and the pointed hat), repainted for other robes. */
    private val ROBE_HUES = 330f..18f

    fun imp(kind: ImpKind, frame: Int): Sprite = Art.imp(kind, frame)

    /** The apprentice in [robe] and [hat]; the pointed hat takes the robe's colour. */
    fun apprentice(robe: Garment, hat: Garment, frame: Int): Sprite = synchronized(apprentices) {
        apprentices.getOrPut(Triple(robe, hat, frame and 1)) {
            val ramp = (robe.ramp ?: Garment.CRIMSON.ramp!!).map { Color(it.toInt()) }
            fun dress(s: Sprite) = if (robe == Garment.CRIMSON) s else s.repaint(ROBE_HUES, 0.35f, ramp)
            val body = dress(Art.apprenticeBody(frame))
            val top = Art.hat(hat.name).let { if (hat == Garment.POINTED) dress(it) else it }
            val (hx, hy) = Art.HAT_AT
            body.under(top, hx, hy + top.rh * Art.hatSink(hat.name) / 100)
        }
    }

    fun apprentice(robe: String, hat: String, frame: Int): Sprite =
        apprentice(Garment.of(robe)?.takeIf { it.slot == Garment.Slot.ROBE } ?: Garment.defaultRobe, Garment.of(hat)?.takeIf { it.slot == Garment.Slot.HAT } ?: Garment.defaultHat, frame)
}
