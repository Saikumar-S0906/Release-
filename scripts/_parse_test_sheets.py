import zipfile
import xml.etree.ElementTree as ET
import re
import json
from pathlib import Path

NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL_NS = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id"


def col_row(cell_ref: str):
    m = re.match(r"([A-Z]+)(\d+)", cell_ref)
    col, row = m.group(1), int(m.group(2))
    n = 0
    for c in col:
        n = n * 26 + (ord(c) - 64)
    return n, row


def shared_strings(z):
    if "xl/sharedStrings.xml" not in z.namelist():
        return []
    root = ET.fromstring(z.read("xl/sharedStrings.xml"))
    out = []
    for si in root.findall("m:si", NS):
        texts = [t.text or "" for t in si.findall(".//m:t", NS)]
        out.append("".join(texts))
    return out


def sheet_map(z):
    wb = ET.fromstring(z.read("xl/workbook.xml"))
    rels = ET.fromstring(z.read("xl/_rels/workbook.xml.rels"))
    rid_to_target = {}
    for rel in rels:
        rid = rel.get(REL_NS) or rel.get("Id")
        rid_to_target[rid] = rel.get("Target")
    out = {}
    for s in wb.findall("m:sheets/m:sheet", NS):
        name = s.get("name")
        rid = s.get(REL_NS)
        target = rid_to_target[rid]
        if target.startswith("/"):
            target = target[1:]
        if target.startswith("xl/"):
            pass
        else:
            target = "xl/" + target.lstrip("/")
        out[name] = target
    return out


def load_sheet(z, path, shared):
    root = ET.fromstring(z.read(path))
    rows = {}
    for c in root.findall(".//m:sheetData/m:row/m:c", NS):
        ref = c.get("r")
        if not ref:
            continue
        col, row = col_row(ref)
        t = c.get("t")
        v = c.find("m:v", NS)
        is_el = c.find("m:is", NS)
        val = ""
        if t == "s" and v is not None and v.text is not None:
            val = shared[int(v.text)]
        elif t == "inlineStr" and is_el is not None:
            texts = [t.text or "" for t in is_el.findall(".//m:t", NS)]
            val = "".join(texts)
        elif v is not None and v.text is not None:
            val = v.text
        rows.setdefault(row, {})[col] = val
    return rows


def rows_as_table(rows, max_cols=14):
    if not rows:
        return [], []
    # find header: first row with many non-empty
    header_row = None
    for r in sorted(rows):
        vals = [str(rows[r].get(c, "")).strip() for c in range(1, max_cols + 1)]
        nonempty = sum(1 for v in vals if v)
        if nonempty >= 3:
            header_row = r
            break
    if header_row is None:
        header_row = min(rows)
    headers = [str(rows[header_row].get(c, f"C{c}")).strip() or f"C{c}" for c in range(1, max_cols + 1)]
    data = []
    for r in sorted(rows):
        if r <= header_row:
            continue
        vals = [str(rows[r].get(c, "")).strip() for c in range(1, max_cols + 1)]
        if not any(vals):
            continue
        data.append(dict(zip(headers, vals)))
    return headers, data


def summarize(xlsx: Path, sheet: str, max_cols=12):
    with zipfile.ZipFile(xlsx) as z:
        shared = shared_strings(z)
        sm = sheet_map(z)
        rows = load_sheet(z, sm[sheet], shared)
        headers, data = rows_as_table(rows, max_cols=max_cols)
        return headers, data


def main():
    base = Path(r"c:\Users\andro\AndroidStudioProjects\MyApplication")
    out = {}

    # Consolidated
    for sheet in ["01 Coverage matrix", "02 Master cases", "03 Device lab", "04 P0 exit"]:
        headers, data = summarize(base / "PixiJoy_Consolidated_Test_Sheet.xlsx", sheet, max_cols=12)
        out[f"cons::{sheet}"] = {"headers": headers, "count": len(data), "rows": data}

    # Master
    for sheet in ["Dashboard", "Master Test Cases", "Flow Coverage", "Test Data", "Traceability"]:
        headers, data = summarize(base / "PixiJoy_Master_Test_Sheet.xlsx", sheet, max_cols=14)
        # truncate huge master to keep file manageable — keep all for analysis later
        out[f"master::{sheet}"] = {"headers": headers, "count": len(data), "rows": data}

    dest = base / "scripts" / "_test_sheet_dump.json"
    dest.write_text(json.dumps(out), encoding="utf-8")
    print("wrote", dest)
    for k, v in out.items():
        print(k, "count=", v["count"], "headers=", v["headers"][:10])


if __name__ == "__main__":
    main()
