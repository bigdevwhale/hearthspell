package app.hearthspell.domain

import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.data.Cast
import app.hearthspell.data.Decor
import app.hearthspell.data.Floor
import app.hearthspell.data.Reagent
import app.hearthspell.data.Spell
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

    /** Days left before it's due; 0 when due today or late. */
    fun daysLeft(s: Spell): Int {
        if (s.paused) return s.intervalDays
        val left = s.lastCastAt + s.intervalDays * DAY_MS - now
        return if (left <= 0) 0 else (left / DAY_MS).toInt()
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
    val manaSpent: Int get() = decor.sumOf { it.cost }
    val mana: Int get() = manaEarned - manaSpent

    fun canAfford(kind: DecorKind): Boolean = mana >= kind.cost

    fun decorOn(floorId: Long): List<Decor> = decor.filter { it.floorId == floorId }.sortedBy { it.slot }

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
            val lastBefore = castsOf(s.id).firstOrNull { it.at < weekStartMs }?.at ?: s.createdAt.coerceAtMost(s.lastCastAt)
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

    /** What casting [s] right now is worth: doubled for a pending request. */
    fun manaFor(s: Spell): Int = Mana.base(s.effort) * if (isPendingRequest(s.id)) 2 else 1

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
