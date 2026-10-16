package app.hearthspell.content

/**
 * The letters of Master Ambrose, who left the tower to travel and wrote home once a week.
 * Each has a title, a place it was sent from and a body; the greeting and the signature are added by the screen.
 * Russian text addresses the apprentice with {м|ж} marks.
 */
data class LetterText(val title: L, val from: L, val body: L)

object LetterTexts {
    val all: List<LetterText> = listOf(
        LetterText(
            L("The keys are yours", "Ключи теперь у тебя"),
            L("The tower, the kitchen table", "Башня, кухонный стол"),
            L(
                "By the time you read this I'll be three hills away, and the kettle will still be warm. Don't look for me in the cellar, I'm truly gone. The tower is yours now. " +
                    "It's not a grand one: a few floors, a stubborn stair and a hearth that sulks when it's ignored. Keep its rooms and it keeps you. Let them go and the imps move in. " +
                    "They are harmless, mind you, only rude: they chew socks and sing off-key. One honest spell sends them packing. " +
                    "Start small. Wipe the table I'm writing on. I'll write every week.",
                "Когда ты прочтёшь это, я буду уже за тремя холмами, а чайник ещё не успеет остыть. Не ищи меня в погребе, я правда уехал. Теперь башня твоя. " +
                    "Она не великая: пара этажей, упрямая лестница и очаг, который дуется, если о нём забывают. Береги её комнаты, и она сбережёт тебя. Запустишь, и заведутся пакостники. " +
                    "Они безобидные, только невоспитанные: жуют носки и фальшиво поют. Одно честное заклинание, и их как ветром сдует. " +
                    "Начни с малого. Протри стол, за которым я это пишу. Буду писать каждую неделю.",
            ),
        ),
        LetterText(
            L("Salt and fog", "Соль и туман"),
            L("The Salt Marshes", "Солёные топи"),
            L(
                "The marshes are wet, which surprised no one but me. I walk with a lantern and a frog who has decided I am his responsibility. " +
                    "A word on helpers, since one may wake soon. They don't wake from wishing. A broom wakes because it was used, again and again, by a patient hand. " +
                    "Five honest sweeps and it opens one eye. Twenty and it hums. Don't be alarmed when it follows you around. It only wants to be useful, like the frog. " +
                    "I've left you a few requests in this letter. Do them if you can. If you can't, the tower will wait. Towers are good at waiting.",
                "В топях сыро, и удивило это только меня. Хожу с фонарём и лягушкой, которая решила, что я теперь на её попечении. " +
                    "Пару слов о помощниках, раз один из них скоро проснётся. Они просыпаются не от желания. Метла просыпается, потому что ею мели, снова и снова, терпеливой рукой. " +
                    "Пять честных взмахов, и она приоткрывает глаз. Двадцать, и она напевает. Не пугайся, если она начнёт ходить за тобой хвостиком. Ей просто хочется быть полезной, как этой лягушке. " +
                    "В письме несколько моих просьб. Сделай, если сможешь. Не сможешь, башня подождёт. Башни умеют ждать.",
            ),
        ),
        LetterText(
            L("The silent brooms", "Молчаливые мётлы"),
            L("A monastery in the mountains", "Монастырь в горах"),
            L(
                "I'm staying with monks who sweep. That's all they do: sweep the yard, sweep the stairs, sweep the snow off the path so the next snow has somewhere to land. " +
                    "I asked the abbot why. He said the yard was never the point. The point is the hour in which your hands are busy and your head goes quiet. " +
                    "Then he gave me a broom, and I swept for an hour, and he was right, which was annoying. " +
                    "So when a chore feels endless, don't fight it. Sweep the hour. The floor is just a bonus.",
                "Гощу у монахов, которые метут. Больше они ничего не делают: метут двор, метут лестницы, сметают снег с тропы, чтобы следующему снегу было куда лечь. " +
                    "Я спросил настоятеля зачем. Он сказал, что дело никогда не было во дворе. Дело в том часе, когда руки заняты, а в голове становится тихо. " +
                    "Потом он дал мне метлу, и я мёл целый час, и он оказался прав, что было досадно. " +
                    "Так что, если дело кажется бесконечным, не воюй с ним. Вымети этот час. Чистый пол просто приятное дополнение.",
            ),
        ),
        LetterText(
            L("An old friend", "Старый друг"),
            L("Thistlewick, a village of bakers", "Чертополохово, деревня пекарей"),
            L(
                "Guess who I found running a bakery: Pudding. You won't remember her. She was a dragon when I knew her, and she still is, but a retired one. " +
                    "She bakes bread in her own breath and charges two copper a loaf. The village adores her. The village used to be terrified of her. " +
                    "I asked what changed. She said: \"I stopped guarding a pile of gold and started tending an oven. A pile only gets bigger. An oven gets warmer.\" " +
                    "I have been thinking about that all week. Your tower is an oven, {my boy|my girl}, not a pile.",
                "Угадай, кого я встретил за прилавком пекарни: Пышку. Ты её не помнишь. Когда я её знал, она была драконом, ею и осталась, только на покое. " +
                    "Она печёт хлеб собственным дыханием и берёт две медяшки за каравай. Деревня её обожает. А раньше деревня её до смерти боялась. " +
                    "Я спросил, что изменилось. Она сказала: «Я перестала стеречь кучу золота и стала топить печь. Куча только растёт. А печь становится теплее». " +
                    "Я думал об этом всю неделю. Твоя башня печь, {мой мальчик|моя девочка}, а не куча.",
            ),
        ),
        LetterText(
            L("On imps", "О пакостниках"),
            L("The road south, under a cart", "Дорога на юг, под телегой"),
            L(
                "Rain, so I'm writing under a cart. A word about imps, since I suspect you've met a few. They are not punishments. They are not signs that you've failed. " +
                    "They're just what happens to any room nobody visits for a while, the way dust happens, or socks happen. " +
                    "I have had imps in every tower I ever kept. Once I had eleven in the bath and named them all. " +
                    "So don't scold yourself. Pick the smallest spell on the floor, cast it, and watch them scatter. There is nothing in the world more satisfying than a scattering imp.",
                "Дождь, так что пишу из-под телеги. Пару слов о пакостниках, ты наверняка уже с ними познаком{ился|илась}. Это не наказание. Это не знак, что ты не справ{ился|илась}. " +
                    "Это просто то, что случается с любой комнатой, куда давно никто не заглядывал, как случается пыль или носки. " +
                    "Пакостники водились во всех башнях, которые я когда-либо хранил. Однажды у меня в купальне жило одиннадцать, и у каждого было имя. " +
                    "Так что не ругай себя. Выбери самое маленькое заклинание на этаже, произнеси его и смотри, как они разбегаются. Нет на свете зрелища приятнее, чем разбегающийся пакостник.",
            ),
        ),
        LetterText(
            L("The market of bells", "Рынок колоколов"),
            L("The City of Bells", "Город колоколов"),
            L(
                "Every stall in this city has a bell and every merchant rings it at you. I bought three onions just to make one stop. " +
                    "A tip from an old shopper: never go to market without a list, and never go hungry. Without a list you come home with a hat and no bread. Hungry, you come home with eleven pies. " +
                    "Write the reagents down as soon as the jar is empty, not when you're standing in front of the shelf trying to remember. " +
                    "Your satchel knows the way. Let it carry the list and you carry the onions.",
                "В этом городе у каждого прилавка колокол, и каждый торговец звонит в него прямо тебе в ухо. Я купил три луковицы, лишь бы один замолчал. " +
                    "Совет старого покупателя: никогда не ходи на рынок без списка и никогда не ходи голодным. Без списка вернёшься со шляпой и без хлеба. Голодным, с одиннадцатью пирогами. " +
                    "Записывай реагенты, как только банка опустела, а не когда стоишь у полки и пытаешься вспомнить. " +
                    "Котомка знает дорогу. Пусть она несёт список, а ты неси лук.",
            ),
        ),
        LetterText(
            L("Why I left", "Почему я уехал"),
            L("A lighthouse on the coast", "Маяк на побережье"),
            L(
                "You've been too polite to ask, so I'll tell you. I didn't leave because I was tired of the tower. I left because I'd started keeping it for you. " +
                    "I swept before you woke, I fixed the stair before you tripped on it. And a tower kept for you is never really yours. " +
                    "The lighthouse keeper here said it well: a light you don't tend yourself is just someone else's light. " +
                    "So I went away, to let the tower get to know you. From the sound of your helpers, it has.",
                "Ты слишком вежлив{|а}, чтобы спросить, так что скажу сам. Я уехал не потому, что устал от башни. Я уехал, потому что начал хранить её за тебя. " +
                    "Подметал до того, как ты просыпал{ся|ась}, чинил ступеньку раньше, чем ты о неё спотыкал{ся|ась}. А башня, которую хранят за тебя, никогда не становится по-настоящему твоей. " +
                    "Смотритель здешнего маяка сказал точно: огонь, который ты не поддерживаешь сам, просто чужой огонь. " +
                    "Вот я и уехал, чтобы башня познакомилась с тобой. Судя по твоим помощникам, познакомилась.",
            ),
        ),
        LetterText(
            L("Sea legs", "Морская болезнь"),
            L("A ship to the Lantern Isles", "Корабль к Фонарным островам"),
            L(
                "I'm at sea and the sea is winning. The captain says sailors keep a ship the way you keep a tower: a little every day, or a lot all at once in a storm, and the storm is worse. " +
                    "Every morning they coil the ropes, even the ropes nobody used. I asked why. \"So we know where they are when we need them,\" she said. " +
                    "Tidying is like that. It's not for now, it's for the moment you're in a hurry and the keys are exactly where they should be. " +
                    "Coil your ropes, apprentice. Put one thing back today.",
                "Я в море, и море побеждает. Капитан говорит, что моряки берегут корабль так же, как ты башню: понемногу каждый день или всё разом в шторм, и шторм хуже. " +
                    "Каждое утро они сворачивают канаты, даже те, которыми никто не пользовался. Я спросил зачем. «Чтобы знать, где они, когда понадобятся», ответила она. " +
                    "С порядком так же. Он не для сейчас, он для той минуты, когда ты торопишься, а ключи лежат ровно там, где должны. " +
                    "Сворачивай свои канаты. Положи сегодня на место одну вещь.",
            ),
        ),
        LetterText(
            L("The island of tea", "Чайный остров"),
            L("The Lantern Isles", "Фонарные острова"),
            L(
                "The islanders have a custom: whoever finishes a hard task brews tea for the house, and everyone stops to drink it, even the goats. " +
                    "I think we skip this part at home. We finish the dishes and go straight to the next thing, as if a chore done were only the start of another. " +
                    "So here is a request that isn't in the list: after your next big spell, stop. Sit. Look at the clean floor like it's a painting. " +
                    "You made that. The goats would be proud.",
                "У островитян есть обычай: кто закончил трудное дело, заваривает чай на весь дом, и все бросают дела, чтобы его выпить, даже козы. " +
                    "Кажется, дома мы эту часть пропускаем. Домыли посуду и сразу к следующему, будто сделанное дело просто начало нового. " +
                    "Так что вот просьба, которой нет в списке: после следующего большого заклинания остановись. Сядь. Посмотри на чистый пол, как на картину. " +
                    "Это сделал{|а} ты. Козы бы гордились.",
            ),
        ),
        LetterText(
            L("The desert library", "Библиотека в пустыне"),
            L("A library buried in sand", "Библиотека, занесённая песком"),
            L(
                "I found a library that the desert is slowly eating. The librarian sweeps sand off the shelves every morning and every night the wind brings it back. " +
                    "She's been doing it for forty years. I asked if she ever wins. She laughed and said she isn't trying to win, she's trying to read. " +
                    "Your tower will never be finished, either. Dishes come back. Dust comes back. That's not failure, that's a home being lived in. " +
                    "Sweep the sand and read the books, apprentice. Both, every day, a little.",
                "Я нашёл библиотеку, которую понемногу съедает пустыня. Библиотекарша каждое утро сметает песок с полок, а каждую ночь ветер приносит его обратно. " +
                    "Она делает это сорок лет. Я спросил, побеждает ли она когда-нибудь. Она рассмеялась и сказала, что не пытается победить, она пытается читать. " +
                    "Твоя башня тоже никогда не будет закончена. Посуда возвращается. Пыль возвращается. Это не поражение, это дом, в котором живут. " +
                    "Сметай песок и читай книги. И то и другое, каждый день, понемногу.",
            ),
        ),
        LetterText(
            L("Northern lights", "Северное сияние"),
            L("The far north, by a fire", "Далёкий север, у костра"),
            L(
                "It's cold enough here that my ink freezes, so this letter is written in candle wax and determination. " +
                    "Last night the sky turned green and pink and nobody in the village even looked up. They see it every winter. " +
                    "I thought of you, and how a clean kitchen stops looking like magic once it's ordinary. It still is magic. It's just magic you've got good at. " +
                    "Look up now and then. Notice the shine on the plates. You did a quiet, difficult thing: you made ordinary feel easy.",
                "Здесь так холодно, что чернила замерзают, поэтому это письмо написано свечным воском и упрямством. " +
                    "Прошлой ночью небо стало зелёным и розовым, и никто в деревне даже не поднял головы. Они видят это каждую зиму. " +
                    "Я подумал о тебе и о том, что чистая кухня перестаёт казаться волшебством, когда становится обычной. Но это всё равно волшебство. Просто ты в нём наловчил{ся|ась}. " +
                    "Иногда поднимай голову. Замечай, как блестят тарелки. Ты сделал{|а} тихую и трудную вещь: обычное стало лёгким.",
            ),
        ),
        LetterText(
            L("Keep the kettle warm", "Держи чайник горячим"),
            L("The road home", "Дорога домой"),
            L(
                "I've turned around. Don't panic and don't scrub everything at once, I'm not coming to inspect. I'm coming because I miss the stair, the hearth and, frankly, you. " +
                    "Pudding sends a loaf. The frog stayed in the marsh, which is for the best. " +
                    "I hear from the owls that the tower is warm and full of chattering helpers. I always knew it would be. I just needed you to know it too. " +
                    "When I knock, it will be your door I'm knocking on. Keep the kettle warm. I take mine with honey.",
                "Я повернул обратно. Не паникуй и не отмывай всё разом, я еду не с проверкой. Я еду, потому что скучаю по лестнице, по очагу и, честно говоря, по тебе. " +
                    "Пышка передаёт каравай. Лягушка осталась в топях, и это к лучшему. " +
                    "Совы говорят, что в башне тепло и полно болтливых помощников. Я всегда знал, что так и будет. Мне просто нужно было, чтобы это узнал{|а} и ты. " +
                    "Когда я постучу, это будет твоя дверь. Держи чайник горячим. Я пью с мёдом.",
            ),
        ),
    )
}
