"""NeuFlow v2 "things" (Apache-2.0, no Sintel in training) -> PixiJoy flow.tflite.

Contract (tools/flow/config.py):
  in0 frame_t  FLOAT32 1x256x256x3 RGB 0..1
  in1 frame_t1 FLOAT32 1x256x256x3 RGB 0..1
  out flow     FLOAT32 1x256x256x2  (dx, dy) / 256
Usage: /app/lab/.conv/bin/python convert_neuflow.py --repo /app/lab/NeuFlow_v2 --out /app/lab/out/flow_neuflow.tflite
"""
import argparse
import sys

import torch
import torch.nn.functional as F


def bilinear_sampler(img, coords, mode="bilinear", mask=False):
    """grid_sample-free bilinear lookup (TFLite has no grid_sample): pixel coords -> gather."""
    b, c, h, w = img.shape
    x, y = coords[..., 0], coords[..., 1]  # (B, Ho, Wo)
    x0, y0 = torch.floor(x), torch.floor(y)
    wx, wy = x - x0, y - y0
    flat = img.reshape(b, c, h * w)

    def tap(yy, xx):
        valid = ((xx >= 0) & (xx <= w - 1) & (yy >= 0) & (yy <= h - 1)).to(img.dtype)
        idx = (yy.clamp(0, h - 1) * w + xx.clamp(0, w - 1)).long().reshape(b, 1, -1).expand(b, c, -1)
        v = torch.gather(flat, 2, idx).reshape(b, c, *x.shape[1:])
        return v * valid.unsqueeze(1)

    out = (tap(y0, x0) * ((1 - wx) * (1 - wy)).unsqueeze(1) + tap(y0, x0 + 1) * (wx * (1 - wy)).unsqueeze(1)
           + tap(y0 + 1, x0) * ((1 - wx) * wy).unsqueeze(1) + tap(y0 + 1, x0 + 1) * (wx * wy).unsqueeze(1))
    return out


def _upsample_4d(self, feature, flow):
    """Same math as NeuFlow UpSample.forward (convex 3x3 upsampling) with tensors kept <= 4-D,
    because LiteRT cannot broadcast the original 7-D softmax/mul. Uses pixel_shuffle for the
    [B,2,K,K,H,W] -> [B,2,KH,KW] interleave (identical index order)."""
    k = self.upsample_factor
    mask = self.conv3(self.relu(self.conv2(self.relu(self.conv1(torch.cat((flow, feature), dim=1))))))
    b, _, h, w = flow.shape
    mask = torch.softmax(mask.reshape(b, 9, k * k, h * w), dim=1)
    uf = F.unfold(flow, [3, 3], padding=1)  # [B, 2*9, H*W]
    outs = [torch.sum(mask * uf[:, c * 9:(c + 1) * 9].reshape(b, 9, 1, h * w), dim=1) for c in range(2)]
    up = torch.cat(outs, dim=1).reshape(b, 2 * k * k, h, w)
    return F.pixel_shuffle(up, k)


class PixiFlow(torch.nn.Module):
    def __init__(self, repo, weights, iters_s8=8):
        super().__init__()
        sys.path.insert(0, repo)
        import NeuFlow.corr as corr
        import NeuFlow.utils as nutils
        from NeuFlow.backbone_v7 import ConvBlock
        from NeuFlow.neuflow import NeuFlow

        corr.bilinear_sample = bilinear_sampler
        from NeuFlow.upsample import UpSample

        UpSample.forward = _upsample_4d
        nutils.bilinear_sample = lambda img, c: bilinear_sampler(img, c.permute(0, 2, 3, 1))
        self.m = NeuFlow()
        self.m.load_state_dict(torch.load(weights, map_location="cpu")["model"], strict=True)
        for mod in self.m.modules():  # BN folding as in the repo's infer.py
            if type(mod) is ConvBlock:
                mod.conv1 = torch.nn.utils.fuse_conv_bn_eval(mod.conv1.eval(), mod.norm1.eval())
                mod.conv2 = torch.nn.utils.fuse_conv_bn_eval(mod.conv2.eval(), mod.norm2.eval())
                del mod.norm1, mod.norm2
                mod.forward = mod.forward_fuse
        self.m.eval()
        self.m.init_bhwd(1, 256, 256, "cpu", False)
        self.iters = iters_s8

    def forward(self, a, b):
        a = a.permute(0, 3, 1, 2) * 255.0
        b = b.permute(0, 3, 1, 2) * 255.0
        fl = self.m(a, b, iters_s16=1, iters_s8=self.iters)[-1]
        return (fl / 256.0).permute(0, 2, 3, 1)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default="/app/lab/NeuFlow_v2")
    ap.add_argument("--weights", default=None)
    ap.add_argument("--iters", type=int, default=8)
    ap.add_argument("--out", default="/app/lab/out/flow_neuflow.tflite")
    a = ap.parse_args()
    m = PixiFlow(a.repo, a.weights or f"{a.repo}/neuflow_things.pth", a.iters).eval()
    sample = (torch.rand(1, 256, 256, 3), torch.rand(1, 256, 256, 3))
    with torch.no_grad():
        print("torch ok", m(*sample).shape)
    import ai_edge_torch

    ai_edge_torch.convert(m, sample, strict_export=False).export(a.out)
    print("exported", a.out)
