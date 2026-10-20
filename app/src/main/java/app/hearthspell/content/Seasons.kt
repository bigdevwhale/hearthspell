package app.hearthspell.content

import java.time.LocalDate
import java.time.MonthDay

/**
 * Times of the year the tower dresses up for. They come back every year, so nothing is ever missed for good.
 * Several can be on at once (winter and the New Year).
 */
enum class Season(val title: L, private val from: MonthDay, private val to: MonthDay) {
    SPRING(L("Spring", "Весна"), MonthDay.of(3, 1), MonthDay.of(5, 31)),
    SUMMER(L("Summer", "Лето"), MonthDay.of(6, 1), MonthDay.of(8, 31)),
    AUTUMN(L("Autumn", "Осень"), MonthDay.of(9, 1), MonthDay.of(11, 30)),
    HALLOWEEN(L("Hallowe'en", "Хэллоуин"), MonthDay.of(10, 15), MonthDay.of(11, 3)),
    WINTER(L("Winter", "Зима"), MonthDay.of(12, 1), MonthDay.of(2, 29)),
    NEW_YEAR(L("New Year", "Новый год"), MonthDay.of(12, 20), MonthDay.of(1, 10));

    /** True on [date]; ranges may wrap over the new year. */
    fun on(date: LocalDate): Boolean {
        val d = MonthDay.from(date)
        return if (from <= to) d in from..to else d >= from || d <= to
    }

    companion object {
        fun of(date: LocalDate): Set<Season> = entries.filterTo(mutableSetOf()) { it.on(date) }
    }
}
