package app.hearthspell

import app.hearthspell.content.HelperKind
import app.hearthspell.data.Cast
import app.hearthspell.data.Decor
import app.hearthspell.data.Floor
import app.hearthspell.data.Reagent
import app.hearthspell.data.Spell
import app.hearthspell.domain.DAY_MS
import app.hearthspell.domain.Glow
import app.hearthspell.domain.HelperState
import app.hearthspell.domain.Rank
import app.hearthspell.domain.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WorldTest {
    private val zone = ZoneOffset.UTC
    /** Wednesday 2026-10-14, noon. */
    private val now = LocalDate.of(2026, 10, 14).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun spell(id: Long, floorId: Long = 1, days: Int = 7, ago: Double = 0.0, helper: HelperKind = HelperKind.BROOM, effort: Int = 2, paused: Boolean = false) =
        Spell(
            id = id, floorId = floorId, name = "s$id", intervalDays = days, effort = effort, helper = helper.name,
            lastCastAt = now - (ago * DAY_MS).toLong(), createdAt = now - DAY_MS / 2, paused = paused,
        )

    private fun casts(spellId: Long, n: Int, mana: Int = 10, from: Long = now - 50 * DAY_MS) =
        (0 until n).map { Cast(id = spellId * 1000 + it, spellId = spellId, at = from + it * 1000L, mana = mana) }

    private fun world(
        spells: List<Spell>,
        casts: List<Cast> = emptyList(),
        floors: List<Floor> = listOf(Floor(1, "KITCHEN"), Floor(2, "BATH")),
        reagents: List<Reagent> = emptyList(),
        decor: List<Decor> = emptyList(),
        at: Long = now,
        startDay: Long = LocalDate.of(2026, 10, 14).toEpochDay(),
    ) = World(floors, spells, casts.sortedByDescending { it.at }, reagents, decor, at, zone, startDay)

    @Test
    fun freshnessFadesOverTheInterval() {
        val w = world(listOf(spell(1, ago = 0.0), spell(2, ago = 3.5), spell(3, ago = 7.0), spell(4, ago = 10.0)))
        assertEquals(1f, w.freshness(w.spell(1)!!), 0.001f)
        assertEquals(0.5f, w.freshness(w.spell(2)!!), 0.001f)
        assertEquals(0f, w.freshness(w.spell(3)!!), 0.001f)
        assertEquals(0f, w.freshness(w.spell(4)!!), 0.001f)
        assertEquals(3, w.overdueDays(w.spell(4)!!))
        assertEquals(0, w.overdueDays(w.spell(2)!!))
        assertEquals(3, w.daysLeft(w.spell(2)!!))
    }

    @Test
    fun glowSteps() {
        val w = world(listOf(spell(1, ago = 1.0), spell(2, ago = 4.0), spell(3, ago = 6.0), spell(4, ago = 7.5), spell(5, ago = 30.0, paused = true)))
        assertEquals(Glow.BRIGHT, w.glow(w.spell(1)!!))
        assertEquals(Glow.FADING, w.glow(w.spell(2)!!))
        assertEquals(Glow.DUSTY, w.glow(w.spell(3)!!))
        assertEquals(Glow.IMPS, w.glow(w.spell(4)!!))
        assertEquals(Glow.BRIGHT, w.glow(w.spell(5)!!))
        assertEquals(1f, w.freshness(w.spell(5)!!), 0.001f)
    }

    @Test
    fun impsCountOverdueSpellsPerFloor() {
        val w = world(listOf(spell(1, 1, ago = 8.0), spell(2, 1, ago = 9.0), spell(3, 1, ago = 1.0), spell(4, 2, ago = 20.0), spell(5, 2, ago = 20.0, paused = true)))
        assertEquals(2, w.imps(1))
        assertEquals(1, w.imps(2))
        assertEquals(3, w.totalImps)
        // Due: overdue ones first.
        assertEquals(listOf(1L, 2L, 4L).toSet(), w.due.take(3).map { it.id }.toSet())
    }

    @Test
    fun dustIsTheMeanFading() {
        val w = world(listOf(spell(1, 1, ago = 0.0), spell(2, 1, ago = 7.0)))
        assertEquals(0.5f, w.dust(1), 0.001f)
        assertEquals(0f, w.dust(2), 0.001f)
    }

    @Test
    fun rankThresholds() {
        assertEquals(Rank.NOVICE, Rank.of(0))
        assertEquals(Rank.NOVICE, Rank.of(4))
        assertEquals(Rank.APPRENTICE, Rank.of(5))
        assertEquals(Rank.ADEPT, Rank.of(20))
        assertEquals(Rank.MASTER, Rank.of(50))
        assertEquals(Rank.ARCHMAGE, Rank.of(100))
        assertEquals(Rank.ARCHMAGE, Rank.of(999))
        val w = world(listOf(spell(1)), casts(1, 21))
        assertEquals(Rank.ADEPT, w.rank(w.spell(1)!!))
    }

    @Test
    fun helperLevelsAndHomeFloor() {
        assertEquals(0, HelperState.levelOf(4))
        assertEquals(1, HelperState.levelOf(5))
        assertEquals(2, HelperState.levelOf(20))
        assertEquals(4, HelperState.levelOf(100))
        val spells = listOf(spell(1, 1, helper = HelperKind.SPONGE), spell(2, 2, helper = HelperKind.SPONGE), spell(3, 1, helper = HelperKind.CAN))
        val w = world(spells, casts(1, 3) + casts(2, 8) + casts(3, 2))
        val sponge = w.helper(HelperKind.SPONGE)
        assertEquals(11, sponge.casts)
        assertEquals(1, sponge.level)
        assertTrue(sponge.awake)
        assertEquals(2L, sponge.homeFloorId)
        assertEquals(20, sponge.nextAt)
        assertFalse(w.helper(HelperKind.CAN).awake)
        assertEquals(listOf(HelperKind.SPONGE), w.helpersOn(2).map { it.kind })
        assertTrue(w.helpersOn(1).isEmpty())
    }

    @Test
    fun manaIsEarnedMinusSpent() {
        val w = world(
            listOf(spell(1)), casts(1, 10, mana = 10),
            decor = listOf(Decor(1, "CAT", 1, 0, 60, now), Decor(2, "FERN", 0, 0, 25, now)),
        )
        assertEquals(100, w.manaEarned)
        assertEquals(85, w.manaSpent)
        assertEquals(15, w.mana)
        assertEquals(1, w.decorOn(1).size)
    }

    @Test
    fun threeRequestsPreferringWeeklyAndStableThroughTheWeek() {
        val spells = listOf(
            spell(1, days = 1, ago = 5.0),
            spell(2, days = 7, ago = 20.0),
            spell(3, days = 14, ago = 30.0),
            spell(4, days = 3, ago = 1.0),
            spell(5, days = 30, ago = 10.0),
        )
        // Spell 2 was last cast well before this week.
        val before = Cast(9, 2, now - 20 * DAY_MS, 10)
        // A daily spell cast yesterday, so this isn't a homecoming (which would double the mana again).
        val recent = Cast(10, 1, now - DAY_MS, 5, prevLastCastAt = now - 5 * DAY_MS)
        val w = world(spells, listOf(before, recent))
        assertEquals(3, w.requests.size)
        assertTrue(w.requests.none { it.intervalDays < 3 })
        assertEquals(listOf(2L, 3L), w.requests.take(2).map { it.id }.sorted())
        // Casting a request mid-week doesn't reshuffle them.
        val castAt = now - DAY_MS / 2
        val after = world(spells.map { if (it.id == 2L) it.copy(lastCastAt = castAt) else it }, listOf(before, recent, Cast(1, 2, castAt, 20, requested = true)))
        assertEquals(w.requests.map { it.id }, after.requests.map { it.id })
        assertTrue(after.requestDone(2))
        assertFalse(after.isPendingRequest(2))
        assertTrue(w.isPendingRequest(2))
        assertEquals(20, w.manaFor(w.spell(2)!!))
        assertEquals(10, after.manaFor(after.spell(2)!!))
        // The same requests on Sunday of the same week.
        val sunday = LocalDate.of(2026, 10, 18).atTime(20, 0).toInstant(zone).toEpochMilli()
        assertEquals(w.requests.map { it.id }, world(spells, listOf(before, recent), at = sunday).requests.map { it.id })
    }

    @Test
    fun fewSpellsFallBackToDailyOnes() {
        val w = world(listOf(spell(1, days = 1, ago = 2.0), spell(2, days = 1, ago = 1.0), spell(3, days = 7, ago = 3.0)))
        assertEquals(3, w.requests.size)
    }

    @Test
    fun lettersArriveWeekly() {
        val start = LocalDate.of(2026, 10, 14).toEpochDay()
        assertEquals(1, world(emptyList(), startDay = start).lettersArrived(12))
        assertEquals(1, world(emptyList(), startDay = start - 6).lettersArrived(12))
        assertEquals(2, world(emptyList(), startDay = start - 7).lettersArrived(12))
        assertEquals(12, world(emptyList(), startDay = start - 400).lettersArrived(12))
        assertEquals(0, world(emptyList(), startDay = start - 3).weekIndex)
    }

    @Test
    fun runningLowAfterTheUsualGap() {
        val r = listOf(
            Reagent(1, "Milk", "DAIRY", needed = false, addedAt = 0, lastBoughtAt = now - 5 * DAY_MS, boughtCount = 3, avgGapDays = 4.0),
            Reagent(2, "Rice", "PANTRY", needed = false, addedAt = 0, lastBoughtAt = now - 2 * DAY_MS, boughtCount = 3, avgGapDays = 20.0),
            Reagent(3, "Bread", "BAKERY", needed = true, addedAt = 0, lastBoughtAt = now - 9 * DAY_MS, boughtCount = 5, avgGapDays = 3.0),
            Reagent(4, "Salt", "PANTRY", needed = false, addedAt = 0, lastBoughtAt = now - 90 * DAY_MS, boughtCount = 1, avgGapDays = 0.0),
        )
        val w = world(emptyList(), reagents = r)
        assertEquals(listOf(1L), w.runningLow.map { it.id })
        assertEquals(listOf(3L), w.shoppingList.map { it.id })
    }
}
