package app.hearthspell.content

/**
 * The imps who move in, each with a name, a habit and a parting word. Which one comes depends on the room and the
 * season, never on how long a spell was left: a rare imp is never a reward for neglect.
 * [tint] names a colour ramp the purple imp sprite is repainted with.
 */
enum class ImpKind(
    val title: L,
    val habit: L,
    val farewell: L,
    val tint: ImpTint,
    val floor: FloorKind? = null,
    val season: Season? = null,
) {
    PIP(
        L("Pip the Sock-Chewer", "Пип Носкожуй"), L("Chews the left sock of every pair.", "Жуёт левый носок из каждой пары."),
        L("Fine, fine, I'm off. The sock comes with me.", "Ладно-ладно, ухожу. Носок забираю."), ImpTint.PURPLE,
    ),
    HIX(
        L("Hix who hides spoons", "Хикс, что прячет ложки"), L("Hides one spoon a day and giggles about it.", "Прячет по ложке в день и хихикает."),
        L("The spoons are in the flowerpot. Probably.", "Ложки в цветочном горшке. Наверное."), ImpTint.PURPLE,
    ),
    GRUMBLE(
        L("Grumble", "Ворчун"), L("Sighs loudly at every chore.", "Громко вздыхает над каждым делом."),
        L("Hmph. Too clean in here anyway.", "Хм. Всё равно тут слишком чисто."), ImpTint.SLATE,
    ),
    NIBS(
        L("Nibs", "Ниблс"), L("Nibbles corners off biscuits and blames the cat.", "Обкусывает уголки печенья и валит на кота."),
        L("It was the cat. Tell no one.", "Это был кот. Никому ни слова."), ImpTint.ROSE,
    ),
    CRUMBWHISTLE(
        L("Crumbwhistle", "Крошкосвист"), L("Whistles through crumbs on the counter.", "Свистит сквозь крошки на столе."),
        L("Phweet! Off to find a messier kitchen.", "Фьюить! Пойду искать кухню погрязнее."), ImpTint.AMBER, floor = FloorKind.KITCHEN,
    ),
    INKBLOT(
        L("Inkblot", "Кляксик"), L("Signs your bills with paw prints.", "Подписывает счета отпечатками лап."),
        L("I've left you a signature. Several.", "Оставил тебе автограф. Несколько."), ImpTint.SLATE, floor = FloorKind.STUDY,
    ),
    BUBBLEGOB(
        L("Bubblegob", "Пузырь-Глот"), L("Blows soap bubbles into the mirror.", "Пускает мыльные пузыри в зеркало."),
        L("Blub. Blub-blub. Goodbye.", "Бульк. Буль-бульк. Прощай."), ImpTint.SEA, floor = FloorKind.BATH,
    ),
    SNOREWICK(
        L("Snorewick", "Храпунчик"), L("Snores under the bed, slightly out of tune.", "Храпит под кроватью, слегка фальшиво."),
        L("Five more minutes... no? Fine.", "Ещё пять минуточек... нет? Ладно."), ImpTint.NIGHT, floor = FloorKind.BEDROOM,
    ),
    BOOTLACE(
        L("Bootlace", "Шнурок"), L("Ties all the shoelaces together.", "Связывает все шнурки между собой."),
        L("Untie me first! ...Thank you. Bye.", "Сначала развяжи! ...Спасибо. Пока."), ImpTint.AMBER, floor = FloorKind.HALL,
    ),
    LEAFNIBBLE(
        L("Leafnibble", "Листогрыз"), L("Takes one bite from every leaf.", "Откусывает по кусочку от каждого листа."),
        L("Crunch. That one was the last. Promise.", "Хрум. Это был последний. Честно."), ImpTint.MOSS, floor = FloorKind.BALCONY,
    ),
    CUSHIONSQUAT(
        L("Cushionsquat", "Подушкосид"), L("Sits on the best cushion and won't budge.", "Сидит на лучшей подушке и не сдвигается."),
        L("It was warm. I'm only human. Well, imp.", "Там было тепло. Я тоже человек. Ну, пакостник."), ImpTint.ROSE, floor = FloorKind.LIVING,
    ),
    BLOCKSTUB(
        L("Blockstub", "Кубикоступ"), L("Leaves toy blocks exactly where feet go.", "Кладёт кубики ровно туда, куда ступит нога."),
        L("Ow! Who left that there? ...Oh. Me.", "Ай! Кто это тут оставил? ...А. Я."), ImpTint.SEA, floor = FloorKind.KIDS,
    ),
    ODDSOCK(
        L("Oddsock", "Непарник"), L("Keeps the lost socks in a secret nest.", "Хранит потерянные носки в тайном гнезде."),
        L("You'll never find the nest. Never!", "Гнездо ты никогда не найдёшь. Никогда!"), ImpTint.MOSS, floor = FloorKind.LAUNDRY,
    ),
    FURBALL(
        L("Furball", "Клубок"), L("Rolls in pet fur and calls it a coat.", "Валяется в шерсти и зовёт это шубой."),
        L("Achoo. I'll take the coat with me.", "Апчхи. Шубу заберу с собой."), ImpTint.AMBER, floor = FloorKind.PETS,
    ),
    DUSTBUNNY(
        L("Dustbunny", "Пыльный зайка"), L("Grows a little bigger every day nobody sweeps.", "Растёт каждый день, пока никто не метёт."),
        L("Poof. I'll be back as a smaller bunny.", "Пуф. Вернусь зайкой поменьше."), ImpTint.SLATE, floor = FloorKind.WHOLE,
    ),
    HALLOWIMP(
        L("Hallowimp", "Тыквенник"), L("Wears a sheet with two holes and says 'boo' politely.", "Ходит в простыне с дырками и вежливо говорит «бу»."),
        L("Boo. Happy Hallowe'en. Boo again.", "Бу. С Хэллоуином. Ещё раз бу."), ImpTint.GHOST, season = Season.HALLOWEEN,
    ),
    FROSTNIP(
        L("Frostnip", "Морозец"), L("Draws ferns on the window and nibbles toes.", "Рисует узоры на окне и щиплет за пятки."),
        L("Brr. Off to a colder tower.", "Брр. Пойду в башню похолоднее."), ImpTint.FROST, season = Season.WINTER,
    ),
    TINSELTOE(
        L("Tinseltoe", "Мишурка"), L("Wraps everything in tinsel, including the cat.", "Обматывает всё мишурой, даже кота."),
        L("Happy New Year! The cat keeps the tinsel.", "С Новым годом! Мишура остаётся коту."), ImpTint.ROSE, season = Season.NEW_YEAR,
    );

    companion object {
        fun of(key: String?): ImpKind? = entries.firstOrNull { it.name == key }

        /**
         * The imp who moves in on a floor of [floor] on [day] for spell [spellId]: seasonal ones in their season,
         * the room's own ones often, a common one otherwise. Stable for a given spell and day.
         */
        fun pick(floor: FloorKind, seasons: Set<Season>, spellId: Long, day: Long): ImpKind {
            val h = mix(spellId, day)
            val seasonal = entries.filter { it.season != null && it.season in seasons }
            if (seasonal.isNotEmpty() && h % 10 < 4) return seasonal[(h / 10 % seasonal.size).toInt()]
            val own = entries.filter { it.floor == floor }
            if (own.isNotEmpty() && h / 7 % 2 == 0L) return own[(h / 31 % own.size).toInt()]
            val common = entries.filter { it.floor == null && it.season == null }
            return common[(h / 101 % common.size).toInt()]
        }

        private fun mix(a: Long, b: Long): Long {
            var x = a * 0x9E3779B97F4A7C15uL.toLong() + b * 0xC2B2AE3D27D4EB4FuL.toLong()
            x = x xor (x ushr 31)
            x *= 0x94D049BB133111EBuL.toLong()
            return (x xor (x ushr 29)) and Long.MAX_VALUE
        }
    }
}

