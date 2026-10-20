package app.hearthspell.content

/** Little things the helpers find and leave for the apprentice after a quiet night. [rare] ones turn up seldom. */
enum class Trinket(val title: L, val blurb: L, val rare: Boolean = false) {
    BUTTON(L("A red button", "Красная пуговица"), L("Off a coat nobody remembers.", "С пальто, которого никто не помнит.")),
    FEATHER(L("A blue feather", "Синее пёрышко"), L("Light enough to write a wish with.", "Лёгкое, хоть желание пиши.")),
    ACORN(L("An acorn", "Жёлудь"), L("Still wearing its little hat.", "Ещё в своей шапочке.")),
    PEBBLE(L("A smooth pebble", "Гладкий камушек"), L("Warm, as if it had been held a long time.", "Тёплый, будто его долго держали в руке.")),
    THIMBLE(L("A thimble", "Напёрсток"), L("Fits a helper's finger. Helpers have no fingers.", "Впору помощнику на палец. У помощников нет пальцев.")),
    BEAD(L("A glass bead", "Стеклянная бусина"), L("From a necklace that went on an adventure.", "С бус, которые ушли в приключение.")),
    MARBLE(L("A marble", "Стеклянный шарик"), L("Rolls towards whoever needs it.", "Катится к тому, кому он нужнее.")),
    SHELL(L("A pink shell", "Розовая ракушка"), L("Hold it to your ear: the baths are in there.", "Приложи к уху: там шумит купальня.")),
    RIBBON(L("A ribbon", "Ленточка"), L("Tied in a bow by somebody very proud.", "Завязана бантиком кем-то очень гордым.")),
    CORK(L("A cork", "Пробка"), L("Smells faintly of a party.", "Слегка пахнет праздником.")),
    SPOOL(L("A spool of thread", "Катушка ниток"), L("Green, for mending green things.", "Зелёная, чтобы штопать зелёное.")),
    SOCK(L("The lost sock", "Пропавший носок"), L("Rescued from an imp's nest.", "Спасён из гнезда пакостника.")),
    KEY(L("A tiny key", "Крошечный ключик"), L("Opens a door nobody has found yet.", "Открывает дверь, которую ещё никто не нашёл.")),
    LEAF(L("A golden leaf", "Золотой листок"), L("Fell from a tree that only grows in letters.", "Упал с дерева, что растёт только в письмах."), rare = true),
    CLOVER(L("A four-leaf clover", "Клевер-четырёхлистник"), L("Luck, pressed flat.", "Удача, засушенная между страниц."), rare = true),
    STAR(L("A star shard", "Осколок звезды"), L("Fell past the observatory window.", "Упал мимо окна обсерватории."), rare = true),
    GOLD_BUTTON(L("A golden button", "Золотая пуговица"), L("From the master's best coat. He'll want it back.", "С лучшего камзола мастера. Он захочет её назад."), rare = true);

    companion object {
        fun of(key: String?): Trinket? = entries.firstOrNull { it.name == key }

        /** What each helper tends to find. */
        fun favourite(h: HelperKind): Trinket = when (h) {
            HelperKind.BROOM, HelperKind.DUSTER -> FEATHER
            HelperKind.SPONGE, HelperKind.BUCKET -> MARBLE
            HelperKind.CLOTH, HelperKind.IRON -> BUTTON
            HelperKind.SACK -> SOCK
            HelperKind.LADLE, HelperKind.TEAPOT -> CORK
            HelperKind.PILLOW -> FEATHER
            HelperKind.BASKET -> SPOOL
            HelperKind.CAN -> ACORN
            HelperKind.CANDLE -> BEAD
            HelperKind.BOOK -> RIBBON
            HelperKind.KEY -> KEY
            HelperKind.BOWL -> SHELL
            HelperKind.SATCHEL -> PEBBLE
        }
    }
}

