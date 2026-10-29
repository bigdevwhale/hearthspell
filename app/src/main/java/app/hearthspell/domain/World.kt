package app.hearthspell.domain

import app.hearthspell.content.Aisle
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.Garment
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind
import app.hearthspell.content.Potion
import app.hearthspell.content.Season
import app.hearthspell.content.Stars
import app.hearthspell.content.Trinket
import app.hearthspell.data.Brew
import app.hearthspell.data.Cast
import app.hearthspell.data.Decor
import app.hearthspell.data.FIND_GIFT
import app.hearthspell.data.FIND_IMP
import app.hearthspell.data.Find
import app.hearthspell.data.Floor
import app.hearthspell.data.Reagent
import app.hearthspell.data.Spell
import app.hearthspell.data.Wear
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

const val DAY_MS = 24L * 60 * 60 * 1000

/** A helper and how far it has come: [level] 0 is still asleep. */
data class HelperState(
    val kind: HelperKind,
    val casts: Int,
    val level: Int,
    /** The floor it likes best: where most of its spells were cast. 0 when it has none. */
    val homeFloorId: Long,
    val spellIds: List<Long>,
) {
    val awake: Boolean get() = level > 0

    /** Casts needed for the next level, or null at the top. */
    val nextAt: Int? get() = HELPER_LEVELS.getOrNull(level)

    companion object {
        /** Casts of all its spells together for levels 1..4. */
        val HELPER_LEVELS = listOf(5, 20, 50, 100)
        fun levelOf(casts: Int): Int = HELPER_LEVELS.count { casts >= it }
    }
}

/** A trinket waiting on a floor: left by [helper] after the night before [day] was quiet there. */
data class Gift(val day: Long, val floorId: Long, val helper: HelperKind, val trinket: Trinket)

/** The week in a few lines, for Sunday tea. */
class TeaReport(
    val weekStart: LocalDate,
    val casts: Int,
    /** The busiest helpers this week, busiest first (awake ones only). */
    val helpers: List<HelperKind>,
    /** The floor with the most casts. */
    val floorId: Long?,
    val impsBanished: Int,
    val gifts: List<Trinket>,
)

/**
 * Everything stored, seen at one moment. Pure: no Android, so it is tested on the JVM.
 * [now] is epoch millis; [startDay] is the epoch day the apprenticeship began.
 */
