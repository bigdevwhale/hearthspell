package app.hearthspell.domain

import app.hearthspell.content.L

enum class Gender { MALE, FEMALE;
    companion object {
        fun of(id: String?): Gender = entries.firstOrNull { it.name == id } ?: FEMALE
    }
}

private val genderMark = Regex("\\{([^{}|]*)\\|([^{}|]*)\\}")

/** Resolves "{ученик|ученица}" marks: the first form for [Gender.MALE], the second for [Gender.FEMALE]. */
fun gendered(text: String, gender: Gender): String =
    if ('|' !in text) text else genderMark.replace(text) { m -> m.groupValues[if (gender == Gender.MALE) 1 else 2] }

/** How well a spell is known, by the number of times it was cast. A helper wakes at [APPRENTICE]. */
enum class Rank(val casts: Int, val title: L) {
    NOVICE(0, L("Novice", "Новичок")),
    APPRENTICE(5, L("Apprentice", "Подмастерье")),
    ADEPT(20, L("Adept", "Знаток")),
    MASTER(50, L("Master", "Мастер")),
    ARCHMAGE(100, L("Archmage", "Архимаг"));

    val next: Rank? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun of(casts: Int): Rank = entries.last { casts >= it.casts }
    }
}

/** How a spell looks right now, from its freshness. */
enum class Glow {
    /** Over half its interval left. */
    BRIGHT,
    /** Fading: up to half the interval left. */
    FADING,
    /** Due within the last fifth of the interval. */
    DUSTY,
    /** Past its interval: imps have moved in. */
    IMPS,
}

object Mana {
    /** Mana for one cast by effort: a cantrip, a usual spell, a great one. */
    fun base(effort: Int): Int = when (effort) {
        1 -> 5
        3 -> 20
        else -> 10
    }

    /** Total casts needed for each circle of apprenticeship, from the first. */
    val circles = listOf(0, 10, 30, 60, 100, 160, 240, 350, 500, 700, 1000, 1400, 2000)

    fun circle(totalCasts: Int): Int = circles.indexOfLast { totalCasts >= it } + 1
}
