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
FONTS = ROOT / "app/src/main/res/font"
HEAD = ImageFont.truetype(str(FONTS / "kurale.ttf"), 76)
BODY = ImageFont.truetype(str(FONTS / "philosopher.ttf"), 46)
GOLD = (244, 198, 110)
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
    ("floor", ("Every chore in its own time", "Every two days, weekly or once a month"),
     ("Каждое дело — в свой срок", "Раз в два дня, раз в неделю или раз в месяц")),
    ("grimoire", ("A grimoire of your spells", "Master them and wake 17 helpers"),
     ("Гримуар твоих заклинаний", "Осваивай их и буди 17 помощников")),
    ("stats", ("A chronicle of your week", "Spells, mana and the tidiest floors"),
     ("Летопись твоей недели", "Заклинания, мана и самые чистые этажи")),
    ("postcards", ("Share your tower", "Postcards of rooms, helpers and imps"),
     ("Делись своей башней", "Открытки с комнатами, помощниками и пакостниками")),
    ("market", ("Shopping as a quest", "The list sorts itself into shop aisles"),
     ("Покупки — поход за реагентами", "Список сам раскладывается по отделам")),
    ("letter", ("Letters from the master", "Twelve of them, one a week, from his travels"),
     ("Письма от мастера", "Двенадцать писем, по одному в неделю")),
]

TAGLINE = {
    "en-US": ("Chores as spells", "in a cozy wizard's tower"),
    "ru-RU": ("Домашние дела —", "заклинания в уютной башне"),
}


def text(s: str, size: int, color, face=None) -> Image.Image:
    """Smooth storybook text with a soft shadow under it."""
    font = face or (HEAD if size >= 60 else BODY)
    l, t, r, b = font.getbbox(s)
    pad = 12
    im = Image.new("RGBA", (r - l + pad * 2, b - t + pad * 2), (0, 0, 0, 0))
    shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text((pad - l + 2, pad - t + 4), s, font=font, fill=(8, 6, 22, 220))
    im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(4)))
    ImageDraw.Draw(im).text((pad - l, pad - t), s, font=font, fill=color + (255,))
    return im


def lines(s: str, size: int, color, width: int, face=None) -> list[Image.Image]:
    """One line if it fits, else split into two at the most even word break."""
    if text(s, size, color, face).width <= width:
        return [text(s, size, color, face)]
    words = s.split()
    splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
    a, b = min(splits, key=lambda ab: max(text(ab[0], size, color, face).width, text(ab[1], size, color, face).width))
    return [text(a, size, color, face), text(b, size, color, face)]


def sky(W: int, H: int) -> Image.Image:
    """The night sky without the moon (it sits above the crop), so captions stay readable."""
    return ground(W, H + 200).crop((0, 200, W, H + 200)).convert("RGBA")


def put(g: Image.Image, im: Image.Image, xy: tuple[int, int]) -> None:
    """Text over a soft dark haze, so the stars behind it don't break up the letters."""
    x, y = xy
    haze = Image.new("RGBA", (im.width + 60, im.height + 60), (0, 0, 0, 0))
    ImageDraw.Draw(haze).rounded_rectangle((34, 34, im.width + 26, im.height + 26), 30, fill=(11, 9, 32, 150))
    g.alpha_composite(haze.filter(ImageFilter.GaussianBlur(16)), (x - 30, y - 30))
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
    block = lines(head, 76, GOLD, W - 100) + [None] + lines(sub, 46, CREAM, W - 100)
    height = sum(10 if im is None else im.height - 10 for im in block)
    y = (CARD_TOP - 20 - height) // 2
    for im in block:
        if im is None:
            y += 10
            continue
        put(g, im, ((W - im.width) // 2, y))
        y += im.height - 10
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
    """The tower as the app paints it: the roof, two floors and the base, without the helpers."""
    res = ROOT / "app/src/main/res/drawable-nodpi"
    roof = Image.open(res / "hs_roof.webp").convert("RGBA")
    floors = [Image.open(res / f"hs_scene_{k}.webp").convert("RGBA") for k in ("bedroom", "kitchen")]
    base = Image.open(res / "hs_base.webp").convert("RGBA")
    h = roof.height + sum(f.height for f in floors) + base.height
    out = Image.new("RGBA", (roof.width, h))
    out.alpha_composite(roof, (0, 0))
    y = roof.height
    for f in floors:
        out.alpha_composite(f, ((roof.width - f.width) // 2, y))
        y += f.height
    out.alpha_composite(base, ((roof.width - base.width) // 2, y))
    return out


def feature(lang: str, tower: Image.Image) -> Image.Image:
    W, H = 1024, 500
    g = sky(W, H)
    # Standing on the meadow, the lantern clear of the top edge.
    foot = H - 46
    k = (foot - 14) / tower.height
    t = tower.resize((round(tower.width * k), round(tower.height * k)), Image.LANCZOS)
    g.alpha_composite(t, (W - t.width - 90, foot - t.height))
    title = text("Hearthspell", 96, GOLD, ImageFont.truetype(str(FONTS / "kurale.ttf"), 96))
    y = 120
    put(g, title, (48, y))
    y += title.height - 6
    for line in TAGLINE[lang]:
        s = text(line, 42, CREAM)
        put(g, s, (56, y))
        y += s.height - 18
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
