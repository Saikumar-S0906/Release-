"""Shared NYU depth eval pieces (same protocol as bench_depth.py)."""
import numpy as np
import torch
import torch.nn.functional as F

d = np.load("/app/lab/eval/depth.npz")
RGB, GT = d["rgb"], d["depth"]
crop = (slice(45, 471), slice(41, 601))


def resize(a, h, w):
    t = torch.from_numpy(np.ascontiguousarray(a)).float()[None, None]
    return F.interpolate(t, (h, w), mode="bilinear", align_corners=False)[0, 0].numpy()


def resize_rgb(rgb, h, w):
    t = torch.from_numpy(rgb).float().permute(2, 0, 1)[None] / 255.0
    return F.interpolate(t, (h, w), mode="bilinear", align_corners=False)[0].permute(1, 2, 0).numpy()


def align(pred, target, mask):
    p, t = pred[mask], target[mask]
    s, b = np.linalg.lstsq(np.stack([p, np.ones_like(p)], 1), t, rcond=None)[0]
    return pred * s + b


def norm01(x, m):
    lo, hi = np.percentile(x[m], 2), np.percentile(x[m], 98)
    return np.clip((x - lo) / (hi - lo + 1e-9), 0, 1)


def metrics(pred_fn, kind):
    absrel, d1, mae = [], [], []
    for i in range(len(RGB)):
        p = resize(pred_fn(RGB[i]), 480, 640)[crop]
        g = GT[i][crop]
        m = (g > 1e-3) & (g < 10)
        if kind == "depth":
            pd = align(p, g, m)
        else:
            pd = 1.0 / np.maximum(align(p, 1.0 / np.maximum(g, 1e-3), m), 1e-3)
        pd = np.clip(pd, 1e-3, 10)
        absrel.append(np.mean(np.abs(pd[m] - g[m]) / g[m]))
        r = np.maximum(pd[m] / g[m], g[m] / pd[m])
        d1.append(np.mean(r < 1.25))
        mae.append(np.mean(np.abs(norm01(pd, m)[m] - norm01(g, m)[m])))
    return dict(abs_rel=round(float(np.mean(absrel)), 4), delta1=round(float(np.mean(d1)), 4),
                mae01=round(float(np.mean(mae)), 4))
