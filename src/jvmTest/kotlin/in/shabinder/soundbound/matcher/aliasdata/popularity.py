# ListenBrainz listen counts for every artist in the extract_dump.py output, 1,000 MBIDs per request, ~1 req/s.
# Usage: python3 -I popularity.py dump_cross.jsonl > pop.json
import json, sys, time, urllib.request

ids = [json.loads(line)["id"] for line in open(sys.argv[1])]
pop = {}
for i in range(0, len(ids), 1000):
    req = urllib.request.Request(
        "https://api.listenbrainz.org/1/popularity/artist",
        data=json.dumps({"artist_mbids": ids[i:i + 1000]}).encode(),
        headers={"Content-Type": "application/json", "User-Agent": "soundbound-extensions-lib alias table build"},
    )
    for row in json.load(urllib.request.urlopen(req, timeout=120)):
        pop[row["artist_mbid"]] = row.get("total_listen_count") or 0
    time.sleep(1)
json.dump(pop, sys.stdout)
