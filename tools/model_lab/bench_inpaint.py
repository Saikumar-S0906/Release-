"""Inpaint benchmark: 100 Places365-val photos, holes from PixiJoy's own make_mask().

Metrics on the 256 contract (what the app binds): masked L1 (= the ship-gate "masked MAE",
gate <= 0.08), PSNR, SSIM (whole image, composited), LPIPS-Alex (perceptual, lower better).
Telea (cv2.inpaint) is the app's always-on classical fallback.
"""
import cv2
import lpips
import numpy as np
import torch
from skimage.metrics import peak_signal_noise_ratio as psnr
from skimage.metrics import structural_similarity as ssim

from common import litert, mb, ort_session, run_litert, save_json, strip, timed

d = np.load("/app/lab/eval/inpaint.npz")
R256, R512, M = d["rgb256"], d["rgb512"], d["mask"]
lp = lpips.LPIPS(net="alex", verbose=False)


def up_mask(m, s):
    return (cv2.resize(m, (s, s), interpolation=cv2.INTER_NEAREST) > 0.5).astype(np.float32)


def telea(rgb, m):
    return cv2.inpaint(rgb, (m * 255).astype(np.uint8), 5, cv2.INPAINT_TELEA).astype(np.float32) / 255


it = litert("/app/lab/models/inpaint.tflite")


def pj(rgb, m):
    return run_litert(it, {"image": (rgb.astype(np.float32) / 255)[None], "mask": m[None, :, :, None]})[0]


mig = ort_session("/app/lab/cand/migan.onnx")


def migan(rgb, m, size=512):
    """MI-GAN pipeline ONNX (MIT): uint8 NCHW image, mask 255=keep 0=hole."""
    x = R512[CUR[0]] if size == 512 else rgb
    mm = up_mask(m, x.shape[0])
    out = mig.run(None, {"image": x.transpose(2, 0, 1)[None],
                         "mask": ((1 - mm) * 255).astype(np.uint8)[None, None]})[0][0]
    out = out.transpose(1, 2, 0).astype(np.float32) / 255
    return cv2.resize(out, (256, 256), interpolation=cv2.INTER_AREA)


def migan256(rgb, m):
    return migan(rgb, m, 256)


lama = ort_session("/app/lab/cand/lama_fp32.onnx")


def big_lama(rgb, m):
    """LaMa big (Apache-2.0) 512 ONNX: float 0..1 NCHW, mask 1=hole; output 0..255."""
    x = R512[CUR[0]].astype(np.float32) / 255
    mm = up_mask(m, 512)
    out = lama.run(None, {"image": x.transpose(2, 0, 1)[None], "mask": mm[None, None]})[0][0]
    out = out.transpose(1, 2, 0)
    out = out / 255 if out.max() > 1.5 else out
    return cv2.resize(np.clip(out, 0, 1), (256, 256), interpolation=cv2.INTER_AREA)


CUR = [0]


def score(fn):
    l1s, ps, ss, lps = [], [], [], []
    for i in range(len(R256)):
        CUR[0] = i
        gt = R256[i].astype(np.float32) / 255
        m = M[i]
        out = fn(R256[i], m)
        comp = gt * (1 - m[..., None]) + out * m[..., None]
        l1s.append(np.sum(np.abs(comp - gt).mean(-1) * m) / m.sum())
        ps.append(psnr(gt, comp, data_range=1))
        ss.append(ssim(gt, comp, channel_axis=2, data_range=1))
        t = lambda a: torch.from_numpy(a).permute(2, 0, 1)[None].float() * 2 - 1
        with torch.no_grad():
            lps.append(lp(t(gt), t(comp)).item())
    return dict(masked_l1=round(float(np.mean(l1s)), 4), psnr=round(float(np.mean(ps)), 2),
                ssim=round(float(np.mean(ss)), 4), lpips=round(float(np.mean(lps)), 4))


def speed(fn):
    CUR[0] = 0
    return round(timed(fn, R256[0], M[0]), 1)


res = {
    "dataset": "Places365 val, 100 photos, 256x256, PixiJoy make_mask() holes "
               f"(mean hole {M.mean() * 100:.1f}% of image)",
    "models": {
        "telea (classical fallback)": {**score(telea), "size_mb": 0, "cpu_ms_4t": speed(telea)},
        "pixijoy_unet_v1 (current)": {**score(pj), "size_mb": mb("/app/lab/models/inpaint.tflite"),
                                       "params_m": 19.7, "cpu_ms_4t": speed(pj)},
        "mi_gan @256": {**score(migan256), "size_mb": mb("/app/lab/cand/migan.onnx"), "cpu_ms_4t": speed(migan256)},
        "mi_gan @512->256": {**score(migan), "cpu_ms_4t": speed(migan)},
        "big_lama @512->256 (reference, too big)": {**score(big_lama), "size_mb": mb("/app/lab/cand/lama_fp32.onnx"),
                                                     "params_m": 51, "cpu_ms_4t": speed(big_lama)},
    },
}
save_json("inpaint", res)
for i in [5, 33, 61, 88]:
    CUR[0] = i
    holed = R256[i].astype(np.float32) / 255 * (1 - M[i][..., None]) + M[i][..., None] * np.array([1, 0, 1])
    strip(f"inpaint_{i}", [holed, telea(R256[i], M[i]), pj(R256[i], M[i]), migan(R256[i], M[i]), R256[i]])
