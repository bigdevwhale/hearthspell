package app.hearthspell.content

/**
 * The constellations over the Observatory. Stars light up one by one as spells are mastered: every rank a spell
 * climbs (Apprentice, Adept, Master, Archmage) lights the next star, filling the constellations in order.
 * Star positions are on a 128x72 sky; [lines] join star indices.
 */
class Constellation(val key: String, val title: L, val line: L, val stars: List<Pair<Int, Int>>, val lines: List<Pair<Int, Int>>)

object Stars {
    private fun c(key: String, en: String, ru: String, lineEn: String, lineRu: String, ox: Int, oy: Int, pts: String, links: String): Constellation {
        val stars = pts.split(' ').map { p -> p.split(',').let { (x, y) -> ox + x.toInt() to oy + y.toInt() } }
        val lines = links.split(' ').map { p -> p.split('-').let { (a, b) -> a.toInt() to b.toInt() } }
        return Constellation(key, L(en, ru), L(lineEn, lineRu), stars, lines)
    }

    val all: List<Constellation> = listOf(
        c("broom", "The Great Broom", "Большая Метла",
            "Every apprentice's first constellation. Sailors steer by it, and so do sweepers.",
            "Первое созвездие любого ученика. По нему сверяют путь моряки и метельщики.",
            4, 4, "2,2 8,7 14,12 18,16 14,20 19,22 24,20", "0-1 1-2 2-3 3-4 3-5 3-6"),
        c("teapot", "The Teapot", "Чайник",
            "It rises when the kettle should go on. Which is always.",
            "Восходит, когда пора ставить чайник. То есть всегда.",
            36, 6, "4,10 10,6 16,6 20,10 16,16 8,16 24,8 0,8", "0-1 1-2 2-3 3-4 4-5 5-0 3-6 0-7"),
        c("cat", "The Sleeping Cat", "Спящий Кот",
            "Never wakes. The ancients tried. It just purred.",
            "Никогда не просыпается. Древние пытались. Он только мурлыкал.",
            68, 8, "2,10 6,4 9,8 16,8 22,10 20,14 10,14 26,6", "0-1 1-2 2-3 3-4 4-5 5-6 6-0 4-7"),
        c("key", "The Key", "Ключ",
            "Points to the door you forgot to lock.",
            "Указывает на дверь, которую ты забыл{|а} запереть.",
            100, 4, "4,4 9,2 10,8 5,9 14,12 20,16 18,20 23,19", "0-1 1-2 2-3 3-0 2-4 4-5 5-6 5-7"),
        c("cauldron", "The Cauldron", "Котёл",
            "Always bubbling. Nobody knows what's for supper.",
            "Всё время булькает. Что на ужин, никто не знает.",
            4, 30, "2,4 22,4 20,14 12,17 4,14 8,20 16,20", "0-1 1-2 2-3 3-4 4-0 4-5 2-6"),
        c("dragon", "Pudding the Dragon", "Дракониха Пудинг",
            "She asked to be put in the sky. She also asked for a bakery. Both are true.",
            "Попросилась на небо. А ещё в пекарню. И то и другое правда.",
            34, 28, "0,10 6,6 12,8 18,4 24,8 22,14 14,16 6,14 28,2", "0-1 1-2 2-3 3-4 4-5 5-6 6-7 7-0 3-8"),
        c("owl", "The Owl", "Сова",
            "Watches over the tower at night, and over you.",
            "Ночью приглядывает за башней. И за тобой.",
            70, 30, "4,2 16,2 4,8 16,8 10,12 6,18 14,18", "0-2 1-3 2-4 3-4 2-5 5-6 6-3"),
        c("lantern", "The Lantern", "Фонарь",
            "Lit for every traveller still on the road.",
            "Зажжён для всех, кто ещё в пути.",
            100, 28, "10,0 4,6 16,6 4,16 16,16 10,20", "0-1 0-2 1-3 2-4 3-5 4-5"),
        c("frog", "The Frog", "Лягушка",
            "The master's marsh companion. He insisted.",
            "Спутник мастера по топям. Он настоял.",
            6, 52, "4,4 14,4 2,10 16,10 9,14 0,16 18,16", "0-2 1-3 2-4 3-4 2-5 3-6"),
        c("tower", "The Tower", "Башня",
            "Your own, seen from very far away. Still standing.",
            "Твоя, если смотреть очень издалека. Стоит.",
            40, 50, "8,0 2,6 14,6 2,14 14,14 2,20 14,20", "0-1 0-2 1-3 2-4 3-5 4-6 3-4"),
        c("ship", "The Ship", "Корабль",
            "Sails between the Lantern Isles and home.",
            "Ходит между Фонарными островами и домом.",
            68, 52, "10,0 10,10 4,8 18,8 0,12 22,12 18,16 4,16", "0-1 0-2 0-3 1-4 1-5 5-6 6-7 7-4"),
        c("hat", "The Master's Hat", "Шляпа мастера",
            "The last to light. When you see it, he is nearly home.",
            "Загорается последней. Увидишь её, и он почти дома.",
            98, 50, "14,0 10,6 18,6 6,12 22,12 2,16 26,16", "0-1 0-2 1-3 2-4 3-5 4-6 5-6"),
    )

    val total: Int = all.sumOf { it.stars.size }

    /** How many of [constellation]'s stars are lit when [lit] stars are lit in all. */
    fun litIn(index: Int, lit: Int): Int {
        val before = all.take(index).sumOf { it.stars.size }
        return (lit - before).coerceIn(0, all[index].stars.size)
    }

    /** Constellations whose every star is lit. */
    fun complete(lit: Int): List<Constellation> = all.filterIndexed { i, k -> litIn(i, lit) == k.stars.size }
}
