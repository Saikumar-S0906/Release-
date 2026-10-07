# Pixi Joy – Better On-Device Models for Inpainting, Depth, Flow & Matting

Pixi Joy is a mobile creative app where every AI effect runs on the phone and nothing gets uploaded. This project checks how good the current models are, swaps in or fine-tunes stronger ones using public datasets, and rebuilds the app around them, aiming at VFX and CGI-style editing.

## Who it's for
- Mobile creators and hobby VFX artists who want object removal, depth effects, background cutouts and motion effects without cloud uploads.
- The Pixi Joy owner, who needs a clear, measured answer to "are the models good, and can they be made better?"

## An honest answer on "can you train these models well?"
- **Full training from scratch is not realistic here.** Inpainting, depth, optical-flow and matting models need days of GPU time and very large datasets. This build environment has no GPU and cannot run long jobs.
- **What works instead:**
  1. **Benchmark** the current models from the `next` branch and Releases on public test sets, so quality, speed and size are measured, not guessed.
  2. **Upgrade** to stronger open-source models that are small enough for phones, where they beat the current ones (for example: Depth Anything V2 Small for depth, MI-GAN/LaMa-style models for inpainting, NeuFlow/RAFT-small for flow, MODNet/RVM for matting).
  3. **Fine-tune** with a ready-to-run training package (Google Colab or any GPU machine) on public datasets. The user runs it, and the resulting models drop back into the app.
- All models stay on the device. No cloud AI calls get added.

## Core features and experience
- **Model Lab report:** for each model, current vs. upgraded quality score, speed on phone, and file size, with before/after sample images.
- **Inpaint / Remove:** brush over an object and the phone fills the area in.
- **Depth:** depth map from one photo, used for portrait blur, fog/atmosphere, and a 3D parallax "pop" effect.
- **Matting / Cutout:** hair-level background removal, then swap or blur the background.
- **Flow (motion):** motion maps between video frames, used for motion trails and smooth slow-motion previews.
- **Privacy badge:** a visible "Processed on device" indicator on every result.
- **Export:** save to Photos, plus depth map / alpha matte as separate files for use in other VFX tools.

## User flow
1. Open the app, pick a photo or short clip from the library (or take one with the camera).
2. Choose a tool: Remove, Depth, Cutout, or Motion.
3. Adjust with simple controls (brush size, blur strength, parallax amount) and see a live preview.
4. Compare before and after with a slider, then export.

## UI/UX feel
- Dark, pro-editor look, close to a compact mobile compositing app: the image fills the screen and the tools sit in a bottom tray.
- Big touch targets, smooth brushing, fast previews, and clear progress while a model runs.

## Implementation phases
**Phase 1 – MVP (built now)**
- Connect the private repo (via GitHub access) and review the existing code and models.
- Benchmark the current models and the candidate upgrades for inpaint, depth, matting and flow on public test sets, then write the Model Lab report.
- Convert the winning models to phone formats (Core ML / TFLite / ONNX) and shrink them (quantization) to keep size and speed in check.
- Rebuild the photo tools (Remove, Depth, Cutout) around the chosen models. All processing stays on device.

**Phase 2 – Fine-tuning & video**
- Training package with Colab notebooks for each model on public datasets (Places2 for inpainting, NYU-Depth/Hypersim for depth, Sintel/FlyingChairs for flow, P3M/AM-2k for matting), plus scripts that export results straight into the app.
- Motion tool and video versions of Cutout and Depth (frame-by-frame with temporal smoothing).

**Phase 3 – VFX & CGI**
- Depth-aware compositing: place 3D objects or text behind or in front of people, relighting, fog.
- Simple camera-motion tracking, multi-layer timeline, and exports for desktop tools (EXR/PNG sequences).

## Assumptions
- The user will give access to the private Pixi Joy repo (the `next` branch and its Releases). The plan gets adjusted once the actual code and models have been reviewed.
- Phase 1 focuses on inpaint, depth, matting and flow, as stated. The existing text/chat and audio models are kept as they are, and are only benchmarked if time allows.
- Only public, commercially permissive datasets and model licenses are used. Any license limits get flagged in the report.
- Real fine-tuning needs a GPU that the user provides (for example Colab). The project supplies the scripts and instructions, not the GPU hours.
- On-device ML needs a native app build. It will not run in Expo Go or the web preview, so testing happens on a development build created through Publish.
- The app keeps working fully offline. The app's backend is only used for optional things like saving project settings, never for running AI.
