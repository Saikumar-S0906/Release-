"""Optical flow benchmark: MPI Sintel clean (training GT), 104 pairs (every 10th GT flow).

EPE = mean endpoint error in pixels at native 1024x436 (lower better). Also EPE measured on the
PixiJoy 256x256 contract grid (ship gate EPE <= 1.5 px @256).
No flow pack exists in Release- yet; current app path is classical (crop-stabilize / heuristic),
so classical OpenCV DIS + Farneback are the baselines.
"""
import glob

import cv2
import numpy as np
import torch
import torch.nn.functional as F
import torchvision

from common import mb, ort_session, save_json, strip, timed

torch.set_num_threads(4)
flos = sorted(glob.glob("/app/lab/data/sintel/training/flow/*/*.flo"))


def read_flo(p):
    with open(p, "rb") as f:
        np.fromfile(f, np.float32, 1)
        w, h = np.fromfile(f, np.int32, 2)
        return np.fromfile(f, np.float32, 2 * w * h).reshape(h, w, 2)


PAIRS = []
for f in flos:
    sc, fr = f.split("/")[-2], f.split("/")[-1]
    k = int(fr[6:10])
    a = cv2.imread(f"/app/lab/data/sintel/training/clean/{sc}/frame_{k:04d}.png")[:, :, ::-1].copy()
    b = cv2.imread(f"/app/lab/data/sintel/training/clean/{sc}/frame_{k + 1:04d}.png")[:, :, ::-1].copy()
    PAIRS.append((a, b, read_flo(f)))
H, W = PAIRS[0][0].shape[:2]


def resize_flow(fl, h, w):
    sy, sx = h / fl.shape[0], w / fl.shape[1]
    out = cv2.resize(fl, (w, h), interpolation=cv2.INTER_LINEAR)
    out[..., 0] *= sx
    out[..., 1] *= sy
    return out


dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)


def dis_flow(a, b):
    return dis.calc(cv2.cvtColor(a, cv2.COLOR_RGB2GRAY), cv2.cvtColor(b, cv2.COLOR_RGB2GRAY), None)


def farneback(a, b):
    return cv2.calcOpticalFlowFarneback(cv2.cvtColor(a, cv2.COLOR_RGB2GRAY), cv2.cvtColor(b, cv2.COLOR_RGB2GRAY),
                                        None, 0.5, 4, 21, 3, 5, 1.1, 0)


raft = torchvision.models.optical_flow.raft_small(weights="DEFAULT").eval()


def raft_small(a, b, iters=12, size=(440, 1024)):
    t = lambda x: F.interpolate(torch.from_numpy(x).permute(2, 0, 1)[None].float() / 127.5 - 1, size,
                                mode="bilinear", align_corners=False)
    with torch.no_grad():
        fl = raft(t(a), t(b), num_flow_updates=iters)[-1][0].permute(1, 2, 0).numpy()
    return resize_flow(fl, a.shape[0], a.shape[1])


neu = ort_session("/tmp/lab-out/neuflow_things.onnx")


def neuflow(a, b):
    t = lambda x: cv2.resize(x, (768, 432)).astype(np.float32).transpose(2, 0, 1)[None] / 255
    fl = neu.run(None, {"input1": t(a), "input2": t(b)})[0][0].transpose(1, 2, 0)
    return resize_flow(fl, a.shape[0], a.shape[1])


def at256(fn):
    """Model sees the 256x256 contract frames; flow scaled back to native pixels."""
    def g(a, b):
        s = lambda x: cv2.resize(x, (256, 256), interpolation=cv2.INTER_AREA)
        if fn is raft_small:
            fl = raft_small(s(a), s(b), size=(256, 256))
        else:
            fl = fn(s(a), s(b))
        return resize_flow(fl, a.shape[0], a.shape[1])
    return g


def score(fn):
    epe, epe256 = [], []
    for a, b, gt in PAIRS:
        p = fn(a, b)
        epe.append(np.linalg.norm(p - gt, axis=-1).mean())
        p2, g2 = resize_flow(p, 256, 256), resize_flow(gt, 256, 256)
        epe256.append(np.linalg.norm(p2 - g2, axis=-1).mean())
    return dict(epe_native=round(float(np.mean(epe)), 3), epe_at_256_grid=round(float(np.mean(epe256)), 3))


def spd(fn):
    a, b, _ = PAIRS[0]
    return round(timed(fn, a, b, reps=3), 1)


import os
res = {"dataset": f"MPI Sintel clean training GT, {len(PAIRS)} pairs, native {W}x{H}", "models": {}}
for name, fn, extra in [
    ("opencv_dis_medium (classical)", dis_flow, {"size_mb": 0}),
    ("opencv_farneback (classical)", farneback, {"size_mb": 0}),
    ("raft_small (C+T, BSD-3) native", raft_small, {"size_mb": 3.8, "params_m": 0.99}),
    ("neuflow_v2 things 432x768 (Apache-2.0, no Sintel train)", neuflow, {"size_mb": mb("/tmp/lab-out/neuflow_things.onnx")}),
    ("opencv_dis @256 contract", at256(dis_flow), {}),
    ("raft_small @256 contract", at256(raft_small), {}),
]:
    res["models"][name] = {**score(fn), **extra, "cpu_ms_4t": spd(fn)}
save_json("flow", res)


def flow_vis(fl):
    mag, ang = cv2.cartToPolar(fl[..., 0], fl[..., 1])
    hsv = np.zeros((*fl.shape[:2], 3), np.uint8)
    hsv[..., 0] = ang * 90 / np.pi
    hsv[..., 1] = 255
    hsv[..., 2] = np.clip(mag / (np.percentile(mag, 99) + 1e-6) * 255, 0, 255)
    return cv2.cvtColor(hsv, cv2.COLOR_HSV2RGB)


for i in [4, 50, 90]:
    a, b, gt = PAIRS[i]
    strip(f"flow_{i}", [a, flow_vis(gt), flow_vis(dis_flow(a, b)), flow_vis(raft_small(a, b)), flow_vis(neuflow(a, b))])
