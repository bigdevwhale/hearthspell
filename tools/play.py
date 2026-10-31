"""Google Play graphics: captioned screenshots, the 512 px icon and the feature graphics, into docs/google-play/graphics.

python tools/play.py <raw_dir>

<raw_dir> holds full-screen phone captures (1080 px wide) named <name>.png (English) and <name>.ru.png (Russian),
taken from the demo build (`./gradlew installDebug -Pdemo`); the names are the first column of SCREENS.
"""
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parent / "art"))
from shots import EDGE, ground  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
FONT = ImageFont.truetype(str(ROOT / "app/src/main/res/font/m6x11.ttf"), 16)
GOLD = (255, 185, 56)
CREAM = (255, 244, 214)
LILAC = (169, 150, 230)

# Status bar and gesture bar of the capturing phone (Mi 9, 1080x2340), cut off the captures.
TOP, BOTTOM = 100, 55

# name, then (headline, subline) in English and Russian.
SCREENS = [
    ("tower", ("Your home is a wizard's tower", "Every room is a floor, every chore a spell"),
     ("Твой дом — башня волшебника", "Каждая комната — этаж, каждое дело — заклинание")),
    ("floors", ("Forget a chore and imps move in", "Harmless and cheeky. One cast sends them off"),
     ("Забытое дело — и заводятся пакостники", "Безобидные. Одно заклинание, и их нет")),
    ("floor", ("Every chore keeps its own rhythm", "Every two days, weekly or once a month"),
     ("У каждого дела свой ритм", "Раз в два дня, раз в неделю или раз в месяц")),
    ("spell", ("Master spells, wake helpers", "From Novice to Archmage, with 17 helpers"),
     ("Осваивай заклинания", "От Новичка до Архимага, 17 помощников")),
    ("letter", ("Letters from the master", "Twelve of them, one a week, from his travels"),
     ("Письма от мастера", "Двенадцать писем, по одному в неделю")),
    ("market", ("Shopping as a quest", "The list sorts itself into shop aisles"),
     ("Покупки — поход за реагентами", "Список сам раскладывается по отделам")),
    ("stars", ("Light stars over the tower", "Twelve constellations to fill"),
     ("Зажигай звёзды над башней", "Двенадцать созвездий")),
    ("bestiary", ("Meet every imp", "Eighteen of them, by room and season"),
     ("Познакомься со всеми пакостниками", "Восемнадцать, по комнатам и сезонам")),
]

TAGLINE = {
    "en-US": ("Chores as spells", "in a cozy pixel tower"),
    "ru-RU": ("Домашние дела —", "заклинания в уютной башне"),
}


