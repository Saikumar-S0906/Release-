# PixiJoy ML — final model decisions (built for scale)

Decision criteria, in order: 1) license safe for a commercial app at any size, 2) quality measured on
public test sets, 3) phone cost (size, speed), 4) upgrade path (bigger variants, video, iOS) without
changing app code. Numbers come from `docs/MODEL_LAB_REPORT.md`.

| Pack | Decision (ship) | Keep as fallback / tier | Rejected and why |
|------|-----------------|-------------------------|------------------|
| **Depth** | **Depth Anything V2 Small**, int8, 26 MB (Apache-2.0) | Manual Z (no pack) | v1 UNet: 3× worse. DA-V2 Base/Large: CC-BY-NC, can't ship commercially. |
| **Inpaint / Remove** | **MI-GAN 256 Places2**, int8, 7 MB (MIT) | Telea for tiny holes / no pack. Later **HQ pack**: MI-GAN 512 (MIT, ~29 MB) | v1 UNet: broken (grey fill). Big-LaMa: best quality, but 200 MB and ~3.5 s on desktop CPU. |
| **Cutout / Matting** | **MODNet** photographic, int8, 6.8 MB (Apache-2.0) for final render | **MediaPipe selfie** (0.24 MB, ~2 ms) for the live preview | RVM: GPL-3.0, viral license. BiRefNet: too heavy for phones today. |
| **Motion / Flow** | **NeuFlow v2** (Apache-2.0, EPE 1.14 px, built for edge) once converted | OpenCV DIS (classical) now. RAFT-small (BSD-3, 3.8 MB) if the NeuFlow port fails | Farneback: 2× worse than DIS. |

## Why this scales
- **One runtime, one format.** Everything is LiteRT `.tflite` with int8 weights and FLOAT32 in/out, which
  is the frozen `PackModelContract`. New versions are just a new file plus SHA-256, with no Kotlin changes.
  The GPU/NNAPI delegate can be switched on later for speed.
- **Two tiers per tool** (fast preview + quality render): a cheap model while the user drags sliders, and
  the stronger model on Apply/Export. The same pattern extends to video (preview at low resolution,
  render frame by frame).
- **On-demand packs, lean APK.** Packs stay out of the base APK (Release-/Play Asset Delivery,
  `ml-packs-v2`). Total for the three new packs is about 40 MB vs 150 MB today.
- **Model families with headroom.** DA-V2 Small can run at a higher input on flagship phones, and
  MI-GAN has a 512 variant. If iOS comes later, the same models exist for Core ML
  (Apple publishes Core ML Depth Anything V2 Small).
- **License-clean growth.** Ship-path fine-tunes use only commercial-safe data: depth self-distill on COCO
  (CC-BY) plus NYU, and inpaint on COCO. Matting and flow fine-tunes (P3M, Sintel) stay internal.

## Training plan (Kaggle, 30 h/week)
1. Inpaint fine-tune (8 h) → ship only if LPIPS beats 0.076.
2. Depth self-distill (4 h) → ship only if AbsRel beats 0.077.
3. Matting / flow fine-tunes (11 h) → internal evaluation only (license).

## Rollout
1. Upload `depth_dav2s_dyn8`, `inpaint_migan256_dyn8`, `matting_modnet_dyn8` to Release- `ml-packs-v2`.
2. `PackCatalog`: new URLs + SHA-256, `VERSION = "2"`. Add `PackId.Matting`.
3. Device smoke on 1 low-end + 1 flagship Android. Record real latency and peak RAM per pack, then decide
   which tier runs on preview vs export per device class.
