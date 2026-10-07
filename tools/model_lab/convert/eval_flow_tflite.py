"""Contract check + Sintel EPE for flow TFLite (2x 1x256x256x3 RGB 0..1 -> 1x256x256x2 (dx,dy)/256).

python eval_flow_tflite.py SINTEL_ROOT model1.tflite [model2 ...]
EPE in pixels of the 256x256 contract grid (ship gate <= 1.5 px), plus OpenCV DIS baseline.
"""
import glob
import hashlib
import sys

sys.path.insert(0, "/app/lab")
import cv2
import numpy as np

from common import litert, mb, save_json, timed

root = sys.argv[1]
pairs = []
for f in sorted(glob.glob(f"{root}/training/flow/*/*.flo")):
    sc, k = f.split("/")[-2], int(f.split("/")[-1][6:10])
    a = cv2.imread(f"{root}/training/clean/{sc}/frame_{k:04d}.png")
    b = cv2.imread(f"{root}/training/clean/{sc}/frame_{k + 1:04d}.png")
    if a is None or b is None:
        continue
    with open(f, "rb") as fh:
        np.fromfile(fh, np.float32, 1)
        w, h = np.fromfile(fh, np.int32, 2)
        fl = np.fromfile(fh, np.float32, 2 * w * h).reshape(h, w, 2)
    g = cv2.resize(fl, (256, 256))
    g[..., 0] *= 256 / w
    g[..., 1] *= 256 / h
    s = lambda x: cv2.resize(x[:, :, ::-1], (256, 256), interpolation=cv2.INTER_AREA)
    pairs.append((s(a), s(b), g))

dis = cv2.DISOpticalFlow_create(cv2.DISOPTICAL_FLOW_PRESET_MEDIUM)
gray = lambda x: cv2.cvtColor(x, cv2.COLOR_RGB2GRAY)
out = {"pairs": len(pairs), "opencv_dis_medium": round(float(np.mean(
    [np.linalg.norm(dis.calc(gray(a), gray(b), None) - g, axis=-1).mean() for a, b, g in pairs])), 3)}
for path in sys.argv[2:]:
    it = litert(path)
    ins, outs = it.get_input_details(), it.get_output_details()
    ok = (len(ins) == 2 and all(i["shape"].tolist() == [1, 256, 256, 3] and i["dtype"] == np.float32 for i in ins)
          and outs[0]["shape"].tolist() == [1, 256, 256, 2])
    order = sorted(range(2), key=lambda j: ins[j]["name"])  # name order: frame_t / args_0 first

    def run(a, b):
        it.set_tensor(ins[order[0]]["index"], (a.astype(np.float32) / 255)[None])
        it.set_tensor(ins[order[1]]["index"], (b.astype(np.float32) / 255)[None])
        it.invoke()
        return it.get_tensor(outs[0]["index"])[0] * 256

    epe = [np.linalg.norm(run(a, b) - g, axis=-1).mean() for a, b, g in pairs]
    out[path.split("/")[-1]] = {"contract_ok": bool(ok), "inputs": [i["name"] for i in ins],
                                "epe_at_256": round(float(np.mean(epe)), 3), "size_mb": mb(path),
                                "sha256": hashlib.sha256(open(path, "rb").read()).hexdigest(),
                                "cpu_ms_4t": round(timed(run, *pairs[0][:2], reps=3), 1)}
save_json("flow_tflite", out)