def text(s: str, scale: int, color) -> Image.Image:
    """Pixel text: drawn at the font's native 16 px without smoothing, then scaled up."""
    l, t, r, b = FONT.getbbox(s)
    im = Image.new("RGBA", (r - l + 2, b - t + 3), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.fontmode = "1"
    d.text((1 - l, 1 - t), s, font=FONT, fill=(16, 12, 36, 255))  # a one-pixel shadow
    d.text((-l, -t), s, font=FONT, fill=color + (255,))
    return im.resize((im.width * scale, im.height * scale), Image.NEAREST)


def lines(s: str, scale: int, color, width: int) -> list[Image.Image]:
    """One line if it fits, else split into two at the most even word break."""
    if text(s, scale, color).width <= width:
        return [text(s, scale, color)]
    words = s.split()
    splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
    a, b = min(splits, key=lambda ab: max(text(ab[0], scale, color).width, text(ab[1], scale, color).width))
    return [text(a, scale, color), text(b, scale, color)]


def sky(W: int, H: int) -> Image.Image:
    """The night sky without the moon (it sits above the crop), so captions stay readable."""
    return ground(W, H + 200).crop((0, 200, W, H + 200)).convert("RGBA")


def put(g: Image.Image, im: Image.Image, xy: tuple[int, int]) -> None:
    """Text over a soft dark haze, so the stars behind it don't break up the letters."""
    x, y = xy
    haze = Image.new("RGBA", (im.width + 60, im.height + 60), (0, 0, 0, 0))
    ImageDraw.Draw(haze).rounded_rectangle((24, 24, im.width + 36, im.height + 36), 18, fill=(11, 9, 32, 200))
    g.alpha_composite(haze.filter(ImageFilter.GaussianBlur(12)), (x - 30, y - 30))
    g.alpha_composite(im, (x, y))


def card(src: Path, width: int) -> Image.Image:
    im = Image.open(src).convert("RGB")
    w, h = im.size
    im = im.crop((0, TOP, w, h - BOTTOM))
    im = im.resize((width, round(im.height * width / w)), Image.LANCZOS)
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), 40, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def screenshot(src: Path, head: str, sub: str) -> Image.Image:
    W, H, CARD_TOP = 1080, 1920, 350
    g = sky(W, H)
    block = lines(head, 4, GOLD, W - 100) + [None] + lines(sub, 3, CREAM, W - 100)
    height = sum(10 if im is None else im.height + 4 for im in block)
    y = (CARD_TOP - 20 - height) // 2
    for im in block:
        if im is None:
            y += 10
            continue
        put(g, im, ((W - im.width) // 2, y))
        y += im.height + 4
    c = card(src, 770)
    x, y = (W - c.width) // 2, CARD_TOP
    shadow = Image.new("RGBA", (c.width + 80, c.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((40, 40, 40 + c.width, 40 + c.height), 40, fill=(0, 0, 0, 170))
    g.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(20)), (x - 40, y - 24))
    g.alpha_composite(c, (x, y))
    frame = Image.new("RGBA", (c.width + 8, c.height + 8), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, frame.width - 1, frame.height - 1), 44, outline=EDGE, width=4)
    g.alpha_composite(frame, (x - 4, y - 4))
    return g.convert("RGB")


def canvas_image(cv) -> Image.Image:
    im = Image.new("RGBA", (cv.w, cv.h), (0, 0, 0, 0))
    for y in range(cv.h):
        for x in range(cv.w):
            c = cv.px[y][x]
            if c is not None:
                im.putpixel((x, y), tuple(c[:3]) + (c[3],))
    return im


def app_icon() -> Image.Image:
    import icon
    both = icon.background().copy()
    both.blit(icon.foreground(), 0, 0)
    im = canvas_image(both).convert("RGB")
    return im.resize((im.width * 19, im.height * 19), Image.NEAREST).resize((512, 512), Image.NEAREST)


def tower_art() -> Image.Image:
    """The tower from the launcher icon, without its square of sky."""
    import icon
    return canvas_image(icon.foreground())


def feature(lang: str, tower: Image.Image) -> Image.Image:
    W, H = 1024, 500
    g = sky(W, H)
    t = tower.resize((tower.width * 8, tower.height * 8), Image.NEAREST)
    g.alpha_composite(t, (W - t.width - 50, H - t.height + 4))
    title = text("Hearthspell", 6, GOLD)
    y = 120
    put(g, title, (60, y))
    y += title.height + 30
    for line in TAGLINE[lang]:
        s = text(line, 3, CREAM)
        put(g, s, (66, y))
        y += s.height + 6
    return g.convert("RGB")


def main() -> None:
    raw = Path(sys.argv[1])
    out = ROOT / "docs/google-play/graphics"
    out.mkdir(parents=True, exist_ok=True)
    app_icon().save(out / "icon-512.png", optimize=True)
    tower = tower_art()
    for lang, short, suffix, idx in (("en-US", "en", "", 1), ("ru-RU", "ru", ".ru", 2)):
        feature(lang, tower).save(out / f"feature-graphic-{short}.png", optimize=True)
        shots = out / f"screenshots-{short}"
        shots.mkdir(exist_ok=True)
        for f in shots.glob("*.png"):
            f.unlink()
        for i, entry in enumerate(SCREENS, 1):
            name, words = entry[0], entry[idx]
            src = raw / f"{name}{suffix}.png"
            if src.exists():
                screenshot(src, *words).save(shots / f"{i}.png", optimize=True)
            else:
                print(f"missing {src}")


if __name__ == "__main__":
    main()
