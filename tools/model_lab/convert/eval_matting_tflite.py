"""Contract check + P3M-500-P score for matting TFLite (in 1x256x256x3 RGB 0..1 -> 1x256x256x1 alpha)."""
import hashlib
import sys

sys.path.insert(0, "/app/lab")
import cv2
import numpy as np

from common import litert, mb, save_json, timed

d = np.load("/app/lab/eval/matting.npz")
RGB, A = d["rgb"], d["alpha"].astype(np.float32) / 255
out = {}
for path in sys.argv[1:]:
    it = litert(path)
    ins, outs = it.get_input_details(), it.get_output_details()
    E = int(ins[0]["shape"][1])
    ok = (len(ins) == 1 and ins[0]["shape"].tolist() == [1, E, E, 3] and ins[0]["dtype"] == np.float32
          and outs[0]["shape"].tolist() == [1, E, E, 1] and outs[0]["dtype"] == np.float32)

    def run(rgb):
        x = cv2.resize(rgb, (E, E), interpolation=cv2.INTER_AREA).astype(np.float32) / 255
        it.set_tensor(ins[0]["index"], x[None])
        it.invoke()
        return cv2.resize(it.get_tensor(outs[0]["index"])[0, :, :, 0], (512, 512))

    mad, edge = [], []
    k = np.ones((15, 15), np.uint8)
    for i in range(len(RGB)):
        e = np.abs(np.clip(run(RGB[i]), 0, 1) - A[i])
        g = A[i]
        band = (cv2.dilate((g > 0.01).astype(np.uint8), k) - cv2.erode((g > 0.99).astype(np.uint8), k)) > 0
        mad.append(e.mean())
        edge.append(e[band].mean())
    out[path.split("/")[-1]] = {"contract_ok": bool(ok), "in": ins[0]["shape"].tolist(), "out": outs[0]["shape"].tolist(),
                                "mad": round(float(np.mean(mad)), 4), "edge_mad": round(float(np.mean(edge)), 4),
                                "size_mb": mb(path), "sha256": hashlib.sha256(open(path, "rb").read()).hexdigest(),
                                "cpu_ms_4t": round(timed(run, RGB[0]), 1)}
save_json("matting_tflite", out)
