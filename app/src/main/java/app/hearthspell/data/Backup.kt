package app.hearthspell.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    suspend fun export(db: HearthDatabase, settings: Settings): String {
        val d = db.dao()
        return JSONObject()
            .put("app", "hearthspell")
            .put("version", VERSION)
            .put("startDay", settings.startDay)
            .put("name", settings.name)
            .put("gender", settings.gender.name)
            .put("lettersRead", JSONArray(settings.lettersRead.toList()))
            .put("floors", JSONArray(d.floors().map { JSONObject().put("id", it.id).put("kind", it.kind).put("name", it.name).put("sort", it.sort) }))
            .put("spells", JSONArray(d.spells().map {
                JSONObject().put("id", it.id).put("floorId", it.floorId).put("presetKey", it.presetKey ?: JSONObject.NULL).put("name", it.name)
                    .put("intervalDays", it.intervalDays).put("effort", it.effort).put("helper", it.helper).put("lastCastAt", it.lastCastAt)
                    .put("createdAt", it.createdAt).put("paused", it.paused).put("sort", it.sort)
            }))
            .put("casts", JSONArray(d.casts().map {
                JSONObject().put("id", it.id).put("spellId", it.spellId).put("at", it.at).put("mana", it.mana)
                    .put("requested", it.requested).put("prevLastCastAt", it.prevLastCastAt)
            }))
            .put("reagents", JSONArray(d.reagents().map {
                JSONObject().put("id", it.id).put("name", it.name).put("aisle", it.aisle).put("needed", it.needed).put("inBasket", it.inBasket)
                    .put("note", it.note).put("addedAt", it.addedAt).put("lastBoughtAt", it.lastBoughtAt ?: JSONObject.NULL)
                    .put("boughtCount", it.boughtCount).put("avgGapDays", it.avgGapDays)
            }))
            .put("decor", JSONArray(d.decor().map {
                JSONObject().put("id", it.id).put("kind", it.kind).put("floorId", it.floorId).put("slot", it.slot).put("cost", it.cost).put("boughtAt", it.boughtAt)
            }))
            .toString(1)
    }

    /** Replaces everything with the backup. Throws [IllegalArgumentException] when the text is not a Hearthspell backup. */
    suspend fun import(db: HearthDatabase, store: SettingsStore, text: String) {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "hearthspell") { "Not a Hearthspell backup" }
        val floors = root.list("floors") { Floor(getLong("id"), getString("kind"), optString("name"), optInt("sort")) }
        val spells = root.list("spells") {
            Spell(
                id = getLong("id"), floorId = getLong("floorId"), presetKey = str("presetKey"), name = optString("name"),
                intervalDays = optInt("intervalDays", 7), effort = optInt("effort", 2), helper = optString("helper", "BROOM"),
                lastCastAt = optLong("lastCastAt"), createdAt = optLong("createdAt"), paused = optBoolean("paused"), sort = optInt("sort"),
            )
        }
        val casts = root.list("casts") {
            Cast(getLong("id"), getLong("spellId"), getLong("at"), optInt("mana"), optBoolean("requested"), optLong("prevLastCastAt"))
        }
        val reagents = root.list("reagents") {
            Reagent(
                id = getLong("id"), name = getString("name"), aisle = optString("aisle", "OTHER"), needed = optBoolean("needed"),
                inBasket = optBoolean("inBasket"), note = optString("note"), addedAt = optLong("addedAt"),
                lastBoughtAt = if (isNull("lastBoughtAt")) null else optLong("lastBoughtAt"), boughtCount = optInt("boughtCount"),
                avgGapDays = optDouble("avgGapDays", 0.0),
            )
        }
        val decor = root.list("decor") { Decor(getLong("id"), getString("kind"), optLong("floorId"), optInt("slot"), optInt("cost"), optLong("boughtAt")) }
        val read = root.optJSONArray("lettersRead")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()

        db.withTransaction {
            val d = db.dao()
            d.wipeCasts(); d.wipeSpells(); d.wipeFloors(); d.wipeReagents(); d.wipeDecor()
            floors.forEach { d.upsertFloor(it) }
            spells.forEach { d.upsertSpell(it) }
            casts.forEach { d.insertCast(it) }
            reagents.forEach { d.upsertReagent(it) }
            decor.forEach { d.upsertDecor(it) }
        }
        store.update {
            it.copy(
                onboarded = true,
                startDay = root.optLong("startDay", it.startDay),
                name = root.optString("name", it.name),
                gender = app.hearthspell.domain.Gender.of(root.optString("gender")),
                lettersRead = read,
            )
        }
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key)

    private fun <T> JSONObject.list(key: String, item: JSONObject.() -> T): List<T> {
        val a = optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it).item() }
    }
}
