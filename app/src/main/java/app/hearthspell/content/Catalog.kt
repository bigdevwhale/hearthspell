package app.hearthspell.content

import java.util.Locale

/** A text in both languages the app speaks. */
data class L(val en: String, val ru: String) {
    fun of(locale: Locale): String = if (locale.language == "ru") ru else en
}

/**
 * A kind of room in the home, and the tower floor it becomes. [sky] floors are drawn near the top of the tower,
 * [ground] ones by the door; the rest go in between in the user's order.
 */
enum class FloorKind(val room: L, val magic: L, val blurb: L) {
    KITCHEN(L("Kitchen", "Кухня"), L("Alchemy Lab", "Алхимическая"), L("Cauldrons, flasks and the smell of supper.", "Котлы, колбы и запах ужина.")),
    LIVING(L("Living room", "Гостиная"), L("Library", "Библиотека"), L("Tall shelves and a deep armchair.", "Высокие полки и глубокое кресло.")),
    BEDROOM(L("Bedroom", "Спальня"), L("Observatory", "Обсерватория"), L("A bed under the stars.", "Постель под звёздами.")),
    BATH(L("Bathroom", "Ванная"), L("Spring Baths", "Купальня"), L("A warm spring bubbles here.", "Здесь бьёт тёплый источник.")),
    HALL(L("Hallway", "Прихожая"), L("Gate Hall", "Врата"), L("Where boots and travellers come in.", "Сюда входят сапоги и путники.")),
    BALCONY(L("Balcony & plants", "Балкон и цветы"), L("Greenhouse", "Оранжерея"), L("Leaves, sun and a watering can.", "Листья, солнце и лейка.")),
    KIDS(L("Kids' room", "Детская"), L("Toy Workshop", "Мастерская игрушек"), L("Wooden knights and runaway blocks.", "Деревянные рыцари и сбежавшие кубики.")),
    STUDY(L("Study", "Кабинет"), L("Scriptorium", "Скрипторий"), L("Scrolls, quills and unpaid bills.", "Свитки, перья и неоплаченные счета.")),
    LAUNDRY(L("Laundry", "Стирка"), L("Weaving Room", "Ткацкая"), L("Linen, thread and a hot iron.", "Лён, нитки и горячий утюг.")),
    PETS(L("Pets", "Питомцы"), L("Menagerie", "Зверинец"), L("Bowls, beds and happy paws.", "Миски, лежанки и довольные лапы.")),
    WHOLE(L("Whole home", "Весь дом"), L("Tower Heart", "Сердце башни"), L("Spells cast on every floor at once.", "Заклинания на все этажи разом."));

    companion object {
        fun of(key: String?): FloorKind = entries.firstOrNull { it.name == key } ?: WHOLE
    }
}

/** The animated helpers that wake up as spells are mastered. */
enum class HelperKind(val title: L, val blurb: L) {
    BROOM(L("Broom", "Метла"), L("Sweeps in circles and hums.", "Метёт кругами и что-то напевает.")),
    SPONGE(L("Sponge", "Губка"), L("Bubbly, cheerful, always a bit damp.", "Пузырчатая, весёлая, слегка мокрая.")),
    CLOTH(L("Rag", "Тряпица"), L("Polishes anything that shines.", "Натирает всё, что может блестеть.")),
    BUCKET(L("Bucket", "Ведро"), L("Sloshes proudly along the floors.", "Гордо плещется по полам.")),
    DUSTER(L("Duster", "Метёлка"), L("Tickles the dust off the shelves.", "Щекочет пыль на полках.")),
    SACK(L("Sack", "Мешок"), L("Swallows rubbish and burps.", "Глотает мусор и икает.")),
    LADLE(L("Ladle", "Половник"), L("Tastes every soup twice.", "Пробует каждый суп дважды.")),
    TEAPOT(L("Teapot", "Чайник"), L("Whistles when the larder is tidy.", "Свистит, когда в кладовой порядок.")),
    PILLOW(L("Pillow", "Подушка"), L("Fluffs itself and naps on duty.", "Взбивается сама и дремлет на посту.")),
    BASKET(L("Basket", "Корзина"), L("Carries linen up and down the stairs.", "Носит бельё вверх и вниз по лестнице.")),
    IRON(L("Iron", "Утюг"), L("Steams with importance.", "Пыхтит с важным видом.")),
    CAN(L("Watering can", "Лейка"), L("Talks to the ferns.", "Разговаривает с папоротниками.")),
    CANDLE(L("Candle", "Свеча"), L("Lights up lost things.", "Освещает потерянные вещи.")),
    BOOK(L("Ledger", "Книжица"), L("Remembers every bill.", "Помнит каждый счёт.")),
    KEY(L("Key", "Ключ"), L("Guards the door and the shoes.", "Сторожит дверь и обувь.")),
    BOWL(L("Bowl", "Миска"), L("Friends with every cat.", "Дружит со всеми котами.")),
    SATCHEL(L("Satchel", "Котомка"), L("Knows the way to every market.", "Знает дорогу на любой рынок."));

