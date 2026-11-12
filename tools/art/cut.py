"""Turns the Codex sheets in art/sheets into the app's art: res/drawable-nodpi/hs_*.webp, ui/art/Art.kt and
ui/tower/SceneMeta.kt.

    python tools/art/cut.py

Every bitmap is ART_D (4) pixels per scene cell. Sprites keep the cell boxes the old procedural art had
(sizes.json), so the scene layout code is unchanged. A sheet that isn't drawn yet falls back to the old procedural
sprite, enlarged, so the app always builds. Room spots (wall pictures, floor decor, steam, flames) are in ROOM_SPOTS.
"""
import json
import os
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import gen  # noqa: E402

SHEETS = ROOT / "art" / "sheets"
RES = ROOT / "app/src/main/res/drawable-nodpi"
ART_KT = ROOT / "app/src/main/java/app/hearthspell/ui/art/Art.kt"
META_KT = ROOT / "app/src/main/java/app/hearthspell/ui/tower/SceneMeta.kt"
D = 4
SIZES = json.loads((HERE / "sizes.json").read_text())
KINDS = ['KITCHEN', 'LIVING', 'BEDROOM', 'BATH', 'HALL', 'BALCONY', 'KIDS', 'STUDY', 'LAUNDRY', 'PETS', 'WHOLE']

SCENE_W, SCENE_H, SCENE_FLOOR = 120, 72, 62
ROOF_W, ROOF_H = 128, 70
BASE_W, BASE_H = 120, 34
WALL_W, WALL_H = 120, 26

# Per room, in scene cells: where pictures hang (top-left), where standing decor goes (left x), steam sources,
# flames (x, y, size) drawn over the art, and the height of the drawn floor where feet stand (fraction of the
# image height, measured on the sheet). Rooms not listed keep the old procedural spots.
ROOM_SPOTS = {
    'KITCHEN': dict(feet=0.834, steam=[(13, 45)]),
    'LIVING': dict(feet=0.84),
    'BEDROOM': dict(feet=0.775),
    'BATH': dict(feet=0.80, steam=[(102, 44), (108, 46)]),
    'HALL': dict(feet=0.78),
    'BALCONY': dict(feet=0.80),
    'KIDS': dict(feet=0.78),
    'STUDY': dict(feet=0.80),
    'LAUNDRY': dict(feet=0.77),
    'PETS': dict(feet=0.78),
    'WHOLE': dict(feet=0.79),
}

# The drawn characters fill their old boxes more fully than the old sprites did: a touch smaller keeps rooms roomy.
AISLES = [(k, "") for k in ["PRODUCE", "BAKERY", "DAIRY", "MEAT", "PANTRY", "FROZEN", "DRINKS", "SWEETS",
                             "HOUSEHOLD", "CARE", "PETS", "OTHER"]]
# Small things stay small in a room: a cup is not the size of a chest.
DECOR_SCALE = {"TEACUP": 0.5, "COMPASS": 0.65, "BELL": 0.75, "SNOWGLOBE": 0.8}
HELPER_SCALE = 0.85
# The ground strip under the tower, in cells tall: grass blades on top, soil down to the bottom of the base canvas.
GROUND_ROWS = 20
IMP_SCALE = 0.72

# Sizes of the pieces that go on the spots, in cells: a picture on the wall and a thing standing on the floor.
WALL_BOX = (28, 30)
FLOOR_BOX = (32, 30)


