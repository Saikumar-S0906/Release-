"""Shared Colab-safe downloads for Wave M quality data. Never import tensorflow_datasets."""

from __future__ import annotations

import shutil
import subprocess
import sys
import urllib.request
import zipfile
from pathlib import Path


def pip_install(*pkgs: str) -> None:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", *pkgs])


def download(url: str, dest: Path, *, label: str = "") -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    print(f"download {label or url}", flush=True)

    def _progress(b: int, bs: int, t: int) -> None:
        if t > 0 and b % 25 == 0:
            print(f"  {min(100, int(100 * b * bs / t))}%", flush=True)

    req = urllib.request.Request(url, headers={"User-Agent": "PixiJoyML/1.0"})
    with urllib.request.urlopen(req, timeout=600) as r, open(dest, "wb") as f:
        total = int(r.headers.get("Content-Length") or 0)
        read = 0
        while True:
            chunk = r.read(1024 * 256)
            if not chunk:
                break
            f.write(chunk)
            read += len(chunk)
            if total and read // (1024 * 256) % 25 == 0:
                print(f"  {min(100, int(100 * read / total))}%", flush=True)


def download_first(urls: list[str], dest: Path) -> str:
    last: Exception | None = None
    for url in urls:
        try:
            download(url, dest, label=url)
            return url
        except Exception as e:
            last = e
            print(f"  fail: {e}", flush=True)
    raise RuntimeError(f"all mirrors failed: {last}")


def extract_images_from_zip(zpath: Path, out_dir: Path, *, limit: int | None = None) -> list[Path]:
    out_dir.mkdir(parents=True, exist_ok=True)
    paths: list[Path] = []
    with zipfile.ZipFile(zpath) as z:
        names = sorted(
            n
            for n in z.namelist()
            if not n.endswith("/")
            and Path(n).suffix.lower() in {".png", ".jpg", ".jpeg", ".webp", ".bmp"}
            and not Path(n).name.startswith(".")
        )
        for n in names:
            if limit is not None and len(paths) >= limit:
                break
            dest = out_dir / Path(n).name
            with z.open(n) as src, open(dest, "wb") as dst:
                shutil.copyfileobj(src, dst)
            paths.append(dest)
    return paths


# Official / academic mirrors only (no Hugging Face).
DIV2K_URLS = [
    "http://data.vision.ee.ethz.ch/cvl/DIV2K/DIV2K_valid_HR.zip",
]

# NYU Depth V2 labeled (~2.8 GB). Official Silberman / NYU host.
NYU_DEPTH_LABELED_URLS = [
    "http://horatio.cs.nyu.edu/mit/silberman/nyu_depth_v2/nyu_depth_v2_labeled.mat",
]

# FlyingChairs full zip is ~33 GB — too large for Colab auto-fetch.
# Prefer DIV2K+exact warp labels; optional local path if already downloaded.
FLYINGCHAIRS_URL = "https://lmb.informatik.uni-freiburg.de/data/FlyingChairs/FlyingChairs.zip"
