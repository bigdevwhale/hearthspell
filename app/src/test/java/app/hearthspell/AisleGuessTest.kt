package app.hearthspell

import app.hearthspell.content.Aisle
import app.hearthspell.content.AisleGuess
import app.hearthspell.content.LetterTexts
import app.hearthspell.ui.letters.Letters
import org.junit.Assert.assertEquals
import org.junit.Test

class AisleGuessTest {
    private fun check(expected: Aisle, vararg names: String) = names.forEach { assertEquals(it, expected, AisleGuess.guess(it)) }

    @Test fun english() {
        check(Aisle.DAIRY, "Milk", "eggs", "Cheddar cheese", "sour cream")
        check(Aisle.BAKERY, "bread", "Croissants")
        check(Aisle.PRODUCE, "apples", "Watermelon", "lemons", "potatoes")
        check(Aisle.DRINKS, "lemonade", "orange juice", "green tea")
        check(Aisle.FROZEN, "ice cream", "frozen peas")
        check(Aisle.HOUSEHOLD, "toilet paper", "dish soap", "bin bags")
        check(Aisle.CARE, "shampoo", "toothpaste")
        check(Aisle.PETS, "cat food")
        check(Aisle.MEAT, "chicken breast", "salmon")
        check(Aisle.PANTRY, "rice", "pasta", "olive oil")
        check(Aisle.OTHER, "birthday card", "")
    }

    @Test fun russian() {
        check(Aisle.DAIRY, "Молоко", "яйца", "сыр", "сметана", "сливочное масло")
        check(Aisle.BAKERY, "хлеб", "батон")
        check(Aisle.PRODUCE, "яблоки", "картошка", "лук", "арбуз")
        check(Aisle.MEAT, "колбаса", "куриное филе", "фарш")
        check(Aisle.DRINKS, "сок", "минеральная вода", "чай")
        check(Aisle.FROZEN, "пельмени", "мороженое")
        check(Aisle.HOUSEHOLD, "стиральный порошок", "мусорные мешки", "туалетная бумага")
        check(Aisle.CARE, "шампунь", "зубная паста")
        check(Aisle.PETS, "корм для кота")
        check(Aisle.PANTRY, "гречка", "макароны", "подсолнечное масло", "соль")
        check(Aisle.SWEETS, "шоколад", "печенье")
    }
}

class LetterTextsTest {
    @Test fun countMatchesTheLetters() {
        assertEquals(Letters.COUNT, LetterTexts.all.size)
    }

    @Test fun genderMarksAreBalanced() {
        LetterTexts.all.forEach { l ->
            listOf(l.title.en, l.title.ru, l.body.en, l.body.ru, l.from.en, l.from.ru).forEach { text ->
                assertEquals(text, text.count { it == '{' }, text.count { it == '}' })
            }
        }
    }
}