    companion object {
        fun of(key: String?): HelperKind = entries.firstOrNull { it.name == key } ?: BROOM
    }
}

/** A ready-made spell: a real chore, its usual interval in days, its effort (1 small … 3 big) and its helper. */
data class SpellPreset(
    val key: String,
    val floor: FloorKind,
    val name: L,
    val days: Int,
    val effort: Int,
    val helper: HelperKind,
    /** The playful incantation shown in the grimoire. */
    val chant: L,
    /** Suggested in onboarding without the user having to tick it. */
    val common: Boolean = true,
)

private fun s(key: String, floor: FloorKind, en: String, ru: String, days: Int, effort: Int, helper: HelperKind, chantEn: String, chantRu: String, common: Boolean = true) =
    SpellPreset(key, floor, L(en, ru), days, effort, helper, L(chantEn, chantRu), common)

object Spells {
    val all: List<SpellPreset> = listOf(
        s("kitchen.dishes", FloorKind.KITCHEN, "Wash the dishes", "Помыть посуду", 1, 1, HelperKind.SPONGE,
            "Spell of Shining Plates. Speak it over the sink until the water runs warm.", "Заклятье сияющих тарелок. Произносить над раковиной, пока вода не станет тёплой."),
        s("kitchen.counters", FloorKind.KITCHEN, "Wipe counters & stove", "Протереть стол и плиту", 2, 1, HelperKind.CLOTH,
            "One circle clockwise, one for luck.", "Круг по солнцу и ещё один на удачу."),
        s("kitchen.trash", FloorKind.KITCHEN, "Take out the rubbish", "Вынести мусор", 2, 1, HelperKind.SACK,
            "Banishment of the Bin. Works best with shoes on.", "Изгнание из ведра. Лучше работает в обуви."),
        s("kitchen.cook", FloorKind.KITCHEN, "Cook a meal at home", "Приготовить еду", 1, 2, HelperKind.LADLE,
            "Stir thrice, taste once, salt to the heart's content.", "Трижды помешать, раз попробовать, посолить по сердцу."),
        s("kitchen.fridge", FloorKind.KITCHEN, "Sort out the fridge", "Разобрать холодильник", 7, 2, HelperKind.TEAPOT,
            "Divination of the Cold Box: what is still good, what has become a potion.", "Гадание на холодном ларе: что ещё годно, а что уже стало зельем."),
        s("kitchen.floor", FloorKind.KITCHEN, "Mop the kitchen floor", "Помыть пол на кухне", 7, 2, HelperKind.BUCKET,
            "Tide of the Tiles. Retreat towards the door.", "Прилив по плитке. Отступать к двери."),
        s("kitchen.oven", FloorKind.KITCHEN, "Clean the oven & microwave", "Помыть духовку и микроволновку", 30, 3, HelperKind.SPONGE,
            "The Dragon's Mouth must be scrubbed once a moon.", "Пасть дракона следует отмывать раз в луну.", common = false),

        s("living.tidy", FloorKind.LIVING, "Put things back in place", "Разложить вещи по местам", 2, 1, HelperKind.CANDLE,
            "Every thing to its shelf, every shelf to its wall.", "Каждой вещи свою полку, каждой полке свою стену."),
        s("living.dust", FloorKind.LIVING, "Dust the shelves", "Протереть пыль", 7, 1, HelperKind.DUSTER,
            "Breath of the Feather. Mind the old books, they sneeze.", "Дыхание пера. Осторожно, старые книги чихают."),
        s("living.vacuum", FloorKind.LIVING, "Vacuum the living room", "Пропылесосить гостиную", 7, 2, HelperKind.BROOM,
            "The Great Whirl. Under the sofa counts.", "Великий вихрь. Под диваном тоже считается."),
        s("living.cushions", FloorKind.LIVING, "Shake out cushions & throws", "Взбить подушки и пледы", 14, 1, HelperKind.PILLOW,
            "Fluff and flourish.", "Взбить и расправить.", common = false),
        s("living.windows", FloorKind.LIVING, "Wash the windows", "Помыть окна", 90, 3, HelperKind.CLOTH,
            "Clear Sight. Lets the sun back in.", "Ясный взор. Впускает солнце обратно.", common = false),

        s("bedroom.bed", FloorKind.BEDROOM, "Make the bed", "Заправить кровать", 1, 1, HelperKind.PILLOW,
            "Smoothing of the Sky. Corners tucked like folded wings.", "Разглаживание неба. Углы подоткнуть, как сложенные крылья."),
        s("bedroom.linen", FloorKind.BEDROOM, "Change the bed linen", "Сменить постельное бельё", 14, 2, HelperKind.PILLOW,
            "Fresh Clouds. Sleep comes faster on them.", "Свежие облака. На них сон приходит быстрее."),
        s("bedroom.vacuum", FloorKind.BEDROOM, "Vacuum the bedroom", "Пропылесосить спальню", 7, 2, HelperKind.BROOM,
            "Sweep the stardust from under the bed.", "Вымести звёздную пыль из-под кровати."),
        s("bedroom.dust", FloorKind.BEDROOM, "Dust the bedroom", "Протереть пыль в спальне", 14, 1, HelperKind.DUSTER,
            "Polish the night table, the lamp and the telescope.", "Протереть тумбочку, лампу и телескоп.", common = false),
        s("bedroom.wardrobe", FloorKind.BEDROOM, "Sort the wardrobe", "Разобрать шкаф", 60, 3, HelperKind.BASKET,
            "Ritual of the Two Piles: keep and let go.", "Ритуал двух куч: оставить и отпустить.", common = false),

        s("bath.sink", FloorKind.BATH, "Clean the sink & mirror", "Помыть раковину и зеркало", 4, 1, HelperKind.CLOTH,
            "Mirror, mirror, shine for me.", "Зеркальце, свет мой, засияй."),
        s("bath.toilet", FloorKind.BATH, "Clean the toilet", "Помыть унитаз", 7, 2, HelperKind.BUCKET,
            "The Unspeakable Rite. Heroes do it without complaint.", "Неназываемый обряд. Герои совершают его молча."),
        s("bath.tub", FloorKind.BATH, "Scrub the bath or shower", "Помыть ванну или душ", 7, 2, HelperKind.SPONGE,
            "Cleansing of the Spring.", "Очищение источника."),
        s("bath.towels", FloorKind.BATH, "Change the towels", "Сменить полотенца", 4, 1, HelperKind.BASKET,
            "Soft and dry, as a cat likes it.", "Мягко и сухо, как любят коты."),
        s("bath.floor", FloorKind.BATH, "Wash the bathroom floor", "Помыть пол в ванной", 7, 2, HelperKind.BUCKET,
            "Puddles, begone.", "Лужи, прочь.", common = false),

        s("hall.shoes", FloorKind.HALL, "Tidy the shoes", "Расставить обувь", 3, 1, HelperKind.KEY,
            "Line up, boots, the master is watching.", "Сапоги, в шеренгу, мастер смотрит."),
        s("hall.floor", FloorKind.HALL, "Sweep the hallway", "Подмести прихожую", 4, 1, HelperKind.BROOM,
            "Sweep out the road dust.", "Вымести дорожную пыль."),
        s("hall.coats", FloorKind.HALL, "Sort coats & bags", "Разобрать куртки и сумки", 30, 2, HelperKind.KEY,
            "Empty every pocket: receipts, pebbles, mysteries.", "Вытряхнуть карманы: чеки, камушки, тайны.", common = false),

        s("balcony.water", FloorKind.BALCONY, "Water the plants", "Полить цветы", 4, 1, HelperKind.CAN,
            "Rain Call. Whisper something kind to the leaves.", "Зов дождя. Шепнуть листьям что-нибудь доброе."),
        s("balcony.sweep", FloorKind.BALCONY, "Sweep the balcony", "Подмести балкон", 14, 1, HelperKind.BROOM,
            "Return the fallen leaves to the wind.", "Вернуть опавшие листья ветру."),
        s("balcony.feed", FloorKind.BALCONY, "Feed or repot the plants", "Подкормить или пересадить цветы", 60, 2, HelperKind.CAN,
            "New pots for growing roots.", "Новые горшки для растущих корней.", common = false),

        s("kids.toys", FloorKind.KIDS, "Put the toys away", "Убрать игрушки", 1, 1, HelperKind.BASKET,
            "Every knight back to the castle.", "Всех рыцарей обратно в замок."),
        s("kids.vacuum", FloorKind.KIDS, "Vacuum the kids' room", "Пропылесосить детскую", 7, 2, HelperKind.BROOM,
            "Find the lost block before your foot does.", "Найти потерянный кубик раньше, чем на него наступит нога."),
        s("kids.clothes", FloorKind.KIDS, "Sort outgrown clothes", "Разобрать выросшую одежду", 90, 2, HelperKind.BASKET,
            "Small boots for smaller travellers.", "Маленькие сапожки для путников поменьше.", common = false),

        s("study.desk", FloorKind.STUDY, "Clear the desk", "Разобрать стол", 3, 1, HelperKind.CANDLE,
            "A clear desk lets thoughts land.", "На чистый стол мысли садятся сами."),
        s("study.papers", FloorKind.STUDY, "Sort papers & pay bills", "Разобрать бумаги и оплатить счета", 14, 2, HelperKind.BOOK,
            "Ledger Binding. The goblins of debt hate it.", "Заклятье учётной книги. Его боятся гоблины долгов."),
        s("study.screens", FloorKind.STUDY, "Wipe screens & keyboard", "Протереть экран и клавиатуру", 14, 1, HelperKind.CLOTH,
            "Unsmudge the scrying glass.", "Очистить магический кристалл от пятен.", common = false),

        s("laundry.wash", FloorKind.LAUNDRY, "Do the laundry", "Постирать", 3, 2, HelperKind.BASKET,
            "Whirlpool of the Washing Barrel.", "Водоворот стиральной бочки."),
        s("laundry.fold", FloorKind.LAUNDRY, "Fold & put away", "Сложить и разложить бельё", 3, 1, HelperKind.BASKET,
            "Socks find their pairs in the end.", "Носки в конце концов находят пару."),
        s("laundry.iron", FloorKind.LAUNDRY, "Ironing", "Погладить", 7, 2, HelperKind.IRON,
            "Flatten the wrinkles of the week.", "Разгладить морщины недели.", common = false),

        s("pets.bowls", FloorKind.PETS, "Wash pet bowls & fresh water", "Помыть миски, свежая вода", 2, 1, HelperKind.BOWL,
            "Fill the bowl, gain a friend.", "Наполни миску, и обретёшь друга."),
        s("pets.litter", FloorKind.PETS, "Clean the litter or cage", "Убрать лоток или клетку", 2, 1, HelperKind.SACK,
            "A noble duty of every familiar's keeper.", "Благородный долг хранителя фамильяра."),
        s("pets.groom", FloorKind.PETS, "Brush & groom", "Вычесать и ухаживать", 7, 1, HelperKind.BOWL,
            "Collect enough fur to knit a second cat.", "Собрать шерсти на второго кота.", common = false),

        s("whole.vacuum", FloorKind.WHOLE, "Vacuum the whole home", "Пропылесосить весь дом", 7, 3, HelperKind.BROOM,
            "The Grand Whirl through every floor.", "Большой вихрь через все этажи."),
        s("whole.mop", FloorKind.WHOLE, "Mop all floors", "Помыть полы везде", 14, 3, HelperKind.BUCKET,
            "The Tide that rises through the tower.", "Прилив, поднимающийся по всей башне."),
        s("whole.bins", FloorKind.WHOLE, "Empty all the bins", "Вынести все мусорки", 7, 1, HelperKind.SACK,
            "Every bin on every floor.", "Каждое ведро на каждом этаже."),
        s("whole.market", FloorKind.WHOLE, "Go to the market", "Сходить в магазин", 7, 2, HelperKind.SATCHEL,
            "Journey for reagents. Bring the list.", "Поход за реагентами. Не забыть список."),
        s("whole.declutter", FloorKind.WHOLE, "Declutter one drawer", "Разобрать один ящик", 14, 1, HelperKind.CANDLE,
            "One drawer at a time, and the tower breathes.", "По одному ящику, и башне легче дышать.", common = false),
    )

