"""Build fixed, seeded eval sets (100 samples each) from public data.

depth   : NYU Depth V2 official test split (HF vikhyatk/nyu_depth_v2 validation shard 0)
inpaint : Places365 validation photos + PixiJoy's own make_mask() hole generator
matting : P3M-500-P (privacy-preserving portrait matting test set, real alpha GT)
flow    : MPI Sintel clean training subset (every 10th GT flow) — built by fetch_sintel.py
"""
import io
import numpy as np
import pyarrow.parquet as pq
from PIL import Image

N = 100
import os
OUT = os.environ.get("PIXI_EVAL_OUT", "/app/lab/eval")
DATA = os.environ.get("PIXI_DATA", "/app/lab/data")
rng = np.random.default_rng(42)


def rows(path, n):
    t = pq.read_table(path)
    idx = np.linspace(0, t.num_rows - 1, n).astype(int)
    return [t.slice(int(i), 1).to_pylist()[0] for i in idx]


def make_mask(rng, size=256):
    """Copy of tools/inpaint/dataset.make_mask (the app's own hole generator)."""
    m = np.zeros((size, size), dtype=np.float32)
    for _ in range(int(rng.integers(1, 5))):
        kind = rng.integers(0, 3)
        yy, xx = np.ogrid[:size, :size]
        if kind == 0:
            cy, cx = rng.integers(0, size, size=2)
            r = int(rng.integers(12, size // 3))
            m[((yy - cy) ** 2 + (xx - cx) ** 2) <= r * r] = 1.0
        elif kind == 1:
            y0, x0 = int(rng.integers(0, size - 8)), int(rng.integers(0, size - 8))
            h, w = int(rng.integers(8, size // 2)), int(rng.integers(8, size // 2))
            m[y0:y0 + h, x0:x0 + w] = 1.0
        else:
            y, x = float(rng.integers(0, size)), float(rng.integers(0, size))
            rad = float(rng.uniform(4.0, 18.0))
            for _ in range(int(rng.integers(20, 80))):
                y = np.clip(y + rng.normal(0, 6), 0, size - 1)
                x = np.clip(x + rng.normal(0, 6), 0, size - 1)
                m[((yy - y) ** 2 + (xx - x) ** 2) <= rad * rad] = 1.0
    if m.sum() < 64:
        m[size // 4:size // 2, size // 4:size // 2] = 1.0
    if m.mean() > 0.6:  # keep holes realistic for "remove object" use
        m[:, size // 2:] = 0
    return m


def img(b, size=None, mode="RGB"):
    im = Image.open(io.BytesIO(b)).convert(mode)
    return im.resize((size, size), Image.BILINEAR) if size else im


def main():
    import os
    os.makedirs(OUT, exist_ok=True)
    # depth: keep 640x480 GT, eval crop (Eigen) applied in bench
    rgb, dep = [], []
    for r in rows(f"{DATA}/nyuval.parquet", N):
        rgb.append(np.array(img(r["image"]["bytes"])))
        dep.append(np.array(Image.open(io.BytesIO(r["depth_map"]["bytes"])), dtype=np.float32))
    np.savez_compressed(f"{OUT}/depth.npz", rgb=np.stack(rgb), depth=np.stack(dep))
    # inpaint: 256 contract and 512 for models that like more pixels
    r256, r512, masks = [], [], []
    for r in rows(f"{DATA}/places0.parquet", N):
        im = img(r["image"]["bytes"])
        w, h = im.size
        s = min(w, h)
        im = im.crop(((w - s) // 2, (h - s) // 2, (w - s) // 2 + s, (h - s) // 2 + s))
        r256.append(np.array(im.resize((256, 256), Image.BICUBIC)))
        r512.append(np.array(im.resize((512, 512), Image.BICUBIC)))
        masks.append(make_mask(rng))
    np.savez_compressed(f"{OUT}/inpaint.npz", rgb256=np.stack(r256), rgb512=np.stack(r512), mask=np.stack(masks))
    # matting: resize to 512 long side square (pad-free resize) keeps eval fast
    rgb, alpha = [], []
    for r in rows(f"{DATA}/p3m500p.parquet", N):
        rgb.append(np.array(img(r["image"]["bytes"], 512)))
        alpha.append(np.array(img(r["mask"]["bytes"], 512, "L")))
    np.savez_compressed(f"{OUT}/matting.npz", rgb=np.stack(rgb), alpha=np.stack(alpha))
    print("prep done")


if __name__ == "__main__":
    main()