class World(
    val floors: List<Floor>,
    val spells: List<Spell>,
    /** Newest first. */
    val casts: List<Cast>,
    val reagents: List<Reagent>,
    val decor: List<Decor>,
    val now: Long,
    val zone: ZoneId,
    val startDay: Long,
    /** Newest first. */
    val finds: List<Find> = emptyList(),
    val brews: List<Brew> = emptyList(),
    val wardrobe: List<Wear> = emptyList(),
) {
    val today: LocalDate = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val day: Long = today.toEpochDay()

    private val spellById = spells.associateBy { it.id }
    private val floorById = floors.associateBy { it.id }
    private val castsBySpell: Map<Long, List<Cast>> = casts.groupBy { it.spellId }

    fun spell(id: Long): Spell? = spellById[id]
    fun floor(id: Long): Floor? = floorById[id]
    fun floorKind(f: Floor): FloorKind = FloorKind.of(f.kind)
    fun spellsOf(floorId: Long): List<Spell> = spells.filter { it.floorId == floorId }
    fun castsOf(spellId: Long): List<Cast> = castsBySpell[spellId].orEmpty()
    fun castCount(spellId: Long): Int = castsOf(spellId).size

    // Freshness.

    /** 1 just after casting, 0 when the interval has run out (and below that it stays 0). */
    fun freshness(s: Spell): Float {
        if (s.paused) return 1f
        val elapsed = (now - s.lastCastAt).coerceAtLeast(0)
        return (1f - elapsed.toFloat() / (s.intervalDays * DAY_MS)).coerceIn(0f, 1f)
    }

    /** How far past due, in days (0 when not overdue). */
    fun overdueDays(s: Spell): Int {
        if (s.paused) return 0
        val late = now - s.lastCastAt - s.intervalDays * DAY_MS
        return if (late <= 0) 0 else (late / DAY_MS).toInt()
    }

    /** Calendar days left before it's due (a daily spell cast today is due tomorrow: 1); 0 when due today or late. */
    fun daysLeft(s: Spell): Int {
        if (s.paused) return s.intervalDays
        val dueAt = s.lastCastAt + s.intervalDays * DAY_MS
        if (dueAt <= now) return 0
        val dueDay = java.time.Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate().toEpochDay()
        return (dueDay - day).toInt().coerceAtLeast(0)
    }

    fun glow(s: Spell): Glow {
        if (s.paused) return Glow.BRIGHT
        if (now - s.lastCastAt >= s.intervalDays * DAY_MS) return Glow.IMPS
        val f = freshness(s)
        return when {
            f > 0.5f -> Glow.BRIGHT
            f > 0.2f -> Glow.FADING
            else -> Glow.DUSTY
        }
    }

    /** 0 spotless … 1 thick with dust: the mean fading of the floor's active spells. */
    fun dust(floorId: Long): Float {
        val active = spellsOf(floorId).filter { !it.paused }
        if (active.isEmpty()) return 0f
        return 1f - active.map { freshness(it) }.average().toFloat()
    }

    /** Imps on a floor: one per overdue spell, at most three are drawn. */
    fun imps(floorId: Long): Int = spellsOf(floorId).count { glow(it) == Glow.IMPS }

    /** The seasons the tower is dressed for today. */
    val seasons: Set<Season> = Season.of(today)

    /** Who moved in when [s] ran out, if it has: chosen by the room and the season of the day it ran out. */
    fun impOf(s: Spell): ImpKind? {
        if (glow(s) != Glow.IMPS) return null
        val f = floor(s.floorId) ?: return null
        val since = java.time.Instant.ofEpochMilli(s.lastCastAt + s.intervalDays * DAY_MS).atZone(zone).toLocalDate()
        return ImpKind.pick(floorKind(f), Season.of(since), s.id, since.toEpochDay())
    }

    /** The imps living on a floor right now, most overdue spell first. */
    fun impsOn(floorId: Long): List<ImpKind> =
        spellsOf(floorId).filter { glow(it) == Glow.IMPS }.sortedBy { it.lastCastAt + it.intervalDays * DAY_MS }.mapNotNull { impOf(it) }

    /** Imps shown the door so far, by kind: the bestiary. */
    val impsMet: Map<ImpKind, Int> by lazy {
        finds.filter { it.type == FIND_IMP }.mapNotNull { ImpKind.of(it.kind) }.groupingBy { it }.eachCount()
    }

    /** Where the spell stood at [t]: when it had last been cast by then. */
    fun lastCastAt(s: Spell, t: Long): Long {
        val own = castsOf(s.id)
        own.firstOrNull { it.at <= t }?.let { return it.at }
        return own.lastOrNull()?.prevLastCastAt ?: s.lastCastAt
    }

    /** True when the floor had no imps and little dust at [t]. */
    fun quietAt(floorId: Long, t: Long): Boolean {
        val ss = spellsOf(floorId).filter { !it.paused && it.createdAt <= t }
        if (ss.isEmpty()) return false
        val fresh = ss.map { s -> 1.0 - (t - lastCastAt(s, t)).toDouble() / (s.intervalDays * DAY_MS) }
        return fresh.none { it <= 0.0 } && fresh.average() >= 0.4
    }

    val totalImps: Int get() = floors.sumOf { imps(it.id) }

    /** Spells that need doing, most urgent first: overdue ones by lateness, then the most faded. */
    val due: List<Spell> by lazy {
        spells.filter { !it.paused && glow(it) >= Glow.DUSTY }
            .sortedWith(compareBy<Spell> { freshness(it) }.thenByDescending { overdueDays(it) / it.intervalDays.toFloat() })
    }

    /** Small spells (effort 1) for a low-energy day, most urgent first. */
    val cantrips: List<Spell> by lazy {
        spells.filter { !it.paused && it.effort == 1 }.sortedBy { freshness(it) }
    }

    // Mastery and helpers.

    fun rank(s: Spell): Rank = Rank.of(castCount(s.id))

    val totalCasts: Int get() = casts.size

    val circle: Int get() = Mana.circle(totalCasts)

    val helpers: List<HelperState> by lazy {
        HelperKind.entries.map { kind ->
            val own = spells.filter { HelperKind.of(it.helper) == kind }
            val count = own.sumOf { castCount(it.id) }
            val home = own.groupBy { it.floorId }.maxByOrNull { (_, ss) -> ss.sumOf { castCount(it.id) } * 1000 + ss.size }?.key ?: 0L
            HelperState(kind, count, HelperState.levelOf(count), home, own.map { it.id })
        }
    }

    fun helper(kind: HelperKind): HelperState = helpers.first { it.kind == kind }

    /** Awake helpers living on a floor. */
    fun helpersOn(floorId: Long): List<HelperState> = helpers.filter { it.awake && it.homeFloorId == floorId }

    // Mana.

    val manaEarned: Int get() = casts.sumOf { it.mana }
    val manaSpent: Int get() = decor.sumOf { it.cost } + wardrobe.sumOf { it.cost }
    val mana: Int get() = manaEarned - manaSpent

    fun canAfford(kind: DecorKind): Boolean = mana >= kind.cost

    fun decorOn(floorId: Long): List<Decor> = decor.filter { it.floorId == floorId }.sortedBy { it.slot }

    fun owns(g: Garment): Boolean = g.free || wardrobe.any { it.kind == g.name }

    fun canAfford(g: Garment): Boolean = mana >= g.cost

    // Gifts.

    /** The gift for [d], if the helpers left one: a quiet floor overnight, an awake helper living there, and a little luck. */
    fun giftFor(d: Long): Gift? {
        val morning = LocalDate.ofEpochDay(d).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()
        if (morning > now || d <= startDay) return null
        val h = mix(d, startDay + 17)
        if (h % 100 >= 65) return null
        val candidates = floors.filter { quietAt(it.id, morning) }.flatMap { f -> helpersOn(f.id).map { f.id to it.kind } }
        if (candidates.isEmpty()) return null
        val (floorId, helper) = candidates[(h / 100 % candidates.size).toInt()]
        val roll = h / 10_000 % 100
        val rare = Trinket.entries.filter { it.rare }
        val common = Trinket.entries.filter { !it.rare }
        val trinket = when {
            roll < 6 -> rare[(h / 1_000_000 % rare.size).toInt()]
            roll < 50 -> Trinket.favourite(helper)
            else -> common[(h / 1_000_000 % common.size).toInt()]
        }
        return Gift(d, floorId, helper, trinket)
    }

    /** Gifts waiting to be picked up: they keep for three days. */
    val gifts: List<Gift> by lazy {
        val claimed = finds.filter { it.type == FIND_GIFT }.map { it.day }.toSet()
        (day - 2..day).filter { it !in claimed }.mapNotNull { giftFor(it) }
    }

    fun giftOn(floorId: Long): Gift? = gifts.firstOrNull { it.floorId == floorId }

    /** Trinkets found so far, by kind. */
    val trinketsFound: Map<Trinket, Int> by lazy {
        finds.filter { it.type == FIND_GIFT }.mapNotNull { Trinket.of(it.kind) }.groupingBy { it }.eachCount()
    }

    /** Trinkets still in the chest: found, less those dropped into potions. */
    fun trinketsLeft(t: Trinket): Int = (trinketsFound[t] ?: 0) - brews.count { Potion.of(it.potion)?.trinket == t }

    // Alchemy.

    /** Essences on the alchemy shelf: one per reagent bought, less what went into potions. */
    val essences: Map<Aisle, Int> by lazy {
        val got = reagents.groupBy { Aisle.of(it.aisle) }.mapValues { (_, rs) -> rs.sumOf { it.boughtCount } }
        val used = brews.mapNotNull { Potion.of(it.potion) }.flatMap { it.needs.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }
        Aisle.entries.associateWith { ((got[it] ?: 0) - (used[it] ?: 0)).coerceAtLeast(0) }
    }

    fun canBrew(p: Potion): Boolean =
        p.needs.all { (a, n) -> (essences[a] ?: 0) >= n } && (p.trinket == null || trinketsLeft(p.trinket) > 0)

    /** Days a potion still works, 0 when it isn't brewing. */
    fun potionDaysLeft(p: Potion): Int {
        val last = brews.filter { it.potion == p.name }.maxOfOrNull { it.at } ?: return 0
        val left = last + Potion.DAYS * DAY_MS - now
        return if (left <= 0) 0 else ((left + DAY_MS - 1) / DAY_MS).toInt()
    }

    val potions: Set<Potion> by lazy { Potion.entries.filterTo(mutableSetOf()) { potionDaysLeft(it) > 0 } }

    // Stars.

    /** Stars lit over the Observatory: one for every rank any spell has climbed. */
    val starsLit: Int get() = spells.sumOf { rank(it).ordinal }.coerceAtMost(Stars.total)

    // The cat.

    /**
     * Where the cat sleeps: the floor just tidied (within half a day, if no imps are there), else a clean floor
     * with a cushion, else the cleanest floor.
     */
    val catFloorId: Long? by lazy {
        if (floors.isEmpty()) return@lazy null
        val recent = casts.firstOrNull { now - it.at < DAY_MS / 2 }?.let { spell(it.spellId)?.floorId }
        if (recent != null && floor(recent) != null && imps(recent) == 0) return@lazy recent
        val cushion = floors.firstOrNull { f -> decorOn(f.id).any { it.kind == DecorKind.CUSHION.name } && imps(f.id) == 0 && dust(f.id) < 0.4f }
        cushion?.id ?: floors.minByOrNull { dust(it.id) + imps(it.id) }?.id
    }

    // Coming back.

    /** Away five days or more: the first spell cast on coming home is a celebration. */
    val homecoming: Boolean get() = casts.isNotEmpty() && now - casts.first().at >= 5 * DAY_MS

    /** Whole days since the last cast. */
    val daysAway: Int get() = casts.firstOrNull()?.let { ((now - it.at) / DAY_MS).toInt() } ?: 0

    private fun mix(a: Long, b: Long): Long {
        var x = a * 6364136223846793005L + b * 1442695040888963407L
        x = x xor (x ushr 29)
        x *= -0x40a7b892e31b1a47L
        return (x ushr 33) and 0x7fffffff
    }

    // Weeks, the master's requests and letters.

    /** Monday of this week. */
    val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val weekStartMs: Long = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Weeks since the apprenticeship began; 0 in the first week. */
    val weekIndex: Int get() = ((day - startDay).coerceAtLeast(0) / 7).toInt()

    /** Letters that have arrived, of [count] written: one on the first day, then one a week. */
    fun lettersArrived(count: Int): Int = (weekIndex + 1).coerceAtMost(count)

    /**
     * The master's three requests for this week: the spells that were the most overdue (relative to their interval)
     * when the week began. Daily chores are left out unless there aren't enough others. Stable through the week.
     */
    val requests: List<Spell> by lazy {
        val candidates = spells.filter { !it.paused && it.createdAt < weekStartMs + 7 * DAY_MS }
        fun staleness(s: Spell): Double {
            val own = castsOf(s.id)
            // The last cast before the week; else where the spell stood before its first cast this week.
            val lastBefore = own.firstOrNull { it.at < weekStartMs }?.at
                ?: own.lastOrNull { it.at >= weekStartMs }?.prevLastCastAt
                ?: s.lastCastAt
            return (weekStartMs - lastBefore).toDouble() / (s.intervalDays * DAY_MS)
        }
        val weekly = candidates.filter { it.intervalDays >= 3 }
        val pool = if (weekly.size >= 3) weekly else candidates
        val salt = weekStart.toEpochDay()
        pool.sortedWith(compareByDescending<Spell> { staleness(it) }.thenBy { (it.id * 31 + salt) % 97 }).take(3)
    }

    /** True once a request has been cast this week. */
    fun requestDone(spellId: Long): Boolean = castsOf(spellId).any { it.at >= weekStartMs && it.requested }

    fun isPendingRequest(spellId: Long): Boolean = requests.any { it.id == spellId } && !requestDone(spellId)

    /** What casting [s] right now is worth: doubled for a pending request, and doubled again on coming home. */
    fun manaFor(s: Spell): Int = Mana.base(s.effort) * (if (isPendingRequest(s.id)) 2 else 1) * (if (homecoming) 2 else 1)

    /** The week whose tea is poured today: this one on Sunday, the last one on Monday; none on other days. */
    val teaWeek: LocalDate? get() = when (today.dayOfWeek) {
        DayOfWeek.SUNDAY -> weekStart
        DayOfWeek.MONDAY -> weekStart.minusWeeks(1)
        else -> null
    }

    fun teaReport(week: LocalDate): TeaReport {
        val from = week.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = week.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val inWeek = casts.filter { it.at in from until to }
        val byHelper = inWeek.mapNotNull { spell(it.spellId) }.groupingBy { HelperKind.of(it.helper) }.eachCount()
        val byFloor = inWeek.mapNotNull { spell(it.spellId)?.floorId }.groupingBy { it }.eachCount()
        val ids = inWeek.map { it.id }.toSet()
        return TeaReport(
            weekStart = week,
            casts = inWeek.size,
            helpers = byHelper.entries.filter { helper(it.key).awake }.sortedByDescending { it.value }.map { it.key }.take(4),
            floorId = byFloor.maxByOrNull { it.value }?.key,
            impsBanished = finds.count { it.type == FIND_IMP && it.castId in ids },
            gifts = finds.filter { it.type == FIND_GIFT && it.at in from until to }.mapNotNull { Trinket.of(it.kind) },
        )
    }

    /** Casts made today. */
    val castsToday: List<Cast> by lazy {
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        casts.filter { it.at >= start }
    }

    // Reagents.

    val shoppingList: List<Reagent> get() = reagents.filter { it.needed }

    /** Remembered reagents that are probably running low: not on the list, and past their usual gap. */
    val runningLow: List<Reagent> by lazy {
        reagents.filter { r ->
            !r.needed && r.boughtCount >= 2 && r.avgGapDays > 0 && r.lastBoughtAt != null &&
                (now - r.lastBoughtAt) >= r.avgGapDays * 0.9 * DAY_MS
        }.sortedByDescending { r -> (now - (r.lastBoughtAt ?: now)) / (r.avgGapDays * DAY_MS) }
    }
}