    private val byKey = all.associateBy { it.key }

    fun of(key: String?): SpellPreset? = key?.let { byKey[it] }

    fun forFloor(kind: FloorKind): List<SpellPreset> = all.filter { it.floor == kind }

    /** The spell the market mode casts when the list is done. */
    const val MARKET = "whole.market"
}

/** Things to furnish the tower with, bought with mana. */
enum class DecorKind(val title: L, val cost: Int) {
    CANDLES(L("Candles", "Свечи"), 25),
    HERBS(L("Drying herbs", "Сушёные травы"), 30),
    LANTERN(L("Lantern", "Фонарь"), 30),
    FERN(L("Fern", "Папоротник"), 35),
    BOOKS(L("Stack of books", "Стопка книг"), 40),
    MUSHROOMS(L("Glowing mushrooms", "Светящиеся грибы"), 40),
    RUG(L("Woven rug", "Тканый коврик"), 45),
    POTIONS(L("Potion shelf", "Полка зелий"), 50),
    BANNER(L("Banner", "Знамя"), 50),
    PAINTING(L("Portrait of the master", "Портрет мастера"), 55),
    GLOBE(L("Globe", "Глобус"), 60),
    HOURGLASS(L("Hourglass", "Песочные часы"), 60),
    CLOCK(L("Cuckoo clock", "Часы с кукушкой"), 70),
    CHEST(L("Old chest", "Старый сундук"), 75),
    CAULDRON(L("Cauldron", "Котёл"), 80),
    CRYSTAL(L("Crystal ball", "Хрустальный шар"), 90),
    CAT(L("Cat", "Кот"), 120),
    TELESCOPE(L("Telescope", "Телескоп"), 140),
    OWL(L("Owl", "Сова"), 150),
    FIREPLACE(L("Fireplace", "Камин"), 160);

