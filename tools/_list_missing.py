import csv, json, pathlib
R = pathlib.Path(__file__).resolve().parent
k = {}
for p in sorted(R.glob("i18n_master*.tsv")):
    with p.open(encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f, delimiter="\t"):
            k[row["key"]] = row
extra = json.loads((R / "i18n_remaining.json").read_text(encoding="utf-8"))
for key, locs in extra.items():
    row = k.setdefault(key, {"key": key})
    row.update(locs)
en = json.loads((R / "strings_en.json").read_text(encoding="utf-8"))
miss = [x for x in en if x not in k or any(not str(k[x].get(loc, "")).strip() for loc in ["zh-rCN","zh-rTW","th","vi","tr","ru","pl","fil","ms","uk"])]
print("\n".join(miss))
print("#", len(miss))
