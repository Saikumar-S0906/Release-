# PixiJoy — features × libraries (photo + movie)

Android 12+ · Kotlin · Jetpack Compose · all work on-device.

Default rule: prefer Google / AndroidX first. Add NDK only when the platform cannot do the job.

---

## App skeleton (both)

| Need | Lib |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Fold / tablet | `androidx.window`, `WindowSizeClass` |
| Lifecycle / process | Lifecycle, ProcessLifecycleOwner |
| Background export / batch | WorkManager + Foreground Service + Notifications |
| Local database (catalog, projects) | Room + SQLite |
| Prefs | DataStore |
| DI | Hilt |
| Coroutines | Kotlin coroutines + Flow |
| Picker | Android Photo Picker, SAF (`DocumentFile`) |
| Permissions | Activity Result APIs |
| Billing | Play Billing Library 7/8 (one-time products) |
| Crash (on device first) | Your own file log; optional Play Vitals |
| Images in UI | Coil 3 |
| Lists | Lazy grids |

Gradle modules to keep the APK small: `:app` `:core-media` `:photo` `:movie` `:ml` `:billing`.

---

## Photo — feature → library

| Feature | Library / API |
|---|---|
| Decode JPEG/PNG/WebP/GIF/BMP | `ImageDecoder`, `BitmapFactory` |
| HEIC / HEIF | Android HEIF (`ImageDecoder`), `androidx.heifwriter` to write |
| AVIF read/write | Device decoder + `AvifWriter` where present; fallback libavif NDK |
| TIFF | `commons-imaging` or small NDK; or restrict TIFF to export via `Bitmap` + own writer |
| EXIF / GPS strip / keep | `androidx.exifinterface` |
| XMP sidecar | Write XML yourself; Adobe XMP toolkit is heavy — prefer a tiny writer |
| Color space / P3 | `ColorSpace`, `Bitmap.Config.RGBA_F16` |
| Ultra HDR / gain map | Android 14+ `Gainmap`, `Bitmap.compress` UltraHDR |
| Crop / rotate / flip / matrix | `RenderEffect`, AGSL, or own GL; `Bitmap` + `Matrix` for simple |
| Perspective / warp | GLES/Vulkan mesh or `ImageProcessor` |
| Exposure, contrast, HSL, curves | Custom AGSL / GLES fragment shaders (own `photo-engine`) |
| Histogram / clipping | Compute shader or CPU on downsample |
| Sharpen / blur / vignette / grain | Same GL engine |
| Lens CA / distortion | GL warp + optional lens profile JSON (own assets) |
| Layers + blend modes | GLES FBO stack or Skia via `android.graphics` SaveLayer |
| Text / frames | Compose + `TextPaint` baked to layer, or Skia |
| Heal / clone / patch | CPU + GL brush; OpenCV Android **optional** (`org.opencv`) for seamless clone |
| RAW / DNG | Adobe DNG SDK **or** LibRaw NDK (`libraw`) + your tone mapper |
| Camera capture | CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-extensions`) |
| Camera2 RAW | CameraX extensions or Camera2 interop |
| Contact sheet / PDF | `PdfDocument` (framework) |
| Batch rename / queue | WorkManager + SAF |
| Soft proof | Color management via `ColorSpace` + ICC is limited; use known profiles (sRGB, P3, AdobeRGB matrix) |

Do **not** use deprecated RenderScript.

---

## Movie — feature → library

| Feature | Library / API |
|---|---|
| Probe duration, codec, size | Media3 `MediaExtractorCompat` / `android.media.MediaMetadataRetriever` + `MediaExtractor` |
| Play + scrub | Media3 ExoPlayer (`media3-exoplayer`) |
| Timeline preview / composition play | Media3 `CompositionPlayer` (Transformer family) |
| Frame step | ExoPlayer seek to previous/next + extractor at sample time; thumbnail cache |
| Filmstrip thumbs | `MediaMetadataRetriever.getFrameAtTime` + disk LRU; or Media3 thumbnailer |
| Proxy file | Media3 Transformer scale-down export to app cache |
| Trim / split / remux (lossless-ish) | Media3 Transformer trim + transmux; `experimentalSetTrimOptimizationEnabled` |
| Re-encode grade / speed / crop | Media3 Transformer + `Effects` (`media3-effect`) |
| Concat clips | Transformer `EditedMediaItemSequence` / `Composition` |
| Audio mix / mute / fade | Transformer audio processors + `DefaultAudioMixer` |
| Extract audio | Extractor + muxer to m4a/wav |
| Speed | `SpeedChangeEffect` / `SpeedChangingMediaSource` |
| Frame-accurate cut UI | Your timeline; snaps to frame using stream fps |
| 4K export | Transformer `TransformationRequest` resolution + encoder bitrate |
| HDR tone map | Media3 HDR effects where device supports |
| Background long render | Foreground service + Transformer listener |
| Fast start MP4 | Transformer muxer flags |
| EDL / markers file | Own writer (CSV / CMX subset) |

Prefer **Media3 over ffmpeg-kit** for Play size, 16 KB page size, and codec policy. Keep **ffmpeg-kit or a tiny NDK ffmpeg** only as escape hatch for odd containers (MKV quirks, PNG sequence). If you add it, isolate in `:optional-ffmpeg` dynamic feature.

---

## Vision / AI packs (photo + movie frames)

| Feature | Library |
|---|---|
| Runtime | **LiteRT** (`com.google.ai.edge.litert`) CompiledModel + GPU / NPU delegates |
| Ready tasks (subject, face, pose) | **MediaPipe Tasks** Vision (image segmenter, face landmarker, pose landmarker, object detector) |
| Vendor NPU | NNAPI, Qualcomm **QNN** / AI Hub exports where available |
| Extra runtimes | ONNX Runtime Mobile only if a model has no LiteRT path |
| Subject / sky / hair | MediaPipe Image Segmenter or your LiteRT mask model (EdgeSAM-class ONNX→LiteRT) |
| Face mesh / catchlight | MediaPipe Face Landmarker |
| Pose / cut-on-action | MediaPipe Pose Landmarker |
| OCR | ML Kit Digital Ink / Text **on-device** model (downloaded) |
| Face cluster | Your embedding model via LiteRT + local ANN (no network) |
| Inpaint / object remove | LiteRT / ncnn port of LaMa or compact diffusion **image-to-image** |
| Upscale 2×/4× | LiteRT or ncnn Real-ESRGAN-class (Qualcomm AI Hub has mobile Real-ESRGAN) |
| Frame interpolate | RIFE-class via ncnn-Vulkan or LiteRT (heavy — flagship) |
| Denoise | MIRNet / NAFNet-class LiteRT |

Models live in on-demand **Play Feature Delivery** or user-initiated download to app storage. Never bake 2 GB into the base APK.

ncnn (`tencent/ncnn`) + Vulkan is a good second path for ESRGAN / RIFE if LiteRT GPU is weak on a device.

---

## Comp / graphics (both)

| Feature | Library |
|---|---|
| Real-time grade on preview | GLES 3 / Vulkan compute, AGSL `RuntimeShader` |
| Keying / track matte | Own GL + mask from MediaPipe |
| 2D / planar track | OpenCV `calcOpticalFlowPyrLK` / ECC **or** a tiny LiteRT tracker |
| Corner pin | GL homography |
| 3D title / puppet | **Filament** |
| AR previz cube | **ARCore** (geometry stays on device; do not enable cloud anchors) |
| Splat viewer | Experimental Vulkan 3DGS (research-grade; flagship only) |
| Scopes | GLES plot of subsampled YUV |

---

## Sound (movie + optional voiceover)

| Feature | Library |
|---|---|
| Low-latency play/record | **Oboe** |
| File decode | Media3 extractors / `MediaCodec` |
| WAV write | Own PCM writer |
| EQ / comp / limiter | Oboe + your DSP or `android.media.audiofx` (limited) |
| Noise reduce | RNNoise NDK or LiteRT audio model |
| LUFS | Own meter (EBU R128 port) or libebur128 NDK |
| USB mic | Android USB audio host when OEM allows |

---

## Enhance (photo stills + video frames)

| Feature | Library |
|---|---|
| Lanczos / bicubic (free) | GL or RenderEffect |
| AI 2×/4× | LiteRT / ncnn ESRGAN |
| 30→60 | RIFE ncnn-Vulkan |
| Slice preview | Transformer clip 5 s → model → player |
| Tile large stills | Process tiles with overlap in engine |

---

## Models / formats on disk (not code libs)

- `.tflite` / LiteRT compiled
- `.onnx` only if ORT ships
- `.bin/.param` ncnn
- `.cube` LUT
- `.xmp` sidecar
- project `.pixijoy.json` + media references
- proxy `*_proxy.mp4` in cache

---

## Suggested Gradle dependencies (core)

```
androidx.media3:media3-exoplayer
androidx.media3:media3-transformer
androidx.media3:media3-effect
androidx.media3:media3-muxer
androidx.camera:camera-core / camera2 / lifecycle / view
androidx.exifinterface:exifinterface
androidx.window:window
androidx.work:work-runtime-ktx
androidx.room:room-ktx
io.coil-kt:coil-compose
com.google.android.material / compose material3
com.android.billingclient:billing-ktx
com.google.ai.edge.litert:litert
com.google.mediapipe:tasks-vision
com.google.ar:core          // only in :spatial
```

NDK optionals (dynamic feature):
`libraw`, `ncnn`, `oboe`, `libebur128`, `ffmpeg` (last resort).

---

## Feature map by engine (short)

**GL/Vulkan engine (you write)**  
Photo sliders, curves, HSL, layers, vignette, grain, heal brush preview, scopes, keyer.

**Media3**  
Open movie, play, trim, remux, concat, speed, export, audio mix, proxies.

**LiteRT + MediaPipe**  
Masks, faces, pose, inpaint, SR, denoise.

**CameraX**  
Capture into the same project.

**Room + SAF**  
Catalog, batch, user files.

**Filament / ARCore**  
Previz only.

---

## Size budget

| Deliverable | Target |
|---|---|
| Base APK | < 40 MB |
| Each AI feature module | 20–200 MB user download |
| Enhance video pack | up to ~400 MB |
| ffmpeg module if any | on-demand, not base |

---

## Do not add

- TensorFlow full (use LiteRT)
- RenderScript
- Cloud ML Kit Vision
- Firebase for the editor path
- Unity as host
- A full FFmpeg in the base APK
