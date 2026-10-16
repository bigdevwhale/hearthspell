"""Full floor scenes (120x72), the roof and the base, with metadata for the animated layers drawn in Kotlin."""
import math
from pixlib import *
import decor as D

R = RAMPS
W, H = 120, 72
FLOOR = 62          # the line feet stand on
IN_L, IN_R = 8, 111  # interior columns


def wall_stone(cv, x0, w, y0=0, h=H, ramp=None, seed=0):
    ramp = ramp or R['stone']
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            row = (y - y0) // 6
            off = 5 if row % 2 else 0
            bx = (x - x0 + off) % 10
            by = (y - y0) % 6
            if by == 0 or bx == 0:
                c = ramp[1]
            else:
                # Each block: lit top-left, shadowed bottom-right, with a little dither.
                t = 0.62 - (by / 6) * 0.3 - (bx / 10) * 0.18 + ((hash((row, (x - x0 + off) // 10, seed)) % 7) - 3) * 0.03
                c = quant(ramp, max(0, min(1, t)), x, y, 0.35, 1, len(ramp) - 2)
                if by == 1 and bx < 9:
                    c = ramp[min(len(ramp) - 1, 4)] if bx < 6 else c
            cv.set(x, y, c)


def outer_walls(cv):
    wall_stone(cv, 0, 8, seed=1)
    wall_stone(cv, 112, 8, seed=2)
    for y in range(H):
        cv.set(7, y, R['stone'][0])
        cv.set(112, y, R['stone'][0])
        cv.set(0, y, R['stone'][1])
        cv.set(119, y, R['stone'][0])


def ceiling(cv):
    shade(cv, rect_mask(0, 0, W, 4), flat(0, -0.4, 1), R['wood'], outline=False)
    cv.hline(0, W - 1, 4, R['wood'][0])
    cv.hline(0, W - 1, 0, R['wood'][1])
    # Joists poking out.
    for x in range(14, 110, 24):
        shade(cv, rect_mask(x, 4, 5, 3), flat(0, 0.3, 1), R['wood'])


def floorboards(cv):
    for y in range(FLOOR, H):
        for x in range(0, W):
            plank = (y - FLOOR) // 3
            seam = (x + plank * 13) % 22 == 0
            t = 0.55 - (y - FLOOR) * 0.035 + (0.08 if (y - FLOOR) % 3 == 0 else 0)
            c = R['wood'][1] if seam else quant(R['wood'], t, x, y, 0.4, 1, 4)
            cv.set(x, y, c)
    cv.hline(0, W - 1, FLOOR, R['wood'][4])
    cv.hline(0, W - 1, FLOOR + 1, R['wood'][3])
    cv.hline(0, W - 1, H - 1, R['wood'][0])
    # Skirting board.
    shade(cv, rect_mask(IN_L, FLOOR - 3, IN_R - IN_L + 1, 3), flat(0, -0.5, 1), R['wood'], outline=False)
    cv.hline(IN_L, IN_R, FLOOR - 1, R['wood'][1])


def back_wall(cv, kind):
    """Each room's own wall: paper, tiles, panels or stone."""
    x0, x1, y0, y1 = IN_L, IN_R, 5, FLOOR - 1
    ramps = {
        'KITCHEN': [(66, 30, 34), (98, 46, 40), (132, 66, 50), (164, 92, 64), (190, 120, 84)],
        'LIVING': [(18, 40, 38), (28, 62, 50), (42, 86, 62), (62, 112, 76), (92, 140, 94)],
        'BEDROOM': [(14, 16, 44), (22, 28, 70), (34, 44, 98), (52, 64, 126), (80, 92, 156)],
        'BATH': [(14, 50, 60), (22, 78, 86), (36, 110, 112), (64, 146, 138), (110, 186, 170)],
        'HALL': [(44, 24, 30), (70, 40, 38), (100, 60, 46), (132, 84, 56), (164, 112, 70)],
        'BALCONY': [(20, 44, 40), (30, 66, 52), (44, 92, 62), (70, 122, 76), (110, 156, 96)],
        'KIDS': [(84, 40, 70), (122, 62, 92), (160, 90, 116), (198, 126, 140), (226, 164, 168)],
        'STUDY': [(30, 18, 48), (48, 30, 72), (70, 46, 98), (96, 66, 126), (128, 94, 154)],
        'LAUNDRY': [(28, 34, 74), (42, 52, 104), (60, 74, 134), (86, 102, 162), (122, 138, 190)],
        'PETS': [(70, 46, 24), (104, 72, 32), (140, 102, 44), (176, 136, 62), (206, 170, 92)],
        'WHOLE': [(64, 32, 20), (98, 52, 26), (134, 78, 34), (170, 108, 46), (204, 144, 66)],
    }
    ramp = ramps[kind]
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            # Darker towards the corners and the ceiling: a soft vignette, dithered.
            vx = abs((x - (x0 + x1) / 2) / ((x1 - x0) / 2))
            vy = (y - y0) / (y1 - y0)
            t = 0.62 - vx * vx * 0.28 - (1 - vy) * 0.12
            c = quant(ramp, t, x, y, 0.5, 0, 3)
            cv.set(x, y, c)
    # Pattern.
    if kind in ('KITCHEN', 'WHOLE'):
        for y in range(y0, 44):
            for x in range(x0, x1 + 1):
                row = (y - y0) // 5
                if (y - y0) % 5 == 0 or (x + (3 if row % 2 else 0)) % 8 == 0:
                    cv.set(x, y, ramp[0], 120)
    elif kind == 'BATH':
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if (y - y0) % 7 == 0 or (x - x0) % 7 == 0:
                    cv.set(x, y, ramp[4], 110)
                elif (y - y0) % 7 == 1 and (x - x0) % 7 in (1, 2):
                    cv.set(x, y, WHITE, 70)
    elif kind in ('HALL',):
        for x in range(x0, x1 + 1, 13):
            cv.vline(x, y0, y1, ramp[0])
            cv.vline(x + 1, y0, y1, ramp[3])
    elif kind == 'KIDS':
        for x in range(x0, x1 + 1):
            if (x // 4) % 2 == 0:
                for y in range(y0, 44):
                    cv.set(x, y, ramp[4], 60)
    elif kind == 'BEDROOM':
        for i in range(40):
            h = hash(('bs', i))
            x, y = x0 + h % (x1 - x0), y0 + (h // 101) % 36
            cv.set(x, y, R['brass'][3] if i % 3 else WHITE, 150)
    else:
        for y in range(y0 + 4, 44, 9):
            for x in range(x0 + 4, x1, 9):
                xx = x + (4 if (y // 9) % 2 else 0)
                cv.set(xx, y, ramp[4], 120)
                cv.set(xx - 1, y + 1, ramp[4], 60)
                cv.set(xx + 1, y + 1, ramp[4], 60)
                cv.set(xx, y + 2, ramp[4], 120)
    # Wainscot below a dado rail.
    if kind not in ('BATH', 'BALCONY'):
        shade(cv, rect_mask(x0, 44, x1 - x0 + 1, 2), flat(0, -0.6, 1), R['wood'], outline=False)
        for y in range(46, y1 + 1):
            for x in range(x0, x1 + 1):
                panel = (x - x0) % 16
                c = R['wood'][1] if panel == 0 else quant(R['wood'], 0.45 - (panel / 16) * 0.15, x, y, 0.3, 1, 3)
                cv.set(x, y, c)
            cv.set(x0, y, R['wood'][0])


def window(cv, x, y, w, h, frame=R['stone'], sill=True, round_=False, mullion=True):
    """An arched window. The glass is left transparent so the live sky shows through. Returns its glass rect."""
    cx = x + w / 2
    r = w / 2
    def inside(px_, py_, inset=0):
        if round_:
            return (px_ + 0.5 - cx) ** 2 + (py_ + 0.5 - (y + h / 2)) ** 2 <= (r - inset) ** 2
        if py_ + 0.5 < y + r:
            return (px_ + 0.5 - cx) ** 2 + (py_ + 0.5 - (y + r)) ** 2 <= (r - inset) ** 2
        return x + inset <= px_ < x + w - inset and py_ < y + h - inset
    for py_ in range(y - 2, y + h + 2):
        for px_ in range(x - 2, x + w + 2):
            if inside(px_, py_, -2) and not inside(px_, py_, 1):
                lit = px_ < cx and py_ < y + h / 2
                cv.set(px_, py_, frame[4] if lit and inside(px_, py_, -1) and not inside(px_, py_, 0) else frame[2 if lit else 1])
            elif inside(px_, py_, 1):
                cv.px[py_][px_] = (0, 0, 0, 0)  # glass: transparent hole
    for py_ in range(y - 2, y + h + 2):
        for px_ in range(x - 2, x + w + 2):
            if inside(px_, py_, -2) and not inside(px_, py_, -1):
                cv.set(px_, py_, frame[0])
    if mullion:
        mx = int(cx)
        for py_ in range(y, y + h):
            if inside(mx, py_, 1):
                cv.set(mx, py_, R['wood'][1])
                cv.set(mx - 1, py_, R['wood'][3])
        my = y + h // 2 + (2 if not round_ else 0)
        for px_ in range(x, x + w):
            if inside(px_, my, 1):
                cv.set(px_, my, R['wood'][1])
                cv.set(px_, my - 1, R['wood'][3])
    if sill:
        shade(cv, rect_mask(x - 4, y + h + 1, w + 8, 3), flat(0, -0.7, 1), frame)
    return (x + 1, y + 1, w - 2, h - 2, round_)


def shadow(cv, cx, cy, rx, ry=2):
    for y in range(int(cy - ry), int(cy + ry) + 1):
        for x in range(int(cx - rx), int(cx + rx) + 1):
            if ellipse_mask(cx, cy, rx, ry)(x, y):
                cv.set(x, y, (20, 10, 26), 90)


def put(cv, sprite, x, y_bottom, shade_floor=True):
    if shade_floor:
        shadow(cv, x + sprite.w / 2, y_bottom - 0.5, sprite.w / 2 + 1, 1.6)
    cv.blit(sprite, int(x), int(y_bottom - sprite.h))


def bookcase(cv, x, y, w, h, seed=0):
    shade(cv, rect_mask(x, y, w, h), lambda a, b: ((a - x - w / 2) / w * 0.6, 0, 1), R['wood'])
    shelves = list(range(y + 3, y + h - 3, 9))
    colors = [R['red'], R['blue'], R['green'], R['purple'], R['brass'], R['teal'], R['orange'], R['cream']]
    for sy in shelves:
        shade(cv, rect_mask(x + 2, sy, w - 4, 8), flat(), R['wood'][:2], outline=False, dither=0.2)
        bx = x + 2
        k = 0
        while bx < x + w - 3:
            bw = 2 + (hash((seed, sy, k)) % 3)
            bh = 5 + (hash((seed, sy, k, 1)) % 3)
            if bx + bw > x + w - 2:
                break
            if hash((seed, sy, k, 2)) % 9 == 0:
                bx += 2
                k += 1
                continue
            ramp = colors[hash((seed, sy, k, 3)) % len(colors)]
            shade(cv, rect_mask(bx, sy + 8 - bh, bw, bh), cyl_normal(bx + bw / 2, bw / 2), ramp, outline=False)
            cv.set(bx, sy + 8 - bh, ramp[0])
            cv.set(bx + bw - 1, sy + 8 - bh + 1, ramp[0])
            if bh > 5:
                cv.set(bx + bw // 2, sy + 8 - bh + 2, R['brass'][3])
            bx += bw
            k += 1
        shade(cv, rect_mask(x + 1, sy + 8, w - 2, 1), flat(), R['wood'][3:], outline=False)
    shadow(cv, x + w / 2, y + h, w / 2 + 1, 1.5)


def table(cv, x, w, top_y, legs=True, ramp=None):
    ramp = ramp or R['wood']
    shade(cv, rect_mask(x, top_y, w, 3), flat(0, -0.6, 1), ramp)
    shade(cv, rect_mask(x + 1, top_y + 3, w - 2, 2), flat(0, 0.4, 1), ramp)
    if legs:
        for lx in (x + 2, x + w - 5):
            shade(cv, rect_mask(lx, top_y + 4, 3, FLOOR - top_y - 4), cyl_normal(lx + 1.5, 1.5), ramp)
    shadow(cv, x + w / 2, FLOOR - 0.5, w / 2, 1.6)


def shelf(cv, x, y, w, items=()):
    shade(cv, rect_mask(x, y, w, 2), flat(0, -0.6, 1), R['wood'])
    for bx in (x + 2, x + w - 4):
        shade(cv, poly_mask([(bx, y + 2), (bx + 2, y + 2), (bx + 2, y + 5)]), flat(), R['wood'][:3])
    for (ix, sprite) in items:
        cv.blit(sprite, x + ix, y - sprite.h)


def jar(color, h=6, w=4):
    cv = Canvas(w + 2, h + 2)
    cylinder(cv, 1, 2, w, h, color)
    shade(cv, rect_mask(1, 0, w, 2), flat(), R['wood'][1:4])
    cv.set(2, 4, WHITE, 180)
    return trim(cv)


def flask(color):
    cv = Canvas(8, 10)
    shade(cv, ellipse_mask(4, 6.5, 3.4, 3.2), ellipsoid_normal(4, 6, 3.4, 3.2), color, spec=True)
    shade(cv, rect_mask(3, 1, 2, 3), cyl_normal(4, 1), R['glass'])
    cv.set(3, 0, R['wood'][2])
    cv.set(4, 0, R['wood'][2])
    return trim(cv)


def pot_plant(size=1.0, kind=0):
    cv = Canvas(int(20 * size) + 2, int(26 * size) + 2)
    w = cv.w
    cx = w / 2
    ph = int(8 * size)
    top = cv.h - ph - 1
    if kind == 0:  # leafy
        for k in range(7):
            a = math.radians(205 + k * 22)
            for t in range(int(11 * size)):
                lx = cx + math.cos(a) * t
                ly = top + 1 + math.sin(a) * t + t * t / (24 * size)
                cv.set(lx, ly, R['green'][1 + (k % 2)])
                if t % 2 and t > 1:
                    cv.set(lx + 1, ly - 1, R['leaf'][3 + (t % 2)])
    elif kind == 1:  # round bush with flowers
        ball(cv, cx, top - 4 * size, 7 * size, R['leaf'], ry=6 * size, spec=False)
        for k in range(5):
            h = hash(('fl', k, size))
            cv.set(cx - 5 * size + h % int(10 * size), top - 9 * size + (h // 7) % int(8 * size), R['pink'][3])
    else:  # tall cactus
        shade(cv, ellipse_mask(cx, top - 6 * size, 3.5 * size, 8 * size), ellipsoid_normal(cx, top - 6 * size, 3.5 * size, 8 * size), R['green'])
        for k in range(4):
            cv.set(cx - 2 + k, top - 12 * size + k * 3, R['cream'][4])
    shade(cv, poly_mask([(cx - 6 * size, top), (cx + 6 * size, top), (cx + 4.5 * size, cv.h - 1), (cx - 4.5 * size, cv.h - 1)]), lambda x, y: ((x - cx) / (8 * size), 0.1, 0.9), R['orange'][:4])
    shade(cv, rect_mask(int(cx - 6.5 * size), top - 1, int(13 * size) + 1, 2), cyl_normal(cx, 6.5 * size), R['orange'])
    return trim(cv)


def armchair():
    cv = Canvas(30, 28)
    shade(cv, poly_mask([(4, 2), (26, 2), (27, 18), (3, 18)]), lambda x, y: ((x - 15) / 26, -0.2 + (y - 2) / 30, 0.9), R['red'])
    for x in range(7, 24, 4):
        for y in range(5, 16, 4):
            cv.set(x + (y // 4) % 2 * 2, y, R['red'][0])
    shade(cv, rect_mask(4, 16, 22, 6), flat(0, -0.6, 1), R['red'])
    for side in (0, 24):
        shade(cv, ellipse_mask(side + 3, 14, 3.2, 8), ellipsoid_normal(side + 3, 13, 3.2, 8), R['red'])
    for lx in (5, 23):
        shade(cv, rect_mask(lx, 22, 3, 6), flat(), R['wood'])
    return cv


def lamp_stand(frame=0):
    cv = Canvas(14, 34)
    shade(cv, rect_mask(6, 10, 2, 22), cyl_normal(7, 1), R['brass'])
    shade(cv, ellipse_mask(7, 32, 5, 1.8), ellipsoid_normal(7, 31, 5, 1.8), R['brass'])
    shade(cv, poly_mask([(3, 1), (11, 1), (13.5, 10), (0.5, 10)]), lambda x, y: ((x - 7) / 10, 0, 0.9), R['orange'][1:] + [R['fire'][4]])
    return cv


def bed(cv, x, w):
    top = FLOOR - 16
    shade(cv, rect_mask(x, top - 8, 4, 24), cyl_normal(x + 2, 2), R['wood'])
    shade(cv, rect_mask(x + w - 4, top - 2, 4, 18), cyl_normal(x + w - 2, 2), R['wood'])
    shade(cv, rect_mask(x + 3, top + 6, w - 6, 7), flat(0, 0.2, 1), R['wood'])
    shade(cv, rect_mask(x + 3, top + 2, w - 6, 5), flat(0, -0.6, 1), R['cream'])
    shade(cv, ellipse_mask(x + 11, top, 7, 3.4), ellipsoid_normal(x + 11, top - 1, 7, 3.4), R['cream'])
    # The quilt, patchwork.
    S = shade(cv, poly_mask([(x + 17, top - 1), (x + w - 3, top - 1), (x + w - 2, top + 8), (x + 17, top + 8)]), lambda a, b: (0, -0.5 + (b - top) / 12, 0.9), R['blue'])
    for (a, b) in S:
        if ((a - x) // 5 + (b - top) // 4) % 2 == 0:
            c = cv.get(a, b)
            cv.set(a, b, R['purple'][3 if c and c[0] > 60 else 2], 140)
    shadow(cv, x + w / 2, FLOOR - 0.5, w / 2, 1.6)


def tub(cv, x, w):
    top = FLOOR - 16
    shade(cv, ellipse_mask(x + w / 2, top + 2, w / 2, 3.4), flat(0, -1, 0.4), R['water'])
    for k in range(5):
        cv.set(x + 4 + k * 5, top + 1, R['water'][4])
    S = shade(cv, poly_mask([(x, top + 2), (x + w, top + 2), (x + w - 2, FLOOR), (x + 2, FLOOR)]), lambda a, b: ((a - x - w / 2) / w, 0.1, 0.9), R['wood'])
    for b in (top + 5, FLOOR - 3):
        for a in range(x, x + w):
            if (a, b) in S:
                cv.set(a, b, R['silver'][2])
                cv.set(a, b + 1, R['silver'][1])
    for a in range(x + 4, x + w - 3, 5):
        for b in range(top + 7, FLOOR - 3):
            if (a, b) in S:
                cv.set(a, b, R['wood'][1])
    shadow(cv, x + w / 2, FLOOR - 0.5, w / 2 + 1, 1.6)


def door(cv, x, w, h):
    top = FLOOR - h
    r = w / 2
    m = lambda a, b: (x <= a < x + w and b >= top + r and b < FLOOR) or ((a + 0.5 - x - r) ** 2 + (b + 0.5 - top - r) ** 2 <= r * r and b < top + r + 1)
    shade(cv, lambda a, b: m(a, b) or m(a - 2, b) and False, lambda a, b: (0, 0, 1), R['stone'], dither=0.2)
    inner = lambda a, b: (x + 2 <= a < x + w - 2 and b >= top + r and b < FLOOR) or ((a + 0.5 - x - r) ** 2 + (b + 0.5 - top - r) ** 2 <= (r - 2) ** 2 and b < top + r + 1)
    S = shade(cv, inner, lambda a, b: ((a - x - r) / w, 0, 1), R['wood'], dither=0.4)
    for a in range(x + 4, x + w - 2, 4):
        for b in range(top, FLOOR):
            if (a, b) in S:
                cv.set(a, b, R['wood'][1])
    for b in (top + h // 3, top + 2 * h // 3):
        for a in range(x + 2, x + w - 2):
            if (a, b) in S:
                cv.set(a, b, R['ink'][1])
                cv.set(a, b + 1, R['silver'][2])
    shade(cv, ellipse_mask(x + w - 5, top + h // 2 + 2, 1.6, 1.6), ellipsoid_normal(x + w - 5, top + h // 2 + 2, 1.6, 1.6), R['brass'], spec=True)


def small_candle(cv, x, y_bottom, h=7):
    """A single candle in a brass dish; returns the flame anchor (wick top)."""
    shade(cv, ellipse_mask(x + 2.5, y_bottom - 1, 4, 1.4), ellipsoid_normal(x + 2.5, y_bottom - 1.5, 4, 1.4), R['brass'])
    cylinder(cv, x + 1, y_bottom - 1 - h, 3, h, R['cream'])
    cv.set(x + 2, y_bottom - 2 - h, R['ink'][1])
    return (x + 2, y_bottom - 2 - h)


def hanging_lamp(cv, x, y_top, length=8):
    cv.vline(x, y_top, y_top + length, R['ink'][2])
    shade(cv, poly_mask([(x - 4, y_top + length + 4), (x + 5, y_top + length + 4), (x + 3, y_top + length), (x - 2, y_top + length)]), flat(0, -0.3, 1), R['brass'])
    return (x, y_top + length + 6)


def vines(cv, x0, x1, y):
    for x in range(x0, x1):
        h = hash(('v', x))
        if h % 3 == 0:
            ln = 3 + h % 9
            for t in range(ln):
                cv.set(x + (t // 3) % 2, y + t, R['green'][1 + (t % 2)])
                if t % 3 == 1:
                    cv.set(x + 1, y + t, R['leaf'][4])


def clothesline(cv, x0, x1, y):
    for x in range(x0, x1):
        sag = int(3 * math.sin(math.pi * (x - x0) / (x1 - x0)))
        cv.set(x, y + sag, R['cream'][1])
    items = [(x0 + 6, R['blue'], 10, 12), (x0 + 20, R['cream'], 9, 14), (x0 + 33, R['red'], 8, 10), (x0 + 45, R['purple'], 9, 12)]
    for (x, ramp, w, h) in items:
        if x + w >= x1:
            continue
        sag = int(3 * math.sin(math.pi * (x - x0) / (x1 - x0)))
        shade(cv, poly_mask([(x, y + sag), (x + w, y + sag), (x + w - 1, y + sag + h), (x + 1, y + sag + h - 1)]), lambda a, b: (math.sin(a / 2) * 0.4, 0, 1), ramp)
        cv.set(x + 1, y + sag, R['wood'][3])
        cv.set(x + w - 1, y + sag, R['wood'][3])


def loom(cv, x):
    top = FLOOR - 30
    for lx in (x, x + 24):
        shade(cv, rect_mask(lx, top, 3, 30), cyl_normal(lx + 1.5, 1.5), R['wood'])
    for ly in (top + 2, top + 22):
        shade(cv, rect_mask(x, ly, 27, 3), cyl_normal(x + 13, 13, 0), R['wood'])
    for k, tx in enumerate(range(x + 4, x + 23)):
        cv.vline(tx, top + 5, top + 21, R['cream'][2 + k % 2])
    # Woven cloth growing at the bottom.
    for ty in range(top + 15, top + 22):
        for tx in range(x + 4, x + 23):
            cv.set(tx, ty, R['red'][2 + ((tx + ty) % 2)] if (ty // 2) % 2 else R['brass'][2])


def coat_rack(cv, x):
    shade(cv, rect_mask(x, FLOOR - 34, 3, 34), cyl_normal(x + 1.5, 1.5), R['wood'])
    shade(cv, ellipse_mask(x + 1.5, FLOOR - 1, 6, 1.6), flat(0, -1, 0.5), R['wood'])
    for side in (-1, 1):
        cv.line(x + 1, FLOOR - 32, x + 1 + side * 5, FLOOR - 35, R['wood'][2])
    # A green cloak and a pointed hat.
    shade(cv, poly_mask([(x - 4, FLOOR - 31), (x + 7, FLOOR - 31), (x + 10, FLOOR - 10), (x - 7, FLOOR - 10)]), lambda a, b: ((a - x) / 16, 0, 0.9), R['green'])
    shade(cv, poly_mask([(x - 5, FLOOR - 36), (x + 8, FLOOR - 36), (x + 3, FLOOR - 48), (x + 6, FLOOR - 50)]), lambda a, b: ((a - x) / 12, -0.4, 0.9), R['purple'])


def scroll_desk(cv, x):
    table(cv, x, 30, FLOOR - 14)
    shade(cv, rect_mask(x + 4, FLOOR - 17, 16, 3), flat(0, -0.6, 1), R['cream'])
    for k in range(4):
        cv.hline(x + 6, x + 14 - k, FLOOR - 16 + (k % 2), R['ink'][2])
    shade(cv, ellipse_mask(x + 4, FLOOR - 16, 1.6, 2.2), ellipsoid_normal(x + 4, FLOOR - 16, 1.6, 2.2), R['cream'])
    shade(cv, ellipse_mask(x + 20, FLOOR - 16, 1.6, 2.2), ellipsoid_normal(x + 20, FLOOR - 16, 1.6, 2.2), R['cream'])
    shade(cv, rect_mask(x + 23, FLOOR - 18, 4, 4), cyl_normal(x + 25, 2), R['ink'])
    cv.line(x + 25, FLOOR - 19, x + 28, FLOOR - 27, R['cream'][4])
    cv.line(x + 26, FLOOR - 19, x + 29, FLOOR - 26, R['cream'][2])


def rocking_horse(cv, x):
    for t in range(0, 181, 4):
        a = math.radians(t)
        cv.set(x + 10 + math.cos(a) * 11, FLOOR - 2 - math.sin(a) * 2.6, R['wood'][2])
        cv.set(x + 10 + math.cos(a) * 11, FLOOR - 1 - math.sin(a) * 2.6, R['wood'][1])
    for lx in (x + 3, x + 15):
        shade(cv, rect_mask(lx, FLOOR - 9, 2, 6), flat(), R['wood'])
    shade(cv, ellipse_mask(x + 10, FLOOR - 11, 8, 3.6), ellipsoid_normal(x + 10, FLOOR - 12, 8, 3.6), R['cream'])
    shade(cv, poly_mask([(x + 15, FLOOR - 13), (x + 19, FLOOR - 22), (x + 23, FLOOR - 19), (x + 18, FLOOR - 11)]), lambda a, b: (0.2, -0.4, 0.9), R['cream'])
    for k in range(5):
        cv.set(x + 16 + k // 2, FLOOR - 21 + k, R['red'][2])
    cv.set(x + 21, FLOOR - 20, EYE)
    shade(cv, rect_mask(x + 8, FLOOR - 15, 5, 2), flat(), R['red'])


def cat_tree(cv, x):
    shade(cv, rect_mask(x + 6, FLOOR - 34, 4, 34), cyl_normal(x + 8, 2), R['cream'][:4])
    for y in range(FLOOR - 32, FLOOR, 3):
        cv.hline(x + 6, x + 9, y, R['cream'][1])
    for (py, w) in ((FLOOR - 34, 16), (FLOOR - 18, 14)):
        shade(cv, rect_mask(x, py - 2, w, 3), flat(0, -0.6, 1), R['red'])
    shade(cv, rect_mask(x - 2, FLOOR - 3, 20, 3), flat(0, -0.6, 1), R['red'])


def fishbowl(cv, x, y):
    shade(cv, ellipse_mask(x + 7, y + 7, 7, 6.5), ellipsoid_normal(x + 7, y + 6, 7, 6.5), R['glass'], dither=0.3)
    shade(cv, poly_mask([(x + 5, y + 7), (x + 9, y + 6), (x + 10, y + 8), (x + 9, y + 9), (x + 5, y + 8)]), flat(), R['orange'], outline=False)
    cv.set(x + 3, y + 3, WHITE)
    cv.set(x + 12, y + 4, R['glass'][3])


def heart_crystal(cv, x, y):
    glow(cv, x, y, 18, R['fire'][3], 90, 5)
    pts = [(x, y - 9), (x + 6, y - 2), (x, y + 9), (x - 6, y - 2)]
    shade(cv, poly_mask(pts), lambda a, b: ((a - x) / 8, (b - y) / 12, 0.8), R['red'][1:] + [R['pink'][3], WHITE], spec=True)
    cv.vline(x, y - 7, y + 6, R['pink'][3])


# ---------------------------------------------------------------- the eleven floors

def scene(kind):
    cv = Canvas(W, H)
    meta = {'flames': [], 'steam': [], 'wall': [], 'floor': [], 'glass': None, 'walk': (IN_L + 2, IN_R - 2)}
    back_wall(cv, kind)
    ceiling(cv)
    floorboards(cv)

    def lights(*pts, r=16, s=55):
        for (lx, ly) in pts:
            glow(cv, lx, ly, r, R['fire'][3], s, 5)

    if kind == 'KITCHEN':
        meta['glass'] = window(cv, 84, 12, 16, 24)
        # A big stone hearth with the cauldron in it.
        shade(cv, rect_mask(10, 18, 34, 44), lambda a, b: ((a - 27) / 34, 0, 1), R['stone'], dither=0.3)
        shade(cv, rect_mask(8, 14, 38, 5), flat(0, -0.6, 1), R['stone'])
        shade(cv, rect_mask(15, 30, 24, 32), flat(), R['ink'][:2], outline=False)
        glow(cv, 27, 56, 16, R['fire'][3], 130, 5)
        cau = D.cauldron(0)
        cv.blit(cau, 27 - cau.w // 2, FLOOR - cau.h)
        meta['flames'] += [(20, 61, 1.3), (27, 61, 1.5), (34, 61, 1.3)]
        meta['steam'] += [(27, 32)]
        # Shelf with jars and flasks, hanging copper pots and garlic.
        shelf(cv, 50, 22, 28, [(2, jar(R['red'])), (8, jar(R['green'], 8)), (15, flask(R['purple'])), (22, jar(R['brass'], 5))])
        for i, px_ in enumerate((52, 60, 68)):
            cv.vline(px_ + 3, 6, 8 + i, R['ink'][2])
            pot = Canvas(9, 8)
            shade(pot, lambda a, b: ellipse_mask(4.5, 2, 4.5, 5)(a, b) and b >= 2, ellipsoid_normal(4.5, 2, 4.5, 5), R['orange'], spec=True)
            cv.blit(trim(pot), px_, 9 + i)
        table(cv, 52, 30, FLOOR - 14)
        cv.blit(flask(R['green']), 56, FLOOR - 14 - 8)
        cv.blit(jar(R['cream'], 5, 5), 66, FLOOR - 14 - 7)
        shade(cv, ellipse_mask(76, FLOOR - 16, 3, 2), ellipsoid_normal(76, FLOOR - 16, 3, 2), R['stone'])
        meta['wall'] = [(86, 42)]
        meta['floor'] = [86, 98]
    elif kind == 'LIVING':
        meta['glass'] = window(cv, 50, 12, 16, 24)
        bookcase(cv, 10, 10, 30, 52, 1)
        bookcase(cv, 96, 20, 15, 42, 2)
        put(cv, D.rug(), 46, FLOOR + 5, False)
        put(cv, armchair(), 68, FLOOR)
        put(cv, lamp_stand(), 54, FLOOR)
        lights((61, 35), r=20, s=45)
        meta['wall'] = [(72, 12), (42, 12)]
        meta['floor'] = [44, 86]
    elif kind == 'BEDROOM':
        meta['glass'] = window(cv, 40, 8, 26, 26, round_=True, sill=False)
        # The brass dome ring around the round window.
        for t in range(0, 360, 6):
            a = math.radians(t)
            cv.set(53 + math.cos(a) * 15, 21 + math.sin(a) * 15, R['brass'][2])
        bed(cv, 66, 42)
        tel = D.telescope()
        cv.blit(tel, 14, FLOOR - tel.h)
        shadow(cv, 30, FLOOR - 0.5, 10, 1.4)
        table(cv, 46, 12, FLOOR - 12)
        meta['flames'] += [small_candle(cv, 49, FLOOR - 12) + (0.7,)]
        lights((51, 40), r=14, s=50)
        meta['wall'] = [(14, 12), (88, 10)]
        meta['floor'] = [30, 112 - 1]
    elif kind == 'BATH':
        meta['glass'] = window(cv, 46, 10, 14, 14, round_=True)
        tub(cv, 60, 40)
        meta['steam'] += [(70, FLOOR - 20), (84, FLOOR - 21), (94, FLOOR - 19)]
        # A spring basin: water spouting from a pipe in the wall into a stone bowl.
        shade(cv, rect_mask(18, 24, 10, 4), cyl_normal(23, 5), R['brass'])
        for y in range(28, FLOOR - 10):
            cv.set(25, y, R['water'][3])
            cv.set(26, y, R['water'][4] if y % 3 else R['water'][2])
        shade(cv, lambda a, b: ellipse_mask(25, FLOOR - 10, 13, 9)(a, b) and b >= FLOOR - 12, ellipsoid_normal(25, FLOOR - 12, 13, 9), R['stone'])
        shade(cv, ellipse_mask(25, FLOOR - 12, 12, 2.4), flat(0, -1, 0.4), R['water'])
        # Towel rail with a fluffy towel.
        cv.hline(40, 54, 40, R['brass'][2])
        shade(cv, poly_mask([(42, 40), (52, 40), (52, 54), (42, 54)]), lambda a, b: (math.sin(a) * 0.2, 0, 1), R['pink'])
        for y in range(46, 54, 3):
            cv.hline(42, 51, y, R['pink'][3])
        meta['wall'] = [(10, 10), (100, 10)]
        meta['floor'] = [8, 102]
    elif kind == 'HALL':
        meta['glass'] = window(cv, 76, 12, 14, 22)
        door(cv, 12, 22, 40)
        coat_rack(cv, 48)
        cv.blit(D.lantern(0), 96, 10)
        meta['flames'] += [(104, 28, 1.0)]
        lights((105, 26), r=16, s=55)
        # A bench with boots under it.
        table(cv, 62, 26, FLOOR - 9)
        for bx in (66, 74):
            shade(cv, poly_mask([(bx, FLOOR - 7), (bx + 3, FLOOR - 7), (bx + 3, FLOOR - 1), (bx + 6, FLOOR - 1), (bx + 6, FLOOR), (bx - 1, FLOOR)]), flat(), R['wood'][:4])
        meta['wall'] = [(38, 12), (98, 40)]
        meta['floor'] = [90, 100]
    elif kind == 'BALCONY':
        # A wall of glass panes: the greenhouse.
        meta['glass'] = (12, 8, 96, 34, False)
        for y in range(8, 43):
            for x in range(12, 108):
                cv.px[y][x] = (0, 0, 0, 0)
        for x in range(10, 110, 16):
            shade(cv, rect_mask(x, 6, 3, 38), cyl_normal(x + 1.5, 1.5), R['silver'])
        for y in (6, 24, 42):
            shade(cv, rect_mask(10, y, 100, 3), flat(0, -0.5, 1), R['silver'])
        vines(cv, 12, 108, 9)
        # Potting bench and pots.
        table(cv, 40, 40, FLOOR - 13)
        for i, (px_, k, s) in enumerate(((10, 0, 1.2), (26, 2, 0.8), (44, 1, 0.7), (60, 0, 0.7), (86, 1, 1.1), (100, 2, 0.9))):
            p = pot_plant(s, k)
            yb = FLOOR - 13 if 40 <= px_ < 78 else FLOOR
            cv.blit(p, px_, yb - p.h)
        meta['wall'] = []
        meta['floor'] = [80]
        meta['walk'] = (IN_L + 2, IN_R - 2)
    elif kind == 'KIDS':
        meta['glass'] = window(cv, 82, 12, 16, 22)
        # A toy shelf, a little bed, a rocking horse, blocks, a kite on the wall.
        bookcase(cv, 10, 26, 22, 36, 7)
        rocking_horse(cv, 40)
        for (bx, by, ramp) in ((66, FLOOR - 6, R['red']), (72, FLOOR - 6, R['blue']), (69, FLOOR - 12, R['brass'])):
            box(cv, bx, by, 6, 6, ramp, depth=1)
            cv.set(bx + 2, by + 3, R['cream'][4])
        shade(cv, poly_mask([(48, 10), (56, 16), (48, 26), (40, 16)]), lambda a, b: ((a - 48) / 10, 0, 0.9), R['orange'])
        cv.line(48, 10, 48, 26, R['red'][1])
        cv.line(40, 16, 56, 16, R['red'][1])
        for k in range(8):
            cv.set(48 + k % 2, 27 + k, R['cream'][3])
        meta['wall'] = [(62, 12)]
        meta['floor'] = [80, 96]
    elif kind == 'STUDY':
        meta['glass'] = window(cv, 50, 10, 14, 22)
        bookcase(cv, 10, 14, 24, 48, 3)
        scroll_desk(cv, 68)
        meta['flames'] += [small_candle(cv, 88, FLOOR - 14, 8) + (0.7,)]
        lights((82, FLOOR - 28), r=16, s=60)
        # Pinned maps.
        shade(cv, rect_mask(72, 10, 20, 14), flat(), R['cream'], dither=0.3)
        cv.line(75, 14, 89, 20, R['red'][2])
        cv.line(76, 20, 86, 12, R['blue'][2])
        cv.set(73, 11, R['red'][3])
        meta['wall'] = [(38, 12), (96, 10)]
        meta['floor'] = [38, 100]
    elif kind == 'LAUNDRY':
        meta['glass'] = window(cv, 92, 12, 12, 20)
        clothesline(cv, 10, 86, 9)
        loom(cv, 12)
        # Washtub with a washboard and a basket of linen.
        tub(cv, 46, 22)
        shade(cv, rect_mask(52, FLOOR - 26, 9, 12), lambda a, b: (0, 0.5 if b % 2 else -0.5, 1), R['wood'])
        meta['steam'] += [(56, FLOOR - 20)]
        meta['wall'] = []
        meta['floor'] = [74, 98]
    elif kind == 'PETS':
        meta['glass'] = window(cv, 50, 12, 14, 22)
        cat_tree(cv, 12)
        put(cv, D.cat(0), 30, FLOOR)
        # A perch with a little bird, and the fishbowl on a stand.
        cv.hline(78, 100, 22, R['wood'][2])
        cv.hline(78, 100, 23, R['wood'][1])
        bird = Canvas(10, 9)
        shade(bird, ellipse_mask(5, 5, 4, 3.4), ellipsoid_normal(5, 4, 4, 3.4), R['teal'])
        bird.set(7, 3, EYE)
        bird.set(9, 4, R['brass'][3])
        cv.blit(bird, 84, 13)
        table(cv, 92, 14, FLOOR - 16)
        fishbowl(cv, 92, FLOOR - 30)
        meta['wall'] = [(66, 10)]
        meta['floor'] = [68, 82]
    else:  # WHOLE: the tower's heart
        meta['glass'] = window(cv, 90, 12, 14, 22)
        fp = D.fireplace(0)
        cv.blit(fp, 42, FLOOR - fp.h)
        meta['flames'] += [(54, FLOOR - 6, 1.6), (60, FLOOR - 7, 1.9), (66, FLOOR - 6, 1.6)]
        heart_crystal(cv, 60, 18)
        # Spiral stairs going up on the left.
        for k in range(8):
            shade(cv, rect_mask(10 + (k % 2) * 4, FLOOR - 6 - k * 6, 18, 3), flat(0, -0.6, 1), R['wood'])
        shade(cv, rect_mask(8, 6, 3, FLOOR - 6), cyl_normal(9.5, 1.5), R['wood'])
        meta['wall'] = [(30, 10), (80, 10)]
        meta['floor'] = [82, 100]

    outer_walls(cv)
    # The room is lit warmer near the floor and the lamps; a cool dim in the top corners.
    for y in range(5, 18):
        for x in list(range(IN_L, IN_L + 10)) + list(range(IN_R - 9, IN_R + 1)):
            if cv.get(x, y) is not None and cv.get(x, y)[3] > 0 and (x + y) % 2 == 0:
                cv.set(x, y, R['night'][1], 50)
    return cv, meta


# ---------------------------------------------------------------- roof and base

def roof():
    cv = Canvas(128, 70)
    cx = 64
    # Chimney behind the cone.
    shade(cv, rect_mask(88, 22, 10, 26), lambda a, b: ((a - 93) / 10, 0, 1), R['red'], dither=0.3)
    for y in range(24, 48, 4):
        cv.hline(88, 97, y, R['red'][0])
    shade(cv, rect_mask(86, 19, 14, 4), flat(0, -0.6, 1), R['stone'])
    # The cone, shingled and shaded round.
    top, base = 12, 62
    pts = [(cx, top - 2), (cx + 62, base), (cx - 62, base)]
    S = shade(cv, poly_mask(pts), lambda x, y: ((x - cx) / ((y - top + 1) * 62 / (base - top) + 1), -0.35, 0.85), R['purple'], dither=0.5)
    for (x, y) in S:
        row = (y - top) // 4
        if (y - top) % 4 == 3 or ((x + row * 3) % 7 == 0 and (y - top) % 4 != 0):
            c = cv.get(x, y)
            cv.set(x, y, R['purple'][0], 150)
        elif (y - top) % 4 == 0:
            cv.set(x, y, R['purple'][4], 70)
    # Eave.
    shade(cv, rect_mask(0, base, 128, 4), flat(0, -0.5, 1), R['wood'])
    cv.hline(0, 127, base + 4, R['wood'][0])
    # Two dormer windows with warm light.
    for dx in (44, 76):
        shade(cv, poly_mask([(dx - 7, 46), (dx + 7, 46), (dx, 38)]), flat(0.2, -0.6, 0.8), R['red'])
        shade(cv, rect_mask(dx - 5, 46, 10, 10), flat(), R['cream'][:3])
        cv.rect(dx - 3, 47, 6, 8, R['fire'][3])
        cv.rect(dx - 3, 51, 6, 4, R['fire'][2])
        cv.vline(dx, 47, 54, R['wood'][1])
        cv.hline(dx - 3, dx + 2, 50, R['wood'][1])
        glow(cv, dx, 51, 9, R['fire'][3], 70, 4)
    # Spire with a crescent finial.
    shade(cv, rect_mask(cx - 1, 0, 3, 12), cyl_normal(cx + 0.5, 1.5), R['brass'])
    return cv, {'chimney': (93, 18), 'lantern': (cx, 2), 'h': 70}


def base():
    cv = Canvas(120, 34)
    wall_stone(cv, 0, 120, 0, 28, seed=9)
    cv.hline(0, 119, 0, R['stone'][0])
    for y in range(28):
        cv.set(0, y, R['stone'][1])
        cv.set(119, y, R['stone'][0])
    # The front door in a pointed arch, with steps.
    m = lambda a, b: abs(a + 0.5 - 60) <= 13 and b >= 6 or (abs(a + 0.5 - 60) <= 13 and (a + 0.5 - 60) ** 2 + (b - 14) ** 2 * 1.4 <= 13 * 13)
    shade(cv, lambda a, b: m(a, b) and b < 28, flat(), R['stone'][2:], dither=0.2)
    inner = lambda a, b: abs(a + 0.5 - 60) <= 10 and b >= 9 or ((a + 0.5 - 60) ** 2 + (b - 15) ** 2 * 1.4 <= 10 * 10)
    S = shade(cv, lambda a, b: inner(a, b) and b < 28, lambda a, b: ((a - 60) / 20, 0, 1), R['wood'], dither=0.4)
    for (a, b) in S:
        if (a - 50) % 4 == 0:
            cv.set(a, b, R['wood'][1])
    for b in (14, 22):
        for a in range(50, 71):
            if (a, b) in S:
                cv.set(a, b, R['ink'][1])
    shade(cv, ellipse_mask(65, 19, 1.8, 1.8), ellipsoid_normal(65, 19, 1.8, 1.8), R['brass'], spec=True)
    for k, (x, w) in enumerate(((44, 32), (40, 40))):
        shade(cv, rect_mask(x, 28 + k * 3, w, 3), flat(0, -0.6, 1), R['stone'])
    # Torch brackets.
    for tx in (34, 84):
        shade(cv, rect_mask(tx, 12, 3, 6), flat(), R['ink'])
        shade(cv, rect_mask(tx - 1, 11, 5, 2), flat(), R['brass'])
        glow(cv, tx + 1.5, 9, 12, R['fire'][3], 80, 5)
    return cv, {'torches': [(35, 10), (85, 10)], 'ground': 28}


# ---------------------------------------------------------------- small animated pieces

def flame_frames():
    out = []
    for f in range(3):
        cv = Canvas(7, 11)
        h = 8 + (f % 2)
        sway = (-0.6, 0, 0.6)[f]
        shade(cv, poly_mask([(3.5 + sway, 0 + (1 - f % 2)), (6.5, 7), (3.5, 10.5), (0.5, 7)]), flat(), R['fire'][1:2], outline=False)
        shade(cv, poly_mask([(3.5 + sway, 2 + (1 - f % 2)), (5.6, 7.5), (3.5, 10), (1.4, 7.5)]), flat(), R['fire'][2:3], outline=False)
        shade(cv, poly_mask([(3.5 + sway * 0.5, 5), (4.6, 8), (3.5, 9.6), (2.4, 8)]), flat(), R['fire'][4:5], outline=False)
        out.append(cv)
    return out


def steam_frames():
    out = []
    for f in range(4):
        cv = Canvas(12, 16)
        for k in range(3):
            y = 14 - ((f * 4 + k * 5) % 15)
            x = 5 + math.sin((f + k) * 1.3) * 2.5
            r = 1.5 + (14 - y) / 7
            a = int(150 * (y / 14))
            for py_ in range(cv.h):
                for px_ in range(cv.w):
                    if ellipse_mask(x, y, r, r * 0.8)(px_, py_):
                        cv.set(px_, py_, R['silver'][4] if px_ < x else R['silver'][3], a)
        out.append(cv)
    return out


def moon():
    cv = Canvas(18, 18)
    shade(cv, ellipse_mask(9, 9, 8, 8), ellipsoid_normal(9, 9, 8, 8), R['cream'][1:] + [WHITE], spec=False, dither=0.4)
    for (x, y, r) in ((6, 6, 2), (11, 11, 1.6), (12, 5, 1.2), (5, 12, 1)):
        shade(cv, ellipse_mask(x, y, r, r), lambda a, b: (0.5, 0.5, 0.7), R['cream'][1:3], outline=False)
    return cv


def cloud(seed=0):
    cv = Canvas(36, 14)
    for k, (x, y, r) in enumerate(((8, 9, 5), (16, 6, 6.5), (25, 8, 5.5), (31, 10, 3.6), (4, 11, 3))):
        shade(cv, ellipse_mask(x, y, r, r * 0.8), ellipsoid_normal(x, y, r, r * 0.8), R['silver'][2:] + [WHITE], outline=False, dither=0.4)
    return cv


def cobweb():
    cv = Canvas(12, 12)
    c = (220, 220, 236)
    for i in range(12):
        cv.set(i, 0, c, 150)
        cv.set(0, i, c, 150)
    for i in range(10):
        cv.set(i, i, c, 170)
    for r in (3, 6, 9):
        for i in range(r + 1):
            cv.set(i, r - i + (1 if i in (0, r) else 0), c, 120)
    return cv


if __name__ == '__main__':
    import sys
    items = []
    for k in ('KITCHEN', 'LIVING', 'BEDROOM', 'BATH', 'HALL', 'BALCONY', 'KIDS', 'STUDY', 'LAUNDRY', 'PETS', 'WHOLE'):
        cv, meta = scene(k)
        items.append((k, cv))
    items.append(('ROOF', roof()[0]))
    items.append(('BASE', base()[0]))
    sheet(items, sys.argv[1], scale=3, cols=3, bg=(70, 120, 170))
