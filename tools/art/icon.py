"""The launcher icon: a detailed little tower with glowing windows under a starry sky, as vector drawables."""
import math
import os
import sys
from pixlib import *

R = RAMPS
N = 54  # pixel grid; each pixel is 2 units of the 108-unit viewport


def foreground():
    cv = Canvas(N, N)
    cx = 27
    # Body: a round stone tower, shaded as a cylinder, with courses of blocks.
    x0, x1, top, bot = 17, 37, 21, 46
    S = shade(cv, rect_mask(x0, top, x1 - x0, bot - top), cyl_normal(cx, (x1 - x0) / 2), R['stone'], dither=0.4)
    for y in range(top + 3, bot, 4):
        for x in range(x0 + 1, x1 - 1):
            cv.set(x, y, R['stone'][1], 150)
        off = 2 if (y // 4) % 2 else 0
        for x in range(x0 + 3 + off, x1 - 1, 5):
            cv.set(x, y + 1, R['stone'][1], 120)
            cv.set(x, y + 2, R['stone'][1], 120)
    # Ground mound.
    shade(cv, ellipse_mask(cx, 47.5, 15, 3.4), ellipsoid_normal(cx, 46.5, 15, 3.4), R['green'])
    # Door.
    shade(cv, lambda a, b: (abs(a + 0.5 - cx) <= 3 and 40 <= b < 46) or ((a + 0.5 - cx) ** 2 + (b + 0.5 - 40) ** 2 <= 9 and b < 40), lambda a, b: ((a - cx) / 6, 0, 1), R['wood'])
    cv.set(cx + 1, 43, R['brass'][3])
    # Glowing windows.
    for (wx, wy) in ((cx - 4, 27), (cx + 3, 33)):
        glow(cv, wx + 1, wy + 2, 6, R['fire'][3], 110, 4)
        cv.rect(wx, wy, 3, 5, R['fire'][3])
        cv.rect(wx, wy + 3, 3, 2, R['fire'][2])
        cv.set(wx, wy, R['fire'][4])
        cv.vline(wx + 1, wy, wy + 4, R['wood'][1])
        for (a, b) in ((wx - 1, wy - 1), (wx + 3, wy - 1)):
            pass
        cv.hline(wx - 1, wx + 3, wy + 5, R['stone'][4])
    # Eave and the pointed roof, shingled.
    shade(cv, rect_mask(13, 19, 28, 3), flat(0, -0.5, 1), R['wood'])
    pts = [(cx, 2), (cx + 15, 20), (cx - 15, 20)]
    S = shade(cv, poly_mask(pts), lambda x, y: ((x - cx) / ((y - 2) * 15 / 18 + 1), -0.3, 0.85), R['purple'], dither=0.5)
    for (x, y) in S:
        if (y - 2) % 3 == 2:
            cv.set(x, y, R['purple'][0], 140)
        elif (x + (y // 3) * 2) % 4 == 0 and (y - 2) % 3 == 1:
            cv.set(x, y, R['purple'][1], 110)
    # A dormer with warm light.
    shade(cv, poly_mask([(cx + 3, 16), (cx + 9, 16), (cx + 6, 11)]), flat(0.2, -0.6, 0.8), R['red'])
    cv.rect(cx + 5, 14, 2, 3, R['fire'][3])
    # Spire with a glowing star.
    shade(cv, rect_mask(cx - 1, 0, 2, 4), cyl_normal(cx, 1), R['brass'])
    glow(cv, cx, 1, 5, R['fire'][3], 120, 3)
    cv.set(cx - 1, 0, R['fire'][4])
    cv.set(cx, 0, R['fire'][4])
    return cv


def background():
    cv = Canvas(N, N)
    night = [(10, 8, 30), (18, 14, 48), (30, 24, 72), (44, 34, 96)]
    for y in range(N):
        for x in range(N):
            t = y / N
            cv.set(x, y, quant(night, t * 0.9 + 0.05, x, y, 0.8))
    # Moon.
    shade(cv, ellipse_mask(13, 13, 5, 5), ellipsoid_normal(13, 13, 5, 5), R['cream'][1:] + [WHITE], outline=False)
    cv.set(11, 12, R['cream'][2])
    cv.set(14, 15, R['cream'][2])
    for i, (x, y) in enumerate(((42, 9), (47, 18), (8, 30), (45, 32), (36, 5), (22, 7), (50, 44), (5, 46), (30, 12))):
        c = (255, 244, 214) if i % 2 == 0 else (190, 170, 240)
        cv.set(x, y, c)
        if i % 3 == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                cv.set(x + dx, y + dy, c, 120)
    # Distant hills.
    for x in range(N):
        h = 44 + int(3 * math.sin(x / 7) + 2 * math.sin(x / 3.3))
        for y in range(h, N):
            cv.set(x, y, (24, 40, 46) if y > h else (36, 60, 60))
    return cv


def to_vector(cv, unit=2.0, off=0.0):
    paths = {}
    for y in range(cv.h):
        x = 0
        while x < cv.w:
            c = cv.px[y][x]
            if c is None or c[3] == 0:
                x += 1
                continue
            x2 = x
            while x2 < cv.w and cv.px[y][x2] == c:
                x2 += 1
            key = '#%02X%02X%02X%02X' % (c[3], c[0], c[1], c[2])
            paths.setdefault(key, []).append(f'M{off + x * unit:g},{off + y * unit:g}h{(x2 - x) * unit:g}v{unit:g}h{-(x2 - x) * unit:g}z')
            x = x2
    s = '<?xml version="1.0" encoding="utf-8"?>\n<!-- Generated pixel art: a little tower under the stars. -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
    for k, p in paths.items():
        s += f'    <path\n        android:fillColor="{k}"\n        android:pathData="{"".join(p)}" />\n'
    return s + '</vector>\n'


def to_svg(cv, unit=2):
    """The same pixels as an SVG with crisp edges, clipped to a rounded square (docs/logo.svg)."""
    W = cv.w * unit
    rows = []
    for y in range(cv.h):
        x = 0
        while x < cv.w:
            c = cv.px[y][x]
            if c is None or c[3] == 0:
                x += 1
                continue
            x2 = x
            while x2 < cv.w and cv.px[y][x2] == c:
                x2 += 1
            op = '' if c[3] == 255 else f' fill-opacity="{c[3] / 255:.3f}"'
            rows.append(f'<rect x="{x * unit}" y="{y * unit}" width="{(x2 - x) * unit}" height="{unit}" fill="#{c[0]:02X}{c[1]:02X}{c[2]:02X}"{op}/>')
            x = x2
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {W}" width="{W}" height="{W}" shape-rendering="crispEdges">'
            f'<defs><clipPath id="r"><rect width="{W}" height="{W}" rx="{W * 0.22:g}"/></clipPath></defs>'
            f'<g clip-path="url(#r)">' + ''.join(rows) + '</g></svg>\n')


if __name__ == '__main__':
    root = sys.argv[1]
    fg, bg = foreground(), background()
    d = os.path.join(root, 'app/src/main/res/drawable')
    open(os.path.join(d, 'ic_launcher_foreground.xml'), 'w', newline='\n').write(to_vector(fg, 1.25, (108 - 54 * 1.25) / 2))
    open(os.path.join(d, 'ic_launcher_background.xml'), 'w', newline='\n').write(to_vector(bg))
    both = bg.copy()
    both.blit(fg, 0, 0)
    to_png(both, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icon_preview.png'), scale=6)
    os.makedirs(os.path.join(root, 'docs'), exist_ok=True)
    open(os.path.join(root, 'docs/logo.svg'), 'w', newline=chr(10)).write(to_svg(both))
