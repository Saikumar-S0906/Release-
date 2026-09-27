#!/usr/bin/env python3
"""M4 flow quality data: prefer HF FlyingChairs (real chairs + GT flow); else DIV2K+exact warp labels."""

from __future__ import annotations

import argparse
import shutil
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


def fetch_hf_chairs(data: Path, n: int) -> int:
    pip_install("datasets", "pillow", "huggingface_hub")
    from datasets import load_dataset
    from PIL import Image

    f0, f1, fl = data / "frame0", data / "frame1", data / "flow_npy"
    for d in (f0, f1, fl):
        d.mkdir(parents=True, exist_ok=True)
    for ds_id in ("matthieumzr/FlyingChairs", "tomg-group-umd/FlyingChairs"):
        try:
            print(f"HF: {ds_id}", flush=True)
            ds = load_dataset(ds_id, split=f"train[:{n}]", trust_remote_code=True)
        except Exception as e:
            print(f"  skip: {e}", flush=True)
            continue
        count = 0
        for i, ex in enumerate(ds):
            # expected keys vary
            a = ex.get("image1") or ex.get("frame1") or ex.get("img1")
            b = ex.get("image2") or ex.get("frame2") or ex.get("img2")
            flow = ex.get("flow") or ex.get("flow_gt")
            if a is None or b is None or flow is None:
                continue
            ia = a.convert("RGB").resize((SIZE, SIZE)) if hasattr(a, "convert") else Image.fromarray(np.asarray(a)).resize((SIZE, SIZE))
            ib = b.convert("RGB").resize((SIZE, SIZE)) if hasattr(b, "convert") else Image.fromarray(np.asarray(b)).resize((SIZE, SIZE))
            fl_arr = np.asarray(flow, dtype=np.float32)
            if fl_arr.ndim != 3 or fl_arr.shape[-1] != 2:
                continue
            h0, w0 = fl_arr.shape[:2]
            ys = np.linspace(0, h0 - 1, SIZE).astype(np.int32)
            xs = np.linspace(0, w0 - 1, SIZE).astype(np.int32)
            fl_arr = fl_arr[ys][:, xs].copy()
            # scale pixel flow when resizing
            fl_arr[..., 0] *= SIZE / float(w0)
            fl_arr[..., 1] *= SIZE / float(h0)
            if np.max(np.abs(fl_arr)) > 1.5:
                fl_arr = fl_arr / float(SIZE)
            stem = f"chairs_{count:04d}"
            ia.save(f0 / f"{stem}.png")
            ib.save(f1 / f"{stem}.png")
            np.save(fl / f"{stem}.npy", fl_arr.astype(np.float32))
            count += 1
            if count >= n:
                break
        if count >= 8:
            return count
    return _count_trips(data)


def fetch_div2k_labeled(data: Path, n: int) -> int:
    """Real photos + exact warp flow (research-valid labels on real RGB)."""
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
    args = parser.parse_args()
    args.data.mkdir(parents=True, exist_ok=True)
    if _count_trips(args.data) >= args.min and (args.data / "READY").is_file():
        print(f"already ready: {_count_trips(args.data)}", flush=True)
        return

    n = 0
    src = ""
    try:
        n = fetch_hf_chairs(args.data, max(args.n, args.min))
        src = "flyingchairs_hf"
        print(f"FlyingChairs pairs={n}", flush=True)
    except Exception as e:
        print(f"FlyingChairs failed: {e}", flush=True)
    if n < args.min:
        n = fetch_div2k_labeled(args.data, max(args.n, args.min))
        src = "div2k_exact_flow"
        print(f"DIV2K exact-flow triplets={n}", flush=True)

    n = _count_trips(args.data)
    if n < args.min:
        raise SystemExit(f"need ≥{args.min} flow triplets, found {n}")
    (args.data / "READY").write_text(f"{src}\n", encoding="utf-8")
    print(f"QUALITY ready: {n} flow triplets ({src})", flush=True)


if __name__ == "__main__":
    main()
