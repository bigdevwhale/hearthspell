package app.hearthspell.data

import android.content.Context
import app.hearthspell.content.Aisle
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.Spells
import app.hearthspell.domain.DAY_MS
import app.hearthspell.domain.Gender
import app.hearthspell.domain.Mana
import app.hearthspell.domain.World
import app.hearthspell.ui.letters.Letters
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** A lived-in tower for screenshots (`-Pdemo`), in the language active on first launch. */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, dao: HearthDao, settings: SettingsStore) {
        if (dao.floors().isNotEmpty()) return
        val ru = context.resources.configuration.locales[0].language == "ru"
        fun t(en: String, r: String) = if (ru) r else en
        val rnd = Random(7)
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val start = now - 60 * DAY_MS

        // Floors top to bottom, with the spells they hold.
        val plan = listOf(
            FloorKind.BEDROOM to listOf("bedroom.bed", "bedroom.linen", "bedroom.vacuum", "bedroom.dust"),
            FloorKind.BALCONY to listOf("balcony.water", "balcony.sweep"),
            FloorKind.LIVING to listOf("living.tidy", "living.dust", "living.vacuum", "living.cushions"),
            FloorKind.KITCHEN to listOf("kitchen.dishes", "kitchen.counters", "kitchen.trash", "kitchen.cook", "kitchen.fridge", "kitchen.floor"),
            FloorKind.BATH to listOf("bath.sink", "bath.toilet", "bath.tub", "bath.towels"),
            FloorKind.WHOLE to listOf("whole.market", "whole.bins", "whole.declutter"),
            FloorKind.HALL to listOf("hall.shoes", "hall.floor"),
        )
        // Overdue on purpose, so a couple of floors have imps; and a few left fading.
        val late = setOf("living.dust", "balcony.sweep", "bath.toilet")
        val fading = setOf("kitchen.fridge", "bedroom.linen", "hall.floor", "whole.declutter")

        val floorIds = mutableMapOf<FloorKind, Long>()
        plan.forEachIndexed { i, (kind, keys) ->
            val floorId = dao.upsertFloor(Floor(kind = kind.name, sort = i))
            floorIds[kind] = floorId
            keys.forEachIndexed { j, key ->
                val p = Spells.of(key) ?: return@forEachIndexed
                val interval = p.days * DAY_MS
                // Where the spell stands now, as a fraction of its interval since the last cast.
                val f = when (key) {
                    in late -> 1.3 + rnd.nextDouble() * 0.4
                    in fading -> 0.75 + rnd.nextDouble() * 0.15
                    else -> 0.05 + rnd.nextDouble() * 0.5
                }
                val last = now - (f * interval).toLong()
                val spellId = dao.upsertSpell(
                    Spell(
                        floorId = floorId, presetKey = key, intervalDays = p.days, effort = p.effort, helper = p.helper.name,
                        lastCastAt = last, createdAt = start - DAY_MS, sort = j,
                    )
                )
                // Walk back from the last cast in roughly-regular steps.
                var at = last
                val history = mutableListOf<Long>()
                while (at > start) {
                    history += at
                    at -= (interval * (0.75 + rnd.nextDouble() * 0.5)).toLong().coerceAtLeast(DAY_MS / 2)
                }
                var prev = start - DAY_MS
                history.sorted().forEach { c ->
                    dao.insertCast(Cast(spellId = spellId, at = c, mana = Mana.base(p.effort), prevLastCastAt = prev))
                    prev = c
                }
            }
        }

        // Decor bought along the way; plenty of mana is left over.
        val placed = listOf(
            DecorKind.CAT to FloorKind.LIVING, DecorKind.BOOKS to FloorKind.LIVING, DecorKind.CAULDRON to FloorKind.KITCHEN,
            DecorKind.HERBS to FloorKind.KITCHEN, DecorKind.CRYSTAL to FloorKind.BEDROOM, DecorKind.FERN to FloorKind.BALCONY,
            DecorKind.LANTERN to FloorKind.HALL,
        )
        val slots = mutableMapOf<Long, Int>()
        placed.forEachIndexed { i, (kind, fk) ->
            val fid = floorIds.getValue(fk)
            val slot = slots.getOrDefault(fid, 0)
            slots[fid] = slot + 1
            dao.upsertDecor(Decor(kind = kind.name, floorId = fid, slot = slot, cost = kind.cost, boughtAt = start + (i + 3) * 6 * DAY_MS))
        }
        dao.upsertDecor(Decor(kind = DecorKind.CANDLES.name, floorId = 0, cost = DecorKind.CANDLES.cost, boughtAt = now - 3 * DAY_MS))

        // The shopping list, and reagents remembered from earlier trips.
        data class R(val en: String, val ru: String, val aisle: Aisle, val needed: Boolean, val gap: Double, val daysAgo: Double)
        listOf(
            R("Milk", "Молоко", Aisle.DAIRY, true, 4.0, 4.5),
            R("Bread", "Хлеб", Aisle.BAKERY, true, 3.0, 3.2),
            R("Apples", "Яблоки", Aisle.PRODUCE, true, 7.0, 6.0),
            R("Eggs", "Яйца", Aisle.DAIRY, false, 8.0, 9.0),
            R("Coffee beans", "Кофе в зёрнах", Aisle.DRINKS, false, 14.0, 15.0),
            R("Dish soap", "Средство для посуды", Aisle.HOUSEHOLD, true, 21.0, 20.0),
            R("Buckwheat", "Гречка", Aisle.PANTRY, false, 20.0, 6.0),
            R("Cheese", "Сыр", Aisle.DAIRY, false, 7.0, 2.0),
            R("Tomatoes", "Помидоры", Aisle.PRODUCE, true, 5.0, 5.0),
            R("Cat food", "Корм для кота", Aisle.PETS, false, 10.0, 11.0),
            R("Dark chocolate", "Горький шоколад", Aisle.SWEETS, true, 0.0, 0.0),
        ).forEach { r ->
            val bought = r.gap > 0
            dao.upsertReagent(
                Reagent(
                    name = t(r.en, r.ru), aisle = r.aisle.name, needed = r.needed, addedAt = now - DAY_MS,
                    lastBoughtAt = if (bought) now - (r.daysAgo * DAY_MS).toLong() else null,
                    boughtCount = if (bought) 3 + rnd.nextInt(5) else 0,
                    avgGapDays = r.gap,
                )
            )
        }

        val startDay = today.toEpochDay() - 45
        val world = World(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), now, zone, startDay)
        val arrived = world.lettersArrived(Letters.COUNT)
        settings.update {
            it.copy(
                onboarded = true,
                name = t("Anna", "Аня"),
                gender = Gender.FEMALE,
                startDay = startDay,
                lettersRead = (0 until arrived - 1).map { n -> n.toString() }.toSet(),
            )
        }
    }
}
