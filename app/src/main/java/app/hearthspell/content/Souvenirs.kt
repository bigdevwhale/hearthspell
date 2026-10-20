package app.hearthspell.content

/** What the master tucked into each letter, with the postscript that explains it. One per letter, in order. */
class Souvenir(val decor: DecorKind, val note: L)

object Souvenirs {
    val all: List<Souvenir> = listOf(
        Souvenir(DecorKind.TEACUP, L(
            "P.S. I left my favourite cup. It has a chip on the rim; drink from the other side.",
            "P.S. Оставляю тебе свою любимую чашку. На ободке скол, пей с другой стороны.",
        )),
        Souvenir(DecorKind.FROG, L(
            "P.S. The frog made you this out of clay. He insisted I post it. He also insists he is a prince. He is not.",
            "P.S. Лягушка слепила тебе это из глины. Велела отправить. Ещё она уверяет, что она принц. Это не так.",
        )),
        Souvenir(DecorKind.SNOWGLOBE, L(
            "P.S. The monks sell these to pay for brooms. Shake it when the head gets noisy.",
            "P.S. Монахи продают такие, чтобы покупать мётлы. Встряхни, когда в голове шумно.",
        )),
        Souvenir(DecorKind.DRAGON_SCALE, L(
            "P.S. Pudding sends a scale. She says it keeps bread warm. It keeps everything warm. Don't put it on a cushion.",
            "P.S. Пудинг шлёт чешуйку. Говорит, она хлеб греет. Она всё греет. На подушку не клади.",
        )),
        Souvenir(DecorKind.IMP_JAR, L(
            "P.S. This one followed me from under the cart. He's very small and very sorry. Let him wave at the others.",
            "P.S. Этот увязался за мной из-под телеги. Он очень маленький, и ему очень стыдно. Пусть машет остальным.",
        )),
        Souvenir(DecorKind.BELL, L(
            "P.S. A bell from the market. Ring it once when a chore is done. The tower likes ceremony.",
            "P.S. Колокольчик с рынка. Звони разок, когда закончишь дело. Башня любит торжественность.",
        )),
        Souvenir(DecorKind.LIGHTHOUSE, L(
            "P.S. The keeper carved this. It really blinks, if you don't look straight at it.",
            "P.S. Смотритель вырезал этот маячок. Он правда мигает, если не смотреть на него в упор.",
        )),
        Souvenir(DecorKind.SHIP, L(
            "P.S. I built the ship in the bottle to stop feeling seasick. It didn't work. The ship is lovely though.",
            "P.S. Собрал кораблик в бутылке, чтобы меня меньше укачивало. Не помогло. Зато кораблик чудесный.",
        )),
        Souvenir(DecorKind.PAPER_LANTERN, L(
            "P.S. On the isles every house hangs one of these for the people far away. Hang this one for me.",
            "P.S. На островах в каждом доме висит такой фонарик для тех, кто далеко. Повесь этот для меня.",
        )),
        Souvenir(DecorKind.OLD_MAP, L(
            "P.S. Found in the library under a dune. The X is our tower. Somebody knew about it long before us.",
            "P.S. Нашёл в библиотеке под барханом. Крестик — это наша башня. Кто-то знал о ней задолго до нас.",
        )),
        Souvenir(DecorKind.AURORA, L(
            "P.S. I caught a little of the sky in a jar. It's quite safe. Open it only on a very grey day.",
            "P.S. Поймал в банку немножко неба. Это совсем безопасно. Открывай только в очень серый день.",
        )),
        Souvenir(DecorKind.COMPASS, L(
            "P.S. My compass. It doesn't point north. It points home, and I won't need it after this road.",
            "P.S. Мой компас. Он показывает не на север, а домой, и после этой дороги он мне больше не нужен.",
        )),
    )

    fun forLetter(n: Int): Souvenir? = all.getOrNull(n)
}
