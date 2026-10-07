"""Matting benchmark: P3M-500-P (real portrait alpha GT), 100 images at 512.

Metrics (x1e-3 like the papers): MAD (mean abs diff; ship gate <= 0.04 i.e. 40e-3), MSE,
SAD (sum abs diff /1000 at 512), plus MAD inside the unknown/edge band (hair edges).
Current: MediaPipe selfie_segmenter.tflite (VisionFacade) — a segmenter, not a matter.
"""
import cv2
import numpy as np

from common import litert, mb, ort_session, run_litert, save_json, strip, timed

d = np.load("/app/lab/eval/matting.npz")
RGB, A = d["rgb"], d["alpha"].astype(np.float32) / 255

seg = litert("/app/ml/src/main/assets/selfie_segmenter.tflite")


def selfie(rgb):
    x = cv2.resize(rgb, (256, 256), interpolation=cv2.INTER_AREA).astype(np.float32) / 255
    seg.set_tensor(seg.get_input_details()[0]["index"], x[None])
    seg.invoke()
    a = seg.get_tensor(seg.get_output_details()[0]["index"])[0, :, :, 0]
    return cv2.resize(a, (512, 512), interpolation=cv2.INTER_LINEAR)


mod = ort_session("/app/lab/cand/modnet.onnx")


def modnet(rgb, size=512):
    x = cv2.resize(rgb, (size, size), interpolation=cv2.INTER_AREA).astype(np.float32) / 255
    x = (x - 0.5) / 0.5
    a = mod.run(None, {"input": x.transpose(2, 0, 1)[None]})[0][0, 0]
    return cv2.resize(a, (512, 512), interpolation=cv2.INTER_LINEAR)


def modnet256(rgb):
    return modnet(rgb, 256)


def score(fn):
    mad, mse, sad, edge = [], [], [], []
    k = np.ones((15, 15), np.uint8)
    for i in range(len(RGB)):
        p = np.clip(fn(RGB[i]), 0, 1)
        g = A[i]
        e = np.abs(p - g)
        band = (cv2.dilate((g > 0.01).astype(np.uint8), k) - cv2.erode((g > 0.99).astype(np.uint8), k)) > 0
        mad.append(e.mean())
        mse.append((e ** 2).mean())
        sad.append(e.sum() / 1000)
        edge.append(e[band].mean())
    return dict(mad=round(float(np.mean(mad)), 4), mse=round(float(np.mean(mse)), 4),
                sad=round(float(np.mean(sad)), 2), edge_mad=round(float(np.mean(edge)), 4))


res = {
    "dataset": "P3M-500-P portrait matting test, 100 images, 512x512",
    "models": {
        "mediapipe_selfie_segmenter (current)": {**score(selfie), "size_mb": mb("/app/ml/src/main/assets/selfie_segmenter.tflite"),
                                                 "input": 256, "cpu_ms_4t": round(timed(selfie, RGB[0]), 1)},
        "modnet @256": {**score(modnet256), "size_mb": mb("/app/lab/cand/modnet.onnx"), "params_m": 6.5,
                        "input": 256, "cpu_ms_4t": round(timed(modnet256, RGB[0]), 1)},
        "modnet @512": {**score(modnet), "input": 512, "cpu_ms_4t": round(timed(modnet, RGB[0]), 1)},
    },
}
save_json("matting", res)
for i in [2, 30, 64, 95]:
    strip(f"matting_{i}", [RGB[i], A[i], selfie(RGB[i]), modnet(RGB[i])])
