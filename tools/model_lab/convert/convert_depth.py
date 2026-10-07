"""Convert Depth Anything V2 Small (Apache-2.0) into a drop-in PixiJoy depth.tflite.

Frozen app contract (PackModelContract.isMonocularDepth):
  in  FLOAT32 NHWC 1x256x256x3, RGB 0..1
  out FLOAT32 NHWC 1x256x256x1, 0 = near, 1 = far
Everything (ImageNet normalize, resize to 266 = 19x14 patches, disparity -> 0..1 depth) is baked
into the graph, so the Kotlin side needs no change except the new SHA-256 pin.

Usage: /tmp/conv/bin/python convert_depth.py --out /app/lab/out/depth_dav2s.tflite [--weights dir]
"""
import argparse

import torch
import torch.nn.functional as F
from transformers import AutoModelForDepthEstimation


class PixiDepth(torch.nn.Module):
    def __init__(self, path, edge=266):
        super().__init__()
        self.m = AutoModelForDepthEstimation.from_pretrained(path).eval()
        self.edge = edge
        self.register_buffer("mean", torch.tensor([0.485, 0.456, 0.406]).view(1, 3, 1, 1))
        self.register_buffer("std", torch.tensor([0.229, 0.224, 0.225]).view(1, 3, 1, 1))

    def forward(self, x):  # x: 1x256x256x3
        x = x.permute(0, 3, 1, 2)
        x = F.interpolate(x, (self.edge, self.edge), mode="bilinear", align_corners=False)
        x = (x - self.mean) / self.std
        disp = self.m(pixel_values=x).predicted_depth.unsqueeze(1)  # 1x1xHxW, big = near
        disp = F.interpolate(disp, (256, 256), mode="bilinear", align_corners=False)
        flat = disp.reshape(1, 256 * 256)
        lo = torch.min(flat, dim=1).values.reshape(1, 1)
        hi = torch.max(flat, dim=1).values.reshape(1, 1)
        far01 = 1.0 - (flat - lo) / (hi - lo + 1e-6)
        return far01.reshape(1, 256, 256, 1)


if __name__ == "__main__":
    import ai_edge_torch

    ap = argparse.ArgumentParser()
    ap.add_argument("--weights", default="/app/lab/kit/dav2s")
    ap.add_argument("--out", default="/app/lab/out/depth_dav2s.tflite")
    a = ap.parse_args()
    mod = PixiDepth(a.weights).eval()
    sample = (torch.rand(1, 256, 256, 3),)
    edge = ai_edge_torch.convert(mod, sample)
    edge.export(a.out)
    print("exported", a.out)
