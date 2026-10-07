"""Contract check + Places365 score for inpaint TFLite (image + mask 1=hole -> RGB), fed like the app."""
import hashlib
import sys

sys.path.insert(0, "/app/lab")
import numpy as np

from common import litert, mb, save_json, strip, timed
import lpips, torch
lp = lpips.LPIPS(net='alex', verbose=False)
T = lambda a: torch.from_numpy(a).permute(2, 0, 1)[None].float() * 2 - 1

d = np.load("/app/lab/eval/inpaint.npz")
R, M = d["rgb256"].astype(np.float32) / 255, d["mask"]
out = {}
for path in sys.argv[1:]:
    it = litert(path)
    ins, outs = it.get_input_details(), it.get_output_details()
    shapes = sorted(i["shape"].tolist() for i in ins)
    ok = (shapes == [[1, 256, 256, 1], [1, 256, 256, 3]] and all(i["dtype"] == np.float32 for i in ins)
          and outs[0]["shape"].tolist() == [1, 256, 256, 3] and outs[0]["dtype"] == np.float32)

    def run(rgb, m):
        for i in ins:
            it.set_tensor(i["index"], (rgb[None] if i["shape"][-1] == 3 else m[None, :, :, None]).astype(np.float32))
        it.invoke()
        return it.get_tensor(outs[0]["index"])[0]

    l1, lps = [], []
    for i in range(len(R)):
        mm = M[i][..., None]
        comp = R[i] * (1 - mm) + run(R[i], M[i]) * mm
        l1.append(np.sum(np.abs(comp - R[i]).mean(-1) * M[i]) / M[i].sum())
        with torch.no_grad():
            lps.append(lp(T(R[i]), T(comp)).item())
        if i in (5, 33, 61, 88):
            strip(f'inpaint_tflite_{path.split("/")[-1][:-7]}_{i}', [R[i] * (1 - mm), comp, R[i]])
    out[path.split("/")[-1]] = {"contract_ok": bool(ok), "inputs": [(i["name"], i["shape"].tolist()) for i in ins],
                                "masked_l1": round(float(np.mean(l1)), 4), "lpips": round(float(np.mean(lps)), 4), "size_mb": mb(path),
                                "sha256": hashlib.sha256(open(path, "rb").read()).hexdigest(),
                                "cpu_ms_4t": round(timed(run, R[0], M[0]), 1)}
save_json("inpaint_tflite", out)
