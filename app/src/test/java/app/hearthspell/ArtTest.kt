package app.hearthspell

import app.hearthspell.content.DecorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.ui.art.Art
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtTest {
    @Test
    fun everySpriteBuilds() {
        // Building throws on a colour that isn't in the palette, so this catches typos in the drawings.
        val all = Art.all()
        assertTrue(all.size > HelperKind.entries.size * 4 + DecorKind.entries.size)
        all.forEach { s ->
            assertTrue("sprite too small: ${s.w}x${s.h}", s.w >= 6 && s.h >= 5)
            assertTrue("sprite too big: ${s.w}x${s.h}", s.w <= 80 && s.h <= 56)
        }
    }

    @Test
    fun helperFramesBob() {
        HelperKind.entries.forEach { k ->
            val a = Art.helper(k, 0)
            val b = Art.helper(k, 1)
            assertEquals(a.w, b.w)
            assertEquals(a.h, b.h)
        }
    }
}
