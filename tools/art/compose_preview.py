"""Mimics drawFloor/drawRoof/drawBase to check the composition of a whole tower."""
import sys
from pixlib import Canvas, to_png
import scenes as Sc
import helpers as Hp
import decor as D

GW, TX = 128, 4
SKY = [(11, 9, 32), (21, 17, 53), (33, 26, 74)]


def sky_rect(cv, x, y, w, h):
    for j in range(h):
        c = SKY[min(2, j * 3 // max(1, h))]
        for i in range(w):
            cv.set(x + i, y + j, c)


def floor(kind, helpers=(), imps=0, dec=()):
    cv = Canvas(GW, Sc.H)
    sky_rect(cv, 0, 0, GW, Sc.H)
    bg, m = Sc.scene(kind)
    gx, gy, gw, gh, _ = m['glass']
    sky_rect(cv, TX + gx, gy, gw, gh)
    for k in range(8):
        cv.set(TX + gx + (k * 7) % gw, gy + (k * 5) % gh, (255, 244, 214))
    cv.blit(bg, TX, 0)
    for i, d in enumerate(dec):
        s = D.DECOR[d]()
        x = m['floor'][i % len(m['floor'])] if m['floor'] else 40
        cv.blit(s, TX + x, Sc.FLOOR + 1 - s.h)
    for (x, y, s) in m['flames']:
        f = Sc.flame_frames()[0]
        if s >= 1:
            cv.blit(f, TX + x - f.w // 2, y - f.h + 1)
        else:
            cv.rect(TX + x - 1, y - 3, 2, 3, (255, 148, 40))
    for (x, y) in m['steam']:
        s = Sc.steam_frames()[1]
        cv.blit(s, TX + x - s.w // 2, y - s.h)
    for i, h in enumerate(helpers):
        s = Hp.HELPERS[h](False).shifted(1)
        x = m['walk'][0] + 10 + i * 34
        cv.blit(s, TX + x, Sc.FLOOR + 2 - s.h)
    for i in range(imps):
        s = Hp.imp(i % 2)
        x = [30, 66, 88][i]
        cv.blit(s, TX + x, Sc.FLOOR + 2 - s.h)
    return cv


def stack(cvs):
    h = sum(c.h for c in cvs)
    out = Canvas(GW, h)
    y = 0
    for c in cvs:
        out.blit(c, 0, y)
        y += c.h
    return out


roof_cv = Canvas(GW, 18 + 70)
sky_rect(roof_cv, 0, 0, GW, 88)
roof_cv.blit(Sc.moon(), 10, 6)
roof_cv.blit(Sc.roof()[0], 0, 18)
base_cv = Canvas(GW, 44)
sky_rect(base_cv, 0, 0, GW, 28)
base_cv.rect(0, 28, GW, 16, (35, 84, 56))
base_cv.rect(55, 28, 18, 16, (200, 168, 120))
base_cv.blit(Sc.base()[0], TX, 0)

tower = stack([
    roof_cv,
    floor('BEDROOM', ['PILLOW'], 0, ['OWL']),
    floor('KITCHEN', ['SPONGE', 'LADLE'], 1, ['POTIONS']),
    floor('LIVING', ['DUSTER'], 2, ['CAT']),
    floor('BATH', ['BUCKET'], 1, []),
    base_cv,
])
to_png(tower, sys.argv[1], scale=3)
