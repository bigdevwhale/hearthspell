package app.hearthspell.ui.art

import app.hearthspell.content.DecorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.ui.theme.Nes

/** Every character and object sprite in the game. */
object Art {
    private val blob = Sprite.of(
        mapOf('o' to Nes.black, 'a' to Nes.amber, 'w' to Nes.white),
        "..oooo..",
        ".oaaaao.",
        "oawaawao",
        "oaoaaoao",
        "oaaaaaao",
        "oaaaaaao",
        ".oaaaao.",
        "..oooo..",
    )

    /** A helper; [frame] 0 or 1 for its idle animation. */
    fun helper(kind: HelperKind, frame: Int = 0): Sprite = blob

    /** A mischievous imp; [frame] 0 or 1. */
    fun imp(frame: Int = 0): Sprite = blob.recolor(mapOf(Nes.amber to Nes.purple))

    fun decor(kind: DecorKind): Sprite = blob.recolor(mapOf(Nes.amber to Nes.wood))

    /** The old master, for his letters. */
    fun master(frame: Int = 0): Sprite = blob.recolor(mapOf(Nes.amber to Nes.blue))

    /** The apprentice (the user). */
    fun apprentice(frame: Int = 0): Sprite = blob.recolor(mapOf(Nes.amber to Nes.red))
}
