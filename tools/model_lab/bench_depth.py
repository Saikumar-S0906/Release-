"""Depth benchmark on NYU Depth V2 official test images (100, seeded spread).

Both models predict *relative* depth, so each prediction gets the standard
per-image least-squares scale+shift alignment before metrics (MiDaS protocol):
  - PixiJoy UNet outputs depth-like (0 near, 1 far)  -> align to GT depth
  - Depth Anything V2 outputs disparity-like          -> align to 1/GT, then invert
Metrics on Eigen crop, valid GT 1e-3..10 m: AbsRel (lower better), delta1 (higher better).
Also the PixiJoy ship-gate metric: MAE on the 0..1 normalized map (p2..p98 like prepare_quality_data.py).
"""
import numpy as np
import torch
import torch.nn.functional as F

from common import litert, mb, run_litert, save_json, strip, timed

d = np.load("/app/lab/eval/depth.npz")
RGB, GT = d["rgb"], d["depth"]
crop = (slice(45, 471), slice(41, 601))


def resize(a, h, w):
    t = torch.from_numpy(np.ascontiguousarray(a)).float()[None, None]
    return F.interpolate(t, (h, w), mode="bilinear", align_corners=False)[0, 0].numpy()


def align(pred, target, mask):
    p, t = pred[mask], target[mask]
    A = np.stack([p, np.ones_like(p)], 1)
    s, b = np.linalg.lstsq(A, t, rcond=None)[0]
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
            disp = align(p, 1.0 / np.maximum(g, 1e-3), m)
            pd = 1.0 / np.maximum(disp, 1e-3)
        pd = np.clip(pd, 1e-3, 10)
        absrel.append(np.mean(np.abs(pd[m] - g[m]) / g[m]))
        r = np.maximum(pd[m] / g[m], g[m] / pd[m])
        d1.append(np.mean(r < 1.25))
        mae.append(np.mean(np.abs(norm01(pd, m)[m] - norm01(g, m)[m])))
    return dict(abs_rel=round(float(np.mean(absrel)), 4), delta1=round(float(np.mean(d1)), 4),
                mae01=round(float(np.mean(mae)), 4))


# --- current PixiJoy depth.tflite (Release- ml-packs-v1) ---
it = litert("/app/lab/models/depth.tflite")


def pj(rgb):
    x = resize_rgb(rgb, 256, 256)
    return run_litert(it, {"image": x[None]})[0, :, :, 0]


def resize_rgb(rgb, h, w):
    t = torch.from_numpy(rgb).float().permute(2, 0, 1)[None] / 255.0
    return F.interpolate(t, (h, w), mode="bilinear", align_corners=False)[0].permute(1, 2, 0).numpy()


# --- Depth Anything V2 Small (Apache-2.0) ---
from transformers import AutoModelForDepthEstimation

torch.set_num_threads(4)
dav2 = AutoModelForDepthEstimation.from_pretrained("/app/lab/kit/dav2s").eval()
MEAN = np.array([0.485, 0.456, 0.406], np.float32)
STD = np.array([0.229, 0.224, 0.225], np.float32)


def da(rgb, edge=266):
    x = (resize_rgb(rgb, edge, edge) - MEAN) / STD
    with torch.no_grad():
        return dav2(pixel_values=torch.from_numpy(x).permute(2, 0, 1)[None]).predicted_depth[0].numpy()


def da518(rgb):
    return da(rgb, 518)


res = {
    "dataset": "NYU Depth V2 official test split, 100 images, Eigen crop, scale+shift aligned",
    "models": {
        "pixijoy_unet_v1 (current)": {**metrics(pj, "depth"), "size_mb": mb("/app/lab/models/depth.tflite"),
                                       "params_m": 19.6, "input": 256,
                                       "cpu_ms_4t": round(timed(pj, RGB[0]), 1)},
        "depth_anything_v2_small @266": {**metrics(da, "disp"), "size_mb_fp32": 94.6, "params_m": 24.8,
                                          "input": 266, "cpu_ms_4t": round(timed(da, RGB[0]), 1)},
        "depth_anything_v2_small @518": {**metrics(da518, "disp"), "input": 518,
                                          "cpu_ms_4t": round(timed(da518, RGB[0]), 1)},
    },
}
save_json("depth", res)
for i in [3, 40, 77]:
    g = GT[i]
    strip(f"depth_{i}", [RGB[i], 1 - norm01(g, g > 0), 1 - resize(pj(RGB[i]), 480, 640),
                         norm01(resize(da(RGB[i]), 480, 640), np.ones_like(g, bool))])
