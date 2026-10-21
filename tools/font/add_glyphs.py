"""Adds the few punctuation marks the app uses that m6x11cyrillic lacks, drawn on its 128-unit pixel grid.

python tools/font/add_glyphs.py <m6x11cyrillic.ttf> <out.ttf>

m6x11 by Daniel Linssen (@managore), Cyrillic by Natto Games: free to use with attribution.
"""
import sys

from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

P = 128  # one font pixel

# Each glyph: advance in pixels, then rows of pixels from the top (row 0 is y = 10, the cap line).
GLYPHS = {
    0x00B7: ('periodcentered', 3, ['', '', '', '', '', '##', '##']),
    0x2013: ('endash', 9, ['', '', '', '', '', '', '########']),
    0x2014: ('emdash', 12, ['', '', '', '', '', '', '###########']),
    0x2212: ('minus', 7, ['', '', '', '', '', '', '######']),
    0x2026: ('ellipsis', 9, ['', '', '', '', '', '', '', '', '', '## ## ##', '## ## ##']),
    0x201C: ('quotedblleft', 6, [' #  #', '#  #', '## ##', '## ##']),
    0x201D: ('quotedblright', 6, ['## ##', '## ##', ' #  #', '#  #']),
    0x2018: ('quoteleft', 3, [' #', '#', '##', '##']),
    0x2019: ('quoteright', 3, ['##', '##', ' #', '#']),
    0x2713: ('checkmark', 9, ['', '', '', '       #', '      ##', '#    ##', '##  ##', ' ####', '  ##']),
}


def draw(rows):
    pen = TTGlyphPen(None)
    for r, row in enumerate(rows):
        y = (10 - r) * P
        for c, ch in enumerate(row):
            if ch == '#':
                x = c * P
                pen.moveTo((x, y))
                pen.lineTo((x, y + P))
                pen.lineTo((x + P, y + P))
                pen.lineTo((x + P, y))
                pen.closePath()
    return pen.glyph()


def main(src, out):
    f = TTFont(src)
    cmap = f.getBestCmap()
    order = f.getGlyphOrder()
    for code, (name, adv, rows) in GLYPHS.items():
        if code in cmap:
            continue
        g = draw(rows)
        f['glyf'][name] = g
        g.recalcBounds(f['glyf'])
        f['hmtx'][name] = (adv * P, g.xMin if g.numberOfContours else 0)
        if name not in order:
            order.append(name)
        for t in f['cmap'].tables:
            if t.isUnicode():
                t.cmap[code] = name
    f.setGlyphOrder(order)
    f['maxp'].numGlyphs = len(order)
    f.save(out)


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
