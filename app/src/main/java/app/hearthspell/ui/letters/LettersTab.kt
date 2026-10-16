package app.hearthspell.ui.letters

import androidx.compose.runtime.Composable
import app.hearthspell.ui.Go
import app.hearthspell.ui.WData

@Composable
fun LettersTab(data: WData, go: Go) {
}

@Composable
fun LetterScreen(n: Int, data: WData, go: Go) {
}

/** The master's letters: one on the first day, then one a week. */
object Letters {
    /** How many letters are written. */
    const val COUNT = 12

    /** Letters that have arrived but haven't been opened. */
    fun unread(data: WData): Int =
        (0 until data.world.lettersArrived(COUNT)).count { it.toString() !in data.settings.lettersRead }
}
