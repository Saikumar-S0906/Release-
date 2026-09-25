# Slider numeric entry — yellow

Photo Light, Color, and Curves use `StudioValueSlider`. Short-tap the live value (`session_slider_value`) to edit `session_slider_value_field`. IME Done or leaving the field commits the number, clamped to that slider’s min and max. Text that is not a finite number leaves the previous value. Long-press the live value resets it. Ranges below zero show a sign toggle (`session_slider_value_sign`) because the decimal keyboard often has no minus key. Curves is 0..1 (`curve_slider`) and has no sign toggle; see `YELLOW-curves-value.md`.

Soft-emu Maestro was not run for this change. This note is not a green. No flow asserts these ids yet.
