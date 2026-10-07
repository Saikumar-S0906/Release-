"""Quantize a float TFLite with ai-edge-quantizer (keeps FLOAT32 input/output = app contract).

Usage: /tmp/conv/bin/python quantize.py in.tflite out_prefix
Writes out_prefix_wi8.tflite (int8 weights, float math: smallest, safest on every phone)
and    out_prefix_dyn8.tflite (dynamic int8: int8 weights + int8 matmuls, fastest on CPU).
"""
import sys

from ai_edge_quantizer import quantizer, recipe

src, prefix = sys.argv[1], sys.argv[2]
for tag, rec in [("dyn8", recipe.dynamic_wi8_afp32())]:
    q = quantizer.Quantizer(src)
    q.load_quantization_recipe(rec)
    q.quantize().export_model(f"{prefix}_{tag}.tflite")
    print("wrote", f"{prefix}_{tag}.tflite")
