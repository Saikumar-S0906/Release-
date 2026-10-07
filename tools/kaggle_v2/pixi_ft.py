#!/usr/bin/env python3
"""PixiJoy fine-tune kit v2 — Kaggle GPU (T4/P100), PyTorch.

Fine-tunes the pretrained winners from the Model Lab benchmark instead of training UNets from scratch:

  depth    Depth Anything V2 Small (Apache-2.0)  — NYU-train GT + self-distill (teacher @518 -> student @266)
  matting  MODNet photographic (Apache-2.0)       — P3M-10k train (research-only data: see LICENSES)
  inpaint  MI-GAN 256 Places2 (MIT)                — COCO-2017 photos (CC-BY) + PixiJoy make_mask holes
  flow     RAFT-small torchvision (BSD-3)           — MPI Sintel train scenes, held-out scenes for eval

Each task: --hours wall-clock budget, auto-resume from ckpt, eval on the SAME held-out set/protocol as
/app/lab (Model Lab), keeps best.pt only if it beats the pretrained baseline. Nothing ever leaves the
phone at runtime: this is offline training only.

  python pixi_ft.py depth   --hours 4
  python pixi_ft.py matting --hours 5
  python pixi_ft.py inpaint --hours 8
  python pixi_ft.py flow    --hours 6
  add --smoke for a 2-minute CPU code-path check.
"""
from __future__ import annotations

import argparse
import glob
import io
import json
import math
import os
import random
import subprocess
import sys
import time
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F
from PIL import Image

WORK = Path(os.environ.get("PIXI_WORK", "/kaggle/working" if Path("/kaggle").exists() else "/tmp/pixi_ft"))
DATA = WORK / "data"
OUT = WORK / "out"
DEV = "cuda" if torch.cuda.is_available() else "cpu"
SEED = 42


def log(*a):
    print(time.strftime("%H:%M:%S"), *a, flush=True)


def sh(cmd):
    log("$", cmd)
    subprocess.check_call(cmd, shell=True)


KIT_GLOBS = ["/kaggle/input/*", "/kaggle/input/*/*", str(WORK / "kit")]


def kit_file(name):
    """1st choice: the private Kaggle Dataset `pixijoy-ml-kit` (upload once, no network at train time)."""
    for g in KIT_GLOBS:
        for d in glob.glob(g):
            for p in (Path(d) / name, Path(d) / "weights" / name, Path(d) / "eval" / name):
                if p.is_file():
                    return p
    return None


def hf_file(repo, path, repo_type="dataset"):
    """Kit -> Hugging Face -> HF mirror (hf-mirror.com). Raises only if all three fail."""
    hit = kit_file(Path(path).name)
    if hit:
        return str(hit)
    from huggingface_hub import hf_hub_download

    last = None
    for endpoint in (None, "https://hf-mirror.com"):
        for attempt in range(3):
            try:
                return hf_hub_download(repo, path, repo_type=repo_type, endpoint=endpoint,
                                       local_dir=str(DATA / repo.replace("/", "_")))
            except Exception as e:  # network / 5xx / rate limit
                last = e
                log("HF download failed", endpoint or "huggingface.co", attempt + 1, repr(e)[:120])
                time.sleep(5 * (attempt + 1))
    raise SystemExit(f"Could not fetch {repo}/{path}: {last}. Add the pixijoy-ml-kit Dataset as Input "
                     f"(tools/kaggle_v2/README.md) — it contains every file this kit needs.")


def parquet_rows(path, cols, idx=None):
    import pyarrow.parquet as pq

    t = pq.read_table(path, columns=cols)
    idx = range(t.num_rows) if idx is None else idx
    for i in idx:
        yield t.slice(int(i), 1).to_pylist()[0]


def pil(b, mode="RGB"):
    return Image.open(io.BytesIO(b)).convert(mode)


def find_resume(name):
    """Kaggle: ckpt from a previous version's output (added as Input) or this session."""
    for p in [OUT / f"{name}.ckpt.pt", *Path("/kaggle/input").glob(f"**/{name}.ckpt.pt")]:
        if p.is_file():
            return p
    return None


class Budget:
    def __init__(self, hours):
        self.end = time.time() + hours * 3600

    def left(self):
        return self.end - time.time() > 0


def save_ckpt(name, model, opt, step, best, extra=None):
    OUT.mkdir(parents=True, exist_ok=True)
    torch.save({"model": model.state_dict(), "opt": opt.state_dict(), "step": step, "best": best,
                **(extra or {})}, OUT / f"{name}.ckpt.pt")


def write_report(name, rep):
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{name}_report.json").write_text(json.dumps(rep, indent=2))
    log(json.dumps(rep, indent=2))


