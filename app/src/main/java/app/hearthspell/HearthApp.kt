package app.hearthspell

import android.app.Application
import android.content.Context
import app.hearthspell.data.DemoSeed
import app.hearthspell.data.HearthDao
import app.hearthspell.data.HearthDatabase
import app.hearthspell.data.SettingsStore
import app.hearthspell.notify.Alarms
import app.hearthspell.notify.TowerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = HearthDatabase.create(context)
    val dao: HearthDao get() = db.dao()
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend HearthDao.() -> Unit) {
        scope.launch {
            dao.block()
            changed()
        }
    }

    /** Re-arm reminders and redraw the home-screen tower after anything changed. */
    suspend fun changed() {
        Alarms.reschedule(context)
        runCatching { TowerWidget.update(context) }
    }
}

class HearthApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@HearthApp, container.dao, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as HearthApp).container