    companion object {
        fun of(key: String?): DecorKind? = entries.firstOrNull { it.name == key }
    }
}

/** Sections of a shop, under their tower names. */
enum class Aisle(val title: L, val shop: L) {
    PRODUCE(L("Gifts of the Field", "Дары полей"), L("Fruit & veg", "Овощи и фрукты")),
    BAKERY(L("Baker's Stall", "Хлебная лавка"), L("Bread", "Хлеб")),
    DAIRY(L("Milk Rivers", "Молочные реки"), L("Dairy & eggs", "Молочное и яйца")),
    MEAT(L("Hunter's Table", "Стол охотника"), L("Meat & fish", "Мясо и рыба")),
    PANTRY(L("Larder", "Кладовая"), L("Grains, tins, spices", "Крупы, консервы, специи")),
    FROZEN(L("Ice Cellar", "Ледяной погреб"), L("Frozen", "Заморозка")),
    DRINKS(L("Potions & Brews", "Зелья и отвары"), L("Drinks", "Напитки")),
    SWEETS(L("Sweet Charms", "Сладкие чары"), L("Sweets & snacks", "Сладости и снеки")),
    HOUSEHOLD(L("Cleaning Elixirs", "Эликсиры чистоты"), L("Household", "Бытовая химия")),
    CARE(L("Healer's Tinctures", "Снадобья лекаря"), L("Personal care & pharmacy", "Уход и аптека")),
    PETS(L("For Familiars", "Для фамильяров"), L("Pet supplies", "Для животных")),
    OTHER(L("Curiosities", "Диковинки"), L("Other", "Прочее"));

    companion object {
        fun of(key: String?): Aisle = entries.firstOrNull { it.name == key } ?: OTHER
    }
}
