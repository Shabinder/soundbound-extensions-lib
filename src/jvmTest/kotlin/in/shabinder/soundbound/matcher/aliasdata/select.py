# MusicBrainz artist-dump extract (extract_dump.py) + ListenBrainz popularity -> alias groups for the generator.
# Usage: python3 -I select.py dump_cross.jsonl pop.json MIN_LATIN MIN_NONLATIN > groups.json
# Latin-primary artists (BTS, The Beatles) need MIN_LATIN listens, non-Latin-primary ones (王菲) MIN_NONLATIN: global
# listen counts underrate CJK/Indic catalogues. Only scripts the romanizer reads are kept (no Georgian/Ethiopic names).
import json, re, sys

dump, pop = sys.argv[1], json.load(open(sys.argv[2]))
min_lat, min_nl = int(sys.argv[3]), int(sys.argv[4])
NONLAT = re.compile(r'[^\x00-ͯḀ-ỿ -⁯]')
OTHER = re.compile(r'[԰-֏܀-ࣿ඀-෿຀-࿿က-᷿Ⰰ-⿿ꀀ-꯿]')
groups = []
for line in open(dump):
    a = json.loads(line)
    names = [a["name"], a.get("sort") or ""]
    # Names stores show first: English primary alias, other "Artist name" aliases (English first), then the rest.
    # Search hints are mostly mis-encoded spellings (¤ýµá); alias sort-names are mostly kana readings no store shows.
    rank = lambda x: (not (x["l"] == "en" and x["p"]), x["t"] != "Artist name", (x["l"] or "")[:2] != "en", x["end"] is not None)
    for x in sorted((x for x in a["aliases"] if x["t"] != "Search hint"), key=rank):
        names.append(x["n"] or "")
    names = [n for n in dict.fromkeys(n.strip() for n in names) if n and not OTHER.search(n) and "(" not in n]
    if len(names) < 2: continue
    listens = pop.get(a["id"], 0)
    if listens < (min_nl if NONLAT.search(a["name"]) else min_lat): continue
    groups.append((listens, names))
groups.sort(key=lambda g: -g[0])
json.dump([{"listens": p, "names": n} for p, n in groups], sys.stdout, ensure_ascii=False)
