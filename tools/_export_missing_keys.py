import csv
import json
import pathlib

R = pathlib.Path(__file__).resolve().parent
k = set()
for p in sorted(R.glob("i18n_master*.tsv")):
    with p.open(encoding="utf-8", newline="") as f:
        k.update(row["key"] for row in csv.DictReader(f, delimiter="\t"))
en = list(json.loads((R / "strings_en.json").read_text(encoding="utf-8")).keys())
miss = [x for x in en if x not in k]
(R / "_missing_keys.txt").write_text("\n".join(miss), encoding="utf-8")
print(len(miss))
