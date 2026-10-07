"""RAFT-small (torchvision, BSD-3, trained FlyingChairs+Things) -> PixiJoy flow.tflite.

Contract (tools/flow/config.py):
  in0 frame_t  FLOAT32 1x256x256x3 RGB 0..1
  in1 frame_t1 FLOAT32 1x256x256x3 RGB 0..1
  out flow     FLOAT32 1x256x256x2  (dx, dy) / 256
"""
import argparse

import torch
import torchvision


class PixiFlow(torch.nn.Module):
    def __init__(self, iters=8):
        super().__init__()
        self.raft = torchvision.models.optical_flow.raft_small(weights="DEFAULT").eval()
        self.iters = iters

    def forward(self, a, b):
        a = a.permute(0, 3, 1, 2) * 2 - 1
        b = b.permute(0, 3, 1, 2) * 2 - 1
        fl = self.raft(a, b, num_flow_updates=self.iters)[-1]
        return (fl / 256.0).permute(0, 2, 3, 1)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--iters", type=int, default=8)
    ap.add_argument("--out", default="/app/lab/out/flow_raft_small.tflite")
    a = ap.parse_args()
    m = PixiFlow(a.iters).eval()
    sample = (torch.rand(1, 256, 256, 3), torch.rand(1, 256, 256, 3))
    import ai_edge_torch

    ai_edge_torch.convert(m, sample, strict_export=False).export(a.out)
    print("exported", a.out)
