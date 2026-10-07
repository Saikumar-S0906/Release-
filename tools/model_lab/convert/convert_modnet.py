"""Fine-tuned MODNet .pt -> ONNX (1x3x256x256) for wrap_onnx.py + o2t.py + quantize.py.

python convert_modnet.py --weights out/matting_best.pt --modnet-src MODNet --out out/matting_ft.onnx
Then:  python wrap_onnx.py matting out/matting_ft.onnx out/matting_ft      (bakes (x-0.5)/0.5, NHWC)
       fold (ORT basic) -> o2t.py -> quantize.py                            (see export_matting in notebook)
"""
import argparse
import sys

import torch

ap = argparse.ArgumentParser()
ap.add_argument("--weights", required=True)
ap.add_argument("--modnet-src", default="MODNet")
ap.add_argument("--out", default="out/matting_ft.onnx")
a = ap.parse_args()
sys.path.append(a.modnet_src)  # append: MODNet ships an onnx/ folder that would shadow the onnx package
from src.models.modnet import MODNet  # noqa: E402


class Infer(torch.nn.Module):
    def __init__(self, net):
        super().__init__()
        self.net = net

    def forward(self, x):
        return self.net(x, True)[2]


net = MODNet(backbone_pretrained=False)
sd = torch.load(a.weights, map_location="cpu")
net.load_state_dict({k.replace("module.", "", 1): v for k, v in sd.items()})
torch.onnx.export(Infer(net.eval()), torch.rand(1, 3, 256, 256), a.out, input_names=["input"],
                  output_names=["output"], opset_version=17, dynamo=False)
print("onnx", a.out)
