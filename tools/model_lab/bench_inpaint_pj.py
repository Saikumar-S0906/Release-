"""Re-score the current PixiJoy inpaint UNet two ways (fairness check).

A) as the app feeds it today (PackLiteRt.inferInpaintDual: original pixels inside the hole)
B) as it was trained (real_data.batch_inpaint: hole pixels zeroed) = its best case
"""
import sys

sys.path.insert(0, "/app/lab")
import numpy as np

from common import litert, run_litert, save_json

d = np.load("/app/lab/eval/inpaint.npz")
R, M = d["rgb256"].astype(np.float32) / 255, d["mask"]
it = litert("/app/lab/models/inpaint.tflite")


def score(zero_hole):
    l1 = []
    for i in range(len(R)):
        m = M[i][..., None]
        x = R[i] * (1 - m) if zero_hole else R[i]
        out = run_litert(it, {"image": x[None], "mask": m[None]})[0]
        comp = R[i] * (1 - m) + out * m
        l1.append(np.sum(np.abs(comp - R[i]).mean(-1) * M[i]) / M[i].sum())
    return round(float(np.mean(l1)), 4)


save_json("inpaint_pj_fairness", {"app_feed_original_pixels": score(False), "trained_feed_zeroed_hole": score(True)})
