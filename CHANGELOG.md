# Changelog

All notable changes to Hearthspell. Versions follow [semantic versioning](https://semver.org/).

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
