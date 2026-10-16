package app.hearthspell.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.hearthspell.domain.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    /** The tower at night is the default; parchment by day is an option. */
    val theme: ThemeMode = ThemeMode.DARK,
    val onboarded: Boolean = false,
    val name: String = "",
    /** Only asked when the app speaks Russian. */
    val gender: Gender = Gender.FEMALE,
    /** Epoch day the apprenticeship began: letters and weeks count from it. */
    val startDay: Long = 0,
    /** A daily nudge from the helpers when something is overdue. */
    val remindOn: Boolean = true,
    val remindAt: Int = 19 * 60,
    val haptics: Boolean = true,
    val sound: Boolean = true,
    /** Letter numbers already opened. */
    val lettersRead: Set<String> = emptySet(),
    /** Moments already shown once (a helper woke up, a rank reached). */
    val seen: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.DARK,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        gender = Gender.of(this[GENDER]),
        startDay = this[START_DAY] ?: 0,
        remindOn = this[REMIND_ON] ?: true,
        remindAt = this[REMIND_AT] ?: (19 * 60),
        haptics = this[HAPTICS] ?: true,
        sound = this[SOUND] ?: true,
        lettersRead = this[LETTERS_READ] ?: emptySet(),
        seen = this[SEEN] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[GENDER] = s.gender.name
            it[START_DAY] = s.startDay
            it[REMIND_ON] = s.remindOn
            it[REMIND_AT] = s.remindAt
            it[HAPTICS] = s.haptics
            it[SOUND] = s.sound
            it[LETTERS_READ] = s.lettersRead
            it[SEEN] = s.seen
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")
        val START_DAY = longPreferencesKey("start_day")
        val REMIND_ON = booleanPreferencesKey("remind_on")
        val REMIND_AT = intPreferencesKey("remind_at")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SOUND = booleanPreferencesKey("sound")
        val LETTERS_READ = stringSetPreferencesKey("letters_read")
        val SEEN = stringSetPreferencesKey("seen")
    }
}
