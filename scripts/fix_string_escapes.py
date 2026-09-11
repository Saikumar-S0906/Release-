#!/usr/bin/env python3
"""Fix Android string escaping in all locale strings.xml files."""
import os
import xml.etree.ElementTree as ET
from xml.dom import minidom

RES_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res"))


def escape_android_string(text: str) -> str:
    escaped = text.replace("\\", "\\\\")
    escaped = escaped.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    escaped = escaped.replace("'", "\\'")
    escaped = escaped.replace('"', '\\"')
    return escaped


def unescape_for_compare(text: str) -> str:
    return (
        text.replace("\\'", "'")
        .replace('\\"', '"')
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("\\\\", "\\")
    )


def write_strings(path: str, strings: dict[str, str]) -> None:
    root = ET.Element("resources")
    for key in sorted(strings.keys()):
        elem = ET.SubElement(root, "string", name=key)
        elem.text = escape_android_string(strings[key])
    xml_str = ET.tostring(root, encoding="unicode")
    pretty = minidom.parseString(xml_str).toprettyxml(indent="    ")
    lines = [line for line in pretty.split("\n") if line.strip()]
    content = '<?xml version="1.0" encoding="utf-8"?>\n' + "\n".join(lines[1:]) + "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def write_plurals(path: str, plurals: dict[str, dict[str, str]]) -> None:
    root = ET.Element("resources")
    for name in sorted(plurals.keys()):
        plural_elem = ET.SubElement(root, "plurals", name=name)
        for qty in sorted(plurals[name].keys()):
            item = ET.SubElement(plural_elem, "item", quantity=qty)
            item.text = escape_android_string(plurals[name][qty])
    xml_str = ET.tostring(root, encoding="unicode")
    pretty = minidom.parseString(xml_str).toprettyxml(indent="    ")
    lines = [line for line in pretty.split("\n") if line.strip()]
    content = '<?xml version="1.0" encoding="utf-8"?>\n' + "\n".join(lines[1:]) + "\n"
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def parse_plurals(path: str) -> dict[str, dict[str, str]]:
    tree = ET.parse(path)
    result: dict[str, dict[str, str]] = {}
    for plural in tree.getroot():
        if plural.tag != "plurals":
            continue
        name = plural.attrib.get("name")
        if not name:
            continue
        items: dict[str, str] = {}
        for item in plural:
            if item.tag == "item":
                qty = item.attrib.get("quantity")
                if qty:
                    items[qty] = unescape_for_compare(item.text or "")
        result[name] = items
    return result


def main() -> None:
    for name in sorted(os.listdir(RES_DIR)):
        if not name.startswith("values"):
            continue
        folder = os.path.join(RES_DIR, name)
        strings_path = os.path.join(folder, "strings.xml")
        if os.path.exists(strings_path):
            tree = ET.parse(strings_path)
            strings = {
                child.attrib["name"]: unescape_for_compare(child.text or "")
                for child in tree.getroot()
                if child.tag == "string" and child.attrib.get("name")
            }
            write_strings(strings_path, strings)
            print(f"Fixed {strings_path}")
        plurals_path = os.path.join(folder, "plurals.xml")
        if os.path.exists(plurals_path):
            plurals = parse_plurals(plurals_path)
            write_plurals(plurals_path, plurals)
            print(f"Fixed {plurals_path}")


if __name__ == "__main__":
    main()
