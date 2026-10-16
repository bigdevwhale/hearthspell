"""The 17 helpers and the imp, ~28x30, with faces."""
import math
from pixlib import *

R = RAMPS


def face(cv, cx, y, spread=3, closed=False, mouth=True, open_=False):
    eyes(cv, cx - spread - 1, cx + spread - 1, y, closed=closed)
    if mouth:
        smile(cv, cx - 1, y + 4, 3, open_)


def broom(closed=False):
    cv = Canvas(26, 32)
    # Handle, slightly leaning.
    for i in range(14):
        x = 13 + i // 7
        shade(cv, rect_mask(x - 1, i, 3, 1), cyl_normal(x + 0.5, 1.5), R['wood'], outline=False)
    shade(cv, poly_mask([(12, 0), (15, 0), (16, 14), (13, 14)]), cyl_normal(14, 2), R['wood'])
    # Binding.
    shade(cv, rect_mask(9, 13, 9, 3), cyl_normal(13.5, 4.5), R['red'])
    # Bristles: a fanned bundle.
    S = shade(cv, poly_mask([(9, 15.5), (18, 15.5), (24, 31), (2, 31)]), lambda x, y: ((x - 13) / 12, 0.1, 0.9), R['brass'], dither=0.7)
    for x in range(4, 23, 2):
        for y in range(18, 31):
            if (x, y) in S and (x + y) % 5 != 0:
                cv.set(x, y, R['brass'][1], 140)
    for x in range(3, 24):
        if (x, 30) in S:
            cv.set(x, 30, R['brass'][0])
    face(cv, 13, 21, 3, closed)
    return cv


def sponge(closed=False):
    cv = Canvas(26, 28)
    # Bubbles.
    for (bx, by, r) in ((5, 4, 3), (19, 3, 2.4), (13, 2, 1.6)):
        shade(cv, ellipse_mask(bx, by, r, r), ellipsoid_normal(bx, by, r, r), R['glass'], dither=0.4)
        cv.set(int(bx - r / 2), int(by - r / 2), WHITE)
    # Green scourer underneath, yellow sponge on top.
    shade(cv, rect_mask(2, 21, 22, 6), lambda x, y: (0, 0.2, 1), R['green'], dither=0.5)
    S = shade(cv, poly_mask([(3, 9), (23, 9), (23.5, 22), (2.5, 22)]), lambda x, y: (((x - 13) / 11) * 0.6, -0.25 + (y - 9) / 26, 0.8), R['brass'][1:] + [WHITE], dither=0.5)
    for (px_, py_) in ((6, 12), (19, 11), (20, 17), (5, 18), (12, 19), (16, 13)):
        cv.set(px_, py_, R['brass'][1])
        cv.set(px_ + 1, py_, R['brass'][2])
    face(cv, 13, 13, 3, closed)
    return cv


def cloth(closed=False):
    cv = Canvas(26, 28)
    pts = [(3, 3), (23, 2), (24, 17), (20, 21), (15, 27), (12, 21), (4, 19)]
    S = shade(cv, poly_mask(pts), lambda x, y: (math.sin(x / 2.6) * 0.5, -0.3, 0.8), R['blue'], dither=0.5)
    # Stripes woven in.
    for y in range(3, 26):
        for x in range(3, 25):
            if (x, y) in S and (y == 6 or y == 7):
                c = cv.get(x, y)
                cv.set(x, y, R['cream'][3] if y == 6 else R['cream'][2])
    face(cv, 14, 10, 3, closed)
    return cv


def bucket(closed=False):
    cv = Canvas(26, 28)
    # Handle arc.
    for t in range(0, 181, 6):
        a = math.radians(t)
        cv.set(13 + math.cos(a) * 10, 9 - math.sin(a) * 8, R['silver'][1])
    shade(cv, poly_mask([(2.5, 9), (23.5, 9), (21, 27), (5, 27)]), lambda x, y: ((x - 13) / 11, 0.05, 0.9), R['silver'], spec=False)
    shade(cv, ellipse_mask(13, 10, 10.5, 2.6), flat(0, -1, 0.3), R['water'], dither=0.5)
    cv.hline(9, 13, 9, R['water'][4])
    # Bands.
    for y in (13, 24):
        for x in range(3, 24):
            if cv.get(x, y) is not None:
                cv.set(x, y, R['silver'][1])
    face(cv, 13, 16, 3, closed)
    return cv


