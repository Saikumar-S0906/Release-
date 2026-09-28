"""Generate train_colab.ipynb for Wave M — Drive ON by default, auto-resume, no TFDS."""
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
                f"# PixiJoy {title} — Colab GPU (quality)\n",
                "\n",
                "Runtime → **GPU** → **Run all**.\n",
                "\n",
                "- **Google Drive is ON** — checkpoints + quality data live under "
                f"`MyDrive/pixijoy_ml/{root_name}/` so disconnects do **not** force a full restart.\n",
                "- Auto-fetches real training data (no tensorflow_datasets / no HF).\n",
                "- Reconnect → Run all again: skips finished data prep, **auto-resumes** from "
                "`*.ckpt.keras` + `train_state.json`.\n",
                "- Pin TFLite only if verify prints **OK**.\n",
                "- Set `USE_DRIVE = False` only if you refuse Drive (ephemeral `/content`).\n",
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
                'print("TF", tf.__version__, "GPUs", tf.config.list_physical_devices("GPU"))\n',
                "assert tf.config.list_physical_devices('GPU'), 'Select Runtime → GPU before Run all'\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "USE_DRIVE = True  # persist ckpt + data on Drive (survive disconnect)\n",
                "\n",
                "import os, urllib.request, zipfile, shutil\n",
                "from pathlib import Path\n",
                "from google.colab import files\n",
                "\n",
                f"ROOT = Path('/content/pixijoy_{root_name}')\n",
                f"DRIVE = Path('/content/drive/MyDrive/pixijoy_ml/{root_name}')\n",
                "ROOT.mkdir(parents=True, exist_ok=True)\n",
                "os.chdir(ROOT)\n",
                "\n",
                "if USE_DRIVE:\n",
                "    from google.colab import drive\n",
                "    drive.mount('/content/drive', force_remount=False)\n",
                "    (DRIVE / 'dist').mkdir(parents=True, exist_ok=True)\n",
                "    (DRIVE / 'data').mkdir(parents=True, exist_ok=True)\n",
                "\n",
                "def _link_to_drive(name: str) -> None:\n",
                "    \"\"\"Point local name/ at Drive so writes survive runtime death.\"\"\"\n",
                "    if not USE_DRIVE:\n",
                "        Path(name).mkdir(parents=True, exist_ok=True)\n",
                "        return\n",
                "    local = Path(name)\n",
                "    target = DRIVE / name\n",
                "    target.mkdir(parents=True, exist_ok=True)\n",
                "    if local.exists() or local.is_symlink():\n",
                "        if local.is_symlink():\n",
                "            if local.resolve() == target.resolve():\n",
                "                print(f'{name} -> Drive OK')\n",
                "                return\n",
                "            local.unlink()\n",
                "        elif local.is_dir():\n",
                "            for p in local.rglob('*'):\n",
                "                if not p.is_file():\n",
                "                    continue\n",
                "                dest = target / p.relative_to(local)\n",
                "                dest.parent.mkdir(parents=True, exist_ok=True)\n",
                "                if not dest.is_file():\n",
                "                    shutil.copy2(p, dest)\n",
                "            shutil.rmtree(local)\n",
                "        else:\n",
                "            local.unlink()\n",
                "    local.symlink_to(target)\n",
                "    print(f'{name} -> {target}')\n",
                "\n",
                f"ZIP_URL = 'https://raw.githubusercontent.com/Saikumar-S0906/Release-/main/{zip_name}'\n",
                "ok = False\n",
                "try:\n",
                f"    urllib.request.urlretrieve(ZIP_URL, '{zip_name}')\n",
                f"    with zipfile.ZipFile('{zip_name}') as z:\n",
                "        z.extractall('.')\n",
                "    ok = Path('train.py').is_file() and Path('prepare_quality_data.py').is_file()\n",
                "    print('lab zip OK', ok)\n",
                "except Exception as e:\n",
                "    print('zip fetch failed:', e)\n",
                "if not ok:\n",
                f"    print('Upload {zip_name}')\n",
                "    up = files.upload()\n",
                "    name = next(iter(up))\n",
                "    with zipfile.ZipFile(name) as z:\n",
                "        z.extractall('.')\n",
                "    ok = Path('train.py').is_file()\n",
                "assert ok, 'lab zip missing train.py / prepare_quality_data.py — refresh Runtime and re-fetch'\n",
                "_link_to_drive('dist')\n",
                "_link_to_drive('data')\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "from pathlib import Path\n",
                "print('prepare_quality_data (real data; skips if data/READY already on Drive)…')\n",
                "rc = get_ipython().system('python -u prepare_quality_data.py --data ./data')\n",
                "assert rc == 0, 'prepare_quality_data failed'\n",
                "assert (Path('data') / 'READY').is_file(), 'data/READY missing — Drive link or prepare failed'\n",
                "print('data READY ok')\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "from pathlib import Path\n",
                "import hashlib\n",
                "USE_DRIVE = globals().get('USE_DRIVE', True)\n",
                f"OUT = Path('/content/drive/MyDrive/pixijoy_ml/{root_name}/dist/{keras_name}') if USE_DRIVE else Path('dist') / '{keras_name}'\n",
                "OUT.parent.mkdir(parents=True, exist_ok=True)\n",
                "ckpt = OUT.with_suffix('.ckpt.keras')\n",
                "print('ckpt exists' if ckpt.is_file() else 'fresh train', OUT)\n",
                "print('state', OUT.parent / 'train_state.json', (OUT.parent / 'train_state.json').is_file())\n",
                "\n",
                'cmd = "python -u train.py --quality --out \\"" + str(OUT) + "\\""\n',
                "if ckpt.is_file():\n",
                '    cmd += " --resume"\n',
                "    print('will resume from checkpoint')\n",
                "print('RUN', cmd)\n",
                "rc = get_ipython().system(cmd)\n",
                "assert rc == 0, 'train failed'\n",
                f'rc = get_ipython().system(\'python -u export_tflite.py --keras "\' + str(OUT) + \'" --out "dist/{artifact}"\')\n',
                "assert rc == 0, 'export failed (refusing untrained is correct)'\n",
                "rc = get_ipython().system('python -u verify_contract.py --quality --data ./data')\n",
                "assert rc == 0, 'verify --quality failed — do NOT pin'\n",
                f"p = Path('dist/{artifact}')\n",
                "assert p.is_file(), p\n",
                "print('SHIP_OK', p.stat().st_size, hashlib.sha256(p.read_bytes()).hexdigest())\n",
            ],
        },
        {
            "cell_type": "code",
            "metadata": {},
            "outputs": [],
            "execution_count": None,
            "source": [
                "from google.colab import files\n",
                f"files.download('dist/{artifact}')\n",
                f"print('Attach {artifact} to ml-packs-v1 ONLY if verify printed OK / SHIP_OK')\n",
            ],
        },
    ]
    return {
        "nbformat": 4,
        "nbformat_minor": 5,
        "metadata": {"kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"}},
        "cells": cells,
    }


def main() -> None:
    specs = [
        ("inpaint", "Inpaint (M1)", "inpaint_lab.zip", "inpaint.tflite", "inpaint.keras"),
        ("depth", "Depth (M2)", "depth_lab.zip", "depth.tflite", "depth.keras"),
        ("matting", "Matting (M3)", "matting_lab.zip", "matting.tflite", "matting.keras"),
        ("flow", "Flow (M4)", "flow_lab.zip", "flow.tflite", "flow.keras"),
    ]
    for folder, title, zname, art, keras in specs:
        path = BASE / folder / "train_colab.ipynb"
        path.write_text(json.dumps(make_nb(title, folder, zname, art, keras), indent=1), encoding="utf-8")
        print("wrote", path)


if __name__ == "__main__":
    main()
