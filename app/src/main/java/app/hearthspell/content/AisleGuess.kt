package app.hearthspell.content

/**
 * Guesses the shop section of a reagent from its name, in English or Russian.
 * Each key is a word stem matched at the start of any word ("молок" finds "молоко", "egg" finds "eggs");
 * keys with a space are matched as phrases. The longest matching key wins, so "cat food" lands with pets.
 */
object AisleGuess {
    private val keys: List<Pair<Aisle, List<String>>> = listOf(
        Aisle.PETS to listOf(
            "cat food", "dog food", "kibble", "litter", "pet food", "pet treats",
            "корм", "наполнител", "кошач", "собач", "лакомств",
        ),
        Aisle.HOUSEHOLD to listOf(
            "detergent", "dish soap", "washing", "bleach", "sponge", "bin bag", "trash bag", "rubbish bag", "toilet paper", "toilet roll",
            "paper towel", "kitchen roll", "foil", "cling", "napkin", "cleaner", "softener", "dishwasher", "battery", "batteries", "bulb",
            "порош", "моющ", "средств", "отбел", "губк", "мешк", "мусор", "туалетн", "бумажн", "полотенц", "фольг", "плёнк", "пленк",
            "салфет", "кондиционер для бель", "капсул", "батарей", "лампоч", "освежит",
        ),
        Aisle.CARE to listOf(
            "shampoo", "conditioner", "soap", "toothpaste", "toothbrush", "deodorant", "razor", "plaster", "vitamin", "aspirin",
            "paracetamol", "ibuprofen", "pill", "hand cream", "face cream", "lotion", "tampon", "pads", "nappies", "diapers", "floss",
            "шампун", "бальзам", "мыл", "зубн", "щётк", "щетк", "дезодор", "бритв", "пластыр", "витамин", "аспирин",
            "парацетамол", "ибупрофен", "таблет", "крем", "лосьон", "тампон", "прокладк", "подгузн", "аптек", "лекарств",
        ),
        Aisle.FROZEN to listOf(
            "frozen", "ice cream", "pelmeni", "dumplings",
            "заморож", "морожен", "пельмен", "вареник", "лёд", "лед",
        ),
        Aisle.SWEETS to listOf(
            "chocolate", "candy", "sweets", "cookie", "biscuit", "cake", "chips", "crisps", "popcorn", "gum", "marshmallow", "jam", "honey",
            "шоколад", "конфет", "печень", "пряник", "торт", "пирожн", "чипс", "попкорн", "жвачк", "зефир", "мармелад", "варень", "мёд", "мед", "халв", "вафл",
        ),
        Aisle.DRINKS to listOf(
            "water", "juice", "soda", "cola", "lemonade", "beer", "wine", "coffee", "tea", "kombucha", "kvass",
            "вод", "сок", "газиров", "кола", "лимонад", "пив", "вин", "кофе", "чай", "морс", "компот", "квас",
        ),
        Aisle.DAIRY to listOf(
            "milk", "cheese", "butter", "yogurt", "yoghurt", "kefir", "egg", "cream", "sour cream", "cream cheese", "cottage",
            "молок", "сыр", "масло сливоч", "сливочн", "йогурт", "кефир", "яйц", "яиц", "сметан", "творог", "ряженк", "сливк", "простокваш",
        ),
        Aisle.BAKERY to listOf(
            "bread", "baguette", "loaf", "bun", "roll", "croissant", "bagel", "pita", "tortilla", "lavash",
            "хлеб", "батон", "багет", "булк", "булочк", "круассан", "бублик", "лаваш", "лепёшк", "лепешк", "сушк", "баранк",
        ),
        Aisle.MEAT to listOf(
            "chicken", "beef", "pork", "lamb", "turkey", "mince", "sausage", "ham", "bacon", "steak", "fish", "salmon", "tuna", "shrimp", "prawn",
            "кур", "говяд", "свинин", "баранин", "индейк", "фарш", "колбас", "сосиск", "ветчин", "бекон", "стейк", "рыб", "лосос", "сёмг", "семг", "кревет", "филе", "мяс",
        ),
        Aisle.PRODUCE to listOf(
            "apple", "banana", "orange", "lemon", "lime", "grape", "pear", "berr", "tomato", "potato", "onion", "garlic", "carrot", "cucumber",
            "pepper", "watermelon", "melon", "cabbage", "lettuce", "salad", "spinach", "broccoli", "mushroom", "avocado", "herb", "dill", "parsley", "fruit", "veg", "zucchini",
            "яблок", "банан", "апельсин", "мандарин", "лимон", "лайм", "виноград", "груш", "ягод", "клубник", "помидор", "томат", "картош", "картоф",
            "лук", "чеснок", "морков", "огур", "перец", "перц", "капуст", "салат", "шпинат", "брокколи", "гриб", "шампиньон", "авокадо", "зелен",
            "укроп", "петруш", "фрукт", "овощ", "кабачк", "свёкл", "свекл", "арбуз", "дын", "кураг", "изюм",
        ),
        Aisle.PANTRY to listOf(
            "rice", "pasta", "spaghetti", "noodle", "flour", "sugar", "salt", "oil", "vinegar", "cereal", "oats", "porridge", "buckwheat",
            "beans", "lentil", "tin", "canned", "sauce", "ketchup", "mayo", "mustard", "spice", "pepper", "nuts", "yeast", "baking",
            "рис", "макарон", "спагетти", "лапш", "мук", "сахар", "соль", "масло", "уксус", "хлопь", "овсян", "каш", "гречк", "греч",
            "фасол", "чечевиц", "консерв", "тушёнк", "тушенк", "соус", "кетчуп", "майонез", "горчиц", "специ", "приправ", "орех", "дрожж", "разрыхл", "крупа",
        ),
    )

    /** Words that name what the thing is, whatever it's made of: "orange juice" is a drink, not an orange. */
    private val heads: List<Pair<Aisle, List<String>>> = listOf(
        Aisle.DRINKS to listOf("juice", "smoothie", "сок", "нектар", "смузи"),
        Aisle.SWEETS to listOf("chips", "crisps", "чипс"),
    )

    private val split = Regex("[^\\p{L}]+")

    /** The section whose longest key matches (so "lemonade" beats "lemon", "колбаса" beats nothing shorter); [Aisle.OTHER] if none. */
    fun guess(name: String): Aisle {
        val text = " " + name.lowercase().replace('ё', 'е').trim() + " "
        val words = text.split(split).filter { it.isNotEmpty() }
        heads.firstOrNull { (_, stems) -> stems.any { st -> words.any { it == st || it.startsWith(st) && st.length >= 4 } } }?.let { return it.first }
        var best = Aisle.OTHER
        var bestLen = 0
        for ((aisle, stems) in keys) {
            for (raw in stems) {
                val stem = raw.lowercase().replace('ё', 'е').trim()
                if (stem.length <= bestLen) continue
                val hit = if (' ' in stem) text.contains(" $stem") else words.any { it.startsWith(stem) }
                if (hit) {
                    best = aisle
                    bestLen = stem.length
                }
            }
        }
        return best
    }
}
