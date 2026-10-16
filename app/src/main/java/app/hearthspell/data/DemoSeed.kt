package app.hearthspell.data

import android.content.Context

/** A lived-in tower for screenshots (`-Pdemo`). */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, dao: HearthDao, settings: SettingsStore) {
        if (dao.floors().isNotEmpty()) return
    }
}
