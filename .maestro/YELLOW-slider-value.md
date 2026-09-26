# Slider numeric entry — green (Pixel_7a `emulator-5556`, 2026-09-25)

Photo Light, Color, and Curves use `StudioValueSlider`. Short-tap the live value (`session_slider_value`) to edit `session_slider_value_field`. IME Done or leaving the field commits the number, clamped to that slider’s min and max. Text that is not a finite number leaves the previous value. Long-press the live value resets it. Ranges below zero show a sign toggle (`session_slider_value_sign`) because the decimal keyboard often has no minus key. Curves is 0..1 (`curve_slider`) and has no sign toggle; see `YELLOW-curves-value.md` (also greened).

`StudioChromeBar` uses `imePadding()` so the glass lifts above the IME and the field stays visible under edge-to-edge + `adjustResize`.

**Maestro:** `.maestro/chrome/slider-value-entry.yaml` — **green** (Exposure type-in `0.4` + sign chip).
