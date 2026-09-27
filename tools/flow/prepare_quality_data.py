#!/usr/bin/env python3
"""M4 flow quality data: DIV2K (ETH) + exact warp GT — no Hugging Face.

Official FlyingChairs zip is ~33 GB (Freiburg); too large for Colab auto-fetch.
If you already have FlyingChairs unpacked locally, pass --chairs-root.
"""

from __future__ import annotations

import argparse
import shutil
import struct
import sys
from pathlib import Path

import numpy as np

_TOOLS = Path(__file__).resolve().parents[1]
if str(_TOOLS) not in sys.path:
    sys.path.insert(0, str(_TOOLS))

from colab_fetch import DIV2K_URLS, download_first, extract_images_from_zip, pip_install

SIZE = 256


def _count_trips(data: Path) -> int:
    from real_data import flow_triplets

    return len(flow_triplets(data))


def _warp(img: np.ndarray, flow: np.ndarray) -> np.ndarray:
    h, w = img.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x2 = np.clip(xx + flow[..., 0], 0, w - 1)
    y2 = np.clip(yy + flow[..., 1], 0, h - 1)
    x0, y0 = np.floor(x2).astype(np.int32), np.floor(y2).astype(np.int32)
    x1, y1 = np.minimum(x0 + 1, w - 1), np.minimum(y0 + 1, h - 1)
    wx, wy = (x2 - x0)[..., None], (y2 - y0)[..., None]
    Ia, Ib = img[y0, x0].astype(np.float32), img[y0, x1].astype(np.float32)
    Ic, Id = img[y1, x0].astype(np.float32), img[y1, x1].astype(np.float32)
    return np.clip((Ia * (1 - wx) + Ib * wx) * (1 - wy) + (Ic * (1 - wx) + Id * wx) * wy, 0, 255).astype(np.uint8)


def _make_flow(rng: np.random.Generator) -> np.ndarray:
    sh = sw = 8
    small = rng.normal(0, 4.0, size=(sh, sw, 2)).astype(np.float32)
    yi = np.linspace(0, sh - 1, SIZE).astype(np.int32).clip(0, sh - 1)
    xi = np.linspace(0, sw - 1, SIZE).astype(np.int32).clip(0, sw - 1)
    flow = small[yi][:, xi].copy()
    flow[..., 0] += float(rng.uniform(-6, 6))
    flow[..., 1] += float(rng.uniform(-6, 6))
    return flow


def _read_flo(path: Path) -> np.ndarray:
    """Middlebury .flo → HxWx2 float32."""
    with open(path, "rb") as f:
        magic = struct.unpack("<f", f.read(4))[0]
        if abs(magic - 202021.25) > 1e-3:
            raise ValueError(f"bad .flo magic in {path}")
        w, h = struct.unpack("<ii", f.read(8))
        data = np.frombuffer(f.read(4 * w * h * 2), dtype=np.float32).reshape(h, w, 2)
    return data.copy()


