import json
from pathlib import Path
from _parse_test_sheets import summarize

base = Path(__file__).resolve().parent.parent
headers, data = summarize(base / "PixiJoy_Consolidated_Test_Sheet.xlsx", "02 Master cases", max_cols=16)
p0 = [r for r in data if r.get("Priority") == "P0"]
out = base / "scripts" / "_p0_cases.json"
# Only write if missing or empty — parent may create richer file
if not out.exists() or out.stat().st_size < 100:
    out.write_text(
        json.dumps({"count": len(p0), "headers": headers, "cases": p0}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print("wrote", out, "count", len(p0))
else:
    print("kept existing", out, "size", out.stat().st_size)

lines = []
for r in p0:
    lines.append(
        "\t".join(
            [
                r.get("ID", ""),
                r.get("Module", ""),
                r.get("Flow", ""),
                r.get("Type", ""),
                r.get("Title", "").replace("\t", " "),
                r.get("Expected", "").replace("\t", " "),
                r.get("Precondition", "").replace("\t", " "),
                r.get("Steps", "").replace("\t", " "),
                r.get("Device", "").replace("\t", " "),
                r.get("Role", "").replace("\t", " "),
            ]
        )
    )
(base / "scripts" / "_p0_compact.tsv").write_text("\n".join(lines), encoding="utf-8")
print("tsv", len(lines))