# ----------------------------------------------------------------------------------------------- masks
def make_mask(rng, size=256):
    """Same hole generator as tools/inpaint/dataset.make_mask (the app's own)."""
    m = np.zeros((size, size), np.float32)
    yy, xx = np.ogrid[:size, :size]
    for _ in range(int(rng.integers(1, 5))):
        k = rng.integers(0, 3)
        if k == 0:
            cy, cx = rng.integers(0, size, 2)
            r = int(rng.integers(12, size // 3))
            m[((yy - cy) ** 2 + (xx - cx) ** 2) <= r * r] = 1
        elif k == 1:
            y0, x0 = int(rng.integers(0, size - 8)), int(rng.integers(0, size - 8))
            m[y0:y0 + int(rng.integers(8, size // 2)), x0:x0 + int(rng.integers(8, size // 2))] = 1
        else:
            y, x, rad = float(rng.integers(0, size)), float(rng.integers(0, size)), float(rng.uniform(4, 18))
            for _ in range(int(rng.integers(20, 80))):
                y, x = np.clip(y + rng.normal(0, 6), 0, size - 1), np.clip(x + rng.normal(0, 6), 0, size - 1)
                m[((yy - y) ** 2 + (xx - x) ** 2) <= rad * rad] = 1
    if m.sum() < 64:
        m[size // 4:size // 2, size // 4:size // 2] = 1
    if m.mean() > 0.6:
        m[:, size // 2:] = 0
    return m


# ============================================================================================== DEPTH
NYU = "vikhyatk/nyu_depth_v2"
IMNET_MEAN = torch.tensor([0.485, 0.456, 0.406]).view(1, 3, 1, 1)
IMNET_STD = torch.tensor([0.229, 0.224, 0.225]).view(1, 3, 1, 1)


def nyu_files(split):
    from huggingface_hub import HfApi

    return sorted(f.path for f in HfApi().list_repo_tree(NYU, repo_type="dataset", recursive=True)
                  if f.path.endswith(".parquet") and f"/{split}-" in f.path)


def kit_npz(name, n):
    """Exact Model Lab eval arrays (same 100 samples / masks) when the kit Dataset is attached."""
    p = kit_file(name)
    if not p:
        return None
    d = np.load(p)
    log("eval set from kit", p)
    return {k: d[k][:n] for k in d.files}


def da_weights():
    """Depth Anything V2 Small (Apache-2.0): kit folder `dav2s/` -> HF -> hf-mirror."""
    for g in KIT_GLOBS:
        for d in glob.glob(g):
            if (Path(d) / "dav2s" / "config.json").is_file():
                return str(Path(d) / "dav2s")
    from huggingface_hub import snapshot_download

    for endpoint in (None, "https://hf-mirror.com"):
        try:
            return snapshot_download("depth-anything/Depth-Anything-V2-Small-hf", endpoint=endpoint,
                                     local_dir=str(DATA / "dav2s"))
        except Exception as e:
            log("DA-V2 download failed", endpoint, repr(e)[:120])
    raise SystemExit("Depth Anything V2 Small weights unavailable — attach the pixijoy-ml-kit Dataset")


def nyu_official_train():
    """HF-free fallback: official NYU labeled .mat (1449) minus the official 654 test images = 795 train."""
    import h5py
    import scipy.io

    base = "http://horatio.cs.nyu.edu/mit/silberman"
    mat, spl = DATA / "nyu_depth_v2_labeled.mat", DATA / "splits.mat"
    if not mat.exists():
        sh(f"wget -q -O {mat} {base}/nyu_depth_v2/nyu_depth_v2_labeled.mat")
    if not spl.exists():
        sh(f"wget -q -O {spl} {base}/indoor_seg_sup/splits.mat")
    train_idx = scipy.io.loadmat(spl)["trainNdxs"].ravel() - 1
    out = []
    with h5py.File(mat, "r") as f:
        for i in train_idx:
            rgb = np.transpose(f["images"][i], (2, 1, 0)).astype(np.uint8)
            dep = np.transpose(f["depths"][i], (1, 0)).astype(np.float32)
            b1, b2 = io.BytesIO(), io.BytesIO()
            Image.fromarray(rgb).save(b1, "PNG")
            Image.fromarray(dep, mode="F").save(b2, "TIFF")
            out.append((b1.getvalue(), b2.getvalue()))
    return out


def nyu_train_pairs(smoke):
    try:
        shards = nyu_files("train")
        shards = shards[:: max(1, len(shards) // 12)][: 1 if smoke else 12]
        pairs = []
        for s in shards:
            p = hf_file(NYU, s)
            for r in parquet_rows(p, ["image", "depth_map"], range(0, 10_000, 1 if smoke else 3)):
                pairs.append((r["image"]["bytes"], r["depth_map"]["bytes"]))
                if smoke and len(pairs) >= 16:
                    break
            if not smoke:
                os.remove(p)
        return pairs
    except (Exception, SystemExit) as e:
        log("NYU via HF failed -> official labeled .mat", repr(e)[:120])
        return nyu_official_train()


def depth_eval_set(n=100):
    """Model Lab protocol: NYU official test (validation shard 0), 100 evenly spread images."""
    k = kit_npz("depth.npz", n)
    if k:
        return k["rgb"], k["depth"]
    p = hf_file(NYU, nyu_files("validation")[0])
    import pyarrow.parquet as pq

    rows = pq.ParquetFile(p).metadata.num_rows
    rgb, dep = [], []
    for r in parquet_rows(p, ["image", "depth_map"], np.linspace(0, rows - 1, n).astype(int)):
        rgb.append(np.array(pil(r["image"]["bytes"])))
        dep.append(np.array(Image.open(io.BytesIO(r["depth_map"]["bytes"])), np.float32))
    return np.stack(rgb), np.stack(dep)


def da_forward(model, rgb01, edge):
    x = F.interpolate(rgb01, (edge, edge), mode="bilinear", align_corners=False)
    x = (x - IMNET_MEAN.to(x.device)) / IMNET_STD.to(x.device)
    return model(pixel_values=x).predicted_depth.unsqueeze(1)  # disparity-like


def ssi_norm(x, m):
    """MiDaS scale-shift-invariant normalization per sample over mask."""
    out = []
    for xi, mi in zip(x, m):
        v = xi[mi]
        t = v.median()
        s = (v - t).abs().mean().clamp_min(1e-6)
        out.append((xi - t) / s)
    return torch.stack(out)


def ssi_loss(pred, tgt, m):
    p, t = ssi_norm(pred, m), ssi_norm(tgt, m)
    l = (p - t).abs() * m
    gx = lambda a: a[..., :, 1:] - a[..., :, :-1]
    gy = lambda a: a[..., 1:, :] - a[..., :-1, :]
    d = (p - t) * m
    grad = (gx(d).abs() * (m[..., :, 1:] & m[..., :, :-1])).sum() + (gy(d).abs() * (m[..., 1:, :] & m[..., :-1, :])).sum()
    return l.sum() / m.sum().clamp_min(1) + 0.5 * grad / m.sum().clamp_min(1)


@torch.no_grad()
def depth_score(model, RGB, GT, edge=266):
    model.eval()
    crop = (slice(45, 471), slice(41, 601))
    ab, d1 = [], []
    for rgb, g in zip(RGB, GT):
        x = torch.from_numpy(rgb).permute(2, 0, 1)[None].float().to(DEV) / 255
        x = F.interpolate(x, (256, 256), mode="bilinear", align_corners=False)  # app contract input
        p = F.interpolate(da_forward(model, x, edge), (480, 640), mode="bilinear", align_corners=False)[0, 0]
        p = p.cpu().numpy()[crop]
        g = g[crop]
        m = (g > 1e-3) & (g < 10)
        A = np.stack([p[m], np.ones(m.sum())], 1)
        s, b = np.linalg.lstsq(A, 1 / g[m], rcond=None)[0]
        pd = np.clip(1 / np.maximum(p * s + b, 1e-3), 1e-3, 10)
        ab.append(np.mean(np.abs(pd[m] - g[m]) / g[m]))
        d1.append(np.mean(np.maximum(pd[m] / g[m], g[m] / pd[m]) < 1.25))
    model.train()
    return {"abs_rel": round(float(np.mean(ab)), 4), "delta1": round(float(np.mean(d1)), 4)}


def unlabeled_photos(n, smoke=False):
    """COCO-2017 train photos (CC-BY via Flickr) pulled by HTTP range from the official zip."""
    d = DATA / "coco"
    have = sorted(d.glob("*.jpg"))
    if len(have) >= n:
        return have[:n]
    from remotezip import RemoteZip

    d.mkdir(parents=True, exist_ok=True)
    z = RemoteZip("http://images.cocodataset.org/zips/train2017.zip")
    names = [x for x in z.namelist() if x.endswith(".jpg")]
    random.Random(SEED).shuffle(names)
    for i, nm in enumerate(names[:n]):
        p = d / Path(nm).name
        if not p.exists():
            p.write_bytes(z.read(nm))
        if i % 2000 == 0:
            log("coco", i, "/", n)
    return sorted(d.glob("*.jpg"))[:n]


def rand_crop_rgb(im, size, rng):
    w, h = im.size
    s = int(min(w, h) * rng.uniform(0.6, 1.0))
    x0, y0 = int(rng.integers(0, w - s + 1)), int(rng.integers(0, h - s + 1))
    im = im.crop((x0, y0, x0 + s, y0 + s)).resize((size, size), Image.BICUBIC)
    if rng.random() < 0.5:
        im = im.transpose(Image.FLIP_LEFT_RIGHT)
    return np.asarray(im, np.float32) / 255


def task_depth(a):
    from transformers import AutoModelForDepthEstimation

    rng = np.random.default_rng(SEED)
    torch.manual_seed(SEED)
    RGB, GT = depth_eval_set(20 if a.smoke else 100)
    name = "depth"
    w = da_weights()
    student = AutoModelForDepthEstimation.from_pretrained(w).to(DEV).train()
    teacher = AutoModelForDepthEstimation.from_pretrained(w).to(DEV).eval()
    for p in teacher.parameters():
        p.requires_grad_(False)
    base = depth_score(student, RGB, GT)
    log("baseline @266", base)
    nyu = nyu_train_pairs(a.smoke)
    photos = unlabeled_photos(64 if a.smoke else 20_000)
    log("nyu pairs", len(nyu), "photos", len(photos))
    enc = [p for n, p in student.named_parameters() if "backbone" in n]
    head = [p for n, p in student.named_parameters() if "backbone" not in n]
    opt = torch.optim.AdamW([{"params": enc, "lr": 2e-6}, {"params": head, "lr": 2e-5}], weight_decay=1e-4)
    step, best = 0, base["abs_rel"]
    r = find_resume(name)
    if r and not a.smoke:
        ck = torch.load(r, map_location=DEV)
        student.load_state_dict(ck["model"]); opt.load_state_dict(ck["opt"]); step, best = ck["step"], ck["best"]
        log("resumed", r, step, best)
    bs = 2 if a.smoke else 8
    bud = Budget(a.hours)
    scaler = torch.amp.GradScaler(enabled=DEV == "cuda")
    while bud.left() and (not a.smoke or step < 4):
        xs, ts, ms, kinds = [], [], [], []
        for _ in range(bs):
            if rng.random() < 0.5:  # GT branch
                ib, db = nyu[int(rng.integers(len(nyu)))]
                im, dm = pil(ib), np.array(Image.open(io.BytesIO(db)), np.float32)
                flip = rng.random() < 0.5
                x = np.asarray(im.resize((256, 256), Image.BILINEAR), np.float32) / 255
                dd = np.array(Image.fromarray(dm).resize((256, 256), Image.NEAREST))
                if flip:
                    x, dd = x[:, ::-1], dd[:, ::-1]
                valid = (dd > 1e-3) & (dd < 10)
                ts.append(np.where(valid, 1 / np.maximum(dd, 1e-3), 0).astype(np.float32))
                ms.append(valid)
                kinds.append(0)
            else:  # self-distill branch: teacher @518 on a real photo
                x = rand_crop_rgb(Image.open(photos[int(rng.integers(len(photos)))]).convert("RGB"), 256, rng)
                ts.append(None); ms.append(np.ones((256, 256), bool)); kinds.append(1)
            xs.append(np.ascontiguousarray(x))
        x = torch.from_numpy(np.stack(xs)).permute(0, 3, 1, 2).to(DEV)
        with torch.no_grad():
            tea = F.interpolate(da_forward(teacher, x, 518), (256, 256), mode="bilinear", align_corners=False)[:, 0]
        tgt = torch.stack([tea[i] if k else torch.from_numpy(t).to(DEV) for i, (k, t) in enumerate(zip(kinds, ts))])
        m = torch.from_numpy(np.stack(ms)).to(DEV)
        with torch.autocast(DEV, enabled=DEV == "cuda"):
            pred = F.interpolate(da_forward(student, x, 266), (256, 256), mode="bilinear", align_corners=False)[:, 0]
        loss = ssi_loss(pred.float(), tgt.float(), m)
        opt.zero_grad(set_to_none=True)
        scaler.scale(loss).backward()
        scaler.unscale_(opt)
        torch.nn.utils.clip_grad_norm_(student.parameters(), 1.0)
        scaler.step(opt); scaler.update()
        step += 1
        if step % 50 == 0:
            log("step", step, "loss", round(loss.item(), 4))
        if step % (2 if a.smoke else 1000) == 0 or not bud.left():
            sc = depth_score(student, RGB, GT)
            log("eval", step, sc, "best", best)
            if sc["abs_rel"] < best:
                best = sc["abs_rel"]
                student.save_pretrained(OUT / "depth_best_hf")
            save_ckpt(name, student, opt, step, best)
    write_report(name, {"baseline_abs_rel@266": base, "best_abs_rel@266": best, "steps": step,
                        "improved": best < base["abs_rel"],
                        "export": "python convert_depth.py --weights out/depth_best_hf" if best < base["abs_rel"]
                        else "no gain: ship the pretrained conversion"})


# ============================================================================================ MATTING
def modnet_src():
    src = WORK / "MODNet"
    if not src.exists():
        sh(f"git clone --depth 1 https://github.com/ZHKKKe/MODNet {src}")
    sys.path.append(str(src))  # append: MODNet's onnx/ folder must not shadow the onnx package
    from src.models.modnet import MODNet

    try:
        ck = hf_file("DavG25/modnet-pretrained-models", "models/modnet_photographic_portrait_matting.ckpt", "model")
    except SystemExit:  # official Apache-2.0 release folder (Google Drive)
        sh(f"pip -q install gdown && gdown -q --folder https://drive.google.com/drive/folders/1umYmlCulvIFNaqPjwod1SayFmSRHziyR -O {DATA}/modnet")
        ck = str(DATA / "modnet/modnet_photographic_portrait_matting.ckpt")
    net = MODNet(backbone_pretrained=False)
    sd = torch.load(ck, map_location="cpu")
    net.load_state_dict({k.replace("module.", "", 1): v for k, v in sd.items()})
    return net


def p3m(split_file, n=None, size=512):
    p = hf_file("nobg/P3M-10K", split_file)
    import pyarrow.parquet as pq

    rows = pq.ParquetFile(p).metadata.num_rows
    idx = np.linspace(0, rows - 1, n).astype(int) if n else None
    for r in parquet_rows(p, ["image", "mask"], idx):
        yield pil(r["image"]["bytes"]), pil(r["mask"]["bytes"], "L")


@torch.no_grad()
def matting_score(net, RGB, A):
    net.eval()
    mad, edge = [], []
    import cv2

    k = np.ones((15, 15), np.uint8)
    for rgb, g in zip(RGB, A):
        x = torch.from_numpy(cv2.resize(rgb, (256, 256), interpolation=cv2.INTER_AREA)).permute(2, 0, 1)[None]
        x = (x.float().to(DEV) / 255 - 0.5) / 0.5
        p = net(x, True)[2][0, 0].cpu().numpy()
        p = cv2.resize(p, (512, 512))
        e = np.abs(np.clip(p, 0, 1) - g)
        band = (cv2.dilate((g > 0.01).astype(np.uint8), k) - cv2.erode((g > 0.99).astype(np.uint8), k)) > 0
        mad.append(e.mean()); edge.append(e[band].mean())
    net.train()
    return {"mad": round(float(np.mean(mad)), 4), "edge_mad": round(float(np.mean(edge)), 4)}


def task_matting(a):
    import cv2

    rng = np.random.default_rng(SEED)
    torch.manual_seed(SEED)
    net = modnet_src().to(DEV).train()
    k = kit_npz("matting.npz", 20 if a.smoke else 100)
    if k:
        RGB, A = k["rgb"], k["alpha"].astype(np.float32) / 255
    else:
        ev = list(p3m("data/P3M_500_P-00000-of-00001.parquet", 20 if a.smoke else 100))
        RGB = np.stack([np.array(i.resize((512, 512))) for i, _ in ev])
        A = np.stack([np.array(m.resize((512, 512)), np.float32) / 255 for _, m in ev])
    base = matting_score(net, RGB, A)
    log("baseline", base)
    from huggingface_hub import HfApi

    train_files = sorted(f.path for f in HfApi().list_repo_tree("nobg/P3M-10K", repo_type="dataset", recursive=True)
                         if "/train-" in f.path)[: 1 if a.smoke else 11]
    pairs = []
    for f in train_files:
        for im, m in p3m(f, 8 if a.smoke else None):
            pairs.append((np.array(im.resize((384, 384), Image.BICUBIC)), np.array(m.resize((384, 384), Image.BILINEAR))))
    log("train pairs", len(pairs))
    opt = torch.optim.Adam(net.parameters(), lr=1e-5)
    step, best = 0, base["mad"]
    r = find_resume("matting")
    if r and not a.smoke:
        ck = torch.load(r, map_location=DEV)
        net.load_state_dict(ck["model"]); opt.load_state_dict(ck["opt"]); step, best = ck["step"], ck["best"]
    bud = Budget(a.hours)
    bs = 2 if a.smoke else 16
    while bud.left() and (not a.smoke or step < 4):
        xb, ab = [], []
        for _ in range(bs):
            im, m = pairs[int(rng.integers(len(pairs)))]
            s = int(rng.integers(256, 384))
            y0, x0 = int(rng.integers(0, 384 - s + 1)), int(rng.integers(0, 384 - s + 1))
            im = cv2.resize(im[y0:y0 + s, x0:x0 + s], (256, 256), interpolation=cv2.INTER_AREA)
            m = cv2.resize(m[y0:y0 + s, x0:x0 + s], (256, 256), interpolation=cv2.INTER_AREA)
            if rng.random() < 0.5:
                im, m = im[:, ::-1], m[:, ::-1]
            im = np.clip(im * rng.uniform(0.8, 1.2) + rng.uniform(-20, 20), 0, 255)
            xb.append(np.ascontiguousarray(im)); ab.append(np.ascontiguousarray(m))
        x = (torch.from_numpy(np.stack(xb)).permute(0, 3, 1, 2).float().to(DEV) / 255 - 0.5) / 0.5
        g = torch.from_numpy(np.stack(ab)).float().to(DEV)[:, None] / 255
        sem, det, mat = net(x, False)
        g16 = F.interpolate(g, scale_factor=1 / 16, mode="bilinear", align_corners=False)
        band = (F.max_pool2d(g, 15, 1, 7) - (-F.max_pool2d(-g, 15, 1, 7))) > 0.01
        l_sem = F.mse_loss(sem, g16)
        l_det = ((det - g).abs() * band).sum() / band.sum().clamp_min(1)
        l_mat = (mat - g).abs().mean() + 4 * ((mat - g).abs() * band).sum() / band.sum().clamp_min(1)
        loss = 10 * l_sem + 10 * l_det + l_mat
        opt.zero_grad(set_to_none=True); loss.backward(); opt.step()
        step += 1
        if step % 50 == 0:
            log("step", step, "loss", round(loss.item(), 4))
        if step % (2 if a.smoke else 500) == 0 or not bud.left():
            sc = matting_score(net, RGB, A)
            log("eval", step, sc, "best", best)
            if sc["mad"] < best:
                best = sc["mad"]
                torch.save(net.state_dict(), OUT / "matting_best.pt")
            save_ckpt("matting", net, opt, step, best)
    write_report("matting", {"baseline": base, "best_mad": best, "steps": step, "improved": best < base["mad"],
                             "license": "P3M-10k is research-only; keep the pretrained MODNet for a commercial ship "
                                        "unless you have rights to the fine-tune data"})


# ============================================================================================ INPAINT
def migan_src():
    src = WORK / "MI-GAN"
    if not src.exists():
        sh(f"git clone --depth 1 https://github.com/Picsart-AI-Research/MI-GAN {src}")
        f = src / "lib/model_zoo/migan_inference.py"
        s = f.read_text().replace("inplace=True", "inplace=False").replace(
            "        if act_gain != 1:\n            x = x * act_gain\n", "        x = x * act_gain\n")
        f.write_text(s)
    sys.path.insert(0, str(src / "lib/model_zoo"))
    from migan_inference import Generator

    w = kit_file("migan_256_places2.pt") or DATA / "migan_256_places2.pt"
    if not Path(w).exists():
        sh(f"pip -q install gdown && gdown --folder https://drive.google.com/drive/folders/1xNtvN2lto0p5yFKOEEg9RioMjGrYM74w -O {DATA}/migan_models")
        sh(f"cp {DATA}/migan_models/migan_256_places2.pt {w}")
    g = Generator(resolution=256)
    g.load_state_dict(torch.load(w, map_location="cpu"))
    return g


def migan_run(g, rgb01, hole):
    """rgb01 Bx3xHxW 0..1, hole Bx1xHxW (1 = hole) -> composited 0..1."""
    keep = 1 - hole
    img = rgb01 * 2 - 1
    out = (g(torch.cat([keep - 0.5, img * keep], 1)).clamp(-1, 1) + 1) / 2
    return rgb01 * keep + out * hole, out


class PatchD(torch.nn.Module):
    def __init__(self, c=64):
        super().__init__()
        sn = torch.nn.utils.spectral_norm
        L = []
        cin = 4
        for i, co in enumerate([c, c * 2, c * 4, c * 4]):
            L += [sn(torch.nn.Conv2d(cin, co, 4, 2 if i < 3 else 1, 1)), torch.nn.LeakyReLU(0.2)]
            cin = co
        L += [sn(torch.nn.Conv2d(cin, 1, 4, 1, 1))]
        self.net = torch.nn.Sequential(*L)

    def forward(self, x, m):
        return self.net(torch.cat([x * 2 - 1, m - 0.5], 1))


def inpaint_eval_set(n=100):
    """Model Lab protocol: Places365 val (eval only), centre crop 256, make_mask seed 42."""
    k = kit_npz("inpaint.npz", n)
    if k:
        return k["rgb256"].astype(np.float32) / 255, k["mask"]
    p = hf_file("dpdl-benchmark/Places365-Validation", "data/train-00000-of-00005.parquet")
    import pyarrow.parquet as pq

    rows = pq.ParquetFile(p).metadata.num_rows
    rng = np.random.default_rng(42)
    R, M = [], []
    for r in parquet_rows(p, ["image"], np.linspace(0, rows - 1, n).astype(int)):
        im = pil(r["image"]["bytes"])
        w, h = im.size
        s = min(w, h)
        im = im.crop(((w - s) // 2, (h - s) // 2, (w - s) // 2 + s, (h - s) // 2 + s)).resize((256, 256), Image.BICUBIC)
        R.append(np.asarray(im, np.float32) / 255)
        M.append(make_mask(rng))
    return np.stack(R), np.stack(M)


@torch.no_grad()
def inpaint_score(g, R, M, lp):
    g.eval()
    l1, lps = [], []
    for i in range(0, len(R), 10):
        x = torch.from_numpy(R[i:i + 10]).permute(0, 3, 1, 2).to(DEV)
        m = torch.from_numpy(M[i:i + 10])[:, None].to(DEV)
        comp, _ = migan_run(g, x, m)
        l1 += list(((comp - x).abs().mean(1, keepdim=True) * m).flatten(1).sum(1) / m.flatten(1).sum(1))
        lps += list(lp(comp * 2 - 1, x * 2 - 1).flatten())
    g.train()
    return {"masked_l1": round(float(torch.stack(l1).mean()), 4), "lpips": round(float(torch.stack(lps).mean()), 4)}


def task_inpaint(a):
    import lpips

    rng = np.random.default_rng(SEED)
    torch.manual_seed(SEED)
    g = migan_src().to(DEV).train()
    D = PatchD().to(DEV)
    lp_eval = lpips.LPIPS(net="alex", verbose=False).to(DEV)
    lp_train = lpips.LPIPS(net="vgg", verbose=False).to(DEV)
    R, M = inpaint_eval_set(20 if a.smoke else 100)
    base = inpaint_score(g, R, M, lp_eval)
    log("baseline", base)
    photos = unlabeled_photos(64 if a.smoke else 40_000)
    og = torch.optim.Adam(g.parameters(), lr=2e-5, betas=(0.0, 0.99))
    od = torch.optim.Adam(D.parameters(), lr=1e-4, betas=(0.0, 0.99))
    step, best = 0, base["lpips"]
    r = find_resume("inpaint")
    if r and not a.smoke:
        ck = torch.load(r, map_location=DEV)
        g.load_state_dict(ck["model"]); og.load_state_dict(ck["opt"]); D.load_state_dict(ck["D"])
        step, best = ck["step"], ck["best"]
    bud = Budget(a.hours)
    bs = 2 if a.smoke else 16
    while bud.left() and (not a.smoke or step < 4):
        x = torch.from_numpy(np.stack([rand_crop_rgb(Image.open(photos[int(rng.integers(len(photos)))]).convert("RGB"), 256, rng)
                                       for _ in range(bs)])).permute(0, 3, 1, 2).to(DEV)
        m = torch.from_numpy(np.stack([make_mask(rng) for _ in range(bs)]))[:, None].to(DEV)
        comp, raw = migan_run(g, x, m)
        # D step (hinge)
        ld = F.relu(1 - D(x, m)).mean() + F.relu(1 + D(comp.detach(), m)).mean()
        od.zero_grad(set_to_none=True); ld.backward(); od.step()
        # G step: hole-weighted L1 + perceptual + small adversarial
        l1 = ((raw - x).abs() * (6 * m + (1 - m))).mean()
        lper = lp_train(comp * 2 - 1, x * 2 - 1).mean()
        ladv = -D(comp, m).mean()
        lg = l1 + lper + 0.01 * ladv
        og.zero_grad(set_to_none=True); lg.backward(); og.step()
        step += 1
        if step % 50 == 0:
            log("step", step, "G", round(lg.item(), 4), "D", round(ld.item(), 4))
        if step % (2 if a.smoke else 1000) == 0 or not bud.left():
            sc = inpaint_score(g, R, M, lp_eval)
            log("eval", step, sc, "best lpips", best)
            if sc["lpips"] < best:
                best = sc["lpips"]
                torch.save(g.state_dict(), OUT / "inpaint_best.pt")
            save_ckpt("inpaint", g, og, step, best, {"D": D.state_dict()})
    write_report("inpaint", {"baseline": base, "best_lpips": best, "steps": step, "improved": best < base["lpips"],
                             "export": "python convert_migan.py --weights out/inpaint_best.pt"})


# =============================================================================================== FLOW
HOLDOUT = {"alley_2", "ambush_5", "bamboo_2", "cave_4", "market_6", "temple_3"}


def sintel():
    root = DATA / "sintel"
    if not (root / "training").exists():
        root.mkdir(parents=True, exist_ok=True)
        sh(f"cd {root} && wget -q http://files.is.tue.mpg.de/sintel/MPI-Sintel-complete.zip && "
           "unzip -q MPI-Sintel-complete.zip 'training/*' && rm MPI-Sintel-complete.zip")
    items = []
    for f in sorted(glob.glob(str(root / "training/flow/*/*.flo"))):
        sc, k = f.split("/")[-2], int(f.split("/")[-1][6:10])
        for pas in ("clean", "final"):
            a, b = root / f"training/{pas}/{sc}/frame_{k:04d}.png", root / f"training/{pas}/{sc}/frame_{k + 1:04d}.png"
            if a.exists() and b.exists():
                items.append((sc, a, b, f, pas))
    return items


def read_flo(p):
    with open(p, "rb") as f:
        np.fromfile(f, np.float32, 1)
        w, h = np.fromfile(f, np.int32, 2)
        return np.fromfile(f, np.float32, 2 * w * h).reshape(h, w, 2)


def flow_batch(items, rng, bs, size=256):
    A, B, Fl = [], [], []
    for _ in range(bs):
        _, a, b, f, _ = items[int(rng.integers(len(items)))]
        a, b, fl = np.asarray(Image.open(a).convert("RGB"), np.float32), np.asarray(Image.open(b).convert("RGB"), np.float32), read_flo(f)
        H, W = fl.shape[:2]
        s = int(rng.integers(256, 436))
        y0, x0 = int(rng.integers(0, H - s + 1)), int(rng.integers(0, W - s + 1))
        sl = (slice(y0, y0 + s), slice(x0, x0 + s))
        k = size / s
        rs = lambda im: np.asarray(Image.fromarray(im[sl].astype(np.uint8)).resize((size, size), Image.BILINEAR), np.float32)
        flr = np.stack([np.asarray(Image.fromarray(fl[sl][..., c]).resize((size, size), Image.BILINEAR)) for c in range(2)], -1) * k
        A.append(rs(a)); B.append(rs(b)); Fl.append(flr)
    t = lambda v: torch.from_numpy(np.stack(v)).permute(0, 3, 1, 2).float().to(DEV)
    return t(A) / 127.5 - 1, t(B) / 127.5 - 1, t(Fl)


@torch.no_grad()
def flow_score(net, items):
    """EPE on held-out scenes (clean pass), app contract: frames resized to 256x256, error in 256-grid px."""
    net.eval()
    e = []
    for sc, a, b, f, pas in items[::4]:
        fa = lambda p: torch.from_numpy(np.asarray(Image.open(p).convert("RGB").resize((256, 256), Image.BILINEAR), np.float32)).permute(2, 0, 1)[None].to(DEV) / 127.5 - 1
        p = net(fa(a), fa(b), num_flow_updates=12)[-1][0].permute(1, 2, 0).cpu().numpy()
        g = read_flo(f)
        gy, gx = 256 / g.shape[0], 256 / g.shape[1]
        g = np.stack([np.asarray(Image.fromarray(g[..., 0]).resize((256, 256), Image.BILINEAR)) * gx,
                      np.asarray(Image.fromarray(g[..., 1]).resize((256, 256), Image.BILINEAR)) * gy], -1)
        e.append(np.linalg.norm(p - g, axis=-1).mean())
    net.train()
    return {"epe_at_256": round(float(np.mean(e)), 3)}


def task_flow(a):
    import torchvision

    rng = np.random.default_rng(SEED)
    torch.manual_seed(SEED)
    hit = kit_file("raft_small_C_T_V2-01064c6d.pth")
    if hit:  # skip download.pytorch.org when the kit is attached
        hub = Path(torch.hub.get_dir()) / "checkpoints"
        hub.mkdir(parents=True, exist_ok=True)
        if not (hub / hit.name).exists():
            sh(f"cp {hit} {hub}/")
    net = torchvision.models.optical_flow.raft_small(weights="DEFAULT").to(DEV).train()
    items = sintel()
    tr = [i for i in items if i[0] not in HOLDOUT]
    ev = [i for i in items if i[0] in HOLDOUT and i[4] == "clean"]
    if a.smoke:
        tr, ev = tr[:8], (ev or tr)[:8]
    base = flow_score(net, ev)
    log("baseline", base, "train", len(tr), "eval", len(ev))
    opt = torch.optim.AdamW(net.parameters(), lr=5e-5, weight_decay=1e-5)
    step, best = 0, base["epe_at_256"]
    r = find_resume("flow")
    if r and not a.smoke:
        ck = torch.load(r, map_location=DEV)
        net.load_state_dict(ck["model"]); opt.load_state_dict(ck["opt"]); step, best = ck["step"], ck["best"]
    bud = Budget(a.hours)
    while bud.left() and (not a.smoke or step < 4):
        x1, x2, gt = flow_batch(tr, rng, 2 if a.smoke else 8)
        preds = net(x1, x2, num_flow_updates=12)
        n = len(preds)
        loss = sum(0.8 ** (n - i - 1) * (p - gt).abs().mean() for i, p in enumerate(preds))
        opt.zero_grad(set_to_none=True); loss.backward()
        torch.nn.utils.clip_grad_norm_(net.parameters(), 1.0); opt.step()
        step += 1
        if step % 50 == 0:
            log("step", step, "loss", round(loss.item(), 4))
        if step % (2 if a.smoke else 1000) == 0 or not bud.left():
            sc = flow_score(net, ev)
            log("eval", step, sc, "best", best)
            if sc["epe_at_256"] < best:
                best = sc["epe_at_256"]
                torch.save(net.state_dict(), OUT / "flow_best.pt")
            save_ckpt("flow", net, opt, step, best)
    write_report("flow", {"baseline": base, "best_epe_at_256": best, "steps": step, "improved": best < base["epe_at_256"],
                          "holdout_scenes": sorted(HOLDOUT),
                          "license": "MPI Sintel is research-use; check terms before shipping a Sintel fine-tune"})


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("task", choices=["depth", "matting", "inpaint", "flow"])
    ap.add_argument("--hours", type=float, default=4)
    ap.add_argument("--smoke", action="store_true")
    a = ap.parse_args()
    if a.smoke:
        a.hours = min(a.hours, 0.05)
    for d in (DATA, OUT):
        d.mkdir(parents=True, exist_ok=True)
    log("device", DEV, "work", WORK)
    {"depth": task_depth, "matting": task_matting, "inpaint": task_inpaint, "flow": task_flow}[a.task](a)