def duster(closed=False):
    cv = Canvas(26, 32)
    shade(cv, rect_mask(11, 20, 4, 12), cyl_normal(13, 2), R['wood'])
    shade(cv, rect_mask(10, 19, 6, 3), cyl_normal(13, 3), R['brass'])
    # Feather tips poking out around the pom.
    tips = [(3, 8, R['teal']), (23, 8, R['purple']), (6, 2, R['orange']), (20, 2, R['teal']), (13, 0.5, R['purple']), (2, 15, R['purple']), (24, 15, R['orange'])]
    for x, y, ramp in tips:
        shade(cv, ellipse_mask(x, y + 2, 3, 2.4), ellipsoid_normal(x, y + 2, 3, 2.4), ramp)
    shade(cv, ellipse_mask(13, 11, 10, 9), ellipsoid_normal(13, 10, 10, 9), R['pink'], spec=True)
    # Fluffy edge: a few lighter tufts.
    for (x, y) in ((6, 6), (19, 5), (9, 3), (16, 3)):
        cv.set(x, y, R['pink'][3])
    face(cv, 13, 10, 3, closed)
    return cv


def sack(closed=False):
    cv = Canvas(26, 30)
    shade(cv, ellipse_mask(13, 19, 11, 10.5), ellipsoid_normal(13, 18, 11, 10.5), R['cream'][:4] + [R['cream'][3]], dither=0.55)
    shade(cv, poly_mask([(9, 2), (17, 2), (15, 9), (11, 9)]), lambda x, y: ((x - 13) / 5, -0.2, 0.8), R['cream'][:4])
    shade(cv, rect_mask(9, 7, 8, 3), cyl_normal(13, 4), R['red'])
    # A patch.
    shade(cv, rect_mask(17, 21, 5, 5), flat(), R['wood'][1:], dither=0.2)
    cv.set(18, 22, R['cream'][3])
    cv.set(20, 24, R['cream'][3])
    face(cv, 12, 15, 3, closed, open_=True)
    return cv


def ladle(closed=False):
    cv = Canvas(26, 30)
    shade(cv, poly_mask([(11.5, 0), (14.5, 0), (14.5, 16), (11.5, 16)]), cyl_normal(13, 1.5), R['silver'])
    cv.set(12, 1, R['silver'][4])
    shade(cv, ellipse_mask(13, 16, 11, 11), lambda x, y: ellipsoid_normal(13, 16, 11, 11)(x, y) if y >= 16 else (0, -1, 0.4), R['silver'], spec=True)
    for y in range(0, 16):
        for x in range(0, 26):
            if y < 15 and cv.get(x, y) is not None and not (11 <= x <= 14):
                cv.px[y][x] = None
    shade(cv, ellipse_mask(13, 16, 10.5, 2.4), flat(0, -1, 0.3), R['orange'], dither=0.6)
    cv.hline(8, 12, 15, R['orange'][4])
    face(cv, 13, 20, 3, closed)
    return cv


def teapot(closed=False):
    cv = Canvas(30, 28)
    # Spout and handle first, the body over them.
    shade(cv, poly_mask([(1, 9), (4, 9), (9, 17), (6, 20)]), lambda x, y: (-0.4, -0.5, 0.7), R['teal'])
    for t in range(-80, 90, 8):
        a = math.radians(t)
        x, y = 23 + math.cos(a) * 5, 15 + math.sin(a) * 6
        shade(cv, ellipse_mask(x, y, 1.6, 1.6), flat(0.3, 0, 1), R['teal'], outline=False)
    shade(cv, ellipse_mask(15, 17, 10, 9.5), ellipsoid_normal(15, 16, 10, 9.5), R['teal'], spec=True)
    shade(cv, ellipse_mask(15, 8.5, 6, 2.5), flat(0, -1, 0.5), R['teal'])
    shade(cv, ellipse_mask(15, 5.5, 2, 2), ellipsoid_normal(15, 5.5, 2, 2), R['brass'], spec=True)
    # A painted flower.
    for (x, y) in ((19, 21), (20, 20), (21, 21), (20, 22)):
        cv.set(x, y, R['cream'][4])
    cv.set(20, 21, R['brass'][3])
    face(cv, 13, 14, 3, closed)
    return cv


