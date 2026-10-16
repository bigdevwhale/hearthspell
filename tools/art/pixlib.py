"""A tiny pixel-art toolkit: shaded shapes with hue-shifted ramps, ordered dither, selective outlines and stepped glow."""
import math

# ---------------------------------------------------------------- palette ramps (dark -> light), hue-shifted
O = (24, 14, 32)  # the darkest outline: deep plum, never pure black
RAMPS = {
    'stone': [(34, 28, 50), (56, 50, 76), (84, 78, 104), (118, 112, 132), (158, 152, 162), (198, 192, 186)],
    'wood': [(44, 22, 32), (80, 40, 38), (122, 66, 46), (162, 98, 58), (202, 140, 82), (232, 186, 124)],
    'brass': [(70, 38, 32), (134, 80, 34), (198, 136, 46), (238, 194, 82), (255, 236, 164)],
    'leaf': [(16, 38, 44), (26, 74, 58), (48, 116, 60), (98, 160, 66), (164, 202, 92), (222, 234, 146)],
    'skin': [(96, 44, 52), (162, 86, 78), (212, 138, 110), (240, 184, 148), (252, 222, 192)],
    'red': [(60, 14, 38), (118, 26, 46), (178, 46, 54), (222, 94, 72), (248, 150, 112)],
    'blue': [(16, 20, 58), (30, 46, 108), (46, 86, 166), (86, 138, 214), (150, 196, 240)],
    'purple': [(34, 14, 50), (70, 28, 92), (112, 52, 142), (160, 94, 192), (206, 150, 228)],
    'teal': [(12, 38, 52), (18, 78, 90), (30, 122, 128), (70, 170, 164), (150, 216, 200)],
    'cream': [(118, 88, 82), (174, 144, 118), (220, 198, 160), (244, 230, 200), (255, 250, 234)],
    'silver': [(38, 38, 62), (78, 82, 110), (128, 134, 160), (178, 186, 206), (230, 236, 246)],
    'water': [(22, 50, 96), (38, 94, 150), (68, 148, 200), (128, 198, 230), (210, 240, 250)],
    'night': [(8, 6, 22), (16, 12, 38), (26, 20, 58), (40, 32, 84), (60, 50, 116), (88, 74, 146)],
    'fire': [(120, 28, 22), (218, 78, 30), (254, 148, 40), (255, 218, 98), (255, 252, 214)],
    'pink': [(104, 36, 78), (176, 72, 116), (232, 128, 162), (252, 192, 206)],
    'imp': [(30, 10, 46), (74, 26, 100), (122, 48, 158), (172, 90, 210), (214, 150, 240)],
    'green': [(14, 46, 40), (24, 92, 62), (44, 140, 76), (96, 190, 96), (170, 226, 130)],
    'orange': [(92, 34, 24), (168, 70, 30), (226, 120, 44), (250, 172, 82), (255, 216, 150)],
    'ink': [(20, 16, 30), (40, 34, 56), (66, 60, 86)],
    'glass': [(40, 70, 110), (80, 130, 170), (150, 200, 225), (220, 245, 255)],
    'mush': [(40, 70, 120), (60, 130, 190), (100, 200, 230), (180, 240, 250)],
}
WHITE = (255, 252, 244)
EYE = (30, 16, 40)
BLUSH = (246, 130, 150)

BAYER4 = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def bayer(x, y):
    return (BAYER4[y % 4][x % 4] + 0.5) / 16.0 - 0.5


