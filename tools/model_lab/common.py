import json
import os
import time

import numpy as np
from PIL import Image

REPORT = "/app/lab/report"
os.makedirs(f"{REPORT}/samples", exist_ok=True)
THREADS = 4  # roughly a phone big-core cluster; desktop x86 CPU, NOT a phone number


def timed(fn, *a, reps=5):
    fn(*a)  # warm
    t = time.perf_counter()
    for _ in range(reps):
        fn(*a)
    return (time.perf_counter() - t) / reps * 1000


def litert(path):
    from ai_edge_litert.interpreter import Interpreter

    it = Interpreter(path, num_threads=THREADS)
    it.allocate_tensors()
    return it


def run_litert(it, feeds: dict):
    """feeds: substring of input name -> array (NHWC float32)."""
    for d in it.get_input_details():
        for k, v in feeds.items():
            if k in d["name"]:
                it.set_tensor(d["index"], v.astype(np.float32))
    it.invoke()
    return it.get_tensor(it.get_output_details()[0]["index"])


def ort_session(path):
    import onnxruntime as ort

    o = ort.SessionOptions()
    o.intra_op_num_threads = THREADS
    return ort.InferenceSession(path, o, providers=["CPUExecutionProvider"])


def mb(path):
    return round(os.path.getsize(path) / 1024 / 1024, 2)


def save_json(name, data):
    with open(f"{REPORT}/{name}.json", "w") as f:
        json.dump(data, f, indent=2)
    print(json.dumps(data, indent=2))


def to_u8(a):
    a = np.asarray(a, dtype=np.float32)
    if a.max() <= 1.5:
        a = a * 255
    return np.clip(a, 0, 255).astype(np.uint8)


def strip(name, tiles, size=256):
    """Save a horizontal strip of tiles (HxW or HxWx3) for the report."""
    ims = []
    for t in tiles:
        t = to_u8(t)
        if t.ndim == 2:
            t = np.stack([t] * 3, -1)
        ims.append(Image.fromarray(t).resize((size, size)))
    out = Image.new("RGB", (size * len(ims), size))
    for i, im in enumerate(ims):
        out.paste(im, (i * size, 0))
    out.save(f"{REPORT}/samples/{name}.jpg", quality=88)