def fetch_local_chairs(data: Path, chairs_root: Path, n: int) -> int:
    """Optional: convert already-downloaded FlyingChairs (official Freiburg layout)."""
    pip_install("pillow")
    from PIL import Image

    data_dir = chairs_root / "data" if (chairs_root / "data").is_dir() else chairs_root
    flows = sorted(data_dir.glob("*_flow.flo"))
    if not flows:
        print(f"no *.flo under {data_dir}", flush=True)
        return 0
    f0, f1, fl = data / "frame0", data / "frame1", data / "flow_npy"
    for d in (f0, f1, fl):
        d.mkdir(parents=True, exist_ok=True)
    count = 0
    print(f"local FlyingChairs: {len(flows)} flo files, taking ≤{n}", flush=True)
    for flo_p in flows:
        if count >= n:
            break
        stem0 = flo_p.name.replace("_flow.flo", "")
        img1 = data_dir / f"{stem0}_img1.ppm"
        img2 = data_dir / f"{stem0}_img2.ppm"
        if not img1.is_file() or not img2.is_file():
            continue
        try:
            flow = _read_flo(flo_p)
            ia = Image.open(img1).convert("RGB").resize((SIZE, SIZE))
            ib = Image.open(img2).convert("RGB").resize((SIZE, SIZE))
            h0, w0 = flow.shape[:2]
            ys = np.linspace(0, h0 - 1, SIZE).astype(np.int32)
            xs = np.linspace(0, w0 - 1, SIZE).astype(np.int32)
            fl_arr = flow[ys][:, xs].copy()
            fl_arr[..., 0] *= SIZE / float(w0)
            fl_arr[..., 1] *= SIZE / float(h0)
            fl_arr = (fl_arr / float(SIZE)).astype(np.float32)
            stem = f"chairs_{count:04d}"
            ia.save(f0 / f"{stem}.png")
            ib.save(f1 / f"{stem}.png")
            np.save(fl / f"{stem}.npy", fl_arr)
            count += 1
        except Exception as e:
            print(f"  skip {flo_p.name}: {e}", flush=True)
    return count


def fetch_div2k_labeled(data: Path, n: int) -> int:
    """Real photos (ETH DIV2K) + exact warp flow labels — primary Colab path."""
    pip_install("pillow")
    from PIL import Image

    tmp = data / "_dl"
    tmp.mkdir(parents=True, exist_ok=True)
    zpath = tmp / "div2k.zip"
    download_first(DIV2K_URLS, zpath)
    imgs = extract_images_from_zip(zpath, tmp / "imgs", limit=max(n, 40))
    f0, f1, fl = data / "frame0", data / "frame1", data / "flow_npy"
    for d in (f0, f1, fl):
        d.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(44)
    count = 0
    for i, p in enumerate(imgs[:n]):
        rgb = np.asarray(Image.open(p).convert("RGB").resize((SIZE, SIZE)), dtype=np.uint8)
        flow_px = _make_flow(rng)
        warped = _warp(rgb, flow_px)
        stem = f"d2k_{i:04d}"
        Image.fromarray(rgb).save(f0 / f"{stem}.png")
        Image.fromarray(warped).save(f1 / f"{stem}.png")
        np.save(fl / f"{stem}.npy", (flow_px / float(SIZE)).astype(np.float32))
        count += 1
    shutil.rmtree(tmp, ignore_errors=True)
    return count


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", type=Path, default=Path(__file__).parent / "data")
    parser.add_argument("--min", type=int, default=32)
    parser.add_argument("--n", type=int, default=64)
    parser.add_argument(
        "--chairs-root",
        type=Path,
        default=None,
        help="Optional local FlyingChairs_release (or .../data) — skips 33GB download",
    )
    args = parser.parse_args()
    args.data.mkdir(parents=True, exist_ok=True)
    if _count_trips(args.data) >= args.min and (args.data / "READY").is_file():
        print(f"already ready: {_count_trips(args.data)}", flush=True)
        return

    n = 0
    src = ""
    if args.chairs_root is not None:
        try:
            n = fetch_local_chairs(args.data, args.chairs_root, max(args.n, args.min))
            src = "flyingchairs_local"
            print(f"local FlyingChairs triplets={n}", flush=True)
        except Exception as e:
            print(f"local FlyingChairs failed: {e}", flush=True)

    if n < args.min:
        n = fetch_div2k_labeled(args.data, max(args.n, args.min))
        src = "div2k_exact_flow"
        print(f"DIV2K exact-flow triplets={n}", flush=True)

    n = _count_trips(args.data)
    if n < args.min:
        raise SystemExit(f"need ≥{args.min} flow triplets, found {n}")
    (args.data / "READY").write_text(f"{src}\n", encoding="utf-8")
    print(f"QUALITY ready: {n} flow triplets ({src}, no HF)", flush=True)


if __name__ == "__main__":
    main()
