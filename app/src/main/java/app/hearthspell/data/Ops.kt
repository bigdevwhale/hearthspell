package app.hearthspell.data

import app.hearthspell.content.Aisle
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.SpellPreset
import app.hearthspell.content.Spells
import app.hearthspell.domain.DAY_MS
import app.hearthspell.domain.World
import kotlin.random.Random

/** Operations that touch several rows or tables. */

/** Casts a spell: logs it with the mana it's worth right now and refreshes it. Returns the cast id (for undo). */
suspend fun HearthDao.cast(spell: Spell, world: World, at: Long = System.currentTimeMillis()): Long {
    val id = insertCast(
        Cast(
            spellId = spell.id,
            at = at,
            mana = world.manaFor(spell),
            requested = world.isPendingRequest(spell.id),
            prevLastCastAt = spell.lastCastAt,
        )
    )
    upsertSpell(spell.copy(lastCastAt = maxOf(spell.lastCastAt, at)))
    return id
}

/** Takes a cast back: the spell fades to where it was. */
suspend fun HearthDao.undoCast(castId: Long) {
    val c = cast(castId) ?: return
    deleteCast(castId)
    val s = spell(c.spellId) ?: return
    val restored = lastCastOf(s.id)?.let { maxOf(it, c.prevLastCastAt) } ?: c.prevLastCastAt
    upsertSpell(s.copy(lastCastAt = restored))
}

/** A new spell from the catalog (or the user's own when [preset] is null), with a made-up last cast so it starts part-faded. */
suspend fun HearthDao.addSpell(
    floorId: Long,
    preset: SpellPreset?,
    now: Long,
    name: String = "",
    intervalDays: Int = preset?.days ?: 7,
    effort: Int = preset?.effort ?: 2,
    helper: String = preset?.helper?.name ?: "BROOM",
    random: Random = Random.Default,
    /** Freshness to start at; random between 0.25 and 0.9 by default so a new tower isn't all due at once. */
    freshness: Float = 0.25f + random.nextFloat() * 0.65f,
): Long {
    val sort = (spellIdsOf(floorId).size)
    val back = ((1f - freshness) * intervalDays * DAY_MS).toLong()
    return upsertSpell(
        Spell(
            floorId = floorId,
            presetKey = preset?.key,
            name = name,
            intervalDays = intervalDays.coerceAtLeast(1),
            effort = effort.coerceIn(1, 3),
            helper = helper,
            lastCastAt = now - back,
            createdAt = now,
            sort = sort,
        )
    )
}

/** Builds the tower from onboarding: floors in the given order, each with the chosen catalog spells. */
suspend fun HearthDao.setupTower(choice: List<Pair<FloorKind, List<SpellPreset>>>, now: Long, random: Random = Random.Default) {
    val start = floors().size
    choice.forEachIndexed { i, (kind, presets) ->
        val floorId = upsertFloor(Floor(kind = kind.name, sort = start + i))
        presets.forEach { addSpell(floorId, it, now, random = random) }
    }
}

suspend fun HearthDao.addFloor(kind: FloorKind, name: String = ""): Long {
    val sort = (floors().maxOfOrNull { it.sort } ?: -1) + 1
    return upsertFloor(Floor(kind = kind.name, name = name, sort = sort))
}

suspend fun HearthDao.deleteSpell(id: Long) {
    deleteCastsOf(id)
    deleteSpellRow(id)
}

/** Removes a floor with its spells; its decor goes back to the chest. */
suspend fun HearthDao.deleteFloor(id: Long) {
    spellIdsOf(id).forEach { deleteSpell(it) }
    unplaceDecorOf(id)
    deleteFloorRow(id)
}

/** Moves a floor one step up (-1) or down (+1) the tower. */
suspend fun HearthDao.moveFloor(id: Long, by: Int) {
    val all = floors().toMutableList()
    val i = all.indexOfFirst { it.id == id }
    val j = i + by
    if (i < 0 || j !in all.indices) return
    val a = all[i]
    all[i] = all[j]
    all[j] = a
    all.forEachIndexed { k, f -> if (f.sort != k) upsertFloor(f.copy(sort = k)) }
}

/** Buys a piece of decor if there's mana for it, placing it on [floorId] (0 = the chest). Returns false if too dear. */
suspend fun HearthDao.buyDecor(kind: DecorKind, floorId: Long, world: World, now: Long = System.currentTimeMillis()): Boolean {
    if (!world.canAfford(kind)) return false
    val slot = if (floorId == 0L) 0 else (world.decorOn(floorId).maxOfOrNull { it.slot } ?: -1) + 1
    upsertDecor(Decor(kind = kind.name, floorId = floorId, slot = slot, cost = kind.cost, boughtAt = now))
    return true
}

// Reagents.

/** Puts a reagent on the list: an existing one by name (case-insensitive) comes back, or a new one is remembered. */
suspend fun HearthDao.need(name: String, aisle: Aisle, now: Long = System.currentTimeMillis()): Long {
    val clean = name.trim()
    if (clean.isEmpty()) return 0
    val existing = reagents().firstOrNull { it.name.equals(clean, ignoreCase = true) }
    return if (existing != null) {
        upsertReagent(existing.copy(needed = true, inBasket = false, addedAt = now))
        existing.id
    } else {
        upsertReagent(Reagent(name = clean, aisle = aisle.name, addedAt = now))
    }
}

/**
 * Ends a market trip: everything in the basket is bought (its purchase rhythm updated) and leaves the list.
 * Casts the market spell, if the tower has one and anything was bought. Returns how many were bought.
 */
suspend fun HearthDao.finishTrip(world: World, now: Long = System.currentTimeMillis()): Int {
    val bought = reagents().filter { it.needed && it.inBasket }
    bought.forEach { r ->
        val last = r.lastBoughtAt
        val gap = if (last != null) (now - last).toDouble() / DAY_MS else 0.0
        // Same-day repeats don't count as a rhythm.
        val avg = when {
            last == null || gap < 0.5 -> r.avgGapDays
            r.avgGapDays <= 0.0 -> gap
            else -> r.avgGapDays * 0.6 + gap * 0.4
        }
        upsertReagent(
            r.copy(
                needed = false,
                inBasket = false,
                lastBoughtAt = now,
                boughtCount = r.boughtCount + if (last == null || gap >= 0.5) 1 else 0,
                avgGapDays = avg,
            )
        )
    }
    if (bought.isNotEmpty()) {
        world.spells.firstOrNull { it.presetKey == Spells.MARKET }?.let { cast(it, world, now) }
    }
    return bought.size
}
