package app.hearthspell.content

/**
 * Potions brewed in the Alchemy Lab from essences: every reagent bought at the market leaves one essence of its shop
 * section on the alchemy shelf. A potion does nothing useful at all; it makes the tower prettier for [DAYS] days.
 */
enum class Potion(val title: L, val effect: L, val needs: Map<Aisle, Int>, val trinket: Trinket? = null, val color: Long) {
    EMBERSMOKE(
        L("Embersmoke", "Цветной дым"), L("The chimney puffs pink and gold smoke.", "Из трубы валит розовый и золотой дым."),
        mapOf(Aisle.PRODUCE to 2, Aisle.SWEETS to 1), color = 0xFFF878B8,
    ),
    FIREFLIES(
        L("Firefly draught", "Светлячковый отвар"), L("Rainbow fireflies dance around the tower.", "Вокруг башни танцуют радужные светлячки."),
        mapOf(Aisle.DRINKS to 2, Aisle.PRODUCE to 1), color = 0xFFFCE070,
    ),
    STARDUST(
        L("Stardust milk", "Звёздное молоко"), L("Helpers leave a trail of sparkles.", "Помощники оставляют след из искорок."),
        mapOf(Aisle.DAIRY to 2, Aisle.BAKERY to 1), color = 0xFFFCFCFC,
    ),
    BUBBLES(
        L("Bubble tonic", "Пузырчатый тоник"), L("Soap bubbles drift up through every floor.", "По всем этажам плывут мыльные пузыри."),
        mapOf(Aisle.HOUSEHOLD to 2, Aisle.CARE to 1), color = 0xFF5FD6FF,
    ),
    BLOSSOM(
        L("Blossom brew", "Цветочный настой"), L("Flowers bloom along the tower walls.", "По стенам башни распускаются цветы."),
        mapOf(Aisle.PRODUCE to 1, Aisle.PANTRY to 1), trinket = Trinket.ACORN, color = 0xFF7DDB5A,
    ),
    MOONGLOW(
        L("Moonglow elixir", "Эликсир лунного света"), L("The moon glows violet and the stars twinkle twice as often.", "Луна светится фиолетовым, а звёзды мерцают вдвое чаще."),
        mapOf(Aisle.FROZEN to 1, Aisle.DRINKS to 1), trinket = Trinket.STAR, color = 0xFFB062E0,
    );

    companion object {
        const val DAYS = 7
        fun of(key: String?): Potion? = entries.firstOrNull { it.name == key }
    }
}

/** The apprentice's clothes, bought with mana. The first of each slot is worn from the start and costs nothing. */
enum class Garment(val title: L, val slot: Slot, val cost: Int, val ramp: LongArray? = null) {
    CRIMSON(L("Crimson robe", "Алая мантия"), Slot.ROBE, 0, longArrayOf(0xFF3C0E26, 0xFF761A2E, 0xFFB22E36, 0xFFDE5E48, 0xFFF89670)),
    MIDNIGHT(L("Midnight robe", "Полночная мантия"), Slot.ROBE, 40, longArrayOf(0xFF10143A, 0xFF1E2E6C, 0xFF2E56A6, 0xFF568AD6, 0xFF96C4F0)),
    MOSS(L("Moss robe", "Моховая мантия"), Slot.ROBE, 40, longArrayOf(0xFF0E2E28, 0xFF185C3E, 0xFF2C8C4C, 0xFF60BE60, 0xFFAAE282)),
    PLUM(L("Plum robe", "Сливовая мантия"), Slot.ROBE, 40, longArrayOf(0xFF220E32, 0xFF461C5C, 0xFF70348E, 0xFFA05EC0, 0xFFCE96E4)),
    SEA(L("Sea robe", "Морская мантия"), Slot.ROBE, 40, longArrayOf(0xFF0C2634, 0xFF124E5A, 0xFF1E7A80, 0xFF46AAA4, 0xFF96D8C8)),
    AMBER(L("Amber robe", "Янтарная мантия"), Slot.ROBE, 40, longArrayOf(0xFF5C2218, 0xFFA8461E, 0xFFE2782C, 0xFFFAAC52, 0xFFFFD896)),
    SNOW(L("Snow robe", "Снежная мантия"), Slot.ROBE, 60, longArrayOf(0xFF4E5270, 0xFF8086A0, 0xFFB2BACE, 0xFFE6ECF6, 0xFFFFFFFF)),
    POINTED(L("Pointed hat", "Острая шляпа"), Slot.HAT, 0),
    FLOWERS(L("Flower crown", "Венок"), Slot.HAT, 50),
    WIDE(L("Witch's brim", "Широкие поля"), Slot.HAT, 60),
    CAT(L("Cat-ear cap", "Шапка с ушками"), Slot.HAT, 70),
    FROG(L("Frog cap", "Лягушачья шапка"), Slot.HAT, 70),
    STAR(L("Starry hat", "Звёздный колпак"), Slot.HAT, 80),
    CROWN(L("Little crown", "Маленькая корона"), Slot.HAT, 150);

    enum class Slot { ROBE, HAT }

    val free: Boolean get() = cost == 0

    companion object {
        fun of(key: String?): Garment? = entries.firstOrNull { it.name == key }
        val defaultRobe = CRIMSON
        val defaultHat = POINTED
    }
}
