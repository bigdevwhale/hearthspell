# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

All notable changes to Hearthspell. Versions follow [semantic versioning](https://semver.org/).

## [1.5.0] - 2026-11-13

### Added
- A spell can live on several floors (#9). A new spell's floor chips now take several floors at once, one spell each; and a spell's page has "Copy to other floors": tick the floors, and each gets the same name, rhythm, effort and helper, with its own casts and its own day. Washing the windows in three rooms on three different days no longer means typing it three times or casting it in the Heart of the tower.

<!-- tg
Новое: заклинание можно скопировать на другие этажи – кнопка на его странице. А при создании нового можно сразу отметить несколько этажей: у каждой копии свой день и свои взмахи.
-->

## [1.4.0] - 2026-11-12

### Look
- The whole tower is painted anew: every room, the roof, the base and the meadow, the helpers, the eighteen imps (no longer round balls, each one its own creature), the decor, the apprentice and the master, the letters and the menu icons. Movement is smooth instead of stepped.
- Storybook type: Kurale for titles, Alegreya SC for small capitals, Philosopher for text, in English and Russian.
- Soft rounded cards, buttons and bars instead of the pixel frames.

### Added
- **Chronicle**, a new tab: this week against the last, a chart of the last fourteen days, how tidy each floor is, your favourite spells and the totals since the tower became yours. Letters moved to an envelope in the tower's header, with a dot when one is unread, and to the grimoire's collections.
- **Postcards** to share: your week, the tower, the apprentice, every room, the helpers, the bestiary and every imp shown the door, the treasures and your best spells. Each card carries a QR code to the app on Google Play; share it or save it to the gallery. Share buttons on floors, helpers, spells, the bestiary and the treasures.
- **Holiday**: Settings → Holiday pauses every spell at once. When you're back, each picks up where it was. ([#8](https://github.com/bigdevwhale/hearthspell/issues/8))
- The grimoire opens under a banner, with three pages: spells (filtered by floor), helpers and collections. The market has a banner too, and each aisle its own picture.

### Fixed
- The master's three requests are picked once a week and stay the same until Sunday, so casting a spell no longer swaps the list under your finger. ([#6](https://github.com/bigdevwhale/hearthspell/issues/6))
- A request cast earlier in the week counts as done, even if it was cast before the list appeared. ([#7](https://github.com/bigdevwhale/hearthspell/issues/7))
- Pending requests are never folded into "Resting" on their floor.
- Hats sit on the apprentice's head, and the master's teacup is cup-sized on the floor.

<!-- tg
Большое обновление: башня перерисована целиком, движения стали плавными, шрифты сказочными.
Новая вкладка «Летопись»: твоя неделя, график заклинаний, чистота этажей и любимые дела.
Открытки, чтобы делиться башней, комнатами, помощниками и пакостниками, с QR-кодом на приложение.
Режим отпуска, а просьбы мастера теперь держатся всю неделю и засчитываются сразу. Спасибо за сообщения и идеи!
-->

## [1.3.1] - 2026-11-07

### Changed
- In the grimoire, each floor's spells are in order of when they're due: overdue and today's on top, the freshly cast lower, the paused last (#5).

<!-- tg
В гримуаре заклинания каждого этажа теперь идут по сроку: просроченные и сегодняшние сверху, на паузе внизу.
-->

## [1.3.0] - 2026-10-31

### Added
- Pick the day a spell is due next: today, tomorrow, a day of the week or any day within its interval, when adding a spell or later in the editor. ([#2](https://github.com/bigdevwhale/hearthspell/issues/2))
- A spell's page lists its last five casts, and any of them can be undone while its mana is unspent. ([#4](https://github.com/bigdevwhale/hearthspell/issues/4))
- Spells with more than a week to go rest folded away at the bottom of their floor. ([#3](https://github.com/bigdevwhale/hearthspell/issues/3))

### Changed
- Fresh spells look calmer on a floor, and ones done today say so, so it's clear at a glance what still needs doing. ([#3](https://github.com/bigdevwhale/hearthspell/issues/3))

<!-- tg
Новое: при создании и редактировании задания можно выбрать, когда оно понадобится в следующий раз, например в воскресенье.
На странице задания видны последние выполнения, и любое можно отменить, если мана за него ещё не потрачена.
Задания, до которых больше недели, прячутся в свёрнутый список «Отдыхают», а сделанные выглядят спокойнее.
Спасибо за идеи!
-->

## [1.2.3] - 2026-10-29

### Fixed
- A daily spell cast today now says it's due tomorrow instead of still "Due today", and longer spells no longer show one day too few. Days left are counted by the calendar. ([#1](https://github.com/bigdevwhale/hearthspell/issues/1))

<!-- tg
Исправлено: ежедневное задание после отметки теперь показывает «Через 1 день», а не «Пора сегодня».
Остальные задания тоже больше не показывают на день меньше, чем осталось.
Спасибо за сообщение об ошибке!
-->

## [1.2.2] - 2026-10-25

### Added
- Community links in settings: report a bug, suggest an idea or follow updates through the StillCraft bot on Telegram, and the StillCraft chat.

<!-- tg skip -->

## [1.2.1] - 2026-10-21

### Look
- A new typeface everywhere, in English and Russian: m6x11 by Daniel Linssen with Cyrillic by Natto Games, with dashes, an ellipsis, curly quotes and a tick drawn in to match. Credited in the settings.
- Screen titles shrink to fit on one line instead of breaking mid-word.
- The tower cat is drawn in front of the helpers, so it no longer hides behind them.
- New screenshots for every screen, including the ones added in 1.2.

## [1.2.0] - 2026-10-21

### A living tower
- At night a tidy floor glows warm, a dusty one sinks into the dark and an imp's floor flickers violet. Late at night the helpers doze with little z's.
- Every floor tidy: a comet crosses the night sky, a rainbow the day sky.
- A home-screen widget: the tower from outside, a window per floor, warm where it's tidy and violet where imps moved in.

### Friends
- Tap a helper on a floor: it hops, says a line of its own and hearts float up.
- A cat moves in after the first few spells, gets a name and sleeps on the floor you've just tidied (or on its cushion). Tap it and it purrs.
- The apprentice walks about the floors too.
- Eighteen named imps, each with a habit and a parting word on being shown the door, and a bestiary of the ones you've met. The room and the season decide who moves in, never how long a chore waited.

### Things to find
- Gifts: after a quiet night on a tidy floor, its awake helpers may leave a trinket on the windowsill (seventeen, a few of them rare). They wait three days.
- Souvenirs: every letter from the master brings something in the envelope, to keep in the tower.
- The star chart: each rank a spell climbs lights a star; twelve constellations, shown over the roof at night.

### Things to do with mana and market trips
- The cauldron: reagents bought at the market leave essences; brew potions that make the tower prettier for a week (pink smoke, rainbow fireflies, sparkles, bubbles, blossom, moonglow).
- The wardrobe: seven robes and seven hats.
- Seasonal decor (a pumpkin lantern, a little fir) and a cat cushion.

### Seasons
- Autumn leaves, Hallowe'en pumpkins and bats, winter snow, New Year garlands and fireworks, spring petals, summer butterflies.

### Gentle rhythm
- Sunday tea: on Sunday and Monday the helpers tell you about the week in a few warm lines.
- Coming home after five days or more: a welcome from the master, and the first spell is a Great Spring Cleaning with confetti and double mana. The reminder after a long absence comes from the master himself.
- Sound and touch: chiptune chimes synthesised on the phone; spells in a row climb a scale and the buzz grows.

### Under the hood
- Database version 2 (finds, brews, wardrobe), migrated automatically; backups include the new data and still read version 1 files.

## [1.1.0] - 2026-10-17

### Hints
- A tour of the tower from the master right after onboarding: floors and spells, fading, imps, casting and undo, his weekly requests, "Low on energy" and letters.
- Every screen explains itself the first time it opens, in the voice of the master or a helper: the Broom on a floor, the Candle in the grimoire, the Satchel at the market, the Cat in the decor shop and so on.
- A "?" in each screen's header brings its hint back, "Skip all" closes them for good, and the settings can show them all again.

## [1.0.0] - 2026-10-16

The first release.

### The tower
- Your home as a wizard's tower: one floor per room, drawn as a detailed pixel scene. Eleven kinds of room, from the Alchemy Lab (kitchen) and the Observatory (bedroom) to the Spring Baths, the Gate Hall, the Greenhouse and the Tower Heart for whole-home chores.
- A night sky with a moon, twinkling stars and chimney smoke over the roof; flickering candles, steam, fireflies and dust motes inside.
- Chores are spells with their own interval. Their freshness fades slowly, the floor gathers dust and cobwebs, and an overdue spell brings a harmless imp. One cast sweeps it out, with an undo.
- 45 ready-made spells with sensible intervals and incantations, or your own with any name, interval and effort. Pause a spell for a holiday.
- "Low on energy" shows only the small spells.

### Growing
- Ranks from Novice to Archmage for every spell, and circles of apprenticeship for you.
- 17 animated helpers that wake up as their spells are mastered and pace their favourite floor, with four levels each.
- Mana for every cast (5, 10 or 20), doubled for the master's three requests of the week.
- A decor shop of 20 pieces, up to four on each floor, plus a chest.

### Around the house
- The grimoire: helpers, ranks and a page per spell with a twelve-week calendar.
- The market: reagents sorted into shop sections, the section guessed from the name in English and Russian, "running low" suggestions from your buying rhythm, and a one-hand market mode that casts your market spell when you're done.
- Twelve letters from Master Ambrose, one a week, with this week's requests attached.
- A quick onboarding: the master's note, your name, your rooms and their chores, the reminder time.
- One daily reminder in a helper's voice, only when imps have moved in or a letter has arrived.
- Night and parchment themes, English and Russian, JSON backup. No internet permission.
