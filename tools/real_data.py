"""Real-data loaders for quality mode. Synthetic-only is never a ship path.

Stage datasets under ./data then create an empty file ./data/READY
(see each lab data/README.md and tools/ML_QUALITY_BAR.md).
"""

from __future__ import annotations

from pathlib import Path

import numpy as np

DEFAULT_SIZE = 256


def require_ready(data_dir: Path) -> None:
    ready = data_dir / "READY"
    if not data_dir.is_dir() or not ready.is_file():
        raise SystemExit(
            f"QUALITY mode requires {data_dir}/READY after staging real datasets.\n"
            "Synthetic smoke weights must NOT be pinned to Release-.\n"
            "See tools/ML_QUALITY_BAR.md and data/README.md"
        )


def _list_images(folder: Path) -> list[Path]:
    exts = {".png", ".jpg", ".jpeg", ".webp"}
    if not folder.is_dir():
        return []
    return sorted(p for p in folder.iterdir() if p.suffix.lower() in exts)


def _load_rgb(path: Path, size: int) -> np.ndarray:
    import tensorflow as tf

    raw = tf.io.read_file(str(path))
    img = tf.io.decode_image(raw, channels=3, expand_animations=False)
    img = tf.image.resize(img, (size, size), method="bilinear")
    return (img.numpy().astype(np.float32) / 255.0).clip(0, 1)


def _load_gray(path: Path, size: int) -> np.ndarray:
    import tensorflow as tf

    raw = tf.io.read_file(str(path))
    img = tf.io.decode_image(raw, channels=1, expand_animations=False)
    img = tf.image.resize(img, (size, size), method="bilinear")
    return (img.numpy().astype(np.float32) / 255.0).clip(0, 1)


def depth_pairs(data_dir: Path) -> list[tuple[Path, Path]]:
    rgb_dir, d_dir = data_dir / "rgb", data_dir / "depth"
    pairs = []
    for rgb in _list_images(rgb_dir):
        cand = d_dir / rgb.name
        if not cand.is_file():
            cand = d_dir / (rgb.stem + ".png")
        if cand.is_file():
            pairs.append((rgb, cand))
    return pairs


def matte_pairs(data_dir: Path) -> list[tuple[Path, Path]]:
    rgb_dir, a_dir = data_dir / "rgb", data_dir / "alpha"
    pairs = []
    for rgb in _list_images(rgb_dir):
        cand = a_dir / rgb.name
        if not cand.is_file():
            cand = a_dir / (rgb.stem + ".png")
        if cand.is_file():
            pairs.append((rgb, cand))
    return pairs


def flow_triplets(data_dir: Path) -> list[tuple[Path, Path, Path]]:
    f0, f1, fl = data_dir / "frame0", data_dir / "frame1", data_dir / "flow_npy"
    out = []
    for a in _list_images(f0):
        b = f1 / a.name
        npy = fl / (a.stem + ".npy")
        if b.is_file() and npy.is_file():
            out.append((a, b, npy))
    return out


def inpaint_images(data_dir: Path) -> list[Path]:
    """Real RGB photos for quality inpaint (Places2 / DIV2K / phone stills)."""
    return _list_images(data_dir / "rgb")


def inpaint_mask_for(rgb: Path, data_dir: Path) -> Path | None:
    """Optional paired mask under data/masks (same stem). Else None → irregular synth mask on real photo."""
    m_dir = data_dir / "masks"
    for cand in (m_dir / rgb.name, m_dir / (rgb.stem + ".png"), m_dir / (rgb.stem + ".jpg")):
        if cand.is_file():
            return cand
    return None


def batch_inpaint(
    rng: np.random.Generator,
    n: int,
    images: list[Path],
    data_dir: Path,
    size: int = DEFAULT_SIZE,
    make_mask_fn=None,
):
    """Quality batch: real photo as target; hole from paired mask or irregular synth mask."""
    if len(images) < 1:
        raise SystemExit("No inpaint images in data/rgb")
    if make_mask_fn is None:
        raise SystemExit("batch_inpaint requires make_mask_fn from lab dataset")
    imgs, masks, tgts = [], [], []
    for _ in range(n):
        i = int(rng.integers(0, len(images)))
        rgb_p = images[i]
        target = _load_rgb(rgb_p, size)
        mask_p = inpaint_mask_for(rgb_p, data_dir)
        if mask_p is not None:
            mask = _load_gray(mask_p, size)[..., None]
            mask = (mask > 0.5).astype(np.float32)
        else:
            mask = make_mask_fn(rng)
            if mask.ndim == 2:
                mask = mask[..., None]
        if float(mask.mean()) < 1e-4:
            mask = make_mask_fn(rng)
        masked = target * (1.0 - mask)
        imgs.append(masked.astype(np.float32))
        masks.append(mask.astype(np.float32))
        tgts.append(target.astype(np.float32))
    return np.stack(imgs), np.stack(masks), np.stack(tgts)


def batch_depth(
    rng: np.random.Generator,
    n: int,
    pairs: list[tuple[Path, Path]],
    size: int = DEFAULT_SIZE,
):
    if len(pairs) < 1:
        raise SystemExit("No depth pairs in data/rgb + data/depth")
    imgs, depths = [], []
    for _ in range(n):
        i = int(rng.integers(0, len(pairs)))
        rgb_p, d_p = pairs[i]
        imgs.append(_load_rgb(rgb_p, size))
        depths.append(_load_gray(d_p, size)[..., None])
    return np.stack(imgs), np.stack(depths)


def batch_matte(
    rng: np.random.Generator,
    n: int,
    pairs: list[tuple[Path, Path]],
    size: int = DEFAULT_SIZE,
):
    if len(pairs) < 1:
        raise SystemExit("No matte pairs in data/rgb + data/alpha")
    imgs, alphas = [], []
    for _ in range(n):
        i = int(rng.integers(0, len(pairs)))
        rgb_p, a_p = pairs[i]
        imgs.append(_load_rgb(rgb_p, size))
        alphas.append(_load_gray(a_p, size)[..., None])
    return np.stack(imgs), np.stack(alphas)


def batch_flow(
    rng: np.random.Generator,
    n: int,
    trips: list[tuple[Path, Path, Path]],
    size: int = DEFAULT_SIZE,
):
    if len(trips) < 1:
        raise SystemExit("No flow triplets in data/frame0 + frame1 + flow_npy")
    a_s, b_s, f_s = [], [], []
    for _ in range(n):
        i = int(rng.integers(0, len(trips)))
        p0, p1, pn = trips[i]
        a_s.append(_load_rgb(p0, size))
        b_s.append(_load_rgb(p1, size))
        flow = np.load(pn).astype(np.float32)
        if flow.ndim == 2:
            raise SystemExit(f"flow must be HxWx2: {pn}")
        if flow.shape[:2] != (size, size):
            import tensorflow as tf

            flow = tf.image.resize(flow, (size, size), method="nearest").numpy()
        if np.max(np.abs(flow)) > 1.5:
            flow = flow / float(size)
        f_s.append(flow)
    return np.stack(a_s), np.stack(b_s), np.stack(f_s)
