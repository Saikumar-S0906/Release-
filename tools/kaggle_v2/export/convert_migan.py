"""MI-GAN 256 Places2 (Picsart, MIT code + MIT weights) -> PixiJoy inpaint.tflite via ai-edge-torch.

Contract (PackModelContract.isInpaintRgbAndMask, PackLiteRt.inferInpaintDual):
  in0 image FLOAT32 1x256x256x3 RGB 0..1 (original pixels — the app does NOT blank the hole)
  in1 mask  FLOAT32 1x256x256x1 1 = hole
  out       FLOAT32 1x256x256x3 RGB 0..1, known pixels kept, hole generated
Usage: /opt/conv/bin/python convert_migan.py --weights /tmp/migan/models/migan_256_places2.pt
"""
import argparse
import sys
from pathlib import Path

import torch

import importlib.util  # noqa: E402

# MI-GAN inference generator (MIT). Two export-only patches: no in-place leaky_relu, no python
# branch on the activation gain (torch.export cannot trace either).
_SRC = Path(__file__).with_name("migan_inference.py")
if not _SRC.exists():
    import urllib.request

    urllib.request.urlretrieve(
        "https://raw.githubusercontent.com/Picsart-AI-Research/MI-GAN/main/lib/model_zoo/migan_inference.py", _SRC)
_code = _SRC.read_text().replace("inplace=True", "inplace=False").replace(
    "        if act_gain != 1:\n            x = x * act_gain\n", "        x = x * act_gain\n")
_SRC.write_text(_code)
_spec = importlib.util.spec_from_file_location("migan_inference", _SRC)
_mod = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_mod)
Generator = _mod.Generator


class PixiInpaint(torch.nn.Module):
    def __init__(self, weights):
        super().__init__()
        self.g = Generator(resolution=256)
        self.g.load_state_dict(torch.load(weights, map_location="cpu"))
        self.g.eval()
        # MI-GAN keeps FIR filters as plain tensor attributes; torch.export needs them as buffers.
        for mod in self.g.modules():
            for k, v in list(vars(mod).items()):
                if isinstance(v, torch.Tensor) and not isinstance(v, torch.nn.Parameter):
                    delattr(mod, k)
                    mod.register_buffer(k, v, persistent=False)

    def forward(self, image, mask):
        img = image.permute(0, 3, 1, 2) * 2 - 1
        keep = 1 - mask.permute(0, 3, 1, 2)
        x = torch.cat([keep - 0.5, img * keep], dim=1)
        out = self.g(x)  # -1..1
        out = (out.clamp(-1, 1) + 1) * 0.5
        rgb = image.permute(0, 3, 1, 2)
        comp = rgb * keep + out * (1 - keep)
        return comp.permute(0, 2, 3, 1)


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--weights", default="/app/lab/kit/weights/migan_256_places2.pt")
    ap.add_argument("--out", default="/app/lab/out/inpaint_migan256.tflite")
    a = ap.parse_args()
    m = PixiInpaint(a.weights).eval()
    sample = (torch.rand(1, 256, 256, 3), (torch.rand(1, 256, 256, 1) > 0.8).float())
    with torch.no_grad():
        print("torch ok", m(*sample).shape)
    import ai_edge_torch

    ai_edge_torch.convert(m, sample, strict_export=False).export(a.out)
    print("exported", a.out)
