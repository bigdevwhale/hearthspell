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
}