def busy(im):
    """How busy each cell of a room is (edges and contrast), so free wall and floor can be found."""
    g = np.asarray(im.convert("L")).astype(np.float32)
    a = np.asarray(im)[..., 3] < 128
    e = np.abs(np.diff(g, axis=0, prepend=g[:1])) + np.abs(np.diff(g, axis=1, prepend=g[:, :1]))
    e[a] = 255  # windows are not wall
    h, w = e.shape
    return e.reshape(h // D, D, w // D, D).mean(axis=(1, 3))


def free_spots(b, box, y0, y1, n, avoid=()):
    """Up to [n] calm boxes, top-left corners, with tops between rows y0 and y1, not overlapping each other."""
    bw, bh = box
    cs = np.pad(b, ((1, 0), (1, 0))).cumsum(0).cumsum(1)
    best = []
    taken = list(avoid)
    H, W = b.shape
    cands = []
    for y in range(y0, min(y1, H - bh) + 1):
        for x in range(10, W - 10 - bw + 1):
            s_ = cs[y + bh, x + bw] - cs[y, x + bw] - cs[y + bh, x] + cs[y, x]
            cands.append((s_ / (bw * bh), x, y))
    cands.sort()
    for score, x, y in cands:
        if len(best) == n:
            break
        if any(x < tx + tw + 2 and tx < x + bw + 2 and y < ty + th and ty < y + bh for tx, ty, tw, th in taken):
            continue
        best.append((x, y))
        taken.append((x, y, bw, bh))
    return best


# ---------------------------------------------------------------- image helpers

def key(im, thin=False):
    """Magenta becomes clear; its pink fringe is pulled back to the art's own colours."""
    a = np.asarray(im.convert("RGBA")).astype(np.int32)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    m = np.minimum(r, b) - g
    out = a.copy()
    # Only the background's own pure magenta goes clear, and only its rim is softened: purple and pink art
    # (imps, potions) stays solid.
    clear = (r > 170) & (b > 170) & (g < 110) & (np.abs(r - b) < 80)
    if not thin:
        clear = ndimage.binary_opening(clear, iterations=1) | (clear & ndimage.binary_erosion(clear, iterations=1))
    near = ndimage.binary_dilation(clear, iterations=2)
    fringe = near & ~clear & (m > 40)
    k = np.clip((m - 40) / 50.0, 0, 1)
    out[..., 0] = np.where(fringe, r - (r - g) * k, r)
    out[..., 2] = np.where(fringe, b - (b - g) * k, b)
    out[..., 3] = np.where(clear, 0, np.where(fringe, (255 * (1 - k)).astype(np.int32), a[..., 3]))
    out[clear] = 0
    return Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA")


def cells(sheet, cols, rows):
    """The sheet's pieces, one per cell: every blob goes to the cell its centre lies in, so a piece that strays over
    a cell border stays whole."""
    im = key(Image.open(sheet))
    a = np.asarray(im)
    mask = a[..., 3] > 40
    lab, n = ndimage.label(mask)
    h, w = mask.shape
    cw, ch = w / cols, h / rows
    boxes = [None] * (cols * rows)
    keep = [np.zeros_like(mask) for _ in range(cols * rows)]
    objs = ndimage.find_objects(lab)
    for i, sl in enumerate(objs, 1):
        if sl is None:
            continue
        comp = lab[sl] == i
        if comp.sum() < 30:
            continue
        ys, xs = np.nonzero(comp)
        cy, cx = ys.mean() + sl[0].start, xs.mean() + sl[1].start
        c = min(cols - 1, int(cx // cw)) + cols * min(rows - 1, int(cy // ch))
        keep[c][sl] |= comp
    out = []
    for c in range(cols * rows):
        if not keep[c].any():
            out.append(None)
            continue
        # Soft edges next to the kept blobs come along (anti-aliasing), nothing else.
        grown = ndimage.binary_dilation(keep[c], iterations=3) & (a[..., 3] > 0)
        piece = a.copy()
        piece[~grown] = 0
        ys, xs = np.nonzero(grown)
        out.append(Image.fromarray(piece[ys.min():ys.max() + 1, xs.min():xs.max() + 1], "RGBA"))
    return out


def fit(im, box, scale=1.0):
    """Scaled to fit [box] cells (times [scale]), padded to whole cells: bottom-aligned, centred."""
    bw, bh = box[0] * D * scale, box[1] * D * scale
    k = min(bw / im.width, bh / im.height)
    w, h = max(1, round(im.width * k)), max(1, round(im.height * k))
    im = im.resize((w, h), Image.LANCZOS)
    W, H = -(-w // D) * D, -(-h // D) * D
    out = Image.new("RGBA", (W, H))
    out.paste(im, ((W - w) // 2, H - h))
    return out


def old(cv, scale=D):
    """An old procedural canvas, enlarged."""
    im = Image.new("RGBA", (cv.w, cv.h))
    p = im.load()
    for y in range(cv.h):
        for x in range(cv.w):
            if cv.px[y][x] is not None:
                p[x, y] = cv.px[y][x]
    return im.resize((cv.w * scale, cv.h * scale), Image.NEAREST)


# ---------------------------------------------------------------- the art

def sheet(name, cols, rows):
    p = SHEETS / f"{name}.png"
    return cells(p, cols, rows) if p.exists() else None


def build_sprites():
    import decor as Dc
    import helpers as Hp
    import extras as X
    import scenes as Sc

    sp = {}  # name -> list of frames (PIL images)

    # Helpers: awake and asleep.
    for n, part in enumerate(gen.chunks(gen.HELPERS, 6), 1):
        got = sheet(f"helpers_{n}", 4, 3)
        for i, (k, _) in enumerate(part):
            box = SIZES[f"helper/{k}"]
            if got and got[2 * i] is not None:
                awake = fit(got[2 * i], box, HELPER_SCALE)
                asleep = fit(got[2 * i + 1] or got[2 * i], box, HELPER_SCALE)
                asleep = asleep.resize(awake.size) if asleep.size != awake.size else asleep
            else:
                awake, asleep = old(Hp.HELPERS[k](False)), old(Hp.HELPERS[k](True))
            sp[f"helper_{k}"] = [awake, asleep]

    for n, part in enumerate(gen.chunks(gen.IMPS, 6), 1):
        got = sheet(f"imps_{n}", 3, 2)
        for i, (k, _) in enumerate(part):
            sp[f"imp_{k}"] = [fit(got[i], SIZES["imp"], IMP_SCALE)] if got and got[i] is not None else [old(Hp.imp(0)), old(Hp.imp(1))]

    olddecor = dict(Dc.DECOR)
    olddecor.update(X.SEASONAL)
    olddecor.update(X.SOUVENIRS)
    for n, part in enumerate(gen.chunks(gen.DECOR, 12), 1):
        got = sheet(f"decor_{n}", 4, 3)
        for i, (k, _) in enumerate(part):
            if got and got[i] is not None:
                sp[f"decor_{k}"] = [fit(got[i], SIZES[f"decor/{k}"], DECOR_SCALE.get(k, 1.0))]
            else:
                f = olddecor[k]
                try:
                    sp[f"decor_{k}"] = [old(f(0)), old(f(1))]
                except TypeError:
                    sp[f"decor_{k}"] = [old(f())]

    got = sheet("trinkets", 5, 4)
    for i, (k, _) in enumerate(gen.TRINKETS):
        sp[f"trinket_{k}"] = [fit(got[i], SIZES[f"trinket/{k}"])] if got and got[i] is not None else [old(X.TRINKETS[k]())]

    got = sheet("people", 3, 2)
    if got:
        sp["master"] = [fit(got[0], SIZES["master"])]
        sp["body"] = [demagenta(fit(got[1], SIZES["body"]))]
        sp["cat_sleep"] = [fit(got[2], SIZES["cat/sleep"])]
        sp["cat_sit"] = [fit(got[3], SIZES["cat/sit"])]
        sp["bat"] = [fit(got[4], SIZES["bat"]), fit(got[5] or got[4], SIZES["bat"])]
    else:
        sp["master"] = [old(Dc.master(0)), old(Dc.master(1))]
        sp["body"] = [old(X.apprentice_body(0)), old(X.apprentice_body(1))]
        sp["cat_sleep"] = [old(X.cat_sleep(0)), old(X.cat_sleep(1))]
        sp["cat_sit"] = [old(X.cat_sit(0)), old(X.cat_sit(1))]
        sp["bat"] = [old(X.bat(0)), old(X.bat(1))]

    # Hats sit on the drawn body's head: sized to it, placed by HAT_AT and each hat's sink.
    got = sheet("hats", 4, 2)
    body = sp["body"][0]
    if got and SHEETS.joinpath("people.png").exists():
        hx, hy, head = head_of(body)
        for i, (k, _) in enumerate(gen.HATS):
            im = got[i]
            k_w = HAT_WIDTH.get(k, 1.3) * head
            s = k_w / im.width
            im = im.resize((max(1, round(im.width * s)), max(1, round(im.height * s))), Image.LANCZOS)
            sp[f"hat_{k}"] = [im]
        hat_at = (hx, hy)
    else:
        # The old hats share the body's grid: drawn over it as they are.
        for k, f in X.HATS.items():
            sp[f"hat_{k}"] = [old(f(0))]
        hat_at = None

    # Menu icons: full-colour pictures for the tab bar and header buttons, when drawn.
    got = sheet("icons", 4, 3)
    if got:
        for i, (k, _) in enumerate(gen.ICONS):
            if got[i] is not None:
                sp[f"icon_{k}"] = [fit(got[i], (32, 32))]

    got = sheet("icon_stats", 1, 1)
    if got and got[0] is not None:
        sp["icon_stats"] = [fit(got[0], (32, 32))]
    got = sheet("aisles", 4, 3)
    if got:
        for i, (k, _) in enumerate(AISLES):
            if got[i] is not None:
                sp[f"aisle_{k}"] = [fit(got[i], (24, 24))]

    got = sheet("letters", 3, 1)
    if got:
        for name, im in zip(["sealed", "open", "waiting"], got):
            if im is not None:
                sp[f"envelope_{name}"] = [fit(im, (44, 30))]

    got = sheet("fx", 4, 3)
    if got:
        sp["flame"] = [fit(got[i], SIZES["flame"][0]) for i in range(3)]
        sp["steam"] = [fit(got[3 + i], SIZES["steam"][0]) for i in range(4)]
        for i, k in enumerate(["moon", "cloud", "cobweb", "heart", "tea"]):
            sp[f"extra_{k}"] = [fit(got[7 + i], SIZES[f"extra/{k}"])]
    else:
        sp["flame"] = [old(c) for c in Sc.flame_frames()]
        sp["steam"] = [old(c) for c in Sc.steam_frames()]
        for k, c in {'moon': Sc.moon(), 'cloud': Sc.cloud(), 'cobweb': Sc.cobweb(), 'tea': X.tea_table(), 'heart': X.heart()}.items():
            sp[f"extra_{k}"] = [old(c)]
    return sp, hat_at


HAT_WIDTH = {"POINTED": 1.45, "WIDE": 1.7, "FLOWERS": 1.15, "CAT": 1.15, "FROG": 1.2, "STAR": 1.2, "CROWN": 0.8}
# How much of each hat's height comes down over the head, in percent.
HAT_SINK = {"POINTED": 34, "WIDE": 33, "FLOWERS": 58, "CAT": 50, "FROG": 47, "STAR": 42, "CROWN": 62}


def demagenta(im):
    """Magenta caught between locks of hair, where the key couldn't reach: it goes clear at the rim, brown inside."""
    a = np.asarray(im).astype(np.int32).copy()
    r, g, b, al = a[..., 0], a[..., 1], a[..., 2], a[..., 3]
    bad = (al > 0) & (b > g + 50) & (b > 90) & (r > b * 0.8)
    a[..., 2] = np.where(bad, g + (r - g) // 4, b)
    a[..., 3] = np.where(bad & (al < 200), 0, al)
    return Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA")


def head_of(body):
    """The drawn body's head: centre x, top y and width, in its pixels."""
    a = np.asarray(body)[..., 3] > 60
    rows = np.nonzero(a.any(axis=1))[0]
    top = rows[0]
    band = a[top + int(body.height * 0.06): top + int(body.height * 0.12)]
    xs = np.nonzero(band.any(axis=0))[0]
    return int((xs[0] + xs[-1]) / 2), int(top), int(xs[-1] - xs[0])


# ---------------------------------------------------------------- scenes

def room(kind):
    """The room bitmap (SCENE_W x SCENE_H cells) and its meta, or None when not drawn yet."""
    p = SHEETS / f"room_{kind.lower()}.png"
    if not p.exists():
        return None
    src = Image.open(p).convert("RGBA")
    im = key(src, thin=True)
    # Only window-sized clear areas are glass: small magenta-ish bits (flasks, lavender) stay painted.
    a = np.asarray(im).copy()
    lab, n = ndimage.label(a[..., 3] < 200)
    sizes = ndimage.sum(np.ones_like(lab), lab, range(1, n + 1))
    orig = np.asarray(src).astype(np.int32)
    means = ndimage.mean(orig[..., 1], lab, range(1, n + 1))
    reds = ndimage.mean(orig[..., 0], lab, range(1, n + 1))
    blues = ndimage.mean(orig[..., 2], lab, range(1, n + 1))
    # Small and not pure magenta (a flask, lavender): painted. Pure magenta is glass, however small the pane.
    keep = [i + 1 for i, z in enumerate(sizes)
            if z < a.shape[0] * a.shape[1] * 0.0008 and not (reds[i] > 225 and blues[i] > 225 and means[i] < 45)]
    small = np.isin(lab, keep)
    a[small] = np.asarray(src)[small]
    # Any pure magenta left (glass glimpsed between leaves) is glass too.
    o = a.astype(np.int32)
    pure = (o[..., 0] > 200) & (o[..., 2] > 200) & (o[..., 1] < 70) & (np.abs(o[..., 0] - o[..., 2]) < 60)
    a[pure] = 0
    im = Image.fromarray(a, "RGBA")
    spots = ROOM_SPOTS.get(kind, {})
    # Crop the bottom so the drawn feet line lands on SCENE_FLOOR, then scale to the scene.
    feet = spots.get("feet", 0.84) * im.height
    want = SCENE_FLOOR / SCENE_H
    h = min(im.height, round(feet / want))
    im = im.crop((0, 0, im.width, h)).resize((SCENE_W * D, SCENE_H * D), Image.LANCZOS)
    glass = clear_box(im)
    return im, glass, spots


def clear_box(im):
    """The see-through window: x, y, w, h in cells and whether it's round."""
    a = np.asarray(im)[..., 3] < 128
    if not a.any():
        return (0, 0, 0, 0, False)
    ys, xs = np.nonzero(a)
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    fill = a[y0:y1 + 1, x0:x1 + 1].mean()
    return (x0 // D, y0 // D, -(-(x1 + 1 - x0) // D), -(-(y1 + 1 - y0) // D), bool(fill < 0.8))


def outside(name, cells_w, cells_h):
    p = SHEETS / f"{name}.png"
    if not p.exists():
        return None
    im = key(Image.open(p))
    a = np.asarray(im)[..., 3] > 40
    ys, xs = np.nonzero(a)
    im = im.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
    return im.resize((cells_w * D, cells_h * D), Image.LANCZOS)


def tileable(im, size):
    """A texture that repeats without seams: each half cross-faded into the other, then scaled to [size]."""
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    h, w, _ = a.shape
    a = a[:min(h, w), :min(h, w)]
    n = a.shape[0]
    rolled = np.roll(np.roll(a, n // 2, 0), n // 2, 1)
    ramp = 1 - np.abs(np.linspace(-1, 1, n))
    wgt = np.minimum.outer(ramp, ramp)[..., None]
    # Weight towards the original in the middle, the rolled copy at the edges, where the original would seam.
    out = a * wgt + rolled * (1 - wgt)
    return Image.fromarray(out.clip(0, 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)


def tileable_x(im, height_px):
    """A strip that repeats sideways without a seam: its ends cross-faded into each other, scaled to [height_px]."""
    a = np.asarray(im).astype(np.float32)
    h, w, _ = a.shape
    k = w // 6
    out = a[:, k // 2: w - k // 2].copy()
    # The last k columns blend into the first ones, so the right edge runs on into the left.
    left = a[:, :k // 2]
    right = a[:, w - k // 2:]
    joined = np.concatenate([right, left], axis=1)
    wgt = np.linspace(0, 1, joined.shape[1])[None, :, None]
    out[:, :joined.shape[1]] = out[:, :joined.shape[1]] * wgt + joined * (1 - wgt)
    im = Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA")
    wpx = -(-round(im.width * height_px / im.height) // D) * D
    return im.resize((wpx, height_px), Image.LANCZOS)


def save(im, name):
    for ext in (".png", ".webp"):
        q = RES / f"{name}{ext}"
        if q.exists():
            q.unlink()
    im.save(RES / f"{name}.webp", "WEBP", lossless=True, method=6)


# ---------------------------------------------------------------- Kotlin

def kt_name(name):
    return "hs_" + name.lower()


def write_art(sp, hat_at):
    # Clear out old sprite drawables so none linger.
    for q in RES.glob("hs_s_*"):
        q.unlink()
    ids = {}
    for name, frames in sp.items():
        ids[name] = []
        for i, im in enumerate(frames):
            n = f"s_{name}_{i}".lower()
            save(im, "hs_" + n)
            ids[name].append(f"R.drawable.hs_{n}")

    def arr(name):
        return "arrayOf(" + ", ".join(ids[name]) + ")"

    helpers = ",\n".join(f"        HelperKind.{k} to {arr('helper_' + k)}" for k, _ in gen.HELPERS)
    imps = ",\n".join(f"        ImpKind.{k} to {arr('imp_' + k)}" for k, _ in gen.IMPS)
    decors = ",\n".join(f"        DecorKind.{k} to {arr('decor_' + k)}" for k, _ in gen.DECOR)
    trinkets = ",\n".join(f'        "{k}" to {ids["trinket_" + k][0]}' for k, _ in gen.TRINKETS)
    hats = ",\n".join(f'        "{k}" to {ids["hat_" + k][0]}' for k, _ in gen.HATS)
    scaffold = "R.drawable.hs_scaffold" if (RES / "hs_scaffold.webp").exists() else "null"
    ground = "R.drawable.hs_ground" if (RES / "hs_ground.webp").exists() else "null"
    sky_day = "R.drawable.hs_sky_day" if (RES / "hs_sky_day.webp").exists() else "null"
    sky_night = "R.drawable.hs_sky_night" if (RES / "hs_sky_night.webp").exists() else "null"
    envelopes = ",\n".join(f'        "{k}" to {ids["envelope_" + k][0]}' for k in ("sealed", "open", "waiting") if "envelope_" + k in ids)
    icons = ",\n".join(f'        "{k}" to {ids["icon_" + k][0]}' for k, _ in gen.ICONS + [("stats", "")] if "icon_" + k in ids)
    aisles = ",\n".join(f'        "{k}" to {ids["aisle_" + k][0]}' for k, _ in AISLES if "aisle_" + k in ids)
    market = "R.drawable.hs_market" if (RES / "hs_market.webp").exists() else "null"
    grim = "R.drawable.hs_grimoire" if (RES / "hs_grimoire.webp").exists() else "null"
    sinks = ", ".join(f'"{k}" to {HAT_SINK[k] if hat_at else 0}' for k, _ in gen.HATS)
    if not hat_at:
        body = sp["body"][0]
        hat_at = (body.width // 2, body.height)
    at = f"{hat_at[0]} to {hat_at[1]}"
    src = f'''package app.hearthspell.ui.art

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import app.hearthspell.R
import app.hearthspell.content.DecorKind
import app.hearthspell.content.FloorKind
import app.hearthspell.content.HelperKind
import app.hearthspell.content.ImpKind

/**
 * Every character and object of the game: fine pixel art drawn with the Codex image tool and cut by
 * tools/art/cut.py into res/drawable-nodpi ({D} pixels per scene cell). Generated: edit the tools, not this file.
 */
object Art {{
    private lateinit var res: Resources

    /** Call once at start-up, before anything is drawn. */
    fun init(context: Context) {{
        res = context.applicationContext.resources
    }}

    private val cache = HashMap<Int, Sprite>()

    private fun load(id: Int): Sprite = synchronized(cache) {{
        cache.getOrPut(id) {{
            val b = BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply {{ inScaled = false }})
            val px = IntArray(b.width * b.height)
            b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
            Sprite(b.width, b.height, px)
        }}
    }}

    /** The same sprite one cell lower inside a cell-taller frame: the down step of a bob. */
    private val lifted = HashMap<Sprite, Sprite>()
    private fun down(s: Sprite): Sprite = synchronized(lifted) {{
        lifted.getOrPut(s) {{
            val out = IntArray(s.rw * (s.rh + ART_D))
            System.arraycopy(s.argb, 0, out, s.rw * ART_D, s.argb.size)
            Sprite(s.rw, s.rh + ART_D, out)
        }}
    }}
    private fun up(s: Sprite): Sprite {{
        val out = IntArray(s.rw * (s.rh + ART_D))
        System.arraycopy(s.argb, 0, out, 0, s.argb.size)
        return Sprite(s.rw, s.rh + ART_D, out)
    }}

    private val helpers = mapOf(
{helpers},
    )
    private val imps = mapOf(
{imps},
    )
    private val decors = mapOf(
{decors},
    )
    private val trinkets = mapOf(
{trinkets},
    )
    private val hats = mapOf(
{hats},
    )
    private val hatSinks = mapOf({sinks})
    private val envelopes = mapOf<String, Int>(
{envelopes}
    )
    private val aisles = mapOf<String, Int>(
{aisles}
    )

    /** The picture for a market aisle, by [app.hearthspell.content.Aisle] name; null until drawn. */
    fun aisle(name: String): Sprite? = aisles[name]?.let {{ load(it) }}

    /** The banner over the market: a stall at dusk; null until drawn. */
    val marketBanner: Sprite? get() = {market}?.let {{ load(it) }}

    /** The banner over the grimoire: an open book on a lectern; null until drawn. */
    val grimoireBanner: Sprite? get() = {grim}?.let {{ load(it) }}

    private val icons = mapOf<String, Int>(
{icons}
    )
    private val masters = {arr("master")}
    private val bodies = {arr("body")}
    private val catsAsleep = {arr("cat_sleep")}
    private val catsAwake = {arr("cat_sit")}
    private val bats = {arr("bat")}
    private val flames = {arr("flame")}
    private val steams = {arr("steam")}

    private fun frame(ids: Array<Int>, f: Int): Sprite = load(ids[Math.floorMod(f, ids.size)])

    private val helperFrames = HashMap<HelperKind, Array<Sprite>>()

    /** A helper; [frame] 0..3 cycles rest, lift, rest, eyes shut (also its sleeping face). */
    fun helper(kind: HelperKind, frame: Int = 0): Sprite = synchronized(helperFrames) {{
        helperFrames.getOrPut(kind) {{
            val ids = helpers.getValue(kind)
            val awake = load(ids[0])
            val shut = load(ids[1])
            arrayOf(down(awake), up(awake), down(awake), down(shut))
        }}[frame and 3]
    }}

    private val asleep = HashMap<HelperKind, Sprite>()

    /** A helper still asleep: greyed out, cached. */
    fun helperAsleep(kind: HelperKind): Sprite = synchronized(asleep) {{ asleep.getOrPut(kind) {{ helper(kind, 3).muted() }} }}

    /** An imp, each kind its own creature; [frame] 0 or 1. */
    fun imp(kind: ImpKind, frame: Int = 0): Sprite = frame(imps.getValue(kind), frame)

    fun decor(kind: DecorKind): Sprite = load(decors.getValue(kind)[0])

    /** A decor item's animation frame; still items have one frame. */
    fun decor(kind: DecorKind, frame: Int): Sprite = frame(decors.getValue(kind), frame)

    /** The old master, for his letters. */
    fun master(frame: Int = 0): Sprite = frame(masters, frame)

    /** A candle flame, three frames. */
    fun flame(frame: Int): Sprite = frame(flames, frame)

    /** A puff of rising steam, four frames. */
    fun steam(frame: Int): Sprite = frame(steams, frame)

    val moon: Sprite get() = load(R.drawable.hs_s_extra_moon_0)
    val cloud: Sprite get() = load(R.drawable.hs_s_extra_cloud_0)
    val cobweb: Sprite get() = load(R.drawable.hs_s_extra_cobweb_0)
    val teaTable: Sprite get() = load(R.drawable.hs_s_extra_tea_0)
    val heart: Sprite get() = load(R.drawable.hs_s_extra_heart_0)

    /** A trinket a helper brought, by [app.hearthspell.content.Trinket] name. */
    fun trinket(name: String): Sprite = load(trinkets.getValue(name))

    /** The tower cat: curled up asleep or sitting up. */
    fun catAsleep(frame: Int): Sprite = frame(catsAsleep, frame)
    fun catAwake(frame: Int): Sprite = frame(catsAwake, frame)

    /** The apprentice without a hat, in the red robe. */
    fun apprenticeBody(frame: Int): Sprite = frame(bodies, frame)

    /** A hat, drawn over the body with its bottom centre at [HAT_AT] lowered by [hatSink] percent of its height. */
    fun hat(name: String): Sprite = load(hats.getValue(name))
    fun hatSink(name: String): Int = hatSinks[name] ?: 30
    val HAT_AT: Pair<Int, Int> = {at}

    fun bat(frame: Int): Sprite = frame(bats, frame)

    /** A floor still being built (the "add a floor" card), SCENE_W x SCENE_H / 2 cells; null until drawn. */
    val scaffold: Sprite? get() = {scaffold}?.let {{ load(it) }}

    /** The grass and soil strip under the tower, repeating sideways; null until drawn. */
    val ground: Sprite? get() = {ground}?.let {{ load(it) }}

    /** The tiling sky texture, night or day, ART_D pixels per cell; null until drawn. */
    fun sky(day: Boolean): Sprite? = (if (day) {sky_day} else {sky_night})?.let {{ load(it) }}

    /** An envelope for the master's letters: "sealed", "open" or "waiting"; null until drawn. */
    fun envelope(state: String): Sprite? = envelopes[state]?.let {{ load(it) }}

    /** The full-colour picture for a UI icon, by name, if one was drawn. */
    fun icon(name: String): Sprite? = icons[name]?.let {{ load(it) }}

    /** A furnishing that tells a floor's kind at a glance. */
    fun prop(kind: FloorKind): Sprite = when (kind) {{
        FloorKind.KITCHEN -> decor(DecorKind.CAULDRON)
        FloorKind.LIVING -> decor(DecorKind.BOOKS)
        FloorKind.BEDROOM -> decor(DecorKind.TELESCOPE)
        FloorKind.BATH -> helper(HelperKind.BUCKET)
        FloorKind.HALL -> decor(DecorKind.LANTERN)
        FloorKind.BALCONY -> decor(DecorKind.FERN)
        FloorKind.KIDS -> decor(DecorKind.RUG)
        FloorKind.STUDY -> decor(DecorKind.CANDLES)
        FloorKind.LAUNDRY -> helper(HelperKind.BASKET)
        FloorKind.PETS -> decor(DecorKind.CAT)
        FloorKind.WHOLE -> decor(DecorKind.FIREPLACE)
    }}
}}
'''
    ART_KT.write_text(src, encoding="utf-8", newline="\n")


def write_scenes():
    import scenes as Sc
    import extras as X

    meta = {}
    for k in KINDS:
        got = room(k)
        cv, m = Sc.scene(k)
        if got is None:
            save(old(cv), f"hs_scene_{k.lower()}")
            meta[k] = dict(glass=m['glass'], flames=m['flames'], steam=m['steam'], wall=m['wall'], floor=m['floor'], walk=m['walk'])
        else:
            im, glass, spots = got
            save(im, f"hs_scene_{k.lower()}")
            b = busy(im)
            wall = spots.get('wall') or free_spots(b, WALL_BOX, 7, SCENE_FLOOR - WALL_BOX[1] - 8, 2)
            floor = spots.get('floor') or [x for x, _ in free_spots(b, FLOOR_BOX, SCENE_FLOOR - FLOOR_BOX[1], SCENE_FLOOR - FLOOR_BOX[1], 2)]
            if not floor:
                floor = [40, 76]
            meta[k] = dict(glass=glass, flames=spots.get('flames', []), steam=spots.get('steam', []),
                           wall=wall, floor=floor, walk=m['walk'])
            im.save(HERE / f"preview_{k.lower()}.png")

    roof, rm = Sc.roof()
    new = outside("roof", ROOF_W, ROOF_H)
    save(new if new else old(roof), "hs_roof")
    rmeta = OUTSIDE_SPOTS.get("roof", rm) if new else rm
    base, bm = Sc.base()
    new = outside("base", BASE_W, BASE_H)
    save(new if new else old(base), "hs_base")
    bmeta = OUTSIDE_SPOTS.get("base", bm) if new else bm
    p = SHEETS / "grimoire.png"
    if p.exists():
        m = Image.open(p).convert("RGBA")
        save(m.resize((1000, round(1000 * m.height / m.width)), Image.LANCZOS), "hs_grimoire")
    p = SHEETS / "market.png"
    if p.exists():
        m = Image.open(p).convert("RGBA")
        save(m.resize((1000, round(1000 * m.height / m.width)), Image.LANCZOS), "hs_market")
    p = SHEETS / "scaffold.png"
    if p.exists():
        save(Image.open(p).convert("RGBA").resize((SCENE_W * D, SCENE_H // 2 * D), Image.LANCZOS), "hs_scaffold")
    p = SHEETS / "ground.png"
    if p.exists():
        g = key(Image.open(p))
        al = np.asarray(g)[..., 3] > 40
        ys = np.nonzero(al.any(axis=1))[0]
        g = g.crop((0, ys[0], g.width, ys[-1] + 1))
        save(tileable_x(g, GROUND_ROWS * D), "hs_ground")
    for k in ("night", "day"):
        p = SHEETS / f"sky_{k}.png"
        if p.exists():
            save(tileable(Image.open(p), 128 * D), f"hs_sky_{k}")
    wall, wg = X.wall()
    new = outside("wall", WALL_W, WALL_H)
    save(new if new else old(wall), "hs_wall")
    wglass = clear_box(new)[:4] if new else wg[:4]

    out = ['''package app.hearthspell.ui.tower

import app.hearthspell.R
import app.hearthspell.content.FloorKind

/** Where things are in a floor scene bitmap, in scene cells (generated by tools/art/cut.py). */
internal class SceneMeta(
    val res: Int,
    /** The window glass: x, y, w, h, and whether it is round. The live sky is drawn there, under the bitmap. */
    val glass: IntArray,
    val round: Boolean,
    /** Wick tops of the candles and fires that flicker, with a size. */
    val flames: List<Triple<Int, Int, Float>>,
    /** Where steam rises from. */
    val steam: List<Pair<Int, Int>>,
    /** Free spots on the wall for hanging decor (top-left corners). */
    val wall: List<Pair<Int, Int>>,
    /** Free spots along the floor for standing decor (left x). */
    val floor: List<Int>,
    /** How far helpers walk. */
    val walk: IntRange,
)
''']
    out.append(f'''
internal const val SCENE_W = {SCENE_W}
internal const val SCENE_H = {SCENE_H}
internal const val SCENE_FLOOR = {SCENE_FLOOR}
internal const val ROOF_W = {ROOF_W}
internal const val ROOF_H = {ROOF_H}
internal const val BASE_W = {BASE_W}
internal const val BASE_H = {BASE_H}
internal const val BASE_GROUND = {bmeta['ground']}
internal val ROOF_CHIMNEY = {rmeta['chimney'][0]} to {rmeta['chimney'][1]}
internal val ROOF_LANTERN = {rmeta['lantern'][0]} to {rmeta['lantern'][1]}
internal val BASE_TORCHES = listOf({', '.join(f'{x} to {y}' for x, y in bmeta['torches'])})
internal const val WALL_H = {WALL_H}
/** The window of the outside wall (hs_wall): x, y, w, h. */
internal val WALL_GLASS = intArrayOf({', '.join(str(int(v)) for v in wglass)})

internal val SCENES: Map<FloorKind, SceneMeta> = mapOf(
''')
    for k in KINDS:
        mm = meta[k]
        g = mm['glass']
        fl = ', '.join(f'Triple({int(x)}, {int(y)}, {float(s)}f)' for (x, y, s) in mm['flames'])
        st = ', '.join(f'{int(x)} to {int(y)}' for (x, y) in mm['steam'])
        wl = ', '.join(f'{int(x)} to {int(y)}' for (x, y) in mm['wall'])
        flo = ', '.join(str(int(x)) for x in mm['floor'])
        out.append(f'    FloorKind.{k} to SceneMeta(\n        R.drawable.hs_scene_{k.lower()},\n        intArrayOf({int(g[0])}, {int(g[1])}, {int(g[2])}, {int(g[3])}), {str(bool(g[4])).lower()},\n'
                   f'        listOf({fl}),\n        listOf({st}),\n        listOf({wl}),\n        listOf({flo}),\n        {mm["walk"][0]}..{mm["walk"][1]},\n    ),\n')
    out.append(')\n')
    META_KT.write_text(''.join(out), encoding="utf-8", newline="\n")


# The roof's chimney top and lantern, the base's torches and ground line, in cells, for the drawn versions.
OUTSIDE_SPOTS = {
    "roof": dict(chimney=(90, 26), lantern=(64, 2)),
    "base": dict(torches=[(38, 9), (82, 9)], ground=34),
}


def main():
    sp, hat_at = build_sprites()
    write_scenes()
    write_art(sp, hat_at)
    print("ok", len(sp), "sprites")


if __name__ == "__main__":
    main()
