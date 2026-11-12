"""Generates Hearthspell's art with the Codex CLI image tool, into art/sheets/<name>.png.

Codex saves images into ~/.codex/generated_images/<session>/; after each run the image of that session is copied
to art/sheets. Sheets already there are skipped (delete one to draw it again).

    python tools/art/gen.py                  # everything missing, four at a time
    python tools/art/gen.py room_kitchen imps_1   # only these

Then `python tools/art/cut.py` turns the sheets into app drawables.
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n"
STYLE = (
    "Style: detailed high-resolution pixel art like a modern cozy indie game (fine pixels, around 4x finer than "
    "classic 16-bit), clean dark outline, soft 4-5 tone shading with gentle dithering, warm candle-lit palette, "
    "whimsical and cute, storybook wizard-tower mood."
)
MAGENTA = "flat solid pure magenta (#FF00FF) background"


def grid(cols, rows, what):
    return (
        f"Layout: {cols} columns x {rows} rows of equal square cells on a {MAGENTA}. One {what} per cell, centered, "
        "standing on the bottom third of the cell where it has feet or a base, with clear margin; nothing touches the "
        "cell borders. No text, no labels, no grid lines, no drop shadows on the background, no magenta inside the art."
    )


# ---------------------------------------------------------------- rooms

ROOM_FRAME = (
    "Image: one floor of a cozy wizard's tower, seen from the front as a 2D side-view cutaway room, like a room in a "
    "side-scrolling game. Landscape, exactly 5:3.\n"
    "Frame (must match exactly, it joins other floors): a narrow column of grey stone blocks on the far left and far "
    "right edges (each about 6% of the width), a dark wooden ceiling beam along the top (about 4% of the height), and "
    "a wooden plank floor whose top surface, where characters stand, is at 86% of the height from the top; the floor "
    "band fills the rest down to the bottom edge.\n"
    "Window glass: every window pane is filled with flat solid pure magenta (#FF00FF) (the live sky is painted there "
    "later); the frame and mullions are normal. Nowhere else uses magenta.\n"
    "IMPORTANT: the lower middle of the room (from 15% to 85% of the width, the band just above the floor) must stay "
    "EMPTY floor so characters can walk there: furniture stands against the back wall or at the sides, low. Leave two "
    "or three bare patches of wall for pictures to be hung later. No characters, no animals, no people, no text, no "
    "lit candles or flames except where named."
)

ROOMS = {
    "kitchen": "The Alchemy Lab (a kitchen): a stone hearth on the left with a big black cauldron of bubbling green "
               "potion over a low fire, copper pots hanging from a rack, a shelf of coloured flasks and jars, a small "
               "wooden worktable with a mortar, an arched window on the right, brick back wall.",
    "living": "The Library (a living room): tall wooden bookshelves full of colourful books on both sides, a deep red "
              "armchair, a small round side table with a lamp, a tall arched window in the middle of the back wall, "
              "green wallpaper with a subtle pattern, a patterned rug edge.",
    "bedroom": "The Observatory (a bedroom): a big round porthole window in the upper middle of the back wall "
               "(its glass magenta), a cosy bed with a starry indigo quilt on the right, a brass telescope on a "
               "tripod near the window, a small night table with a candle (unlit), deep blue walls with tiny painted "
               "stars, a crescent-moon mobile.",
    "bath": "The Spring Baths (a bathroom): a round wooden tub of steaming turquoise water on the right, blue and white "
            "tiles on the walls, a small round window high on the back wall, a washstand with a mirror and a jug, "
            "towels on a rail, a few potted ferns, soap bubbles.",
    "hall": "The Gate Hall (a hallway): a big arched wooden front door with iron studs on the left, a coat rack with "
            "a cloak and a wizard hat, a row of boots on a mat, a narrow window next to the door, an umbrella stand "
            "with a staff, stone and wood panelled walls, a hanging iron lamp (unlit).",
    "balcony": "The Greenhouse (a balcony with plants): big glass windows across the back wall (glass magenta) in a "
               "white iron frame, many potted plants, ferns and hanging vines, a watering can, a wooden plant stand, "
               "terracotta pots, a small bench, green and wooden tones.",
    "kids": "The Toy Workshop (a kids' room): a small wooden bed with a patchwork quilt, shelves of toys (wooden "
            "knights, a rocking horse, a toy castle, a drum), colourful bunting, a small round window, warm yellow "
            "walls with painted clouds, a toy chest.",
    "study": "The Scriptorium (a study): a heavy writing desk with scrolls, a quill and an inkwell, shelves of "
             "ledgers, a map pinned on the wall, a candelabra (unlit), a tall arched window, dark wood panelling, "
             "piles of parchment.",
    "laundry": "The Weaving Room (a laundry): a wooden washtub and washboard, a clothes line across the room near the "
               "ceiling with linen hanging on it, a spinning wheel, a basket of folded linen, an iron on a board, "
               "skeins of coloured thread on pegs, a small square window, light plaster walls with wooden beams.",
    "pets": "The Menagerie (a pets' room): a plush pet bed, food bowls on a mat, a scratching post with a little "
            "platform, a perch with a bell, a basket of balls of yarn, a small hutch, a round window, warm orange "
            "walls with paw-print stencils.",
    "whole": "The Tower Heart (the heart of the home): a grand stone fireplace in the middle of the back wall with a "
             "softly glowing magic fire, a mantel with an hourglass and a crystal, two tall arched windows on either "
             "side, rune banners, a big round woven rug, warm golden light.",
}

OUTSIDE = {
    "roof": "Image: the top of a cozy wizard's tower seen from the front: a tall conical roof of purple slate tiles "
            "with two little dormer windows glowing warm yellow, a brick chimney on the right, a small glowing lantern "
            "spire at the very tip, and the top edge of the stone tower wall at the bottom. Landscape, about 9:5. The "
            "roof fills the full width at its base (the bottom 10% is the stone wall top, full width) and narrows to "
            "the tip at the top centre. Everything around the roof (the sky) is " + MAGENTA + ". No moon, no stars, "
            "no clouds, no text.",
    "base": "Image: the bottom of a cozy wizard's tower seen from the front, as a wide strip: grey stone tower wall "
            "with a round wooden door in the middle, two iron wall torches (unlit) on either side of the door, two "
            "low stone steps in front of the door, a few potted flowers and a barrel against the wall. Landscape, "
            "about 7:2. The tower wall spans the full width and goes right down to the bottom edge. NO grass, NO "
            "ground, NO soil, NO path anywhere: the grass is added later in front of it. Fill any space that is not "
            "tower (above it) with " + MAGENTA + ". No text.",
    "wall": "Image: a short section of a cozy wizard tower's outer wall seen from the front, as a wide strip: grey "
            "stone blocks spanning the full width, one small arched window in the exact centre whose glass is flat "
            "solid pure magenta (#FF00FF), a little wooden sill with a flower pot. Landscape, about 9:2. Stone fills "
            "the whole image except the window glass. No text.",
}

# ---------------------------------------------------------------- characters

HELPERS = [
    ("BROOM", "a straw broom with a wooden handle and a red binding"),
    ("SPONGE", "a yellow kitchen sponge with a green scrubby top, a few soap bubbles"),
    ("CLOTH", "a folded blue cleaning rag with a soft fringe"),
    ("BUCKET", "a wooden bucket with iron bands and a little water sloshing"),
    ("DUSTER", "a feather duster with fluffy pink and white feathers"),
    ("SACK", "a patched brown burlap rubbish sack tied with string"),
    ("LADLE", "a copper soup ladle standing on its handle"),
    ("TEAPOT", "a round blue-green ceramic teapot with a lid"),
    ("PILLOW", "a fluffy cream pillow with tassels"),
    ("BASKET", "a wicker laundry basket with a folded towel"),
    ("IRON", "an old black clothes iron with a wooden handle, a puff of steam"),
    ("CAN", "a green tin watering can"),
    ("CANDLE", "a short white wax candle in a little brass holder, with a small gentle flame"),
    ("BOOK", "a small leather ledger book with a ribbon bookmark"),
    ("KEY", "a big ornate golden key standing upright"),
    ("BOWL", "a ceramic pet food bowl with a fish pattern"),
    ("SATCHEL", "a leather market satchel with a buckle and a baguette sticking out"),
]

IMPS = [
    ("PIP", "a lanky purple imp with a long whip tail and tiny horns, a striped sock dangling from its mouth"),
    ("HIX", "a small violet imp with huge bat ears, hugging a bundle of silver spoons and giggling"),
    ("GRUMBLE", "a stout slate-grey imp with bushy eyebrows and a big nose, arms crossed, frowning"),
    ("NIBS", "a pink mouse-like imp with round ears and a long tail, nibbling a biscuit"),
    ("CRUMBWHISTLE", "an amber imp with pursed lips whistling, crumbs flying, wearing a tiny apron"),
    ("INKBLOT", "a slinky slate-blue imp dripping ink, with ink paw prints and a feather quill"),
    ("BUBBLEGOB", "a teal frog-like imp with a wide grin, blowing a big soap bubble"),
    ("SNOREWICK", "a sleepy night-blue imp in a long nightcap, hugging a pillow, eyes half shut"),
    ("BOOTLACE", "an amber imp wearing a boot as a hat, tangled in shoelaces"),
    ("LEAFNIBBLE", "a moss-green caterpillar-like imp with leafy antennae, munching a leaf"),
    ("CUSHIONSQUAT", "a plump rose imp sitting smugly on a velvet cushion"),
    ("BLOCKSTUB", "a sea-teal imp with square horns, holding up a wooden toy block with a letter-less star"),
    ("ODDSOCK", "a mossy imp wearing two mismatched socks, carrying a little nest of socks"),
    ("FURBALL", "a scruffy amber imp covered in a coat of fluffy pet fur, sneezing"),
    ("DUSTBUNNY", "a grey dust-bunny imp: a fluffy dust creature with long bunny ears and tiny horns"),
    ("HALLOWIMP", "an imp hiding under a white sheet with two eye holes, a little pumpkin bucket, only its tail showing"),
    ("FROSTNIP", "an icy pale-blue imp with icicle horns and frost patterns, a snowflake on its tail"),
    ("TINSELTOE", "a rose imp wrapped in shiny gold tinsel, holding a red bauble"),
]
IMP_LOOK = (
    "These are the 'imps': small mischievous house sprites, cheeky but harmless and cute, NOT round balls: each has a "
    "distinct body shape with arms, legs, little horns or ears and a big expressive face. Every imp looks clearly "
    "different from the others. Facing three-quarters right, full body."
)

DECOR = [
    ("CANDLES", "a cluster of three wax candles of different heights on a brass dish, lit with small flames"),
    ("HERBS", "bundles of drying herbs and lavender hanging from a short wooden rod (wall piece)"),
    ("LANTERN", "an iron hanging lantern with warm glowing glass"),
    ("FERN", "a lush fern in a terracotta pot"),
    ("BOOKS", "a messy stack of old leather books with a candle stub on top"),
    ("MUSHROOMS", "a clump of glowing blue and teal mushrooms on a mossy log"),
    ("RUG", "a woven oval rug with a red and gold pattern, seen from the front at a low angle (flat and wide)"),
    ("POTIONS", "a small wall shelf with five colourful potion bottles (wall piece)"),
    ("BANNER", "a hanging cloth banner, deep blue with a gold star and moon (wall piece)"),
    ("PAINTING", "a gilded framed portrait of an old bearded wizard (wall piece)"),
    ("GLOBE", "an antique world globe on a wooden stand"),
    ("HOURGLASS", "a brass hourglass with glowing sand"),
    ("CLOCK", "a carved wooden cuckoo clock with pine-cone weights (wall piece)"),
    ("CHEST", "an old wooden treasure chest with iron bands, closed"),
    ("CAULDRON", "a black iron cauldron on three legs with bubbling purple potion"),
    ("CRYSTAL", "a crystal ball on a carved stand, glowing violet inside"),
    ("CAT", "a black cat with yellow eyes sitting and curling its tail"),
    ("TELESCOPE", "a brass telescope on a wooden tripod"),
    ("OWL", "a round brown owl sitting on a wooden perch"),
    ("FIREPLACE", "a small stone fireplace with a cosy fire"),
    ("CUSHION", "a flat round velvet cat cushion, plum coloured (low and wide)"),
    ("PUMPKIN", "a carved jack-o'-lantern pumpkin glowing from inside, friendly face"),
    ("FIR", "a little fir tree in a pot decorated with baubles and a gold star"),
    ("TEACUP", "a delicate porcelain teacup and saucer with a blue pattern"),
    ("FROG", "a small glazed clay frog figurine"),
    ("SNOWGLOBE", "a snow globe with a tiny mountain monastery inside"),
    ("DRAGON_SCALE", "a single iridescent green-gold dragon scale on a little stand"),
    ("IMP_JAR", "a glass jar with a cork holding a tiny cheeky purple imp"),
    ("BELL", "a small brass hand bell with a ribbon"),
    ("LIGHTHOUSE", "a little red and white lighthouse figurine"),
    ("SHIP", "a ship in a bottle lying on a wooden stand (wide and low)"),
    ("PAPER_LANTERN", "a round red paper lantern with a tassel"),
    ("OLD_MAP", "a rolled-open old treasure map with a red X, slightly curled (wide and low)"),
    ("AURORA", "a glass jar full of swirling green and violet northern lights"),
    ("COMPASS", "an open brass travel compass"),
]

TRINKETS = [
    ("BUTTON", "a red coat button"), ("FEATHER", "a blue feather"), ("ACORN", "an acorn with its cap"),
    ("PEBBLE", "a smooth grey pebble"), ("THIMBLE", "a silver thimble"), ("BEAD", "a glass bead"),
    ("MARBLE", "a glass marble with a swirl"), ("SHELL", "a pink sea shell"), ("RIBBON", "a red ribbon bow"),
    ("CORK", "a wine cork"), ("SPOOL", "a spool of green thread"), ("SOCK", "a lost striped sock"),
    ("KEY", "a tiny brass key"), ("LEAF", "a golden leaf"), ("CLOVER", "a four-leaf clover"),
    ("STAR", "a glowing star shard"), ("GOLD_BUTTON", "a golden button"),
]

HATS = [
    ("POINTED", "a classic tall pointed wizard hat with a wide brim, deep crimson red"),
    ("FLOWERS", "a crown of wildflowers"), ("WIDE", "a black witch's hat with a very wide brim and a buckle"),
    ("CAT", "a knitted cap with cat ears"), ("FROG", "a green frog-face cap with big eyes"),
    ("STAR", "a dark blue starry nightcap with a pom-pom"), ("CROWN", "a little golden crown with gems"),
]


ICONS = [
    ("tower", "a little wizard's tower with a purple conical roof and a glowing window"),
    ("book", "a closed spell book (grimoire) with a gold clasp and a star on the cover"),
    ("basket", "a wicker market basket with a loaf and greens peeking out"),
    ("letter", "a sealed envelope with a red wax seal"),
    ("gear", "a brass cog wheel"),
    ("question", "a round blue speech bubble with a white question mark"),
    ("chest", "a small wooden treasure chest"),
    ("pencil", "a feather quill pen"),
    ("flask", "a round potion flask with glowing green liquid"),
    ("star", "a shining gold star"),
    ("mana", "a glowing blue mana drop"),
    ("imp", "the head of a cheeky purple imp with little horns"),
]


def sheet_prompt(title, items, cols, rows, what, extra=""):
    lines = "\n".join(f"{i + 1}) {d}" for i, (_, d) in enumerate(items))
    return (HEAD + f"Image: {title}\n{STYLE}\n{grid(cols, rows, what)}\n{extra}\n"
            f"Cells, left to right, top to bottom:\n{lines}\nLeave any remaining cells empty magenta.")


def chunks(lst, n):
    return [lst[i:i + n] for i in range(0, len(lst), n)]


def jobs():
    out = {}
    for k, d in ROOMS.items():
        out[f"room_{k}"] = HEAD + ROOM_FRAME + "\n" + STYLE + "\nThe room: " + d
    for k, d in OUTSIDE.items():
        out[k] = HEAD + d + "\n" + STYLE
    # Helpers: each one twice, awake then asleep, so the pair matches.
    for n, part in enumerate(chunks(HELPERS, 6), 1):
        cells = []
        for k, d in part:
            cells.append((k, f"{d}, alive: a cute face with big shiny eyes, a happy smile, tiny stubby arms and feet"))
            cells.append((k, f"the SAME {d.split(',')[0]} character, identical, but asleep: eyes closed, peaceful"))
        out[f"helpers_{n}"] = sheet_prompt(
            "a sprite sheet of enchanted household helpers for a cozy wizard-tower chores game.", cells, 4, 3,
            "character", "The same scale in every cell. Pairs of cells show one character awake then asleep.")
    for n, part in enumerate(chunks(IMPS, 6), 1):
        out[f"imps_{n}"] = sheet_prompt("a sprite sheet of six different imps for a cozy wizard-tower game.", part, 3, 2,
                                        "imp", IMP_LOOK)
    for n, part in enumerate(chunks(DECOR, 12), 1):
        out[f"decor_{n}"] = sheet_prompt("a sprite sheet of furnishings and curiosities for a wizard's tower.", part, 4, 3,
                                         "object", "Each object seen from the front, side-on like a 2D game room.")
    out["trinkets"] = sheet_prompt("a sprite sheet of tiny treasures and trinkets.", TRINKETS, 5, 4, "item",
                                   "Each item drawn large, filling its cell.")
    out["people"] = sheet_prompt(
        "a sprite sheet of characters for a cozy wizard-tower game.", [
            ("MASTER", "the old master wizard Ambrose: a kind old man with a long white beard, a tall blue starry "
                       "hat and a blue robe with gold trim, holding a gnarled staff, full body, standing"),
            ("BODY", "the young apprentice, full body, standing, WITHOUT any hat: short dark brown hair, light peach "
                     "skin, rosy cheeks, a deep crimson red robe with a brown belt and a satchel strap, brown boots. "
                     "Nothing else is red. Facing the viewer"),
            ("SLEEP", "a fluffy ginger tabby cat curled up asleep in a ball (wide and low)"),
            ("SIT", "the same ginger tabby cat sitting up, awake, happy"),
            ("BAT1", "a tiny cute purple bat flying, wings up"),
            ("BAT2", "the same tiny purple bat, wings down"),
        ], 3, 2, "character")
    out["hats"] = sheet_prompt("a sprite sheet of hats, each shown alone (no head, no person), from the front.",
                               HATS, 4, 2, "hat", "All hats at the same scale, sized for the same small head.")
    out["icons"] = sheet_prompt("a set of app menu icons for a cozy wizard-tower game.", ICONS, 4, 3, "icon",
                                "Bold, simple, instantly readable at small size: chunky shapes, thick dark outline, "
                                "few details, each icon filling most of its cell, front view.")
    out["letters"] = sheet_prompt("envelopes for the master's letters, each shown flat from the front.", [
        ("SEALED", "a cream parchment envelope closed with a big red wax seal stamped with a star"),
        ("OPEN", "the same envelope opened, its flap up and a folded letter with handwriting lines peeking out"),
        ("WAITING", "the same envelope, faded and greyish, tied with string, a tiny hourglass on it (a letter still on its way)"),
    ], 3, 1, "envelope", "Wide envelopes filling most of each cell's width.")
    out["sky_night"] = HEAD + (
        "Image: a square seamless tileable texture of a deep night sky for a cozy pixel-art game: very dark indigo to "
        "deep violet, scattered tiny pixel stars of a few sizes (white, pale yellow, lilac), a few sparkling cross-shaped "
        "stars, faint soft wisps of violet nebula. No moon, no planets, no horizon, no ground, no text. Evenly spread "
        "so it can repeat in every direction.\n" + STYLE)
    out["ground"] = HEAD + (
        "Image: a wide horizontal strip of ground in front of a cozy wizard tower, seen from the front like a side-view "
        "2D game: lush green grass with a soft top edge of grass blades, small wildflowers (white, yellow, pink, "
        "lilac), a few clover patches and tiny pebbles, rich soil darkening towards the bottom. Landscape, about 4:1. "
        "Seamless: the left and right edges must continue into each other so it can repeat sideways. Above the grass "
        "blades the background is " + MAGENTA + ". No path, no stones, no buildings, no text.\n" + STYLE)
    out["scaffold"] = HEAD + (
        "Image: a new floor of a cozy wizard tower still being built, seen from the front as a 2D side-view cutaway, "
        "as a wide strip (landscape, exactly 10:3): wooden scaffolding with vertical posts and diagonal braces across a "
        "dark, empty unfinished room, loose planks on the floor, a short ladder, a bucket of mortar with a trowel, a "
        "stack of stone blocks, a coil of rope, a little lantern hanging from a post. A narrow column of grey stone "
        "blocks on the far left and far right edges, a wooden beam along the top and the bottom. The middle is calm "
        "and darker so a caption can sit on it. No people, no text, no signs." + chr(10) + STYLE)
    out["market"] = HEAD + (
        "Image: a wide banner illustration for the market screen of a cozy wizard-tower chores game: a magical market "
        "street at dusk, a striped awning stall heaped with fruit, vegetables, bread loaves, cheese wheels, jars of "
        "honey and glowing potion bottles, strings of warm lanterns, a cobbled lane, little floating sparkles, a "
        "friendly crescent moon. Landscape, exactly 5:2. The lower third is calmer and a bit darker so a title can sit "
        "on it. No people, no animals, no text, no signs with letters." + chr(10) + STYLE)
    out["grimoire"] = HEAD + (
        "Image: a wide banner illustration for the spellbook screen of a cozy wizard-tower chores game: a huge old "
        "grimoire lying open on a carved wooden lectern in a warm wizard's study at night, glowing golden runes and "
        "little sparkles rising from its pages, a ribbon bookmark, a quill in an inkpot, two candles, a stack of old "
        "books, a crystal on a shelf, a round window with stars behind. Landscape, exactly 5:2. The lower third is "
        "calmer and a bit darker so a title can sit on it. No people, no animals, no readable text or letters."
        + chr(10) + STYLE)
    out["aisles"] = sheet_prompt("market aisle icons for a cozy wizard-tower game.", [
        ("PRODUCE", "a small heap of an apple, a carrot and a leek"), ("BAKERY", "a round loaf and a baguette"),
        ("DAIRY", "a milk jug, a wedge of cheese and an egg"), ("MEAT", "a fish and a ham on a board"),
        ("PANTRY", "a jar of grain and a sack of flour with a spice tin"), ("FROZEN", "an ice block with a snowflake"),
        ("DRINKS", "two glowing potion bottles"), ("SWEETS", "a wrapped candy and a cookie"),
        ("HOUSEHOLD", "a spray bottle with soap bubbles"), ("CARE", "a healer's tincture bottle with a herb leaf"),
        ("PETS", "a pet bowl with a paw print and a bone"), ("OTHER", "a mysterious little box with a star"),
    ], 4, 3, "icon", "Bold, simple, instantly readable at small size, thick dark outline, each icon filling most of its cell.")
    out["icon_stats"] = sheet_prompt("one app menu icon for a cozy wizard-tower game.", [
        ("STATS", "an open parchment chronicle scroll with a small golden bar chart drawn on it, a quill and a sparkle"),
    ], 1, 1, "icon", "Bold, simple, instantly readable at small size, thick dark outline, filling most of the image, "
                     "matching a set of chunky cozy fantasy icons.")
    out["sky_day"] = HEAD + (
        "Image: a square seamless tileable texture of a soft daytime sky for a cozy pixel-art game: gentle light blue "
        "with a few small soft white pixel clouds, evenly spread. No sun, no birds, no ground, no text, so it can "
        "repeat in every direction.\n" + STYLE)
    out["fx"] = sheet_prompt("a sprite sheet of small effects and sky pieces.", [
        ("FLAME1", "a small candle flame, teardrop, orange and yellow"),
        ("FLAME2", "the same small flame, leaning left"),
        ("FLAME3", "the same small flame, taller"),
        ("STEAM1", "a small wisp of white steam rising"),
        ("STEAM2", "a different small wisp of steam"),
        ("STEAM3", "a third small wisp of steam, curling"),
        ("STEAM4", "a fourth small wisp of steam, fading"),
        ("MOON", "a full moon with soft craters, glowing"),
        ("CLOUD", "a small fluffy night cloud (wide)"),
        ("COBWEB", "a dusty corner cobweb filling the top-left corner of the cell"),
        ("HEART", "a small red pixel heart"),
        ("TEA", "a low wooden tea table set with a teapot, cups and a plate of biscuits (wide and low)"),
    ], 4, 3, "piece")
    return out


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(name, prompt):
    dst = OUT / f"{name}.png"
    if dst.exists():
        return f"skip {name}"
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        return f"FAIL {name}: {(r.stdout or '')[-300:]} {(r.stderr or '')[-300:]}"
    shutil.copy(p, dst)
    return f"ok {name} {p}"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    todo = [(k, v) for k, v in jobs().items() if not want or k in want]
    with ThreadPoolExecutor(4) as ex:
        for line in ex.map(lambda kv: run(*kv), todo):
            print(line, flush=True)


if __name__ == "__main__":
    main()
