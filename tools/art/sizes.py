"""Dumps the logical size (in scene cells) of every sprite the old generator drew, so new art fits the same boxes."""
import json
import os
import decor as D
import helpers as Hp
import scenes as Sc
import extras as X

def wh(c):
    return [c.w, c.h]

out = {}
for k, f in Hp.HELPERS.items():
    out[f'helper/{k}'] = wh(f(False))
out['imp'] = wh(Hp.imp(0))
for k, f in D.DECOR.items():
    try:
        out[f'decor/{k}'] = wh(f(0))
    except TypeError:
        out[f'decor/{k}'] = wh(f())
for k, f in list(X.SEASONAL.items()) + list(X.SOUVENIRS.items()):
    out[f'decor/{k}'] = wh(f(0))
out['master'] = wh(D.master(0))
out['apprentice'] = wh(D.apprentice(0))
out['flame'] = [wh(c) for c in Sc.flame_frames()]
out['steam'] = [wh(c) for c in Sc.steam_frames()]
for k, c in {'moon': Sc.moon(), 'cloud': Sc.cloud(), 'cobweb': Sc.cobweb(), 'tea': X.tea_table(), 'heart': X.heart()}.items():
    out[f'extra/{k}'] = wh(c)
for k, f in X.TRINKETS.items():
    out[f'trinket/{k}'] = wh(f())
out['cat/sleep'] = wh(X.cat_sleep(0))
out['cat/sit'] = wh(X.cat_sit(0))
out['body'] = wh(X.apprentice_body(0))
for k, f in X.HATS.items():
    out[f'hat/{k}'] = wh(f(0))
out['bat'] = wh(X.bat(0))
json.dump(out, open(os.path.join(os.path.dirname(__file__), 'sizes.json'), 'w'), indent=0)
print(json.dumps(out))
