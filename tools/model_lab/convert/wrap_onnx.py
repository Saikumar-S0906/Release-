"""Wrap an NCHW float ONNX vision model into the PixiJoy NHWC 0..1 contract, then onnx2tf -> TFLite.

matting (MODNet, Apache-2.0):  in 1x256x256x3 RGB 0..1  -> out 1x256x256x1 alpha (contract of tools/matting)
The (x-0.5)/0.5 normalize is baked into the graph.

Usage: /tmp/conv/bin/python wrap_onnx.py matting /app/lab/cand/modnet.onnx /tmp/lab-out/matting_modnet
"""
import subprocess
import sys

import numpy as np
import onnx
from onnx import TensorProto, helper, numpy_helper


def fix_shapes(model, shapes):
    for inp in model.graph.input:
        if inp.name in shapes:
            dims = inp.type.tensor_type.shape.dim
            for d, v in zip(dims, shapes[inp.name]):
                d.ClearField("dim_param")
                d.dim_value = v
    return model


def prepend_norm(model, in_name, scale, bias):
    """New float input `in_name` gets x*scale+bias before the original first consumer."""
    g = model.graph
    raw = in_name + "_raw"
    for n in g.node:
        for i, x in enumerate(n.input):
            if x == in_name:
                n.input[i] = raw
    g.initializer.extend([numpy_helper.from_array(np.array(scale, np.float32), "pj_scale"),
                          numpy_helper.from_array(np.array(bias, np.float32), "pj_bias")])
    mul = helper.make_node("Mul", [in_name, "pj_scale"], ["pj_mul"])
    add = helper.make_node("Add", ["pj_mul", "pj_bias"], [raw])
    g.node.insert(0, add)
    g.node.insert(0, mul)
    return model


if __name__ == "__main__":
    task, src, prefix = sys.argv[1:4]
    m = onnx.load(src)
    if task == "matting":
        edge = int(sys.argv[4]) if len(sys.argv) > 4 else 256  # 512 = HQ tier
        m = fix_shapes(m, {"input": [1, 3, edge, edge]})
        m = prepend_norm(m, "input", 2.0, -1.0)  # (x-0.5)/0.5
    onnx.save(m, prefix + ".onnx")
    # TFLite step: o2t.py (onnx2tf without its online test-image download)
    print("done", prefix)
