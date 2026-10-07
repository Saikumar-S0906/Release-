#!/usr/bin/env python3
"""Build the `pixijoy-ml-kit` folder/zip anywhere (laptop or a Kaggle CPU session), once.

Upload the zip as a private Kaggle Dataset named pixijoy-ml-kit; after that no notebook needs
Hugging Face. Every file is fetched with fallbacks: huggingface.co -> hf-mirror.com -> official source.

  python make_kit.py --out pixijoy-ml-kit        # ~430 MB zip
Contents: dav2s/ (Depth Anything V2 Small), weights/ (MI-GAN 256, MODNet, RAFT-small),
          eval/ (depth.npz, inpaint.npz, matting.npz = the exact Model Lab 100-sample test sets).
"""
import argparse
import os
import shutil
import subprocess
import sys
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent


def hf(repo, path, dest, repo_type="model"):
    from huggingface_hub import hf_hub_download

    for ep in (None, "https://hf-mirror.com"):
        try:
            p = hf_hub_download(repo, path, repo_type=repo_type, endpoint=ep)
            shutil.copy(p, dest)
            return True
        except Exception as e:
            print("HF failed", ep or "huggingface.co", repr(e)[:100])
    return False


def gdrive_folder(url, out):
    subprocess.check_call([sys.executable, "-m", "pip", "-q", "install", "gdown"])
    subprocess.check_call([sys.executable, "-m", "gdown", "-q", "--folder", url, "-O", str(out)])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="pixijoy-ml-kit")
    a = ap.parse_args()
    out = Path(a.out)
    (out / "weights").mkdir(parents=True, exist_ok=True)
    (out / "dav2s").mkdir(exist_ok=True)
    (out / "eval").mkdir(exist_ok=True)

    for f in ("config.json", "model.safetensors", "preprocessor_config.json"):
        if not (out / "dav2s" / f).exists() and not hf("depth-anything/Depth-Anything-V2-Small-hf", f, out / "dav2s" / f):
            raise SystemExit("Depth Anything V2 Small is only published on Hugging Face; retry later or use a VPN")

    w = out / "weights"
    if not (w / "migan_256_places2.pt").exists():
        gdown = out / "_migan"
        gdrive_folder("https://drive.google.com/drive/folders/1xNtvN2lto0p5yFKOEEg9RioMjGrYM74w", gdown)
        shutil.copy(gdown / "migan_256_places2.pt", w)
        shutil.rmtree(gdown)
    if not (w / "modnet_photographic_portrait_matting.ckpt").exists():
        if not hf("DavG25/modnet-pretrained-models", "models/modnet_photographic_portrait_matting.ckpt",
                  w / "modnet_photographic_portrait_matting.ckpt"):
            g = out / "_modnet"
            gdrive_folder("https://drive.google.com/drive/folders/1umYmlCulvIFNaqPjwod1SayFmSRHziyR", g)
            shutil.copy(g / "modnet_photographic_portrait_matting.ckpt", w)
            shutil.rmtree(g)
    raft = w / "raft_small_C_T_V2-01064c6d.pth"
    if not raft.exists():
        urllib.request.urlretrieve("https://download.pytorch.org/models/raft_small_C_T_V2-01064c6d.pth", raft)

    # Eval sets: same seeded builder as the Model Lab (tools/model_lab/prep.py)
    if not (out / "eval" / "matting.npz").exists():
        env = dict(os.environ, PIXI_EVAL_OUT=str((out / "eval").resolve()))
        subprocess.check_call([sys.executable, str(HERE.parent / "model_lab" / "fetch_eval_parquets.py")], env=env)
        subprocess.check_call([sys.executable, str(HERE.parent / "model_lab" / "prep.py")], env=env)
    shutil.make_archive(str(out), "zip", out)
    print("kit ready:", f"{out}.zip")


if __name__ == "__main__":
    main()
