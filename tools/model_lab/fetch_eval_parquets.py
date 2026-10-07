"""Download the public test parquets used by prep.py into /app/lab/data (or $PIXI_DATA).

NYU Depth V2 official test (HF vikhyatk/nyu_depth_v2 validation shard 0), P3M-500-P (HF nobg/P3M-10K),
Places365 val shard 0 (HF dpdl-benchmark/Places365-Validation). Tries huggingface.co then hf-mirror.com.
"""
import os
import shutil

from huggingface_hub import hf_hub_download

DATA = os.environ.get("PIXI_DATA", "/app/lab/data")
FILES = {
    "nyuval.parquet": ("vikhyatk/nyu_depth_v2", "data/validation-00000-of-00003-0c667cfcd871aca4.parquet"),
    "p3m500p.parquet": ("nobg/P3M-10K", "data/P3M_500_P-00000-of-00001.parquet"),
    "places0.parquet": ("dpdl-benchmark/Places365-Validation", "data/train-00000-of-00005.parquet"),
}
os.makedirs(DATA, exist_ok=True)
for name, (repo, path) in FILES.items():
    dst = os.path.join(DATA, name)
    if os.path.exists(dst):
        continue
    for ep in (None, "https://hf-mirror.com"):
        try:
            shutil.copy(hf_hub_download(repo, path, repo_type="dataset", endpoint=ep), dst)
            break
        except Exception as e:
            print("failed", ep or "huggingface.co", repr(e)[:100])
    else:
        raise SystemExit(f"could not fetch {repo}/{path}")
print("eval parquets ready in", DATA)
