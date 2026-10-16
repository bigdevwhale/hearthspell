# Art generator

All pixel art in Hearthspell is drawn by these scripts (Python 3 + Pillow), not by hand in an editor.

- `pixlib.py`: the toolkit (hue-shifted colour ramps, shading by surface normals with ordered dither, selective outlines, stepped glow).
- `helpers.py`: the 17 helpers and the imp. `decor.py`: the 20 decor items, the master and the apprentice.
- `scenes.py`: the eleven floor rooms, the roof and the base, plus flames, steam, moon, cloud and cobweb.
- `export.py <repo root>`: writes `ui/art/Art.kt` (encoded sprites), `res/drawable-nodpi/hs_*.png` (scenes) and `ui/tower/SceneMeta.kt` (where windows, flames and decor spots are).
- `icon.py <repo root>`: the launcher icon as vector drawables.
- `compose_preview.py out.png`: a whole tower composed the way the app draws it, to check placement.

Each module can also be run on its own with an output PNG path to get a preview sheet.
