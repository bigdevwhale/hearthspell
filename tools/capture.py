"""Screenshots of the demo build for README, Stillcraft and Google Play, one language at a time.

ANDROID_SERIAL=<device> python tools/capture.py <en|ru> <raw_dir> [names...]

Needs the demo build (`./gradlew installDebug -Pdemo`) on a 1080 px wide phone. The demo build takes two routes
for this: lang/<en|ru> switches its language and reseed rebuilds the lived-in tower as of now, in that language.
Each screen is then opened by its route and captured as <name>.png (English) or <name>.ru.png (Russian);
tools/shots.py and tools/play.py turn the captures into README and Play images.
"""
import os
import sqlite3
import subprocess
import sys
import time
from pathlib import Path

DEV = os.environ.get("ANDROID_SERIAL", "")
PKG = "app.hearthspell.demo"
ACT = f"{PKG}/app.hearthspell.MainActivity"
lang, out = sys.argv[1], Path(sys.argv[2])
only = set(sys.argv[3:])
out.mkdir(parents=True, exist_ok=True)
suffix = ".ru" if lang == "ru" else ""


def adb(*a, **kw):
    return subprocess.run(["adb", *(["-s", DEV] if DEV else []), *a], capture_output=True, **kw)


def route(r, wait=2.5):
    adb("shell", "am", "start", "-n", ACT, "--es", "route", r)
    time.sleep(wait)


def shot(name):
    png = adb("exec-out", "screencap", "-p").stdout
    (out / f"{name}{suffix}.png").write_bytes(png)
    print("shot", name, len(png), flush=True)


def tap(x, y, wait=1.5):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)


def swipe(y0, y1, wait=1.5):
    adb("shell", "input", "swipe", "540", str(y0), "540", str(y1), "600")
    time.sleep(wait)


def ids():
    """Floor and spell ids from the demo database."""
    tmp = out / "_db"
    tmp.mkdir(exist_ok=True)
    for f in ["hearthspell.db", "hearthspell.db-wal", "hearthspell.db-shm"]:
        data = adb("exec-out", "run-as", PKG, "cat", f"databases/{f}").stdout
        (tmp / f).write_bytes(data)
    db = sqlite3.connect(tmp / "hearthspell.db")
    floors = {k: i for i, k in db.execute("select id, kind from floors")}
    spells = {k: i for i, k in db.execute("select id, presetKey from spells")}
    return floors, spells


if "noreseed" not in only:
    route(f"lang/{lang}", 4)
    route("reseed", 5)
    route("tower", 3)
floors, spells = ids()
print(floors)
want = lambda n: not only or n in only

if want("tower"):
    route("tower", 3); shot("tower")
if want("floors"):
    route("tower", 2); swipe(1900, 500, 2.5); shot("floors")
if want("floor"):
    route(f"floor/{floors['KITCHEN']}", 3); shot("floor")
if want("cat"):
    route(f"floor/{floors['LIVING']}", 3); shot("cat")
if want("spell"):
    route(f"spell/{spells['kitchen.dishes']}", 3); shot("spell")
if want("helper"):
    route("helper/SPONGE", 3); shot("helper")
if want("grimoire"):
    route("grimoire", 3); shot("grimoire")
if want("helpers"):
    route("grimoire", 2); tap(540, 680, 2); shot("helpers"); tap(200, 680, 1)
if want("collections"):
    route("grimoire", 2); tap(866, 680, 2); shot("collections"); tap(200, 680, 1)
if want("market"):
    route("market", 3); shot("market")
if want("trip"):
    route("trip", 3); shot("trip")
if want("stats"):
    route("stats", 3); shot("stats")
if want("postcards"):
    route("postcards", 6); shot("postcards")
for n in ["decor?floor=0", "letters", "bestiary", "stars", "tea", "wardrobe", "brew", "treasures"]:
    name = n.split("?")[0]
    if want(name):
        route(n, 3); shot(name)
if want("letter"):
    route("letter/0", 3); shot("letter")
if want("note"):
    route("welcome", 3); shot("note")
