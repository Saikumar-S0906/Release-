import pathlib, re, sys
sys.stdout.reconfigure(encoding="utf-8")
ROOT = pathlib.Path(r"c:\Users\andro\PixiJoy\PixiJoy")
for loc in ["hi", "ar", "iw", "ur", "ja", "ko"]:
    p = ROOT / f"app/src/main/res/values-{loc}/strings.xml"
    text = p.read_text(encoding="utf-8")
    names = re.findall(r'<string name="([^"]+)"', text)
    m = re.search(r'<string name="studio_tagline">(.*?)</string>', text)
    print(loc, "xml_keys", len(names), "tagline", (m.group(1)[:50] if m else "?"), "ok", "machine-assisted" in text)
