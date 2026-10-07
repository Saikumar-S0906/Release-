"""Wrap MI-GAN (Picsart, MIT) ONNX into the PixiJoy inpaint contract and fold it for onnx2tf.

App contract (PackModelContract.isInpaintRgbAndMask + PackLiteRt.inferInpaintDual):
  in  image FLOAT32 1x256x256x3 RGB 0..1   (original pixels; MI-GAN masks them itself)
  in  mask  FLOAT32 1x256x256x1 1 = hole
  out       FLOAT32 1x256x256x3 RGB 0..1
MI-GAN raw graph: image uint8 NCHW 0..255, mask uint8 255 = keep / 0 = hole, result uint8.
"""
import sys

import numpy as np
import onnx
import onnxruntime as ort
from onnx import TensorProto, helper, numpy_helper

src, dst = sys.argv[1], sys.argv[2]
S = int(sys.argv[3]) if len(sys.argv) > 3 else 256
m = onnx.load(src)
g = m.graph
for inp in list(g.input):
    g.input.remove(inp)
for n in g.node:
    n.input[:] = [{"image": "u8_image", "mask": "u8_mask"}.get(x, x) for x in n.input]
    n.output[:] = ["u8_result" if x == "result" else x for x in n.output]
for o in list(g.output):
    g.output.remove(o)
c = lambda name, v: numpy_helper.from_array(np.array(v, np.float32), name)
g.initializer.extend([c("k255", 255.0), c("k1", 1.0), c("kinv", 1 / 255.0), c("k05", 0.5)])
pre = [
    helper.make_node("Mul", ["image", "k255"], ["img255"]),
    helper.make_node("Add", ["img255", "k05"], ["img255r"]),
    helper.make_node("Cast", ["img255r"], ["u8_image"], to=TensorProto.UINT8),
    helper.make_node("Sub", ["k1", "mask"], ["keep"]),
    helper.make_node("Mul", ["keep", "k255"], ["keep255"]),
    helper.make_node("Add", ["keep255", "k05"], ["keep255r"]),
    helper.make_node("Cast", ["keep255r"], ["u8_mask"], to=TensorProto.UINT8),
]
post = [
    helper.make_node("Cast", ["u8_result"], ["res_f"], to=TensorProto.FLOAT),
    helper.make_node("Mul", ["res_f", "kinv"], ["result"]),
]
for i, n in enumerate(pre):
    g.node.insert(i, n)
g.node.extend(post)
g.input.extend([helper.make_tensor_value_info("image", TensorProto.FLOAT, [1, 3, S, S]),
                helper.make_tensor_value_info("mask", TensorProto.FLOAT, [1, 1, S, S])])
g.output.extend([helper.make_tensor_value_info("result", TensorProto.FLOAT, [1, 3, S, S])])
onnx.save(m, dst + ".wrapped.onnx")
o = ort.SessionOptions()
o.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_BASIC
o.optimized_model_filepath = dst
s = ort.InferenceSession(dst + ".wrapped.onnx", o, providers=["CPUExecutionProvider"])
r = s.run(None, {"image": np.random.rand(1, 3, S, S).astype(np.float32),
                 "mask": np.zeros((1, 1, S, S), np.float32)})[0]
print("folded", dst, r.shape, r.dtype)
