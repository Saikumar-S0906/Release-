"""Contract check + NYU score for any depth.tflite (current or converted).

python eval_depth_tflite.py file1.tflite [file2 ...]
Contract = PackModelContract.isMonocularDepth: FLOAT32 1x256x256x3 -> 1x256x256x1, 0 near 1 far.
"""
import hashlib
import sys

sys.path.insert(0, "/app/lab")
import numpy as np

from common import litert, mb, save_json, timed

import bench_depth_core as core  # noqa: E402

out = {}
for path in sys.argv[1:]:
    it = litert(path)
    ins, outs = it.get_input_details(), it.get_output_details()
    ok = (len(ins) == 1 and ins[0]["shape"].tolist() == [1, 256, 256, 3] and ins[0]["dtype"] == np.float32
          and outs[0]["shape"].tolist() == [1, 256, 256, 1] and outs[0]["dtype"] == np.float32)

    def run(rgb):
        it.set_tensor(ins[0]["index"], core.resize_rgb(rgb, 256, 256)[None].astype(np.float32))
        it.invoke()
        return it.get_tensor(outs[0]["index"])[0, :, :, 0]

    # near/far sanity: on NYU the bottom rows (floor near camera) must be nearer than the top
    v = np.mean([run(core.RGB[i])[200:].mean() < run(core.RGB[i])[:56].mean() for i in range(0, 100, 10)])
    # far01 is affine in disparity (1 - norm disp), so align in disparity space with -pred
    m = core.metrics(lambda r: -run(r), "disp")
    out[path.split("/")[-1]] = {"contract_ok": bool(ok), "near_is_low": float(v), **m, "size_mb": mb(path),
                                "sha256": hashlib.sha256(open(path, "rb").read()).hexdigest(),
                                "cpu_ms_4t": round(timed(run, core.RGB[0]), 1)}
save_json("depth_tflite", out)
