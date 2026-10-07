"""Generate the four Kaggle notebooks (self-contained: every script is embedded with %%writefile).

python tools/kaggle_v2/gen_notebooks.py   ->  tools/kaggle_v2/notebooks/pixijoy_{task}_kaggle.ipynb
"""
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
EXPORT = HERE / "export"
OUTD = HERE / "notebooks"

TASKS = {
    "depth": dict(hours=4, title="M2 Depth — Depth Anything V2 Small fine-tune + self-distill",
                  export="""!$PY export/convert_depth.py --weights out/depth_best_hf --out out/depth_ft.tflite
!$PY export/quantize.py out/depth_ft.tflite out/depth_ft"""),
    "matting": dict(hours=5, title="M3 Matting — MODNet fine-tune (P3M-10k)",
                    export="""!$PY export/convert_modnet.py --weights out/matting_best.pt --modnet-src MODNet --out out/matting_ft.onnx
!$PY export/wrap_onnx.py matting out/matting_ft.onnx out/matting_ft_w
!$PY -c "import onnxruntime as o;s=o.SessionOptions();s.graph_optimization_level=o.GraphOptimizationLevel.ORT_ENABLE_BASIC;s.optimized_model_filepath='out/matting_ft_fold.onnx';o.InferenceSession('out/matting_ft_w.onnx',s,providers=['CPUExecutionProvider'])"
!$PY export/o2t.py out/matting_ft_fold.onnx out/matting_ft_tf
!$PY export/quantize.py out/matting_ft_tf/matting_ft_fold_float32.tflite out/matting_ft"""),
    "inpaint": dict(hours=8, title="M1 Inpaint — MI-GAN 256 fine-tune (COCO photos, app hole masks)",
                    export="""!$PY export/convert_migan.py --weights out/inpaint_best.pt --out out/inpaint_ft.tflite
!$PY export/quantize.py out/inpaint_ft.tflite out/inpaint_ft"""),
    "flow": dict(hours=6, title="M4 Flow — RAFT-small fine-tune (Sintel, held-out scenes)",
                 export="""# TFLite export of RAFT is blocked (tfl.div legalize error in torchvision raft.py:497).
# Keep out/flow_best.pt; the app has no flow pack slot yet (Phase 2)."""),
}


def md(*lines):
    return {"cell_type": "markdown", "metadata": {}, "source": [l + "\n" for l in lines]}


def code(src):
    return {"cell_type": "code", "metadata": {}, "outputs": [], "execution_count": None,
            "source": [l + "\n" for l in src.strip("\n").split("\n")]}


def writefile(path, text):
    return code(f"%%writefile {path}\n{text}")


def build(task, cfg):
    files = [writefile("pixi_ft.py", (HERE / "pixi_ft.py").read_text())]
    if task != "flow":
        files += [writefile(f"export/{p.name}", p.read_text()) for p in sorted(EXPORT.glob("*.py"))]
        files.append(writefile("export/setup_conv_env.sh", (EXPORT / "setup_conv_env.sh").read_text()))
    cells = [
        md(f"# PixiJoy {cfg['title']}",
           "",
           "**Settings:** Accelerator **GPU T4 x1** (or P100) · Internet **On** · Persistence **Files only**.",
           "**Inputs:** add your private Dataset **pixijoy-ml-kit** (weights + the exact Model Lab eval sets).",
           "With the kit attached nothing is fetched from Hugging Face; without it the script tries",
           "HF → hf-mirror.com → official sources.",
           "",
           f"Budget: **{cfg['hours']} GPU-hours** (whole kit ≈ 23 h, fits Kaggle's 30 h/week).",
           "Session ends at 12 h — the run checkpoints to `out/` every eval; *Save Version* keeps it and the",
           "next run auto-resumes when you add that version's output as an Input.",
           "",
           "Ship rule: export only if `out/<task>_report.json` says `improved: true` — otherwise ship the",
           "pretrained conversion from the Model Lab report (already better than v1)."),
        code("!nvidia-smi -L || echo 'NO GPU: Settings -> Accelerator -> GPU T4'\n"
             "!pip -q install remotezip lpips h5py\n"
             "import os; os.makedirs('export', exist_ok=True); os.makedirs('out', exist_ok=True)"),
        *files,
        md("## Train (wall-clock budgeted, resumable)"),
        code(f"!python pixi_ft.py {task} --hours {cfg['hours']}\n!cat out/{task}_report.json"),
        md("## Export to the frozen app contract (CPU converter env, ~6 min to install)"),
        code("import json\nrep = json.load(open('out/%s_report.json'))\nprint('improved:', rep['improved'])" % task),
        code("!bash export/setup_conv_env.sh /tmp/conv\n%env PY=/tmp/conv/bin/python\n" + cfg["export"]),
        code("!sha256sum out/*.tflite 2>/dev/null; ls -la out"),
    ]
    return {"cells": cells, "metadata": {"kernelspec": {"display_name": "Python 3", "language": "python",
                                                        "name": "python3"},
                                         "language_info": {"name": "python"}},
            "nbformat": 4, "nbformat_minor": 5}


if __name__ == "__main__":
    OUTD.mkdir(exist_ok=True)
    for t, c in TASKS.items():
        p = OUTD / f"pixijoy_{t}_kaggle.ipynb"
        p.write_text(json.dumps(build(t, c), indent=1))
        print("wrote", p)
