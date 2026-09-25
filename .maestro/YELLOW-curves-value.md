# Curves numeric entry — yellow

Curves uses the same `StudioValueSlider` as Photo Light and Color. The sheet stays a hint, the five point chips, one slider, and Apply.

Short-tap the live value (`session_slider_value`) to edit `session_slider_value_field`. IME Done or leaving the field commits the number, clamped to 0..1. Text that is not a finite number leaves the previous point. Long-press resets that point to the identity diagonal (0, 0.25, 0.5, 0.75, 1). The range does not go below zero, so there is no sign toggle. The slider tag stays `curve_slider`.

Soft-emu Maestro was not run for this change. This note is not a green. `.maestro/curves/curves-apply.yaml` does not tap the live value.
