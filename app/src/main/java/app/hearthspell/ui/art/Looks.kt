package app.hearthspell.ui.art

import androidx.compose.ui.graphics.Color
import app.hearthspell.content.Garment
import app.hearthspell.content.ImpKind
import app.hearthspell.content.ImpTint

/** Sprites dressed up at runtime: imps repainted in their own colours, the apprentice in the chosen clothes. */
object Looks {
    private val imps = HashMap<Pair<ImpTint, Int>, Sprite>()
    private val apprentices = HashMap<Triple<Garment, Garment, Int>, Sprite>()

    private fun ramp(r: LongArray): List<Color> = r.map { Color(it.toInt()) }

    private fun swap(from: LongArray, to: LongArray): Map<Color, Color> = ramp(from).zip(ramp(to)).toMap()

    fun imp(kind: ImpKind, frame: Int): Sprite = synchronized(imps) {
        imps.getOrPut(kind.tint to (frame and 1)) {
            val base = Art.imp(frame)
            if (kind.tint == ImpTint.PURPLE) base else base.recolor(swap(ImpTint.PURPLE.ramp, kind.tint.ramp))
        }
    }

    /** The apprentice in [robe] and [hat]; the pointed hat takes the robe's colour. */
    fun apprentice(robe: Garment, hat: Garment, frame: Int): Sprite = synchronized(apprentices) {
        apprentices.getOrPut(Triple(robe, hat, frame and 1)) {
            val swap = swap(Garment.CRIMSON.ramp!!, robe.ramp ?: Garment.CRIMSON.ramp)
            val body = Art.apprenticeBody(frame).recolor(swap)
            val top = Art.hat(hat.name, frame).let { if (hat == Garment.POINTED) it.recolor(swap) else it }
            body.under(top)
        }
    }

    fun apprentice(robe: String, hat: String, frame: Int): Sprite =
        apprentice(Garment.of(robe)?.takeIf { it.slot == Garment.Slot.ROBE } ?: Garment.defaultRobe, Garment.of(hat)?.takeIf { it.slot == Garment.Slot.HAT } ?: Garment.defaultHat, frame)
}
