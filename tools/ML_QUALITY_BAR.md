# Wave M — ML quality bar (non-negotiable)

**Rule:** Do **not** ship synthetic-smoke weights as product packs.  
Smoke labs prove the **contract** (shapes / LiteRT). **Quality mode** proves the **look**.

App must never label Telea / HSV sky / classical Z as neural. Pack CTA = Ready only when quality TFLite is pinned.

---

## Scope you asked for (no compromise)

| Pack | Must deliver | Not acceptable |
|------|--------------|----------------|
| **M1 Inpaint** | Seamless hole fill on **real** photos (logo/boom/wire remove) | Synthetic-only smoke weights; untrained export; Telea labeled neural |
| **M2 Depth** | Soft Z that places CG **behind / in front** of subject on real photos/film stills | Synthetic blob MAE-only; flat cardboard depth |
| **M3 Matting** | Clean hair / edge alpha for plates & sky under subject | Hard selfie oval; green spill leftovers as “done” |
| **M4 Flow** | Stabilize / attach assist that beats crop-zoom on handheld | Global translation toy net only |
| **M7 Sky** (photo gap) | Sky segmenter that beats C4 HSV heuristic on trees / dusk / wires | Shipping heuristic forever |

M5/M6 stay optional. **No cheap quality on any ship pack.**

---

## Two train modes

| Mode | Purpose | Ship to Release-? |
|------|---------|-------------------|
| **`--smoke`** (default in short Colab) | Contract + export path works | **No** |
| **`--quality`** | Real datasets + longer train + hard gates | **Yes — only this** |

```bash
# Contract only
python train.py --smoke
python export_tflite.py && python verify_contract.py

# Product pack (required before pin)
python train.py --quality --data ./data
python export_tflite.py && python verify_contract.py --quality
```

Colab: set the notebook cell `TRAIN_MODE = "quality"` and mount / upload real data.

---

## Datasets (quality)

| Pack | Primary data (license must allow on-device distill) | Notes |
|------|-----------------------------------------------------|-------|
| **M1** | Places2 / DIV2K / COCO stills / phone photos (≥32 in `data/rgb`) | Optional paired `masks/`; else irregular holes on real RGB |
| **M2** | NYU Depth V2 and/or KITTI depth (eigen split) + mixed phone stills | Relative depth OK; calibrate near=0 far=1 |
| **M3** | VideoMatte240K / MODNet-style portrait+BG / AIM | Prefer soft hair edges; augment motion blur |
| **M4** | MPI Sintel + FlyingChairs/FlyingThings (or subset) | Output flow as `(dx,dy)/SIZE` per contract |
| **M7** | SkySeg / ADE20K sky class / custom phone sky masks | Optional after M2–M4 if GPU free |

Document exact paths in each lab `data/README.md` when you drop the set.

---

## Architecture (quality)

- Prefer **teacher → student distill** when a strong open teacher exists (Depth Anything / MiDaS-class → our UNet; robust matte teacher → our UNet).
- Student must still match **frozen LiteRT contract** (`verify_contract.py`).
- If student MAE on held-out **real** val fails the gate, **do not upload** — widen capacity or train longer, do not loosen the gate for a fake green.

---

## Ship gates (hard)

| Pack | Smoke MAE (synthetic) | **Quality gate (real val)** |
|------|----------------------|-----------------------------|
| M1 | masked MAE ≤ **0.12** | masked MAE ≤ **0.08** on held-out **real** photos |
| M2 | ≤ 0.18 | Abs-rel ≤ **0.12** *or* MAE ≤ **0.08** on held-out real |
| M3 | ≤ 0.16 | MAD ≤ **0.04** on hair/edge crops; SAD/MSE recorded |
| M4 | ≤ 0.08 | EPE ≤ **1.5 px** @ 256 on Sintel clean subset |
| M7 | — | IoU ≥ **0.85** on sky class val |

Also required for every ship:

1. `verify_contract.py` PASS (shapes + FLOAT32)  
2. SHA-256 recorded next to Release- asset  
3. Device smoke: preview ≈ export with pack **Ready**; classical fallback when Missing  
4. UI copy never says “AI depth” until `engineReady`

Size: prefer Release- host if > ~3 MB; base APK stays lean.

---

## Colab order (quality, full)

1. Finish **M1** quality inpaint  
2. **M2** quality depth (2nd GPU / next runtime)  
3. **M3** quality matting  
4. **M4** quality flow  
5. **M7** sky when photo sky still gaps after M3 (matting helps BG swap; sky segmenter closes C4)

Parallel GPUs = run M2 and M3 together after M1. **No skipping quality mode to “just get a file.”**

---

## Honesty in product

| User sees | Truth |
|-----------|--------|
| Capability **Live** / classical | Heuristic / Telea / Z-order |
| Pack **Needs download** | No weights |
| Pack **Ready** + Apply neural | Pinned quality TFLite bound |

Cheap quality is a product failure — same as a fake Maestro green.
