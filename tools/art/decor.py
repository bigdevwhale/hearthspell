"""The 20 decor items, the master's portrait and the apprentice."""
import math
from pixlib import *

R = RAMPS


def flame(cv, x, y, s=1.0, frame=0):
    """A little candle flame with its glow; (x, y) is the wick top."""
    h = 5 * s
    shade(cv, poly_mask([(x + 0.5, y - h - frame * 0.6), (x + 2.5 * s, y - 1), (x + 0.5, y + 1), (x - 1.5 * s, y - 1)]), flat(), R['fire'][2:3], outline=False)
    shade(cv, poly_mask([(x + 0.5, y - h * 0.6), (x + 1.4 * s, y - 0.5), (x + 0.5, y + 0.6), (x - 0.4 * s, y - 0.5)]), flat(), R['fire'][4:5], outline=False)


def candles(frame=0):
    cv = Canvas(26, 26)
    for (x, h, w) in ((3, 14, 6), (11, 20, 6), (19, 11, 5)):
        cylinder(cv, x, 25 - h, w, h, R['cream'])
        cv.set(x + w // 2, 25 - h - 1, R['ink'][1])
        flame(cv, x + w // 2 - 0.5, 25 - h - 2, 1.0, frame)
    return cv


def herbs():
    cv = Canvas(30, 24)
    shade(cv, rect_mask(0, 0, 30, 3), cyl_normal(15, 15, -0.3), R['wood'])
    for i, (x, ramp) in enumerate(((5, R['leaf']), (15, R['green']), (24, R['leaf']))):
        cv.vline(x, 3, 6, R['cream'][1])
        for k in range(12):
            a = (k / 12) * math.pi
            lx = x + math.cos(a) * (2 + k % 3) * (1 if k % 2 else -1)
            ly = 7 + k * 1.2
            shade(cv, ellipse_mask(lx, ly, 2.2, 1.3), ellipsoid_normal(lx, ly, 2.2, 1.3), ramp)
        if i == 1:
            for k in range(4):
                cv.set(x - 2 + k, 18 + k % 2, R['purple'][3])
    return cv


def lantern(frame=0):
    cv = Canvas(18, 28)
    cv.vline(9, 0, 3, R['ink'][2])
    shade(cv, ellipse_mask(9, 4.5, 3, 2), ellipsoid_normal(9, 4.5, 3, 2), R['brass'])
    shade(cv, poly_mask([(3, 7), (15, 7), (13, 10), (5, 10)]), flat(0, -1, 0.5), R['ink'])
    shade(cv, rect_mask(4, 10, 10, 13), flat(), R['glass'][1:] + [R['fire'][3]], outline=False, dither=0.3)
    glow(cv, 9, 17, 6, R['fire'][3], 200, 4)
    flame(cv, 8.5, 18, 1.2, frame)
    for x in (4, 9, 13):
        cv.vline(x, 10, 22, R['ink'][1])
    shade(cv, rect_mask(3, 22, 12, 3), flat(0, -0.5, 1), R['ink'])
    shade(cv, rect_mask(5, 25, 8, 2), flat(), R['brass'])
    return cv


def fern():
    cv = Canvas(28, 30)
    for k in range(9):
        a = math.radians(200 + k * 17.5)
        for t in range(13):
            x = 14 + math.cos(a) * t
            y = 18 + math.sin(a) * t + (t * t) / 30
            ramp = R['green'] if k % 2 else R['leaf']
            cv.set(x, y, ramp[1])
            if t % 2 == 0 and t > 2:
                cv.set(x + 1, y - 1, ramp[3])
                cv.set(x - 1, y - 1, ramp[2])
    shade(cv, poly_mask([(6, 19), (22, 19), (20, 29), (8, 29)]), lambda x, y: ((x - 14) / 9, 0.1, 0.9), R['red'][:4] + [R['orange'][3]])
    shade(cv, rect_mask(5, 18, 18, 3), cyl_normal(14, 9), R['orange'])
    return cv


def books():
    cv = Canvas(30, 24)
    spines = [(R['red'], 5, 20), (R['blue'], 4, 23), (R['green'], 5, 18), (R['purple'], 4, 22), (R['brass'], 3, 16), (R['teal'], 5, 21)]
    x = 1
    for ramp, w, h in spines:
        shade(cv, rect_mask(x, 24 - h, w, h), cyl_normal(x + w / 2, w / 2), ramp)
        cv.hline(x + 1, x + w - 2, 24 - h + 3, R['brass'][3])
        cv.hline(x + 1, x + w - 2, 24 - 4, R['brass'][2])
        x += w
    # One leaning book.
    shade(cv, poly_mask([(27, 24), (30, 24), (30, 6), (29, 6)]), flat(0.6, 0, 0.8), R['wood'])
    return cv


def mushrooms(frame=0):
    cv = Canvas(28, 24)
    for (x, y, r, h) in ((8, 9, 6, 13), (19, 14, 4.5, 9), (24, 18, 2.6, 5)):
        cylinder(cv, x - 1.5, y, 3, h, R['cream'])
        shade(cv, lambda a, b, x=x, y=y, r=r: ellipse_mask(x, y, r, r * 0.75)(a, b) and b <= y + 1, ellipsoid_normal(x, y, r, r * 0.75), R['mush'], spec=True)
        glow(cv, x, y, r * 1.6, R['mush'][3], 60 + frame * 30, 3)
        cv.set(x - 2, y - 2, WHITE)
        cv.set(x + 1, y - 1, R['mush'][3])
    for x in range(0, 28):
        cv.set(x, 23, R['leaf'][2])
        if x % 3 == 0:
            cv.set(x, 22, R['leaf'][3])
    return cv


def rug():
    cv = Canvas(36, 12)
    shade(cv, ellipse_mask(18, 6, 17.5, 5.5), flat(0, -0.6, 0.8), R['red'], dither=0.2)
    shade(cv, ellipse_mask(18, 6, 13, 3.8), flat(0, -0.6, 0.8), R['brass'][1:4], outline=False, dither=0.2)
    shade(cv, ellipse_mask(18, 6, 8, 2.2), flat(0, -0.6, 0.8), R['red'][1:4], outline=False, dither=0.2)
    for x in range(4, 33, 4):
        cv.set(x, 6, R['cream'][3])
    return cv


def potions(frame=0):
    cv = Canvas(32, 28)
    shade(cv, rect_mask(0, 22, 32, 4), flat(0, -0.6, 0.8), R['wood'])
    shade(cv, rect_mask(2, 26, 3, 2), flat(), R['wood'])
    shade(cv, rect_mask(27, 26, 3, 2), flat(), R['wood'])
    # A round red flask, a tall blue one, a green vial and a little purple one.
    shade(cv, ellipse_mask(6, 17, 5, 5), ellipsoid_normal(6, 16, 5, 5), R['red'], spec=True)
    shade(cv, rect_mask(5, 8, 3, 5), cyl_normal(6.5, 1.5), R['glass'])
    shade(cv, rect_mask(4, 7, 5, 2), flat(), R['wood'])
    shade(cv, poly_mask([(13, 22), (21, 22), (19, 11), (15, 11)]), lambda x, y: ((x - 17) / 4, 0, 0.9), R['blue'], spec=True)
    shade(cv, rect_mask(15, 4, 4, 8), cyl_normal(17, 2), R['glass'])
    shade(cv, rect_mask(14, 2, 6, 3), flat(), R['brass'])
    shade(cv, rect_mask(23, 11, 4, 11), cyl_normal(25, 2), R['green'], spec=True)
    shade(cv, rect_mask(23, 9, 4, 2), flat(), R['wood'])
    shade(cv, ellipse_mask(29.5, 19, 2.6, 3), ellipsoid_normal(29.5, 19, 2.6, 3), R['purple'], spec=True)
    for (x, y) in ((17, 14 - frame), (6, 13 + frame)):
        cv.set(x, y, WHITE)
    return cv


def banner():
    cv = Canvas(22, 30)
    shade(cv, rect_mask(0, 0, 22, 3), cyl_normal(11, 11, -0.3), R['wood'])
    pts = [(2, 3), (20, 3), (20, 29), (11, 23), (2, 29)]
    shade(cv, poly_mask(pts), lambda x, y: (math.sin(x / 3) * 0.4, 0, 0.9), R['blue'])
    # A golden star and crescent.
    for (x, y) in ((11, 8), (10, 9), (11, 9), (12, 9), (9, 10), (10, 10), (11, 10), (12, 10), (13, 10), (10, 11), (12, 11), (9, 12), (13, 12)):
        cv.set(x, y, R['brass'][3])
    for t in range(40, 320, 20):
        a = math.radians(t)
        cv.set(11 + math.cos(a) * 4, 17 + math.sin(a) * 3.4, R['cream'][3])
    for x in range(2, 21):
        cv.set(x, 4, R['brass'][2])
    return cv


def painting():
    cv = Canvas(26, 30)
    shade(cv, rect_mask(0, 0, 26, 30), flat(), R['brass'], spec=False)
    shade(cv, rect_mask(3, 3, 20, 24), lambda x, y: (0, (y - 15) / 30, 1), R['night'][1:5], outline=False)
    # The master in miniature: hat, face, beard.
    shade(cv, poly_mask([(13, 4), (19, 13), (7, 13)]), flat(0.2, -0.3, 0.9), R['blue'])
    shade(cv, ellipse_mask(13, 15, 4, 3.4), ellipsoid_normal(13, 15, 4, 3.4), R['skin'], outline=False)
    shade(cv, poly_mask([(8, 16), (18, 16), (16, 26), (13, 27), (10, 26)]), lambda x, y: (0, 0.2, 1), R['cream'], outline=False)
    cv.set(11, 14, EYE)
    cv.set(15, 14, EYE)
    shade(cv, rect_mask(3, 25, 20, 2), flat(), R['blue'][1:], outline=False)
    return cv


def globe():
    cv = Canvas(24, 30)
    shade(cv, ellipse_mask(12, 11, 10, 10), ellipsoid_normal(12, 10, 10, 10), R['water'], spec=True)
    for (x, y, r) in ((9, 8, 3.5), (15, 13, 3), (8, 15, 2), (16, 6, 2)):
        shade(cv, ellipse_mask(x, y, r, r * 0.8), ellipsoid_normal(12, 10, 10, 10), R['leaf'], outline=False)
    for t in range(-60, 241, 10):
        a = math.radians(t)
        cv.set(12 + math.cos(a) * 11.5, 11 + math.sin(a) * 11.5, R['brass'][2])
    shade(cv, rect_mask(11, 22, 3, 4), flat(), R['brass'])
    shade(cv, ellipse_mask(12, 27, 7, 2.6), ellipsoid_normal(12, 26, 7, 2.6), R['wood'])
    return cv


def hourglass(frame=0):
    cv = Canvas(20, 28)
    shade(cv, rect_mask(1, 0, 18, 3), flat(0, -1, 0.5), R['wood'])
    shade(cv, rect_mask(1, 25, 18, 3), flat(0, -1, 0.5), R['wood'])
    for x in (2, 17):
        shade(cv, rect_mask(x, 3, 1, 22), flat(), R['wood'][1:])
    shade(cv, poly_mask([(4, 3), (16, 3), (11, 14), (9, 14), (4, 3)]), flat(), R['glass'], outline=True, dither=0.2)
    shade(cv, poly_mask([(9, 14), (11, 14), (16, 25), (4, 25)]), flat(), R['glass'], outline=True, dither=0.2)
    shade(cv, poly_mask([(6, 7), (14, 7), (11, 13), (9, 13)]), flat(), R['brass'][2:4], outline=False)
    shade(cv, poly_mask([(10, 18), (15, 25), (5, 25)]), flat(), R['brass'][2:4], outline=False)
    cv.vline(10, 13, 18 + frame, R['brass'][3])
    return cv


def clock(frame=0):
    cv = Canvas(24, 36)
    shade(cv, poly_mask([(12, 0), (23, 9), (1, 9)]), lambda x, y: ((x - 12) / 12, -0.4, 0.8), R['wood'])
    shade(cv, rect_mask(2, 9, 20, 18), lambda x, y: ((x - 12) / 20, 0, 0.9), R['wood'])
    shade(cv, ellipse_mask(12, 17, 6.5, 6.5), ellipsoid_normal(12, 17, 6.5, 6.5), R['cream'])
    cv.line(12, 17, 12, 13, EYE)
    cv.line(12, 17, 15, 18, EYE)
    # The cuckoo's door, and the bird popping out on frame 1.
    shade(cv, rect_mask(10, 3, 4, 4), flat(), R['wood'][:3])
    if frame:
        shade(cv, ellipse_mask(12, 4, 2.6, 2), ellipsoid_normal(12, 4, 2.6, 2), R['orange'])
        cv.set(9, 4, R['brass'][3])
    # Pendulum and weights.
    sw = -2 if frame else 2
    cv.line(12, 27, 12 + sw, 33, R['brass'][1])
    shade(cv, ellipse_mask(12 + sw, 33, 2.2, 2.2), ellipsoid_normal(12 + sw, 33, 2.2, 2.2), R['brass'], spec=True)
    for x in (7, 17):
        cv.vline(x, 27, 30, R['ink'][1])
        shade(cv, rect_mask(x - 1, 30, 3, 5), cyl_normal(x + 0.5, 1.5), R['brass'])
    return cv


def chest():
    cv = Canvas(32, 24)
    shade(cv, lambda x, y: ellipse_mask(16, 9, 15, 7)(x, y) and y <= 9, lambda x, y: ellipsoid_normal(16, 9, 15, 7)(x, y), R['wood'])
    shade(cv, rect_mask(1, 9, 30, 14), lambda x, y: ((x - 16) / 30, 0.1, 0.9), R['wood'])
    for x0 in (5, 25):
        shade(cv, rect_mask(x0, 3, 3, 20), cyl_normal(x0 + 1.5, 1.5), R['brass'])
    shade(cv, rect_mask(1, 9, 30, 2), flat(0, -0.5, 1), R['brass'])
    shade(cv, rect_mask(13, 10, 6, 7), flat(), R['brass'], spec=True)
    cv.rect(15, 13, 2, 2, EYE)
    return cv


def cauldron(frame=0):
    cv = Canvas(32, 30)
    # Fire under it.
    for i, x in enumerate(range(7, 26, 4)):
        h = 4 + (i + frame) % 3
        shade(cv, poly_mask([(x, 29), (x + 2, 29 - h), (x + 4, 29)]), flat(), R['fire'][2 + (i % 2):3 + (i % 2)], outline=False)
    shade(cv, rect_mask(4, 28, 24, 2), flat(), R['wood'][:3])
    for x in (7, 23):
        shade(cv, rect_mask(x, 22, 3, 5), flat(), R['ink'])
    shade(cv, lambda x, y: ellipse_mask(16, 13, 14, 12)(x, y) and y >= 8, ellipsoid_normal(16, 12, 14, 12), R['ink'] + [R['stone'][3]], spec=True)
    shade(cv, ellipse_mask(16, 8.5, 14, 3), flat(0, -1, 0.4), R['ink'])
    shade(cv, ellipse_mask(16, 8.5, 11.5, 2), flat(0, -1, 0.3), R['green'][1:], outline=False)
    # Bubbles.
    for (x, y, r) in ((10 + frame, 6, 1.6), (20, 5 - frame, 2), (15, 3 + frame, 1.2)):
        shade(cv, ellipse_mask(x, y, r, r), ellipsoid_normal(x, y, r, r), R['green'][2:])
    return cv


def crystal(frame=0):
    cv = Canvas(26, 30)
    glow(cv, 13, 11, 12, R['purple'][4], 30 + frame * 25, 4)
    shade(cv, ellipse_mask(13, 11, 10, 10), ellipsoid_normal(13, 10, 10, 10), R['purple'][1:] + [WHITE], spec=True, dither=0.6)
    for (x, y) in ((10, 9 + frame), (16, 13), (12, 15 - frame), (15, 7)):
        cv.set(x, y, R['glass'][3])
    shade(cv, poly_mask([(5, 21), (21, 21), (24, 28), (2, 28)]), lambda x, y: ((x - 13) / 12, -0.2, 0.8), R['brass'])
    return cv


def cat(frame=0):
    cv = Canvas(32, 26)
    # Tail curls up and sways.
    for i in range(14):
        a = i / 13 * math.pi * 0.9
        x = 26 + math.sin(a) * 4 + (frame and i > 8)
        y = 22 - i
        shade(cv, ellipse_mask(x, y, 1.6, 1.6), flat(0.3, -0.3, 0.9), R['orange'], outline=False)
    shade(cv, ellipse_mask(18, 18, 10, 7), ellipsoid_normal(18, 17, 10, 7), R['orange'], dither=0.5)
    shade(cv, ellipse_mask(9, 12, 7, 6), ellipsoid_normal(9, 11, 7, 6), R['orange'], spec=False)
    for ex in (4, 13):
        shade(cv, poly_mask([(ex - 2, 8), (ex, 2), (ex + 2.5, 7)]), flat(0, -0.5, 0.8), R['orange'])
        cv.set(ex, 6, R['pink'][2])
    # Stripes.
    for (x, y) in ((16, 12), (17, 13), (21, 12), (22, 13), (25, 15), (8, 7)):
        cv.set(x, y, R['orange'][1])
    if frame:
        cv.hline(5, 6, 11, EYE)
        cv.hline(11, 12, 11, EYE)
    else:
        for ex in (5, 11):
            cv.rect(ex, 10, 2, 2, (60, 140, 60))
            cv.set(ex, 10, WHITE)
    cv.set(8, 13, R['pink'][2])
    cv.set(9, 13, R['pink'][2])
    cv.set(7, 14, EYE)
    cv.set(10, 14, EYE)
    for x in (11, 15, 22):
        shade(cv, ellipse_mask(x, 24, 2, 1.4), flat(), R['cream'])
    return cv


def telescope():
    cv = Canvas(34, 30)
    # Tripod.
    for (x1, y1) in ((8, 29), (18, 29), (26, 29)):
        cv.line(17, 14, x1, y1, R['wood'][1])
        cv.line(17 + (1 if x1 > 17 else 0), 14, x1 + 1, y1, R['wood'][3])
    # The tube in three sections, pointing at the sky.
    sections = [((3, 22), (13, 15), 3.0, R['brass']), ((12, 16), (23, 8), 2.4, R['blue']), ((22, 9), (31, 2), 1.8, R['brass'])]
    for (a, b, r, ramp) in sections:
        dx, dy = b[0] - a[0], b[1] - a[1]
        ln = math.hypot(dx, dy)
        nx, ny = -dy / ln, dx / ln
        pts = [(a[0] + nx * r, a[1] + ny * r), (b[0] + nx * r, b[1] + ny * r), (b[0] - nx * r, b[1] - ny * r), (a[0] - nx * r, a[1] - ny * r)]
        shade(cv, poly_mask(pts), lambda x, y, nx=nx, ny=ny, a=a, r=r: (nx * ((x - a[0]) * nx + (y - a[1]) * ny) / r, ny * ((x - a[0]) * nx + (y - a[1]) * ny) / r, 0.7), ramp, spec=True)
    shade(cv, ellipse_mask(2.5, 23, 2, 3), flat(-1, 0, 0.3), R['glass'])
    return cv


def owl(frame=0):
    cv = Canvas(24, 30)
    shade(cv, rect_mask(0, 26, 24, 4), cyl_normal(12, 12, -0.4), R['wood'])
    shade(cv, ellipse_mask(12, 16, 10, 11), ellipsoid_normal(12, 15, 10, 11), R['wood'], dither=0.5)
    shade(cv, ellipse_mask(12, 19, 6.5, 7), ellipsoid_normal(12, 19, 6.5, 7), R['cream'], outline=False)
    for y in range(15, 25, 3):
        for x in range(8, 17, 3):
            cv.set(x + (y % 2), y, R['wood'][3])
    for ex in (7.5, 16.5):
        shade(cv, ellipse_mask(ex, 11, 4, 4), ellipsoid_normal(ex, 11, 4, 4), R['cream'])
        if frame:
            cv.hline(int(ex) - 2, int(ex) + 1, 11, EYE)
        else:
            shade(cv, ellipse_mask(ex, 11, 2.2, 2.2), flat(), R['brass'][2:4], outline=False)
            cv.rect(int(ex) - 1, 10, 2, 2, EYE)
            cv.set(int(ex) - 1, 10, WHITE)
    for side in (-1, 1):
        shade(cv, poly_mask([(12 + side * 5, 6), (12 + side * 9, 0), (12 + side * 8, 7)]), flat(), R['wood'])
    shade(cv, poly_mask([(11, 13), (13, 13), (12, 16)]), flat(), R['brass'], outline=False)
    for x in (9, 14):
        cv.hline(x, x + 2, 27, R['brass'][2])
    return cv


def fireplace(frame=0):
    cv = Canvas(36, 30)
    shade(cv, rect_mask(0, 0, 36, 5), flat(0, -0.5, 1), R['wood'])
    for (x, w) in ((1, 7), (28, 7)):
        S = shade(cv, rect_mask(x, 5, w, 25), lambda a, b: ((a - x - w / 2) / w, 0, 1), R['stone'])
        for y in range(7, 30, 4):
            cv.hline(x + 1, x + w - 2, y, R['stone'][1])
            cv.set(x + (3 if (y // 4) % 2 else 1), y + 2, R['stone'][1])
    shade(cv, rect_mask(8, 5, 20, 4), flat(), R['stone'])
    shade(cv, rect_mask(8, 9, 20, 21), flat(), R['ink'][:2], outline=False, dither=0.3)
    glow(cv, 18, 26, 12, R['fire'][3], 120, 5)
    shade(cv, rect_mask(10, 27, 16, 3), cyl_normal(18, 8), R['wood'])
    for i, x in enumerate(range(10, 25, 3)):
        h = 7 + ((i + frame) % 3) * 2
        shade(cv, poly_mask([(x, 27), (x + 1.6, 27 - h), (x + 3.4, 27)]), flat(), R['fire'][1:2], outline=False)
        shade(cv, poly_mask([(x + 0.6, 27), (x + 1.6, 27 - h * 0.6), (x + 2.8, 27)]), flat(), R['fire'][3:4], outline=False)
    return cv


def master(frame=0):
    """The old master's portrait for his letters, ~44x52."""
    cv = Canvas(44, 52)
    # Robe with stars.
    shade(cv, poly_mask([(10, 30), (34, 30), (42, 52), (2, 52)]), lambda x, y: ((x - 22) / 30, 0.1, 0.9), R['blue'], dither=0.5)
    for (x, y) in ((8, 46), (35, 44), (14, 40), (30, 49), (38, 50)):
        cv.set(x, y, R['brass'][3])
        cv.set(x + 1, y, R['brass'][2])
    # Hands holding a staff.
    shade(cv, rect_mask(36, 14, 3, 38), cyl_normal(37.5, 1.5), R['wood'])
    shade(cv, ellipse_mask(37.5, 12, 3.4, 3.4), ellipsoid_normal(37.5, 11, 3.4, 3.4), R['teal'], spec=True)
    glow(cv, 37.5, 12, 7, R['teal'][4], 60, 3)
    shade(cv, ellipse_mask(36, 35, 3, 2.6), ellipsoid_normal(36, 35, 3, 2.6), R['skin'])
    # Face.
    shade(cv, ellipse_mask(22, 22, 8, 8), ellipsoid_normal(22, 21, 8, 8), R['skin'][1:], oc=R['skin'][0])
    # Beard: long, wavy, white.
    shade(cv, poly_mask([(13, 23), (31, 23), (30, 36), (26, 46), (22, 49), (18, 46), (14, 36)]), lambda x, y: ((x - 22) / 18, (y - 30) / 40, 0.9), R['silver'][1:] + [WHITE], dither=0.6)
    for y in range(28, 46, 3):
        cv.set(20 + (y % 4), y, R['silver'][2])
        cv.set(24 - (y % 3), y + 1, R['silver'][2])
    # Moustache and nose.
    shade(cv, ellipse_mask(22, 25, 5.5, 1.6), flat(0, -0.4, 1), R['silver'][2:] + [WHITE])
    shade(cv, ellipse_mask(22, 22.5, 1.8, 1.6), ellipsoid_normal(22, 22, 1.8, 1.6), R['skin'][1:], outline=False)
    # Eyebrows and kind eyes.
    for ex in (17, 25):
        cv.hline(ex - 1, ex + 2, 17, R['silver'][4])
        if frame:
            cv.hline(ex, ex + 1, 19, EYE)
        else:
            cv.rect(ex, 18, 2, 2, EYE)
            cv.set(ex, 18, WHITE)
    cv.set(15, 22, BLUSH, 150)
    cv.set(29, 22, BLUSH, 150)
    # The tall pointed hat with a brim, a little crooked.
    shade(cv, poly_mask([(13, 16), (31, 16), (26, 4), (30, 0), (22, 3)]), lambda x, y: ((x - 22) / 14, -0.3, 0.9), R['blue'], spec=True)
    shade(cv, ellipse_mask(22, 16, 14, 2.6), flat(0, -1, 0.4), R['blue'])
    for (x, y) in ((20, 10), (24, 7), (19, 14)):
        cv.set(x, y, R['brass'][3])
    cv.hline(14, 30, 14, R['brass'][2])
    return cv


def apprentice(frame=0):
    """The apprentice: a young wizard in a red robe, ~26x34."""
    cv = Canvas(26, 34)
    up = frame
    shade(cv, poly_mask([(7, 18 - up), (19, 18 - up), (23, 33), (3, 33)]), lambda x, y: ((x - 13) / 20, 0.1, 0.9), R['red'], dither=0.5)
    shade(cv, rect_mask(5, 25, 16, 2), flat(), R['brass'], outline=False)
    shade(cv, ellipse_mask(13, 13 - up, 6, 6), ellipsoid_normal(13, 12 - up, 6, 6), R['skin'][1:], oc=R['skin'][0])
    # Hair peeking out.
    # Hair: a fringe and two locks by the ears.
    for (x, y) in ((8, 10), (9, 10), (17, 10), (18, 10), (7, 11), (7, 12), (19, 11), (19, 12), (7, 13), (19, 13)):
        cv.set(x, y - up, R['wood'][2])
    shade(cv, poly_mask([(5, 9 - up), (21, 9 - up), (15, 1 - up), (19, -1 - up), (12, 0 - up)]), lambda x, y: ((x - 13) / 10, -0.3, 0.9), R['red'], spec=True)
    shade(cv, ellipse_mask(13, 9 - up, 9, 2), flat(0, -1, 0.4), R['red'])
    eyes(cv, 9, 15, 12 - up, big=False)
    smile(cv, 12, 16 - up, 3)
    # Hands and a little wand.
    shade(cv, ellipse_mask(5, 24, 2, 2), ellipsoid_normal(5, 24, 2, 2), R['skin'])
    shade(cv, ellipse_mask(21, 23 - up, 2, 2), ellipsoid_normal(21, 23, 2, 2), R['skin'])
    cv.line(21, 22 - up, 24, 17 - up, R['wood'][3])
    cv.set(24, 16 - up, R['brass'][4])
    cv.set(25, 15 - up, WHITE)
    for x in (9, 16):
        shade(cv, rect_mask(x - 1, 32, 4, 2), flat(), R['wood'][:3])
    return cv


DECOR = {
    'CANDLES': candles, 'HERBS': herbs, 'LANTERN': lantern, 'FERN': fern, 'BOOKS': books, 'MUSHROOMS': mushrooms,
    'RUG': rug, 'POTIONS': potions, 'BANNER': banner, 'PAINTING': painting, 'GLOBE': globe, 'HOURGLASS': hourglass,
    'CLOCK': clock, 'CHEST': chest, 'CAULDRON': cauldron, 'CRYSTAL': crystal, 'CAT': cat, 'TELESCOPE': telescope,
    'OWL': owl, 'FIREPLACE': fireplace,
}

if __name__ == '__main__':
    import sys
    items = [(k, f()) for k, f in DECOR.items()]
    items += [('MASTER', master()), ('MASTER1', master(1)), ('APPR', apprentice()), ('APPR1', apprentice(1))]
    sheet(items, sys.argv[1], scale=4, cols=8)
