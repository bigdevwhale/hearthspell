"""Art added in 1.2: trinkets the helpers bring, souvenirs from the master's letters, seasonal decor, the tower cat,
the apprentice's wardrobe, the tea table, a bat, a heart and the outside wall the home-screen widget is built from."""
import math
from pixlib import *
import scenes as Sc

R = RAMPS


# ---------------------------------------------------------------- trinkets: small finds, 14x14

def _tr():
    return Canvas(14, 14)


def t_button(gold=False):
    cv = _tr()
    ramp = R['brass'] if gold else R['red']
    shade(cv, ellipse_mask(7, 7, 6, 6), ellipsoid_normal(7, 6, 6, 6), ramp, spec=True)
    shade(cv, ellipse_mask(7, 7, 3.6, 3.6), flat(0, 0.4, 0.8), ramp[1:4], outline=False)
    for (x, y) in ((5, 5), (8, 5), (5, 8), (8, 8)):
        cv.set(x, y, ramp[0])
    if gold:
        cv.set(3, 3, WHITE)
        cv.set(11, 10, R['brass'][4])
    return cv


def t_feather():
    cv = _tr()
    for i in range(12):
        x, y = 2 + i * 0.85, 12 - i * 0.95
        w = 2.4 * math.sin(i / 11 * math.pi) + 0.6
        shade(cv, ellipse_mask(x + 0.8, y - 0.6, w, 1.1), flat(-0.3, -0.4, 0.9), R['blue'][1:] + [WHITE], outline=False, dither=0.4)
    cv.line(1, 13, 11, 2, R['cream'][3])
    cv.set(10, 3, R['teal'][3])
    cv.set(9, 4, R['brass'][3])
    return cv


def t_acorn():
    cv = _tr()
    shade(cv, ellipse_mask(7, 8.5, 4.4, 4.6), ellipsoid_normal(7, 8, 4.4, 4.6), R['orange'], spec=True)
    shade(cv, lambda x, y: ellipse_mask(7, 5.5, 5.4, 3.4)(x, y) and y <= 6, ellipsoid_normal(7, 5, 5.4, 3.4), R['wood'])
    for x in range(3, 12, 2):
        cv.set(x, 5, R['wood'][1])
    cv.vline(7, 1, 2, R['wood'][1])
    cv.set(8, 1, R['leaf'][3])
    return cv


def t_pebble():
    cv = _tr()
    shade(cv, ellipse_mask(7, 8, 6, 4.4), ellipsoid_normal(7, 7, 6, 4.4), R['teal'], spec=True, dither=0.6)
    cv.hline(4, 9, 8, R['teal'][4])
    cv.set(3, 6, WHITE)
    return cv


