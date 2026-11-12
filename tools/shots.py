"""Builds README / Stillcraft screenshots from raw phone captures.

python tools/shots.py <raw_dir> <out_dir>

<raw_dir> holds full-screen captures (1080x2400) named <name>.png and <name>.ru.png.
Each one is cropped below the status bar and scaled to 540 px wide with rounded corners, and
four of them are laid out on the tower's night sky as overview.png / overview.ru.png.
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

STATUS = 110  # status bar height at 1080 px wide
OVERVIEW = ["tower", "floor", "grimoire", "postcards"]
SINGLE = ["tower", "floors", "floor", "spell", "grimoire", "helpers", "collections", "helper", "decor", "market", "trip",
          "stats", "postcards", "letters", "letter", "note", "cat", "bestiary", "stars", "tea", "wardrobe", "brew", "treasures"]

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


RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi"


def ground(w: int, h: int, px: int = 8) -> Image.Image:
    """The tower's night as the app paints it: a deep gradient, the starry sky texture, a warm glow, the meadow."""
    g = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(g)
    for y in range(h):
        t = y / max(1, h - 1) * (len(NIGHT) - 1)
        i = min(int(t), len(NIGHT) - 2)
        f = t - i
        d.line([(0, y), (w, y)], fill=tuple(round(a + (b - a) * f) for a, b in zip(NIGHT[i], NIGHT[i + 1])))
    g = g.convert("RGBA")
    sky = RES / "hs_sky_night.webp"
    if sky.exists():
        tile = Image.open(sky).convert("RGBA")
        tile = tile.resize((tile.width * 2, tile.height * 2), Image.LANCZOS)
        layer = Image.new("RGBA", (w, h))
        for y in range(0, h, tile.height):
            for x in range(0, w, tile.width):
                layer.alpha_composite(tile, (x, y))
        layer.putalpha(layer.getchannel("A").point(lambda a: a * 150 // 255))
        g.alpha_composite(layer)
    # A warm glow up high, as if from the tower's lantern.
    glow = Image.new("RGBA", (w, h))
    ImageDraw.Draw(glow).ellipse((w * 0.15, -h * 0.35, w * 0.85, h * 0.45), fill=(244, 198, 110, 46))
    g.alpha_composite(glow.filter(ImageFilter.GaussianBlur(max(w, h) // 10)))
    # The meadow along the bottom.
    strip = RES / "hs_ground.webp"
    if strip.exists():
        m = Image.open(strip).convert("RGBA")
        k = max(1.0, h / 900)
        m = m.resize((round(m.width * k), round(m.height * k)), Image.LANCZOS)
        for x in range(0, w, m.width):
            g.alpha_composite(m, (x, h - m.height))
        shade = Image.new("RGBA", (w, m.height), (11, 9, 32, 90))
        g.alpha_composite(shade, (0, h - m.height))
    return g


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
