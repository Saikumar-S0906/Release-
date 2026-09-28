"""Generate train_kaggle.ipynb for Wave M — Kaggle GPU (~30h/week)."""
from __future__ import annotations

import json
from pathlib import Path

BASE = Path(__file__).resolve().parent


def make_nb(title: str, root_name: str, zip_name: str, artifact: str, keras_name: str) -> dict:
    cells = [
        {
            "cell_type": "markdown",
            "metadata": {},
            "source": [
                f"# PixiJoy {title} — Kaggle GPU (quality)\n",
                "\n",
                "**Settings (right panel):** Accelerator → **GPU T4** (or P100) → Internet **ON** → Save.\n",
                "\n",
                "Kaggle free tier ≈ **30 GPU hours / week**, then resets.\n",
                "\n",
                "**Resume across sessions:** after a run, download `dist/*.ckpt.keras` + `train_state.json`,\n",
                "or *Save Version* (Save & Run All) and add the output as a Dataset Input next time under\n",
                f"`/kaggle/input/.../`. Put files into `dist/` before train.\n",
                "\n",
                "Pin TFLite only if verify prints **OK** / **SHIP_OK**.\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "!nvidia-smi || true\n",
                "import tensorflow as tf\n",
                "gpus = tf.config.list_physical_devices('GPU')\n",
                'print("TF", tf.__version__, "GPUs", gpus, flush=True)\n',
                "assert gpus, 'Enable GPU: Notebook settings → Accelerator → GPU T4 → Save → Restart'\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "import os, sys, urllib.request, zipfile, shutil\n",
                "from pathlib import Path\n",
                "\n",
                f"ROOT = Path('/kaggle/working/pixijoy_{root_name}')\n",
                "ROOT.mkdir(parents=True, exist_ok=True)\n",
                "os.chdir(ROOT)\n",
                "(ROOT / 'dist').mkdir(exist_ok=True)\n",
                "(ROOT / 'data').mkdir(exist_ok=True)\n",
                "print('cwd', ROOT, flush=True)\n",
                "\n",
                "# Optional: copy prior ckpt from a Kaggle Dataset Input\n",
                "for inp in Path('/kaggle/input').glob('*'):\n",
                "    for name in (f'{keras_name}', f'{Path(keras_name).stem}.ckpt.keras', 'train_state.json'):\n",
                "        src = next(inp.rglob(name), None)\n",
                "        if src and src.is_file():\n",
                "            dst = ROOT / 'dist' / src.name\n",
                "            shutil.copy2(src, dst)\n",
                "            print('restored from input', src, '->', dst, flush=True)\n",
                "\n",
                f"ZIP_URL = 'https://raw.githubusercontent.com/Saikumar-S0906/Release-/main/{zip_name}'\n",
                "LAB_ZIP_VERSION = '20260928e'\n",
                f"urllib.request.urlretrieve(ZIP_URL + f'?v={{LAB_ZIP_VERSION}}', '{zip_name}')\n",
                f"with zipfile.ZipFile('{zip_name}') as z:\n",
                "    z.extractall('.')\n",
                "assert Path('train.py').is_file()\n",
                "print('lab zip OK', flush=True)\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "import os, subprocess, sys\n",
                "from pathlib import Path\n",
                "\n",
                "def run_stream(cmd):\n",
                "    print('STREAM', ' '.join(map(str, cmd)), flush=True)\n",
                "    env = {**os.environ, 'PYTHONUNBUFFERED': '1'}\n",
                "    p = subprocess.Popen([str(c) for c in cmd], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, env=env)\n",
                "    assert p.stdout is not None\n",
                "    for line in p.stdout:\n",
                "        print(line, end='', flush=True)\n",
                "    rc = p.wait()\n",
                "    print(f'[exit {rc}]', flush=True)\n",
                "    return rc\n",
                "\n",
                "rc = run_stream([sys.executable, '-u', 'prepare_quality_data.py', '--data', './data'])\n",
                "assert rc == 0\n",
                "assert (Path('data') / 'READY').is_file()\n",
                "print('data READY ok', flush=True)\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "import os, hashlib, subprocess, sys\n",
                "from pathlib import Path\n",
                "\n",
                f"OUT = Path('dist') / '{keras_name}'\n",
                "ckpt = OUT.with_suffix('.ckpt.keras')\n",
                "\n",
                "def run_stream(cmd):\n",
                "    print('STREAM', ' '.join(map(str, cmd)), flush=True)\n",
                "    env = {**os.environ, 'PYTHONUNBUFFERED': '1'}\n",
                "    p = subprocess.Popen([str(c) for c in cmd], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1, env=env)\n",
                "    assert p.stdout is not None\n",
                "    for line in p.stdout:\n",
                "        print(line, end='', flush=True)\n",
                "    return p.wait()\n",
                "\n",
                "cmd = [sys.executable, '-u', 'train.py', '--quality', '--out', str(OUT)]\n",
                "if ckpt.is_file():\n",
                "    cmd.append('--resume')\n",
                "    print('will resume', ckpt, flush=True)\n",
                "rc = run_stream(cmd)\n",
                "assert rc == 0\n",
                f"rc = run_stream([sys.executable, '-u', 'export_tflite.py', '--keras', str(OUT), '--out', 'dist/{artifact}'])\n",
                "assert rc == 0\n",
                f"rc = run_stream([sys.executable, '-u', 'verify_contract.py', '--quality', '--data', './data', '--model', 'dist/{artifact}'])\n",
                "assert rc == 0, 'verify failed — do NOT pin'\n",
                f"p = Path('dist/{artifact}')\n",
                "print('SHIP_OK', p.stat().st_size, hashlib.sha256(p.read_bytes()).hexdigest(), flush=True)\n",
                "print('Download dist/ from Output, or Save Version to keep GPU session artifacts.', flush=True)\n",
            ],
        },
    ]
    return {
        "nbformat": 4,
        "nbformat_minor": 5,
        "metadata": {
            "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
            "accelerator": "GPU",
        },
        "cells": cells,
    }


def main() -> None:
    specs = [
        ("inpaint", "Inpaint (M1)", "inpaint_lab.zip", "inpaint.tflite", "inpaint.keras"),
        ("depth", "Depth (M2)", "depth_lab.zip", "depth.tflite", "depth.keras"),
        ("matting", "Matting (M3)", "matting_lab.zip", "matting.tflite", "matting.keras"),
        ("flow", "Flow (M4)", "flow_lab.zip", "flow.tflite", "flow.keras"),
    ]
    out_dir = BASE / "ml_release"
    out_dir.mkdir(exist_ok=True)
    for folder, title, zname, art, keras in specs:
        nb = make_nb(title, folder, zname, art, keras)
        path = BASE / folder / "train_kaggle.ipynb"
        path.write_text(json.dumps(nb, indent=1), encoding="utf-8")
        # also stage for Release-
        rel = {
            "inpaint": "train_kaggle.ipynb",
            "depth": "train_depth_kaggle.ipynb",
            "matting": "train_matting_kaggle.ipynb",
            "flow": "train_flow_kaggle.ipynb",
        }[folder]
        (out_dir / rel).write_text(json.dumps(nb, indent=1), encoding="utf-8")
        print("wrote", path, "and", out_dir / rel)


if __name__ == "__main__":
    main()
