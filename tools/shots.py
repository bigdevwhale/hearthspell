"""Builds README / Stillcraft screenshots from raw phone captures.

python tools/shots.py <raw_dir> <out_dir>

<raw_dir> holds full-screen captures (1080x2400) named <name>.png and <name>.ru.png.
Each one is cropped below the status bar and scaled to 540 px wide with rounded corners, and
four of them are laid out on a pixel night sky as overview.png / overview.ru.png.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

STATUS = 110  # status bar height at 1080 px wide
OVERVIEW = ["tower", "cat", "stars", "letter"]
SINGLE = ["tower", "floors", "floor", "spell", "grimoire", "helper", "decor", "market", "trip", "letters", "letter", "note",
          "cat", "bestiary", "stars", "tea", "wardrobe", "brew", "treasures"]

# The tower's night: deep indigo at the top, a little lighter at the horizon.
NIGHT = [(11, 9, 32), (21, 17, 53), (33, 26, 74)]
STAR = (255, 244, 214)
STAR_DIM = (169, 150, 230)
HILLS = [(24, 40, 46), (36, 60, 60)]
EDGE = (74, 63, 140)
BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def card(src: Path) -> Image.Image:
    im = Image.open(src).convert("RGB")
    w, h = im.size
    im = im.crop((0, STATUS, w, STATUS + 2290))
    im = im.resize((540, 1145), Image.LANCZOS)
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.size[0] - 1, im.size[1] - 1), 34, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), mask)
    return out


def ground(w: int, h: int, px: int = 8) -> Image.Image:
    """A pixel-art night sky: a dithered gradient, twinkling stars, a moon and dark hills, in big pixels."""
    gw, gh = w // px, h // px
    g = Image.new("RGB", (gw, gh))
    for y in range(gh):
        t = y / gh * (len(NIGHT) - 1)
        for x in range(gw):
            i = int(t)
            frac = t - i
            j = min(i + 1, len(NIGHT) - 1)
            g.putpixel((x, y), NIGHT[j] if frac * 16 > BAYER[y % 4][x % 4] else NIGHT[i])
    rnd = random.Random(7)
    for _ in range(gw * gh // 60):
        x, y = rnd.randrange(gw), rnd.randrange(int(gh * 0.8))
        bright = rnd.random() < 0.3
        g.putpixel((x, y), STAR if bright else STAR_DIM)
        if bright and rnd.random() < 0.4:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= x + dx < gw and 0 <= y + dy < gh:
                    g.putpixel((x + dx, y + dy), STAR_DIM)
    # The moon, top right.
    d = ImageDraw.Draw(g)
    mx, my, r = gw - 22, 14, 7
    d.ellipse((mx - r, my - r, mx + r, my + r), fill=(244, 232, 200))
    for cx, cy in ((mx - 2, my - 1), (mx + 2, my + 3), (mx + 3, my - 3)):
        g.putpixel((cx, cy), (205, 190, 160))
        g.putpixel((cx + 1, cy), (205, 190, 160))
    # Rolling hills along the bottom.
    import math
    for x in range(gw):
        top = gh - 10 + int(2.5 * math.sin(x / 9) + 1.5 * math.sin(x / 4.1))
        for y in range(top, gh):
            g.putpixel((x, y), HILLS[1] if y == top else HILLS[0])
    return g.resize((gw * px, gh * px), Image.NEAREST).crop((0, 0, w, h))


def overview(cards: list[Image.Image]) -> Image.Image:
    W, H = 2400, 1304
    g = ground(W, H).convert("RGBA")
    xs = [64, 646, 1228, 1810]
    ys = [30, 92, 30, 92]
    for c, x, y in zip(cards, xs, ys):
        shadow = Image.new("RGBA", (c.size[0] + 80, c.size[1] + 80), (0, 0, 0, 0))
        ImageDraw.Draw(shadow).rounded_rectangle((40, 40, 40 + c.size[0], 40 + c.size[1]), 34, fill=(0, 0, 0, 160))
        shadow = shadow.filter(ImageFilter.GaussianBlur(18))
        g.alpha_composite(shadow, (x - 40, y - 26))
        frame = Image.new("RGBA", (c.size[0] + 6, c.size[1] + 6), (0, 0, 0, 0))
        ImageDraw.Draw(frame).rounded_rectangle((0, 0, frame.size[0] - 1, frame.size[1] - 1), 37, outline=EDGE, width=3)
        g.alpha_composite(c, (x, y))
        g.alpha_composite(frame, (x - 3, y - 3))
    return g.convert("RGB")


def main() -> None:
    raw, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for suffix in ["", ".ru"]:
        made = {}
        for name in SINGLE:
            src = raw / f"{name}{suffix}.png"
            if src.exists():
                made[name] = card(src)
                made[name].save(out / f"{name}{suffix}.png", optimize=True)
        if all(n in made for n in OVERVIEW):
            overview([made[n] for n in OVERVIEW]).save(out / f"overview{suffix}.png", optimize=True)


if __name__ == "__main__":
    main()