def t_thimble():
    cv = _tr()
    shade(cv, poly_mask([(3, 13), (11, 13), (10, 4), (4, 4)]), lambda x, y: ((x - 7) / 5, 0, 0.9), R['silver'], spec=True)
    shade(cv, ellipse_mask(7, 4, 3.2, 2), flat(0, -1, 0.5), R['silver'])
    for y in range(6, 12, 2):
        for x in range(5, 10, 2):
            cv.set(x + (y // 2) % 2, y, R['silver'][1])
    return cv


def t_bead():
    cv = _tr()
    shade(cv, ellipse_mask(7, 7, 5.5, 5.5), ellipsoid_normal(7, 6, 5.5, 5.5), R['purple'][1:] + [R['pink'][3]], spec=True)
    cv.rect(6, 6, 2, 2, R['purple'][0])
    return cv


def t_marble():
    cv = _tr()
    shade(cv, ellipse_mask(7, 7, 5.6, 5.6), ellipsoid_normal(7, 6, 5.6, 5.6), R['glass'] + [WHITE], spec=True, dither=0.3)
    for t in range(0, 300, 30):
        a = math.radians(t)
        cv.set(7 + math.cos(a) * (1 + t / 120), 7 + math.sin(a) * (1 + t / 120), R['orange'][3])
    return cv


def t_shell():
    cv = _tr()
    shade(cv, poly_mask([(7, 2), (13, 9), (11, 12), (3, 12), (1, 9)]), lambda x, y: ((x - 7) / 8, (y - 7) / 10, 0.9), R['pink'], spec=True)
    for k in range(-2, 3):
        cv.line(7, 3, 7 + k * 2, 11, R['pink'][1])
    shade(cv, rect_mask(5, 11, 4, 2), flat(), R['pink'][:3])
    return cv


def t_ribbon():
    cv = _tr()
    for side in (-1, 1):
        shade(cv, poly_mask([(7, 6), (7 + side * 6, 2), (7 + side * 6, 9)]), flat(side * 0.3, -0.3, 0.9), R['red'])
        shade(cv, poly_mask([(7, 7), (7 + side * 3, 13), (7 + side * 1, 13)]), flat(side * 0.3, 0.2, 0.9), R['red'])
    shade(cv, ellipse_mask(7, 6.5, 1.8, 1.8), ellipsoid_normal(7, 6.5, 1.8, 1.8), R['red'][1:] + [R['orange'][4]])
    return cv


def t_cork():
    cv = _tr()
    shade(cv, poly_mask([(3, 4), (11, 4), (10, 12), (4, 12)]), lambda x, y: ((x - 7) / 6, 0, 0.9), R['wood'][2:] + [R['cream'][3]], dither=0.6)
    shade(cv, ellipse_mask(7, 4, 4, 1.6), flat(0, -1, 0.4), R['wood'][2:])
    for (x, y) in ((5, 7), (8, 9), (6, 10), (9, 6)):
        cv.set(x, y, R['wood'][1])
    return cv


def t_spool():
    cv = _tr()
    shade(cv, rect_mask(2, 1, 10, 2), flat(0, -1, 0.5), R['wood'])
    shade(cv, rect_mask(2, 11, 10, 2), flat(0, -1, 0.5), R['wood'])
    shade(cv, rect_mask(3, 3, 8, 8), cyl_normal(7, 4), R['green'], dither=0.4)
    for y in range(4, 11, 2):
        cv.hline(4, 9, y, R['green'][1])
    cv.line(11, 7, 13, 12, R['green'][3])
    return cv


def t_leaf():
    cv = _tr()
    shade(cv, poly_mask([(1, 12), (4, 4), (12, 1), (11, 9)]), lambda x, y: ((x - 7) / 9, (y - 7) / 9, 0.9), R['brass'], spec=True)
    cv.line(2, 12, 10, 3, R['brass'][1])
    for k in range(3):
        cv.line(4 + k * 2, 10 - k * 2, 7 + k * 2, 9 - k * 2, R['brass'][1])
    return cv


def t_star():
    cv = _tr()
    pts = []
    for i in range(10):
        a = math.radians(-90 + i * 36)
        r = 6.4 if i % 2 == 0 else 2.8
        pts.append((7 + math.cos(a) * r, 7.5 + math.sin(a) * r))
    shade(cv, poly_mask(pts), lambda x, y: ((x - 7) / 8, (y - 7) / 8, 0.9), R['brass'][1:] + [WHITE], spec=True)
    glow(cv, 7, 7.5, 7, R['brass'][4], 60, 3)
    cv.set(6, 6, WHITE)
    return cv


def t_clover():
    cv = _tr()
    for (dx, dy) in ((-2.4, -2.4), (2.4, -2.4), (-2.4, 2.4), (2.4, 2.4)):
        shade(cv, ellipse_mask(7 + dx, 6 + dy, 2.8, 2.8), ellipsoid_normal(7 + dx, 6 + dy, 2.8, 2.8), R['green'], spec=True)
    cv.line(7, 7, 10, 13, R['leaf'][1])
    cv.set(7, 6, R['green'][4])
    return cv


def t_key():
    cv = _tr()
    shade(cv, ellipse_mask(4, 4.5, 3.4, 3.4), ellipsoid_normal(4, 4, 3.4, 3.4), R['brass'], spec=True)
    cv.rect(3, 3, 2, 2, (0, 0, 0, 0))
    cv.px[3][3] = None; cv.px[3][4] = None; cv.px[4][3] = None; cv.px[4][4] = None
    shade(cv, poly_mask([(6, 6), (8, 5), (13, 11), (11, 12)]), flat(0.4, -0.4, 0.8), R['brass'])
    shade(cv, rect_mask(9, 10, 2, 3), flat(), R['brass'])
    return cv


def t_sock():
    cv = _tr()
    shade(cv, poly_mask([(4, 1), (9, 1), (9, 8), (12, 10), (11, 13), (5, 13), (4, 10)]), lambda x, y: ((x - 7) / 8, 0, 0.9), R['blue'], dither=0.4)
    for y in (3, 6):
        cv.hline(4, 8, y, R['red'][3])
    shade(cv, rect_mask(4, 1, 6, 2), flat(), R['cream'][2:])
    cv.set(8, 11, R['ink'][1])
    return cv


TRINKETS = {
    'BUTTON': t_button, 'FEATHER': t_feather, 'ACORN': t_acorn, 'PEBBLE': t_pebble, 'THIMBLE': t_thimble,
    'BEAD': t_bead, 'MARBLE': t_marble, 'SHELL': t_shell, 'RIBBON': t_ribbon, 'CORK': t_cork, 'SPOOL': t_spool,
    'SOCK': t_sock, 'KEY': t_key, 'LEAF': t_leaf, 'CLOVER': t_clover, 'STAR': t_star, 'GOLD_BUTTON': lambda: t_button(True),
}


# ---------------------------------------------------------------- souvenirs from the letters, ~24x26

def s_teacup(frame=0):
    cv = Canvas(24, 22)
    shade(cv, ellipse_mask(12, 19.5, 10, 2.4), flat(0, -1, 0.5), R['cream'])
    shade(cv, lambda x, y: ellipse_mask(11, 10, 7.5, 8)(x, y) and y >= 9, ellipsoid_normal(11, 9, 7.5, 8), R['cream'], spec=True)
    for t in range(-80, 90, 10):
        a = math.radians(t)
        cv.set(18.5 + math.cos(a) * 3, 13 + math.sin(a) * 3, R['cream'][1])
    shade(cv, ellipse_mask(11, 9.5, 7, 1.6), flat(0, -1, 0.4), R['wood'][1:4], outline=False)
    for x in (7, 10, 13, 16):
        cv.set(x, 13, R['blue'][3])
        cv.set(x + 1, 14, R['blue'][2])
    for k in range(2):
        y = 6 - frame - k * 3
        cv.set(9 + k * 3, y, R['silver'][4], 150)
        cv.set(10 + k * 3, y - 1, R['silver'][4], 110)
    return cv


def s_frog(frame=0):
    cv = Canvas(22, 18)
    shade(cv, ellipse_mask(11, 12, 9, 5.5), ellipsoid_normal(11, 11, 9, 5.5), R['green'], spec=True)
    for ex in (6, 16):
        shade(cv, ellipse_mask(ex, 6.5, 3.2, 3.2), ellipsoid_normal(ex, 6, 3.2, 3.2), R['green'], spec=True)
        if frame:
            cv.hline(ex - 1, ex + 1, 6, EYE)
        else:
            cv.rect(ex - 1, 5, 2, 2, EYE)
            cv.set(ex - 1, 5, WHITE)
    for x in range(7, 16):
        cv.set(x, 12, R['green'][0])
    cv.set(4, 13, BLUSH, 200)
    cv.set(18, 13, BLUSH, 200)
    for fx in (4, 18):
        shade(cv, ellipse_mask(fx, 16.5, 2.6, 1.4), flat(), R['green'][1:])
    shade(cv, ellipse_mask(11, 15, 4, 1.8), flat(0, 0.3, 1), R['leaf'][3:], outline=False)
    return cv


def s_snowglobe(frame=0):
    cv = Canvas(22, 26)
    shade(cv, ellipse_mask(11, 10, 9, 9), ellipsoid_normal(11, 9, 9, 9), R['glass'] + [WHITE], spec=True, dither=0.3)
    # A tiny mountain monastery inside.
    shade(cv, poly_mask([(4, 16), (11, 6), (18, 16)]), flat(0.2, -0.5, 0.8), R['stone'][1:] + [WHITE], outline=False)
    shade(cv, rect_mask(9, 12, 5, 4), flat(), R['red'][1:4], outline=False)
    cv.set(11, 11, R['brass'][3])
    for i in range(7):
        x = 4 + (i * 5 + frame * 3) % 14
        y = 3 + (i * 7 + frame * 2) % 12
        cv.set(x, y, WHITE)
    shade(cv, poly_mask([(3, 19), (19, 19), (21, 25), (1, 25)]), lambda x, y: ((x - 11) / 12, 0, 0.9), R['wood'])
    cv.hline(3, 18, 21, R['brass'][3])
    return cv


def s_scale(frame=0):
    cv = Canvas(22, 26)
    pts = [(11, 1), (19, 7), (18, 16), (11, 21), (4, 16), (3, 7)]
    shade(cv, poly_mask(pts), lambda x, y: ((x - 11) / 10, (y - 10) / 14, 0.85), R['red'][1:] + [R['orange'][4]], spec=True)
    for k in range(3):
        cv.line(11, 3 + k * 5, 6 + k, 9 + k * 4, R['red'][1])
        cv.line(11, 3 + k * 5, 16 - k, 9 + k * 4, R['red'][1])
    if frame:
        cv.set(8, 6, WHITE)
        cv.set(14, 11, R['brass'][4])
    glow(cv, 11, 11, 10, R['orange'][3], 30 + frame * 25, 3)
    shade(cv, poly_mask([(6, 21), (16, 21), (18, 25), (4, 25)]), flat(0, -0.5, 1), R['wood'])
    return cv


def s_imp_jar(frame=0):
    cv = Canvas(22, 28)
    shade(cv, rect_mask(4, 7, 14, 19), cyl_normal(11, 7), R['glass'], dither=0.25)
    # A very small imp, waving.
    up = frame
    shade(cv, ellipse_mask(11, 18 - up, 4.4, 4.6), ellipsoid_normal(11, 17 - up, 4.4, 4.6), R['imp'], spec=True)
    for side in (-1, 1):
        cv.set(11 + side * 3, 13 - up, R['cream'][3])
        cv.set(11 + side * 3, 12 - up, R['cream'][3])
    cv.set(9, 17 - up, R['brass'][3])
    cv.set(13, 17 - up, R['brass'][3])
    cv.hline(10, 12, 20 - up, EYE)
    if frame:
        shade(cv, ellipse_mask(16, 13, 1.3, 2), flat(), R['imp'])
    else:
        shade(cv, ellipse_mask(16, 19, 1.3, 1.8), flat(), R['imp'])
    for x in range(5, 18):
        if (x + frame) % 3 == 0:
            cv.set(x, 9, WHITE, 120)
    shade(cv, rect_mask(3, 3, 16, 4), flat(0, -1, 0.6), R['wood'])
    cv.hline(3, 18, 26, R['silver'][1])
    return cv


def s_bell(frame=0):
    cv = Canvas(22, 26)
    shade(cv, rect_mask(2, 0, 18, 3), flat(0, -0.5, 1), R['wood'])
    cv.vline(11, 3, 5, R['ink'][2])
    sw = -1 if frame else 1
    shade(cv, poly_mask([(11 + sw, 5), (16 + sw, 9), (18 + sw, 20), (4 + sw, 20), (6 + sw, 9)]), lambda x, y: ((x - 11) / 9, -0.2, 0.9), R['brass'], spec=True)
    shade(cv, ellipse_mask(11 + sw, 20.5, 7.5, 1.6), flat(0, 1, 0.4), R['brass'][:3])
    shade(cv, ellipse_mask(11 + sw * 2, 22.5, 1.8, 1.8), ellipsoid_normal(11, 22.5, 1.8, 1.8), R['brass'])
    cv.hline(6 + sw, 16 + sw, 16, R['brass'][1])
    if frame:
        for (x, y) in ((1, 10), (20, 12), (2, 14)):
            cv.set(x, y, R['brass'][4])
    return cv


def s_lighthouse(frame=0):
    cv = Canvas(18, 30)
    shade(cv, poly_mask([(5, 29), (13, 29), (11, 8), (7, 8)]), lambda x, y: ((x - 9) / 6, 0, 0.9), R['cream'])
    for y in (12, 18, 24):
        for x in range(4, 15):
            if cv.get(x, y) is not None:
                cv.set(x, y, R['red'][2])
                cv.set(x, y + 1, R['red'][2])
    shade(cv, rect_mask(6, 4, 6, 4), flat(), R['glass'][1:] + [R['fire'][3]], dither=0.2)
    shade(cv, poly_mask([(5, 4), (13, 4), (9, 0)]), flat(0, -0.5, 0.8), R['red'])
    shade(cv, rect_mask(5, 8, 8, 1), flat(), R['ink'])
    if frame:
        glow(cv, 9, 6, 8, R['fire'][3], 140, 4)
        cv.rect(7, 5, 4, 2, R['fire'][4])
    else:
        cv.rect(7, 5, 4, 2, R['fire'][2])
    return cv


def s_ship(frame=0):
    cv = Canvas(30, 18)
    shade(cv, ellipse_mask(14, 9, 13, 7.5), ellipsoid_normal(14, 8, 13, 7.5), R['glass'], dither=0.25)
    shade(cv, rect_mask(26, 7, 3, 4), flat(), R['wood'][2:])
    # A ship inside, rocking.
    dy = frame
    shade(cv, poly_mask([(6, 11 + dy), (20, 11 + dy), (18, 14 + dy), (8, 14 + dy)]), flat(0, -0.3, 1), R['wood'], outline=False)
    cv.vline(13, 3 + dy, 11 + dy, R['wood'][1])
    shade(cv, poly_mask([(14, 4 + dy), (19, 10 + dy), (14, 10 + dy)]), flat(), R['cream'][2:] + [WHITE], outline=False)
    shade(cv, poly_mask([(12, 5 + dy), (12, 10 + dy), (8, 10 + dy)]), flat(), R['cream'][2:], outline=False)
    cv.set(13, 2 + dy, R['red'][3])
    for x in range(4, 24):
        cv.set(x, 14 + (x % 3 == 0), R['water'][2])
    cv.set(6, 4, WHITE)
    return cv


def s_paper_lantern(frame=0):
    cv = Canvas(20, 28)
    cv.vline(10, 0, 3, R['ink'][2])
    shade(cv, rect_mask(7, 3, 6, 2), flat(), R['ink'])
    glow(cv, 10, 14, 10, R['fire'][3], 40 + frame * 30, 3)
    shade(cv, ellipse_mask(10, 14, 8, 9), ellipsoid_normal(10, 13, 8, 9), R['red'][1:] + [R['orange'][4]], dither=0.4)
    for y in range(8, 22, 3):
        for x in range(2, 19):
            if cv.get(x, y) is not None and ellipse_mask(10, 14, 8, 9)(x, y):
                cv.set(x, y, R['red'][1])
    shade(cv, ellipse_mask(10, 14, 3.4, 5), flat(), R['fire'][3:] if frame else R['fire'][2:4], outline=False)
    shade(cv, rect_mask(7, 23, 6, 2), flat(), R['ink'])
    cv.vline(10, 25, 27, R['red'][3])
    return cv


def s_map():
    cv = Canvas(28, 22)
    shade(cv, rect_mask(2, 3, 24, 16), lambda x, y: (math.sin(x / 3) * 0.2, 0, 1), R['cream'], dither=0.5)
    for x in (1, 26):
        shade(cv, rect_mask(x, 1, 2, 20), cyl_normal(x + 1, 1), R['wood'])
    # Coast, a dotted road and an X.
    for x in range(4, 14):
        cv.set(x, 8 + int(math.sin(x / 2) * 2), R['blue'][2])
    for x in range(8, 22, 2):
        cv.set(x, 14 - (x - 8) // 3, R['red'][2])
    cv.line(19, 6, 22, 9, R['red'][1])
    cv.line(22, 6, 19, 9, R['red'][1])
    shade(cv, ellipse_mask(7, 15, 2, 1.4), flat(), R['leaf'][1:4], outline=False)
    return cv


def s_aurora(frame=0):
    cv = Canvas(20, 28)
    shade(cv, rect_mask(3, 6, 14, 20), cyl_normal(10, 7), R['night'][1:5], dither=0.3)
    cols = [R['green'][3], R['teal'][4], R['purple'][4], R['green'][4]]
    for k in range(4):
        for y in range(9, 24):
            x = 5 + k * 3 + int(math.sin((y + frame * 3 + k * 4) / 3) * 1.5)
            cv.set(x, y, cols[(k + frame) % 4], 200)
    for (x, y) in ((6, 10), (14, 21), (11, 8)):
        cv.set(x, y, WHITE)
    shade(cv, rect_mask(2, 2, 16, 4), flat(0, -1, 0.6), R['brass'])
    cv.hline(3, 16, 26, R['night'][4])
    return cv


def s_compass(frame=0):
    cv = Canvas(22, 22)
    shade(cv, ellipse_mask(11, 12, 9.5, 9.5), ellipsoid_normal(11, 11, 9.5, 9.5), R['brass'], spec=True)
    shade(cv, ellipse_mask(11, 12, 7, 7), flat(), R['cream'][2:], outline=False)
    a = math.radians(-70 + frame * 12)
    for t in range(0, 6):
        cv.set(11 + math.cos(a) * t, 12 + math.sin(a) * t, R['red'][2])
        cv.set(11 - math.cos(a) * t, 12 - math.sin(a) * t, R['ink'][1])
    cv.set(11, 12, R['brass'][4])
    for (x, y) in ((11, 6), (17, 12), (11, 18), (5, 12)):
        cv.set(x, y, R['ink'][1])
    shade(cv, ellipse_mask(11, 2, 2, 1.6), flat(), R['brass'])
    return cv


SOUVENIRS = {
    'TEACUP': s_teacup, 'FROG': s_frog, 'SNOWGLOBE': s_snowglobe, 'DRAGON_SCALE': s_scale, 'IMP_JAR': s_imp_jar,
    'BELL': s_bell, 'LIGHTHOUSE': s_lighthouse, 'SHIP': s_ship, 'PAPER_LANTERN': s_paper_lantern, 'OLD_MAP': lambda frame=0: s_map(),
    'AURORA': s_aurora, 'COMPASS': s_compass,
}
STILL_SOUVENIRS = {'OLD_MAP'}


# ---------------------------------------------------------------- seasonal decor and the cat's cushion

def pumpkin(frame=0):
    cv = Canvas(26, 22)
    for (x, r) in ((7, 6), (19, 6), (13, 7.5)):
        shade(cv, ellipse_mask(x, 13, r, 8), ellipsoid_normal(x, 12, r, 8), R['orange'], spec=x == 13, dither=0.5)
    shade(cv, rect_mask(12, 2, 3, 4), flat(0.4, -0.3, 0.8), R['leaf'][:4])
    cv.set(15, 3, R['leaf'][3])
    cv.set(16, 2, R['leaf'][2])
    lit = R['fire'][4] if frame else R['fire'][3]
    for (x, y) in ((8, 10), (9, 10), (8, 11), (17, 10), (18, 10), (18, 11)):
        cv.set(x, y, lit)
    for x in range(8, 19):
        if x % 3 != 1:
            cv.set(x, 16, lit)
        if x % 3 == 0:
            cv.set(x, 17, lit)
    glow(cv, 13, 13, 12, R['fire'][3], 30 + frame * 30, 3)
    return cv


def fir(frame=0):
    cv = Canvas(28, 36)
    shade(cv, rect_mask(12, 29, 4, 4), flat(), R['wood'])
    shade(cv, poly_mask([(6, 36), (22, 36), (20, 31), (8, 31)]), lambda x, y: ((x - 14) / 10, 0, 0.9), R['red'])
    for k, (top, half, bottom) in enumerate(((4, 6, 13), (9, 9, 21), (15, 12, 30))):
        shade(cv, poly_mask([(14, top), (14 + half, bottom), (14 - half, bottom)]), lambda x, y: ((x - 14) / 12, -0.2, 0.9), R['green'], dither=0.5)
        for x in range(14 - half + 1, 14 + half, 2):
            cv.set(x, bottom - 1, R['green'][0])
    lights = [(10, 12), (17, 14), (8, 19), (14, 19), (20, 20), (6, 27), (12, 26), (18, 27), (22, 28), (15, 9)]
    cols = [R['red'][3], R['brass'][3], R['blue'][3], R['pink'][2]]
    for i, (x, y) in enumerate(lights):
        cv.set(x, y, cols[(i + frame) % 4])
    # The star on top.
    for (x, y) in ((14, 0), (13, 1), (14, 1), (15, 1), (12, 2), (13, 2), (14, 2), (15, 2), (16, 2), (13, 3), (15, 3)):
        cv.set(x, y, R['brass'][4 if frame else 3])
    return cv


def cushion():
    cv = Canvas(30, 12)
    shade(cv, ellipse_mask(15, 7, 14, 4.6), ellipsoid_normal(15, 6, 14, 4.6), R['purple'], dither=0.5)
    shade(cv, ellipse_mask(15, 6, 9, 2.4), flat(0, 0.6, 0.6), R['purple'][1:4], outline=False)
    for x in (2, 27):
        cv.set(x, 4, R['brass'][3])
        cv.set(x, 3, R['brass'][2])
    return cv


SEASONAL = {'PUMPKIN': pumpkin, 'FIR': fir, 'CUSHION': lambda frame=0: cushion()}


# ---------------------------------------------------------------- the tower cat: smoky grey with white paws

def _cat_ramp():
    return R['silver'][:4]


def cat_sleep(frame=0):
    cv = Canvas(30, 16)
    g = _cat_ramp()
    breathe = frame
    shade(cv, ellipse_mask(15, 10 - breathe * 0.4, 11, 5.6 + breathe * 0.4), ellipsoid_normal(15, 9, 11, 6), g, dither=0.5)
    # Tail wrapped around the front.
    for i in range(16):
        x = 5 + i * 1.3
        y = 14 - math.sin(i / 15 * math.pi) * 1.5
        shade(cv, ellipse_mask(x, y, 1.7, 1.5), flat(0.2, -0.3, 0.9), g, outline=False)
    shade(cv, ellipse_mask(25, 14, 2, 1.5), flat(), R['cream'][3:], outline=False)
    # Head tucked in on the left.
    shade(cv, ellipse_mask(7, 9, 5.5, 4.6), ellipsoid_normal(7, 8, 5.5, 4.6), g)
    for ex in (4, 9):
        shade(cv, poly_mask([(ex - 2, 6), (ex, 2), (ex + 2, 6)]), flat(0, -0.5, 0.8), g)
        cv.set(ex, 5, R['pink'][2])
    cv.hline(4, 5, 9, EYE)
    cv.hline(8, 9, 9, EYE)
    cv.set(6, 11, R['pink'][2])
    cv.set(3, 11, BLUSH, 170)
    cv.set(10, 11, BLUSH, 170)
    for (x, y) in ((14, 7), (18, 6), (22, 8)):
        cv.set(x, y, g[0])
    return cv


def cat_sit(frame=0):
    cv = Canvas(24, 28)
    g = _cat_ramp()
    for i in range(12):
        a = i / 11 * math.pi * 0.8
        shade(cv, ellipse_mask(18 + math.sin(a) * 3, 25 - i, 1.6, 1.6), flat(0.3, -0.3, 0.9), g, outline=False)
    shade(cv, ellipse_mask(11, 20, 8, 7.5), ellipsoid_normal(11, 19, 8, 7.5), g, dither=0.5)
    shade(cv, ellipse_mask(11, 22, 4, 4.5), flat(0, 0.2, 1), R['cream'][2:], outline=False)
    shade(cv, ellipse_mask(11, 9.5, 7, 6), ellipsoid_normal(11, 8.5, 7, 6), g)
    for ex in (6, 16):
        shade(cv, poly_mask([(ex - 2.5, 6), (ex, 0), (ex + 2.5, 6)]), flat(0, -0.5, 0.8), g)
        cv.set(ex, 4, R['pink'][2])
    for ex in (7, 14):
        if frame:
            cv.hline(ex, ex + 1, 9, EYE)
        else:
            cv.rect(ex, 8, 2, 2, (220, 180, 60))
            cv.set(ex + 1, 9, EYE)
            cv.set(ex, 8, WHITE)
    cv.set(10, 11, R['pink'][2])
    cv.set(11, 11, R['pink'][2])
    cv.set(9, 12, EYE)
    cv.set(12, 12, EYE)
    cv.set(5, 11, BLUSH, 170)
    cv.set(16, 11, BLUSH, 170)
    for x in (8, 14):
        shade(cv, ellipse_mask(x, 26.5, 2.2, 1.4), flat(), R['cream'][3:])
    return cv


# ---------------------------------------------------------------- the apprentice's wardrobe
# The apprentice is drawn 4 rows lower than in decor.py so hats have room above; every piece shares one 26x38 grid.

DY = 4


def apprentice_body(frame=0):
    cv = Canvas(26, 38)
    up = frame
    o = DY
    shade(cv, poly_mask([(7, 18 - up + o), (19, 18 - up + o), (23, 33 + o), (3, 33 + o)]), lambda x, y: ((x - 13) / 20, 0.1, 0.9), R['red'], dither=0.5)
    shade(cv, rect_mask(5, 25 + o, 16, 2), flat(), R['brass'], outline=False)
    shade(cv, ellipse_mask(13, 13 - up + o, 6, 6), ellipsoid_normal(13, 12 - up + o, 6, 6), R['skin'][1:], oc=R['skin'][0])
    for (x, y) in ((8, 10), (9, 10), (17, 10), (18, 10), (7, 11), (7, 12), (19, 11), (19, 12), (7, 13), (19, 13)):
        cv.set(x, y - up + o, R['wood'][2])
    eyes(cv, 9, 15, 12 - up + o, big=False)
    smile(cv, 12, 16 - up + o, 3)
    shade(cv, ellipse_mask(5, 24 + o, 2, 2), ellipsoid_normal(5, 24 + o, 2, 2), R['skin'])
    shade(cv, ellipse_mask(21, 23 - up + o, 2, 2), ellipsoid_normal(21, 23 + o, 2, 2), R['skin'])
    cv.line(21, 22 - up + o, 24, 17 - up + o, R['wood'][3])
    cv.set(24, 16 - up + o, R['brass'][4])
    cv.set(25, 15 - up + o, WHITE)
    for x in (9, 16):
        shade(cv, rect_mask(x - 1, 32 + o, 4, 2), flat(), R['wood'][:3])
    return cv


def _hat_canvas():
    return Canvas(26, 38)


def hat_pointed(frame=0):
    """The classic hat, in the robe's red so it takes the robe's colour."""
    cv = _hat_canvas()
    up, o = frame, DY
    shade(cv, poly_mask([(5, 9 - up + o), (21, 9 - up + o), (15, 1 - up + o), (19, -1 - up + o), (12, 0 - up + o)]), lambda x, y: ((x - 13) / 10, -0.3, 0.9), R['red'], spec=True)
    shade(cv, ellipse_mask(13, 9 - up + o, 9, 2), flat(0, -1, 0.4), R['red'])
    return cv


def hat_wide(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    shade(cv, ellipse_mask(13, 9 - up + o, 12.5, 2.2), flat(0, -1, 0.4), R['ink'] + [R['purple'][2]])
    shade(cv, poly_mask([(7, 9 - up + o), (19, 9 - up + o), (16, 0 - up + o), (20, -3 - up + o), (12, -1 - up + o)]), lambda x, y: ((x - 13) / 10, -0.3, 0.9), R['ink'] + [R['purple'][2]], spec=True)
    cv.hline(8, 18, 7 - up + o, R['purple'][3])
    cv.set(17, 6 - up + o, R['brass'][3])
    return cv


def hat_flowers(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    for x in range(6, 21):
        cv.set(x, 8 - up + o, R['leaf'][2])
        cv.set(x, 7 - up + o + (x % 2), R['leaf'][3])
    cols = [R['pink'][2], R['brass'][3], WHITE, R['blue'][3], R['pink'][3]]
    for i, x in enumerate(range(6, 21, 3)):
        c = cols[i % len(cols)]
        cy = 6 - up + o
        for (dx, dy) in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            cv.set(x + dx, cy + dy, c)
        cv.set(x, cy, R['brass'][2])
    # Hair shows: a soft fringe over the forehead.
    for x in range(8, 19):
        cv.set(x, 9 - up + o, R['wood'][2])
    return cv


def hat_star(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    shade(cv, poly_mask([(4, 9 - up + o), (22, 9 - up + o), (13, -3 - up + o)]), lambda x, y: ((x - 13) / 10, -0.3, 0.9), R['blue'], spec=True)
    shade(cv, ellipse_mask(13, 9 - up + o, 9.5, 2), flat(0, -1, 0.4), R['blue'])
    for (x, y) in ((11, 3), (15, 6), (9, 7), (13, 0)):
        cv.set(x, y - up + o, R['brass'][4])
    for (x, y) in ((13, -4), (12, -3), (14, -3), (13, -2)):
        cv.set(x, y - up + o, R['brass'][4 if frame else 3])
    return cv


def hat_cat(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    shade(cv, lambda x, y: ellipse_mask(13, 9 - up + o, 7.6, 6)(x, y) and y <= 9 - up + o, ellipsoid_normal(13, 8 - up + o, 7.6, 6), R['silver'][:4], dither=0.5)
    for ex in (7, 19):
        shade(cv, poly_mask([(ex - 3, 6 - up + o), (ex, -1 - up + o), (ex + 3, 6 - up + o)]), flat(0, -0.5, 0.8), R['silver'][:4])
        cv.set(ex, 3 - up + o, R['pink'][2])
        cv.set(ex, 4 - up + o, R['pink'][2])
    cv.hline(6, 20, 9 - up + o, R['pink'][2])
    return cv


def hat_frog(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    shade(cv, lambda x, y: ellipse_mask(13, 9 - up + o, 7.6, 5.5)(x, y) and y <= 9 - up + o, ellipsoid_normal(13, 8 - up + o, 7.6, 5.5), R['green'], spec=True)
    for ex in (8, 18):
        shade(cv, ellipse_mask(ex, 3 - up + o, 2.6, 2.6), ellipsoid_normal(ex, 3 - up + o, 2.6, 2.6), R['green'], spec=True)
        cv.rect(ex - 1, 2 - up + o, 2, 2, EYE)
        cv.set(ex - 1, 2 - up + o, WHITE)
    cv.hline(10, 16, 7 - up + o, R['green'][0])
    return cv


def hat_crown(frame=0):
    cv = _hat_canvas()
    up, o = frame, DY
    for x in range(8, 19):
        cv.set(x, 8 - up + o, R['wood'][2])
    shade(cv, poly_mask([(7, 8 - up + o), (19, 8 - up + o), (19, 1 - up + o), (16, 4 - up + o), (13, 0 - up + o), (10, 4 - up + o), (7, 1 - up + o)]), lambda x, y: ((x - 13) / 8, -0.4, 0.9), R['brass'], spec=True)
    cv.set(13, 5 - up + o, R['red'][3])
    cv.set(9, 6 - up + o, R['blue'][3])
    cv.set(17, 6 - up + o, R['green'][3])
    if frame:
        cv.set(14, 0 - up + o, WHITE)
    return cv


HATS = {'POINTED': hat_pointed, 'WIDE': hat_wide, 'FLOWERS': hat_flowers, 'STAR': hat_star, 'CAT': hat_cat, 'FROG': hat_frog, 'CROWN': hat_crown}


# ---------------------------------------------------------------- tea, a bat, a heart

def tea_table():
    cv = Canvas(80, 30)
    shade(cv, rect_mask(2, 8, 76, 4), flat(0, -0.6, 1), R['wood'])
    # A checked cloth hanging over the edge.
    for y in range(9, 17):
        for x in range(4, 76):
            if y < 12 or (x // 4) % 2 == 0 or y < 15:
                c = R['red'][3] if ((x // 3) + (y // 3)) % 2 else R['cream'][3]
                cv.set(x, y, c)
    cv.hline(4, 75, 16, R['red'][1])
    for x in (8, 70):
        shade(cv, rect_mask(x, 17, 3, 13), cyl_normal(x + 1.5, 1.5), R['wood'])
    # Cups, a cake and a jam jar.
    for cx in (14, 30, 52, 66):
        shade(cv, rect_mask(cx - 3, 3, 6, 5), cyl_normal(cx, 3), R['cream'])
        cv.set(cx + 3, 5, R['cream'][1])
        cv.hline(cx - 2, cx + 2, 3, R['wood'][2])
    shade(cv, lambda x, y: ellipse_mask(41, 4, 7, 4)(x, y) and y >= 1, ellipsoid_normal(41, 3, 7, 4), R['pink'], spec=True)
    shade(cv, rect_mask(34, 4, 14, 4), lambda x, y: ((x - 41) / 8, 0, 0.9), R['orange'][2:])
    cv.set(41, 0, R['red'][3])
    shade(cv, rect_mask(22, 2, 4, 6), cyl_normal(24, 2), R['red'][1:])
    return cv


def bat(frame=0):
    cv = Canvas(13, 7)
    up = frame
    for side in (-1, 1):
        pts = [(6.5, 3), (6.5 + side * 6, 1 + up * 3), (6.5 + side * 5, 5), (6.5 + side * 2.5, 4)]
        shade(cv, poly_mask(pts), flat(), R['ink'][1:], outline=False)
    shade(cv, ellipse_mask(6.5, 3.5, 1.8, 2), flat(), R['ink'][:2], outline=False)
    cv.set(6, 3, R['brass'][3])
    cv.set(7, 3, R['brass'][3])
    return cv


def heart():
    cv = Canvas(7, 6)
    rows = ['.##.##.', '#######', '#######', '.#####.', '..###..', '...#...']
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch == '#':
                cv.set(x, y, R['pink'][2] if (x, y) not in ((1, 1), (2, 1)) else R['pink'][3])
    cv.set(1, 1, WHITE)
    return cv


# ---------------------------------------------------------------- the outside of one floor, for the widget

WALL_H = 26


def wall():
    """A stone band with an arched window. The glass is left transparent: the widget fills it with lamplight."""
    cv = Canvas(Sc.W, WALL_H)
    Sc.wall_stone(cv, 0, Sc.W, 0, WALL_H, seed=5)
    for y in range(WALL_H):
        cv.set(0, y, R['stone'][1])
        cv.set(Sc.W - 1, y, R['stone'][0])
    cv.hline(0, Sc.W - 1, WALL_H - 1, R['stone'][0])
    glass = Sc.window(cv, 52, 5, 16, 15, sill=True, mullion=True)
    return cv, glass


if __name__ == '__main__':
    import sys
    items = [(k, f()) for k, f in TRINKETS.items()]
    items += [(k, f(0)) for k, f in SOUVENIRS.items()] + [(k, f(0)) for k, f in SEASONAL.items()]
    items += [('CATZ', cat_sleep(0)), ('CATS', cat_sit(0)), ('BODY', apprentice_body(0))]
    for k, f in HATS.items():
        b = apprentice_body(0)
        b.blit(f(0), 0, 0)
        items.append((k, b))
    items += [('TEA', tea_table()), ('BAT', bat(0)), ('HEART', heart()), ('WALL', wall()[0])]
    sheet(items, sys.argv[1], scale=4, cols=9)
