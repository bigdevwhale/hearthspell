package app.hearthspell

import app.hearthspell.ui.art.ART_D
import app.hearthspell.ui.art.Sprite
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtTest {
    @Test
    fun spriteSizeIsInWholeCells() {
        val s = Sprite(ART_D * 3 + 1, ART_D * 2, IntArray((ART_D * 3 + 1) * ART_D * 2))
        assertEquals(4, s.w)
        assertEquals(2, s.h)
    }

    @Test
    fun aHatHidesTheHairAboveItsBrim() {
        val hair = 0xFF6B3A1E.toInt()
        val felt = 0xFF8B1A1A.toInt()
        // A 1x3 head, all hair; a 1x1 hat whose bottom sits on the head's middle row.
        val head = Sprite(1, 3, IntArray(3) { hair })
        val hat = Sprite(1, 1, intArrayOf(felt))
        val worn = head.under(hat, 0, 2, hides = true)
        assertEquals(listOf(0, felt, hair), worn.argb.toList())
        val perched = head.under(hat, 0, 2)
        assertEquals(listOf(hair, felt, hair), perched.argb.toList())
    }
}