L = (-0.55, -0.75, 0.62)
_n = math.sqrt(sum(v * v for v in L))
L = tuple(v / _n for v in L)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]  # (r,g,b,a) or None

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def set(self, x, y, c, a=255):
        x, y = int(x), int(y)
        if not (0 <= x < self.w and 0 <= y < self.h) or c is None:
            return
        if len(c) == 4:
            c, a = c[:3], c[3]
        if a >= 255 or self.px[y][x] is None and a >= 255:
            self.px[y][x] = (c[0], c[1], c[2], 255)
            return
        base = self.px[y][x]
        if base is None:
            self.px[y][x] = (c[0], c[1], c[2], a)
            return
        t = a / 255.0
        self.px[y][x] = (round(base[0] + (c[0] - base[0]) * t), round(base[1] + (c[1] - base[1]) * t), round(base[2] + (c[2] - base[2]) * t), max(base[3], a))

    def rect(self, x, y, w, h, c, a=255):
        for j in range(y, y + h):
            for i in range(x, x + w):
                self.set(i, j, c, a)

    def hline(self, x0, x1, y, c):
        for i in range(x0, x1 + 1):
            self.set(i, y, c)

    def vline(self, x, y0, y1, c):
        for j in range(y0, y1 + 1):
            self.set(x, j, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def blit(self, other, ox, oy, flip=False):
        for y in range(other.h):
            for x in range(other.w):
                c = other.px[y][other.w - 1 - x if flip else x]
                if c is not None:
                    self.set(ox + x, oy + y, c[:3], c[3])

    def copy(self):
        c = Canvas(self.w, self.h)
        c.px = [row[:] for row in self.px]
        return c

    def shifted(self, dy):
        """A copy moved down by dy (positive) with the canvas one row taller for bobbing frames."""
        c = Canvas(self.w, self.h + 1)
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is not None:
                    c.px[y + dy][x] = self.px[y][x]
        return c


# ---------------------------------------------------------------- shading

def quant(ramp, light, x, y, dither=0.9, lo=0, hi=None):
    """Picks a ramp colour for light 0..1 with ordered dither between levels."""
    hi = len(ramp) - 1 if hi is None else hi
    v = lo + light * (hi - lo) + bayer(x, y) * dither
    i = int(round(v))
    return ramp[max(lo, min(hi, i))]


def shade(cv, mask, normal, ramp, outline=True, dither=0.5, spec=False, ambient=0.18, lo=0, hi=None, rim=None, oc=None):
    """Fills pixels where mask(x,y) is true, lit by normal(x,y). Outlines with the ramp's darkest tone (selective:
    lit top-left edges use the second tone)."""
    pts = [(x, y) for y in range(cv.h) for x in range(cv.w) if mask(x, y)]
    S = set(pts)
    hi = len(ramp) - 1 if hi is None else hi
    for x, y in pts:
        n = normal(x, y)
        l = max(0.0, dot(n, L)) * (1 - ambient) + ambient
        c = quant(ramp, min(1.0, l), x, y, dither, lo, hi)
        if spec and dot(n, L) > 0.965:
            c = ramp[-1] if len(ramp) > 0 else c
            c = tuple(min(255, int(v * 0.5 + 255 * 0.5)) for v in c)
        cv.set(x, y, c)
    if outline:
        dark = oc if oc is not None else ramp[0]
        for x, y in pts:
            edges = [(x + dx, y + dy) not in S for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if any(edges):
                # Top and left edges face the light: softer outline there.
                lit = (edges[3] or edges[1]) and not (edges[0] or edges[2])
                cv.set(x, y, ramp[1] if lit and len(ramp) > 3 and rim is None else (rim if lit and rim else dark))
    return S


def outline_outside(cv, S, color):
    """A 1px outline around set S, drawn outside it."""
    for x, y in list(S):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in S:
                cv.set(p[0], p[1], color)


def ellipse_mask(cx, cy, rx, ry):
    return lambda x, y: ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0


def ellipsoid_normal(cx, cy, rx, ry):
    def n(x, y):
        dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
        z = math.sqrt(max(0.0, 1 - dx * dx - dy * dy))
        return (dx, dy, z)
    return n


def flat(nx=0.0, ny=0.0, nz=1.0):
    m = math.sqrt(nx * nx + ny * ny + nz * nz)
    return lambda x, y: (nx / m, ny / m, nz / m)


def cyl_normal(cx, rx, tilt=0.0):
    """A vertical cylinder's side."""
    def n(x, y):
        dx = (x + 0.5 - cx) / rx
        dx = max(-1.0, min(1.0, dx))
        return (dx, tilt, math.sqrt(max(0.0, 1 - dx * dx)))
    return n


def rect_mask(x0, y0, w, h):
    return lambda x, y: x0 <= x < x0 + w and y0 <= y < y0 + h


def poly_mask(pts):
    def m(x, y):
        px, py = x + 0.5, y + 0.5
        inside = False
        j = len(pts) - 1
        for i in range(len(pts)):
            xi, yi = pts[i]
            xj, yj = pts[j]
            if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi + 1e-9) + xi:
                inside = not inside
            j = i
        return inside
    return m


def ball(cv, cx, cy, r, ramp, ry=None, spec=True, outline=True, dither=0.5):
    ry = ry or r
    return shade(cv, ellipse_mask(cx, cy, r, ry), ellipsoid_normal(cx, cy, r, ry), ramp, outline=outline, spec=spec, dither=dither)


def cylinder(cv, x, y, w, h, ramp, cap=True, outline=True, cap_ramp=None):
    """An upright cylinder: side shaded round, an elliptical cap on top."""
    cx = x + w / 2
    side = shade(cv, rect_mask(x, y + 1, w, h - 1), cyl_normal(cx, w / 2), ramp, outline=outline)
    if cap:
        cr = cap_ramp or ramp
        shade(cv, ellipse_mask(cx, y + 1.5, w / 2, 1.6), flat(0, -1, 0.4), cr, outline=outline)
    return side


def box(cv, x, y, w, h, ramp, depth=2, outline=True):
    """A box seen slightly from above: front face, a lit top strip."""
    shade(cv, rect_mask(x, y + depth, w, h - depth), lambda a, b: (0, 0.15 * ((b - y) / max(1, h) - 0.5), 1), ramp, outline=outline, dither=0.6)
    if depth:
        shade(cv, rect_mask(x, y, w, depth + 1), flat(0, -1, 0.5), ramp, outline=outline, dither=0.3)


def eyes(cv, x1, x2, y, big=True, look=0, blush=True, closed=False):
    """Two cute eyes: a dark oval with a white sparkle; blush below."""
    for ex in (x1, x2):
        if closed:
            cv.hline(ex, ex + 1, y + 1, EYE)
            continue
        cv.rect(ex + look, y, 2, 3 if big else 2, EYE)
        cv.set(ex + look, y, WHITE)
    if blush:
        cv.set(x1 - 1, y + 3, BLUSH, 200)
        cv.set(x1, y + 3, BLUSH, 160)
        cv.set(x2 + 1, y + 3, BLUSH, 160)
        cv.set(x2 + 2, y + 3, BLUSH, 200)


def smile(cv, x, y, w=3, open_=False):
    cv.set(x, y, EYE)
    cv.set(x + w - 1, y, EYE)
    for i in range(1, w - 1):
        cv.set(x + i, y + 1, EYE)
    if open_:
        for i in range(1, w - 1):
            cv.set(x + i, y + 2, (200, 60, 80))


def glow(cv, cx, cy, r, color, strength=70, steps=4):
    """Soft light as stepped rings, strongest in the centre."""
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r
            if d >= 1:
                continue
            step = math.floor((1 - d) * steps + bayer(x, y) * 0.8) / steps
            if step > 0 and cv.get(x, y) is not None:
                cv.set(x, y, color, int(strength * step))


def to_png(cv, path, scale=6, bg=(30, 24, 48)):
    from PIL import Image
    im = Image.new('RGB', (cv.w * scale, cv.h * scale), bg)
    pix = im.load()
    for y in range(cv.h):
        for x in range(cv.w):
            c = cv.px[y][x]
            if c is None:
                continue
            a = c[3] / 255.0
            col = tuple(int(bg[i] + (c[i] - bg[i]) * a) for i in range(3))
            for j in range(scale):
                for i in range(scale):
                    pix[x * scale + i, y * scale + j] = col
    im.save(path)


def sheet(items, path, scale=4, cols=6, pad=8, bg=(30, 24, 48)):
    """Many canvases on one preview image with names."""
    from PIL import Image, ImageDraw
    cw = max(c.w for _, c in items) * scale + pad
    ch = max(c.h for _, c in items) * scale + pad + 12
    rows = (len(items) + cols - 1) // cols
    im = Image.new('RGB', (cols * cw, rows * ch), bg)
    d = ImageDraw.Draw(im)
    pix = im.load()
    for k, (name, cv) in enumerate(items):
        ox, oy = (k % cols) * cw + pad // 2, (k // cols) * ch + 12
        d.text((ox, oy - 11), name, fill=(200, 200, 210))
        for y in range(cv.h):
            for x in range(cv.w):
                c = cv.px[y][x]
                if c is None:
                    continue
                a = c[3] / 255.0
                col = tuple(int(bg[i] + (c[i] - bg[i]) * a) for i in range(3))
                for j in range(scale):
                    for i in range(scale):
                        pix[ox + x * scale + i, oy + y * scale + j] = col
    im.save(path)


def trim(cv, pad=0):
    xs = [x for y in range(cv.h) for x in range(cv.w) if cv.px[y][x] is not None]
    ys = [y for y in range(cv.h) for x in range(cv.w) if cv.px[y][x] is not None]
    if not xs:
        return cv
    x0, x1, y0, y1 = max(0, min(xs) - pad), min(cv.w - 1, max(xs) + pad), max(0, min(ys) - pad), min(cv.h - 1, max(ys) + pad)
    c = Canvas(x1 - x0 + 1, y1 - y0 + 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            c.px[y - y0][x - x0] = cv.px[y][x]
    return c
