# Curves numeric entry — green (Pixel_7a `emulator-5556`, 2026-09-25)

Curves uses the same `StudioValueSlider` as Photo Light and Color. The sheet stays a hint, the five point chips, one slider, and Apply.

Short-tap the live value (`session_slider_value`) to edit `session_slider_value_field`. IME Done or leaving the field commits the number, clamped to 0..1. Text that is not a finite number leaves the previous point. Long-press resets that point to the identity diagonal (0, 0.25, 0.5, 0.75, 1). The range does not go below zero, so there is no sign toggle. The slider tag stays `curve_slider`.

**Maestro:** `.maestro/curves/curves-apply.yaml` — tap mid point, type `0.55`, scroll to Apply, assert `session_export`.

2026-10-02 Pixel_7a tip `d53bae5`: **FAIL** on `session_apply_curves` after Enter. Same IME-dismiss Back as the Light slider (the session had already closed). Covered by the slider commit guard. Device flow not re-run — do not mark green from the unit test alone.
