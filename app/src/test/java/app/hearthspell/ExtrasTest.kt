package app.hearthspell

import app.hearthspell.content.Aisle
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind
import app.hearthspell.content.LetterTexts
import app.hearthspell.content.Potion
import app.hearthspell.content.Season
import app.hearthspell.content.Souvenirs
import app.hearthspell.content.Stars
import app.hearthspell.data.Brew
import app.hearthspell.data.Cast
import app.hearthspell.data.FIND_GIFT
import app.hearthspell.data.Find
import app.hearthspell.data.Floor
import app.hearthspell.data.Reagent
import app.hearthspell.data.Spell
import app.hearthspell.domain.DAY_MS
import app.hearthspell.domain.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ExtrasTest {
    private val zone = ZoneOffset.UTC
    /** Wednesday 2026-10-14, noon. */
    private val day0 = LocalDate.of(2026, 10, 14)
    private val now = day0.atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun spell(id: Long, floorId: Long = 1, days: Int = 7, ago: Double = 0.0, helper: HelperKind = HelperKind.BROOM) =
        Spell(id = id, floorId = floorId, name = "s$id", intervalDays = days, helper = helper.name, lastCastAt = now - (ago * DAY_MS).toLong(), createdAt = now - 90 * DAY_MS)

    /** [n] casts of a spell, one a day, the newest [lastAgo] days before now. */
    private fun casts(spellId: Long, n: Int, lastAgo: Double = 0.0) = (0 until n).map {
        val at = now - ((lastAgo + it) * DAY_MS).toLong()
        Cast(id = spellId * 1000 + it, spellId = spellId, at = at, mana = 10, prevLastCastAt = at - DAY_MS)
    }

    private fun world(
        spells: List<Spell>,
        casts: List<Cast> = emptyList(),
        at: Long = now,
        floors: List<Floor> = listOf(Floor(1, "KITCHEN"), Floor(2, "BATH")),
        reagents: List<Reagent> = emptyList(),
        finds: List<Find> = emptyList(),
        brews: List<Brew> = emptyList(),
        startDay: Long = day0.toEpochDay() - 60,
    ) = World(floors, spells, casts.sortedByDescending { it.at }, reagents, emptyList(), at, zone, startDay, finds, brews)

    @Test
    fun theImpDependsOnTheRoomAndSeasonNotOnLateness() {
        val s = spell(1, ago = 8.0)
        val a = world(listOf(s)).impOf(s)
        // Two weeks later the same spell, still undone, has the same imp: lateness never changes who lives there.
        val b = world(listOf(s), at = now + 14 * DAY_MS).impOf(s)
        assertNotNull(a)
        assertEquals(a, b)
        assertNull(world(listOf(spell(2, ago = 1.0))).impOf(spell(2, ago = 1.0)))
    }

    @Test
    fun seasonalImpsOnlyInTheirSeason() {
        val march = LocalDate.of(2026, 3, 10)
        val october = LocalDate.of(2026, 10, 25)
        val inMarch = (1L..300L).map { ImpKind.pick(FloorKind.KITCHEN, Season.of(march), it, march.toEpochDay()) }
        val inOctober = (1L..300L).map { ImpKind.pick(FloorKind.KITCHEN, Season.of(october), it, october.toEpochDay()) }
        assertTrue(inMarch.none { it.season != null })
        assertTrue(inOctober.any { it == ImpKind.HALLOWIMP })
        // A room's own imps never wander into other rooms.
        assertTrue(inMarch.none { it.floor != null && it.floor != FloorKind.KITCHEN })
        assertTrue(inMarch.any { it.floor == FloorKind.KITCHEN })
    }

    @Test
    fun seasonsWrapOverTheNewYear() {
        assertEquals(setOf(Season.WINTER, Season.NEW_YEAR), Season.of(LocalDate.of(2027, 1, 5)))
        assertEquals(setOf(Season.AUTUMN, Season.HALLOWEEN), Season.of(LocalDate.of(2026, 10, 31)))
        assertEquals(setOf(Season.SUMMER), Season.of(LocalDate.of(2026, 7, 1)))
        assertTrue(DecorKind.PUMPKIN.forSale(Season.of(LocalDate.of(2026, 10, 31))))
        assertFalse(DecorKind.PUMPKIN.forSale(Season.of(LocalDate.of(2026, 7, 1))))
        assertFalse(DecorKind.TEACUP.forSale(Season.of(LocalDate.of(2026, 7, 1))))
    }

    @Test
    fun giftsComeOnlyFromQuietFloorsWithAwakeHelpers() {
        // A daily spell cast every day for 30 days: the broom is awake and the kitchen is quiet every night.
        val tidy = world(listOf(spell(1, days = 7)), casts(1, 30, lastAgo = 0.1))
        val gifts = (0 until 40).mapNotNull { tidy.giftFor(day0.toEpochDay() - it) }
        assertTrue("some nights bring gifts", gifts.isNotEmpty())
        assertTrue("but not every night", gifts.size < 40)
        assertTrue(gifts.all { it.floorId == 1L && it.helper == HelperKind.BROOM })
        // Nothing before the apprenticeship began, nothing from tomorrow.
        assertNull(tidy.giftFor(day0.toEpochDay() + 1))

        // Overdue for weeks: imps all night, no gifts.
        val messy = world(listOf(spell(1, days = 1, ago = 30.0)), casts(1, 6, lastAgo = 30.0))
        assertTrue((0 until 20).all { messy.giftFor(day0.toEpochDay() - it) == null })

        // A claimed gift is gone.
        val pending = tidy.gifts
        if (pending.isNotEmpty()) {
            val g = pending.first()
            val claimed = world(listOf(spell(1, days = 7)), casts(1, 30, lastAgo = 0.1), finds = listOf(Find(type = FIND_GIFT, kind = g.trinket.name, day = g.day, at = now)))
            assertTrue(claimed.gifts.none { it.day == g.day })
            assertEquals(1, claimed.trinketsFound[g.trinket])
        }
    }

    @Test
    fun essencesComeFromTheMarketAndGoIntoPotions() {
        val reagents = listOf(
            Reagent(1, "Apples", Aisle.PRODUCE.name, needed = false, addedAt = 0, boughtCount = 3),
            Reagent(2, "Pears", Aisle.PRODUCE.name, needed = false, addedAt = 0, boughtCount = 1),
            Reagent(3, "Chocolate", Aisle.SWEETS.name, needed = false, addedAt = 0, boughtCount = 1),
        )
        val w = world(emptyList(), reagents = reagents)
        assertEquals(4, w.essences[Aisle.PRODUCE])
        assertTrue(w.canBrew(Potion.EMBERSMOKE))
        assertFalse(w.canBrew(Potion.BUBBLES))
        val brewed = world(emptyList(), reagents = reagents, brews = listOf(Brew(potion = Potion.EMBERSMOKE.name, at = now - DAY_MS)))
        assertEquals(2, brewed.essences[Aisle.PRODUCE])
        assertEquals(0, brewed.essences[Aisle.SWEETS])
        assertFalse(brewed.canBrew(Potion.EMBERSMOKE))
        assertTrue(Potion.EMBERSMOKE in brewed.potions)
        assertEquals(6, brewed.potionDaysLeft(Potion.EMBERSMOKE))
        // A week later it has worn off.
        val later = world(emptyList(), reagents = reagents, brews = listOf(Brew(potion = Potion.EMBERSMOKE.name, at = now - 8 * DAY_MS)))
        assertTrue(later.potions.isEmpty())
    }

    @Test
    fun starsLightAsSpellsClimbRanks() {
        // 20 casts: Adept (2 stars). 5 casts: Apprentice (1). 1 cast: Novice (0).
        val w = world(listOf(spell(1), spell(2), spell(3)), casts(1, 20) + casts(2, 5) + casts(3, 1))
        assertEquals(3, w.starsLit)
        assertEquals(3, Stars.litIn(0, 3))
        assertEquals(0, Stars.litIn(1, 3))
        assertTrue(Stars.complete(3).isEmpty())
        assertEquals(listOf(Stars.all[0]), Stars.complete(Stars.all[0].stars.size))
        // Every star sits on the 128x72 chart, every line joins real stars.
        Stars.all.forEach { c ->
            assertTrue(c.stars.all { (x, y) -> x in 0 until 128 && y in 0 until 72 })
            assertTrue(c.lines.all { (a, b) -> a in c.stars.indices && b in c.stars.indices })
        }
    }

    @Test
    fun comingHomeIsCelebratedWithDoubleMana() {
        val away = world(listOf(spell(1, days = 7)), casts(1, 3, lastAgo = 6.0))
        assertTrue(away.homecoming)
        assertEquals(6, away.daysAway)
        assertEquals(20, away.manaFor(away.spell(1)!!).let { if (away.isPendingRequest(1)) it / 2 else it })
        val here = world(listOf(spell(1, days = 7)), casts(1, 3, lastAgo = 1.0))
        assertFalse(here.homecoming)
        assertFalse(world(listOf(spell(1))).homecoming)
    }

    @Test
    fun teaIsPouredOnSundayAndMonday() {
        val sunday = LocalDate.of(2026, 10, 18).atTime(18, 0).toInstant(zone).toEpochMilli()
        val monday = LocalDate.of(2026, 10, 19).atTime(9, 0).toInstant(zone).toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 12), world(emptyList(), at = sunday).teaWeek)
        assertEquals(LocalDate.of(2026, 10, 12), world(emptyList(), at = monday).teaWeek)
        assertNull(world(emptyList()).teaWeek)
        val w = world(listOf(spell(1)), casts(1, 6, lastAgo = 0.1))
        val r = w.teaReport(LocalDate.of(2026, 10, 12))
        assertEquals(3, r.casts)
        assertEquals(1L, r.floorId)
        assertEquals(listOf(HelperKind.BROOM), r.helpers)
    }

    @Test
    fun theCatSleepsWhereYouJustTidied() {
        val spells = listOf(spell(1, floorId = 1, ago = 3.0), spell(2, floorId = 2, ago = 0.0))
        val w = world(spells, listOf(Cast(1, 2, now - DAY_MS / 8, 10)))
        assertEquals(2L, w.catFloorId)
        // Not onto a floor with imps, though.
        val impish = world(listOf(spell(1, floorId = 1, ago = 1.0), spell(2, floorId = 2, ago = 0.0), spell(3, floorId = 2, ago = 20.0)), listOf(Cast(1, 2, now - DAY_MS / 8, 10)))
        assertEquals(1L, impish.catFloorId)
    }

    @Test
    fun everyLetterCarriesASouvenir() {
        assertEquals(LetterTexts.all.size, Souvenirs.all.size)
        assertTrue(Souvenirs.all.all { it.decor.souvenir })
        assertEquals(Souvenirs.all.size, Souvenirs.all.map { it.decor }.toSet().size)
    }
}