def pillow(closed=False):
    cv = Canvas(30, 24)
    pts = [(1, 2), (8, 4), (22, 4), (29, 2), (27, 12), (29, 22), (22, 20), (8, 20), (1, 22), (3, 12)]
    shade(cv, poly_mask(pts), lambda x, y: ((x - 15) / 26, (y - 12) / 20, 0.8), R['cream'], dither=0.55)
    # Stitching.
    for x in range(6, 25, 2):
        cv.set(x, 6, R['cream'][1], 160)
    face(cv, 15, 10, 4, closed)
    return cv


def basket(closed=False):
    cv = Canvas(28, 28)
    for t in range(10, 171, 5):
        a = math.radians(t)
        x, y = 14 + math.cos(a) * 10, 12 - math.sin(a) * 10
        shade(cv, ellipse_mask(x, y, 1.4, 1.4), flat(0, -0.5, 1), R['wood'], outline=False)
    # Linen peeking out.
    shade(cv, ellipse_mask(10, 11, 6, 3.5), ellipsoid_normal(10, 11, 6, 3.5), R['cream'])
    shade(cv, ellipse_mask(18, 11, 5, 3), ellipsoid_normal(18, 11, 5, 3), R['blue'])
    S = shade(cv, poly_mask([(2, 12), (26, 12), (23, 27), (5, 27)]), lambda x, y: ((x - 14) / 13, 0.1, 0.9), R['wood'], dither=0.6)
    for (x, y) in S:
        if (x // 2 + y // 2) % 2 == 0 and 12 < y < 26 and 3 < x < 25:
            cv.set(x, y, R['wood'][2], 130)
    for x in range(3, 26):
        if (x, 13) in S:
            cv.set(x, 13, R['wood'][4])
    face(cv, 14, 17, 3, closed)
    return cv


def iron(closed=False):
    cv = Canvas(30, 26)
    # Steam puffs.
    for (x, y, r) in ((4, 5, 2), (7, 2, 2.4), (3, 0.5, 1.4)):
        shade(cv, ellipse_mask(x, y, r, r * 0.8), ellipsoid_normal(x, y, r, r * 0.8), R['silver'][2:] + [WHITE], outline=False, dither=0.4)
    for t in range(0, 181, 6):
        a = math.radians(t)
        cv.set(17 + math.cos(a) * 6, 7 - math.sin(a) * 5, R['wood'][1])
        cv.set(17 + math.cos(a) * 5, 7 - math.sin(a) * 4, R['wood'][3])
    shade(cv, poly_mask([(1, 20), (6, 10), (26, 8), (28, 20)]), lambda x, y: (0.1, -0.4 + (y - 7) / 20, 0.9), R['red'], spec=True)
    shade(cv, rect_mask(1, 20, 28, 4), flat(0, -0.2, 1), R['silver'])
    for x in range(5, 26, 4):
        cv.set(x, 23, R['silver'][0])
    face(cv, 16, 13, 3, closed)
    return cv


def can(closed=False):
    cv = Canvas(30, 26)
    shade(cv, poly_mask([(19, 13), (27, 3), (29, 4), (22, 16)]), lambda x, y: (0.3, -0.6, 0.6), R['green'])
    shade(cv, ellipse_mask(28, 3.5, 2, 1.6), flat(0.5, -0.5, 0.6), R['green'])
    for (x, y) in ((28, 8), (29, 11), (27, 13)):
        cv.set(x, y, R['water'][3])
    for t in range(100, 260, 7):
        a = math.radians(t)
        cv.set(4 + math.cos(a) * 3, 14 + math.sin(a) * 6, R['green'][1])
    cylinder(cv, 3, 9, 18, 16, R['green'])
    shade(cv, rect_mask(3, 21, 18, 2), cyl_normal(12, 9), R['brass'], outline=False)
    face(cv, 12, 13, 3, closed)
    return cv


def candle(closed=False, frame=0):
    cv = Canvas(24, 32)
    shade(cv, ellipse_mask(12, 28, 11, 3.5), ellipsoid_normal(12, 27, 11, 3.5), R['brass'], spec=True)
    cylinder(cv, 6, 9, 12, 19, R['cream'], cap_ramp=R['cream'])
    # Drips.
    for (x, y0, y1) in ((7, 10, 14), (15, 10, 12), (11, 10, 13)):
        for y in range(y0, y1):
            cv.set(x, y, R['cream'][4])
    cv.set(12, 8, R['ink'][1])
    cv.set(12, 7, R['ink'][1])
    fl = [(12, 1.5 + frame * 0.5), ]
    shade(cv, poly_mask([(12, 0 + frame), (15, 5), (12, 8), (9, 5)]), flat(0, 0, 1), R['fire'][2:3], outline=False)
    shade(cv, poly_mask([(12, 2 + frame), (13.5, 5), (12, 7), (10.5, 5)]), flat(0, 0, 1), R['fire'][4:5], outline=False)
    face(cv, 12, 15, 2, closed)
    return cv


def book(closed=False):
    cv = Canvas(24, 30)
    shade(cv, rect_mask(4, 2, 18, 26), lambda x, y: (0.1 if x > 6 else -0.6, -0.15, 0.95), R['red'], dither=0.5)
    shade(cv, rect_mask(18, 3, 4, 24), flat(0.8, 0, 0.5), R['cream'], outline=False)
    for y in range(4, 27, 2):
        cv.set(19, y, R['cream'][2])
    for (x, y) in ((4, 2), (17, 2), (4, 24), (17, 24)):
        shade(cv, rect_mask(x, y, 4, 4), flat(), R['brass'], outline=False)
    shade(cv, rect_mask(19, 13, 4, 5), flat(), R['brass'])
    face(cv, 12, 11, 3, closed)
    cv.hline(8, 15, 7, R['brass'][3])
    return cv


def key(closed=False):
    cv = Canvas(22, 32)
    shade(cv, ellipse_mask(11, 9.5, 9.5, 9), ellipsoid_normal(11, 9, 9.5, 9), R['brass'], spec=True)
    # A small hole at the top of the bow for the ring.
    for y in range(2, 5):
        for x in range(9, 13):
            if ellipse_mask(11, 3.5, 1.8, 1.4)(x, y):
                cv.px[y][x] = None
    cv.hline(9, 12, 5, R['brass'][1])
    shade(cv, rect_mask(9, 17, 4, 15), cyl_normal(11, 2), R['brass'])
    shade(cv, rect_mask(13, 23, 5, 3), flat(), R['brass'])
    shade(cv, rect_mask(13, 28, 4, 3), flat(), R['brass'])
    face(cv, 11, 8, 3, closed)
    return cv


def bowl(closed=False):
    cv = Canvas(28, 22)
    # Kibble heap and a fish bone.
    for (x, y) in ((8, 7), (11, 6), (14, 5), (17, 6), (20, 7), (12, 8), (16, 8), (10, 8), (19, 8)):
        shade(cv, ellipse_mask(x, y, 2, 1.6), ellipsoid_normal(x, y, 2, 1.6), R['orange'], dither=0.3)
    S = shade(cv, lambda x, y: ellipse_mask(14, 9, 13, 12)(x, y) and y >= 9, lambda x, y: ellipsoid_normal(14, 9, 13, 12)(x, y), R['blue'], spec=True)
    shade(cv, ellipse_mask(14, 9.5, 13, 2.2), flat(0, -1, 0.4), R['blue'][1:])
    cv.hline(3, 25, 13, R['cream'][3])
    face(cv, 14, 14, 4, closed)
    return cv


def satchel(closed=False):
    cv = Canvas(28, 28)
    for t in range(0, 181, 5):
        a = math.radians(t)
        cv.set(14 + math.cos(a) * 10, 9 - math.sin(a) * 8, R['wood'][1])
        cv.set(14 + math.cos(a) * 10, 10 - math.sin(a) * 8, R['wood'][2])
    shade(cv, rect_mask(2, 9, 24, 18), lambda x, y: ((x - 14) / 24, 0.1, 0.9), R['wood'], dither=0.6)
    shade(cv, poly_mask([(2, 9), (26, 9), (24, 19), (4, 19)]), lambda x, y: ((x - 14) / 30, -0.4, 0.9), R['wood'][1:], dither=0.6)
    shade(cv, rect_mask(12, 17, 4, 5), flat(), R['brass'], spec=True)
    face(cv, 14, 12, 4, closed)
    return cv


def imp(frame=0):
    cv = Canvas(30, 30)
    up = 1 if frame == 1 else 0
    # Tail with a spade tip.
    for i in range(10):
        x, y = 22 + i * 0.6, 24 - math.sin(i / 3) * 4
        cv.set(x, y, R['imp'][1])
        cv.set(x, y + 1, R['imp'][0])
    shade(cv, poly_mask([(27, 17), (30, 19), (27, 21), (26, 19)]), flat(), R['red'])
    # Wings.
    for side in (-1, 1):
        cx = 15 + side * 10
        pts = [(15 + side * 5, 12 - up), (cx + side * 3, 6 - up * 2), (cx + side * 1, 14), (cx - side * 1, 12), (15 + side * 6, 17)]
        shade(cv, poly_mask(pts), flat(side * 0.3, -0.3, 0.9), R['purple'][:3] + [R['pink'][1]], dither=0.4)
    # Feet.
    for fx in (11, 18):
        shade(cv, ellipse_mask(fx, 27.5 - up, 2.6, 1.6), ellipsoid_normal(fx, 27, 2.6, 1.6), R['imp'])
    # Body and head as one round blob.
    shade(cv, ellipse_mask(15, 17 - up, 9, 9.5), ellipsoid_normal(15, 15.5 - up, 9, 9.5), R['imp'], spec=True)
    # Horns.
    for side in (-1, 1):
        shade(cv, poly_mask([(15 + side * 4, 9 - up), (15 + side * 8, 2 - up), (15 + side * 7, 10 - up)]), flat(side * 0.3, -0.5, 0.8), R['cream'])
    # Glowing eyes and a toothy grin.
    for ex in (11, 18):
        cv.rect(ex, 13 - up, 2, 3, R['brass'][3])
        cv.set(ex + (1 if frame else 0), 14 - up, EYE)
        cv.set(ex, 13 - up, WHITE)
    for x in range(10, 21):
        cv.set(x, 19 - up, EYE)
    for x in range(11, 20):
        cv.set(x, 20 - up, (200, 50, 80))
    for x in (12, 15, 18):
        cv.set(x, 20 - up, WHITE)
    cv.set(9, 18 - up, EYE)
    cv.set(21, 18 - up, EYE)
    # Little arms: down, then thrown up.
    for side in (-1, 1):
        if frame == 0:
            shade(cv, ellipse_mask(15 + side * 9, 20, 1.8, 2.6), ellipsoid_normal(15 + side * 9, 20, 1.8, 2.6), R['imp'])
        else:
            shade(cv, ellipse_mask(15 + side * 10, 11, 1.8, 2.8), ellipsoid_normal(15 + side * 10, 11, 1.8, 2.8), R['imp'])
    return cv


HELPERS = {
    'BROOM': broom, 'SPONGE': sponge, 'CLOTH': cloth, 'BUCKET': bucket, 'DUSTER': duster, 'SACK': sack,
    'LADLE': ladle, 'TEAPOT': teapot, 'PILLOW': pillow, 'BASKET': basket, 'IRON': iron, 'CAN': can,
    'CANDLE': candle, 'BOOK': book, 'KEY': key, 'BOWL': bowl, 'SATCHEL': satchel,
}

if __name__ == '__main__':
    import sys
    items = []
    for k, f in HELPERS.items():
        items.append((k, f()))
    items.append(('IMP0', imp(0)))
    items.append(('IMP1', imp(1)))
    sheet(items, sys.argv[1], scale=5, cols=7)
