# Reads MusicBrainz artist JSON-dump lines on stdin (tar stream member mbdump/artist), keeps artists whose name,
# sort-name or aliases span Latin and non-Latin scripts. Writes compact jsonl to stdout.
import json, re, sys

NONLAT = re.compile(r'[^\x00-ͯḀ-ỿ -⁯]')
LAT = re.compile(r'[A-Za-zÀ-ɏ]')

for line in sys.stdin.buffer:
    line = line.strip()
    if not line.startswith(b'{'):
        # tar headers glue onto the first line of a member; find the JSON start
        i = line.find(b'{"')
        if i < 0: continue
        line = line[i:]
    try:
        a = json.loads(line)
    except Exception:
        continue
    names = [a.get("name") or "", a.get("sort-name") or ""]
    al = a.get("aliases") or []
    names += [x.get("name") or "" for x in al] + [x.get("sort-name") or "" for x in al]
    if not al: continue
    if not any(NONLAT.search(n) for n in names) or not any(LAT.search(n) for n in names): continue
    sys.stdout.write(json.dumps({
        "id": a["id"], "name": a["name"], "sort": a.get("sort-name"), "type": a.get("type"),
        "dis": a.get("disambiguation"), "country": a.get("country"),
        "aliases": [{"n": x.get("name"), "s": x.get("sort-name"), "l": x.get("locale"), "t": x.get("type"),
                     "p": x.get("primary"), "end": x.get("end")} for x in al],
    }, ensure_ascii=False) + "\n")