/** What a helper says when patted, two lines each, and what it mumbles when woken up at night. */
object HelperLines {
    private val lines: Map<HelperKind, List<L>> = mapOf(
        HelperKind.BROOM to listOf(L("Swish! Again, again!", "Вжух! Ещё, ещё!"), L("I found a coin under the sofa. I'm keeping it.", "Нашла монетку под диваном. Оставлю себе.")),
        HelperKind.SPONGE to listOf(L("Squish! Hee hee.", "Хлюп! Хи-хи."), L("I'm a little damp. It's fine.", "Я немножко мокрая. Это нормально.")),
        HelperKind.CLOTH to listOf(L("Everything shines when you're around.", "Когда ты рядом, всё блестит."), L("Polish? Polish!", "Натереть? Натереть!")),
        HelperKind.BUCKET to listOf(L("Slosh. That's bucket for 'hello'.", "Плюх. Это по-ведерски «привет»."), L("I'm half full. I choose to be.", "Я наполовину полное. Я так решило.")),
        HelperKind.DUSTER to listOf(L("Hehe, that tickles!", "Хи-хи, щекотно!"), L("The books sneezed at me again.", "Книги опять на меня чихнули.")),
        HelperKind.SACK to listOf(L("Hic! Sorry. Hic!", "Ик! Прости. Ик!"), L("I ate a whole receipt today.", "Сегодня я съел целый чек.")),
        HelperKind.LADLE to listOf(L("Needs more salt. Everything needs more salt.", "Надо посолить. Всё надо посолить."), L("I tasted the soup twice. For science.", "Попробовал суп дважды. Для науки.")),
        HelperKind.TEAPOT to listOf(L("Fweeee! Tea's on!", "Фьюююю! Чай готов!"), L("Shall I put the kettle on? I am the kettle.", "Поставить чайник? Я и есть чайник.")),
        HelperKind.PILLOW to listOf(L("Mmm... comfy.", "Ммм... мягонько."), L("Fluff me later. Or now. Now's good.", "Взбей меня потом. Или сейчас. Сейчас хорошо.")),
        HelperKind.BASKET to listOf(L("Up the stairs, down the stairs!", "Вверх по лестнице, вниз по лестнице!"), L("I found a sock. It's not ours.", "Нашла носок. Он не наш.")),
        HelperKind.IRON to listOf(L("Pssshh. Very important business.", "Пшшш. Очень важное дело."), L("No wrinkles shall pass.", "Ни одна складка не пройдёт.")),
        HelperKind.CAN to listOf(L("The fern says thank you.", "Папоротник говорит спасибо."), L("Drip, drop, a little more?", "Кап-кап, ещё немножко?")),
        HelperKind.CANDLE to listOf(L("I found your keys. They were in your hand.", "Нашла твои ключи. Они были у тебя в руке."), L("Flicker, flicker.", "Мерцаю, мерцаю.")),
        HelperKind.BOOK to listOf(L("All bills paid. Probably.", "Все счета оплачены. Наверное."), L("I remember everything. Ask me anything.", "Я помню всё. Спрашивай.")),
        HelperKind.KEY to listOf(L("Click! The door is safe.", "Щёлк! Дверь под охраной."), L("Shoes in a row. As it should be.", "Обувь в ряд. Как и положено.")),
        HelperKind.BOWL to listOf(L("The cat says hello. Well, 'mrrp'.", "Кот передаёт привет. Ну, «мрр»."), L("Fresh water! The best water!", "Свежая вода! Лучшая вода!")),
        HelperKind.SATCHEL to listOf(L("Market day? Market day!", "Идём на рынок? Идём на рынок!"), L("I know a shortcut. It's longer.", "Я знаю короткую дорогу. Она длиннее.")),
    )

    val sleepy = L("Mmm... five more minutes...", "Ммм... ещё пять минуточек...")

    fun of(kind: HelperKind, n: Int): L = lines.getValue(kind).let { it[Math.floorMod(n, it.size)] }
}
