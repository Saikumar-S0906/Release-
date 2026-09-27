"""Shared train-mode presets for Wave M labs.

smoke  = contract / CI / short Colab (NOT for Release- product packs)
quality = real-data product train (ONLY mode that may be pinned)
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class TrainPreset:
    name: str
    epochs: int
    steps: int
    val_steps: int
    patience: int
    require_data_dir: bool
    # Synthetic smoke gate (verify_contract). Quality gate is documented in ML_QUALITY_BAR.md
    max_smoke_mae: float


SMOKE = TrainPreset(
    name="smoke",
    epochs=20,
    steps=250,
    val_steps=40,
    patience=8,
    require_data_dir=False,
    max_smoke_mae=0.18,
)

QUALITY = TrainPreset(
    name="quality",
    # Full product budget — do not shorten for a faster fake green.
    epochs=100,
    steps=1000,
    val_steps=120,
    patience=15,
    require_data_dir=True,
    max_smoke_mae=0.12,
)


def resolve_preset(quality: bool) -> TrainPreset:
    return QUALITY if quality else SMOKE