/** Colour ramps an imp can be repainted in (dark to light, five steps like the sprite's own). */
enum class ImpTint(val ramp: LongArray) {
    PURPLE(longArrayOf(0xFF1E0A2E, 0xFF4A1A64, 0xFF7A309E, 0xFFAC5AD2, 0xFFD696F0)),
    SLATE(longArrayOf(0xFF1A1A2C, 0xFF34364E, 0xFF555A78, 0xFF8088A4, 0xFFB4BCD2)),
    ROSE(longArrayOf(0xFF3A0C26, 0xFF6E1E46, 0xFFA83A68, 0xFFDA6A92, 0xFFF6A8C4)),
    AMBER(longArrayOf(0xFF3C1A0C, 0xFF7A3A14, 0xFFB86422, 0xFFE89A3C, 0xFFFCCC78)),
    SEA(longArrayOf(0xFF0C2638, 0xFF124E64, 0xFF1E8494, 0xFF46B8BC, 0xFF96E0D8)),
    MOSS(longArrayOf(0xFF102A1C, 0xFF1E5034, 0xFF347E44, 0xFF62AE58, 0xFFA6D884)),
    NIGHT(longArrayOf(0xFF0A0A24, 0xFF1A1E52, 0xFF2E3A8A, 0xFF5068C0, 0xFF8CA4E8)),
    GHOST(longArrayOf(0xFF4C4C66, 0xFF8A8AA6, 0xFFC4C4D8, 0xFFE8E8F2, 0xFFFFFFFF)),
    FROST(longArrayOf(0xFF1C3450, 0xFF346A94, 0xFF5EA2CC, 0xFF9CD2EE, 0xFFE0F6FF)),
}
