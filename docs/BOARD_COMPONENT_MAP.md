# Board ↔ Design Bible component map (Phase 1–2)

Source: [PixiJoy UI-UX Design Bible.pdf](../PixiJoy%20UI-UX%20Design%20Bible.pdf) §57.

| Bible §57 | Board kit | Notes |
|-----------|-----------|-------|
| PixiButton (primary) | `BoardPrimaryButton` | `aiAccent=true` → purple→pink gradient (AI only) |
| PixiButton (secondary) | `BoardSecondaryButton` | Surface muted |
| PixiButton (tertiary) | `BoardTertiaryButton` | Text-only |
| PixiButton (destructive) | `BoardDestructiveButton` | Danger fill |
| PixiIconButton | IconButton ad-hoc | Prefer `pj_ic_*` |
| PixiTextField | ad-hoc | Later |
| PixiCard | ad-hoc Material | Later |
| PixiChip | `BoardCreditChip` | Sparkle + muted surface (dark) |
| PixiTopBar | `BoardScreenHeader` | |
| PixiBottomBar / Nav | `BoardMainShell` / `MainScaffold` | `pj_ic_home` / `add` / `explore` / `folder` |
| PixiBottomSheet | ad-hoc | Use `BoardRadii.sheet` (28) |
| PixiDialog | ad-hoc | |
| PixiSnackbar | rare | |
| PixiLoading | `BoardLoadingIndicator` / `BoardProgressBlock` | Progress on `processingSurface` + sparkles |
| PixiSkeleton | `BoardSkeleton` / `BoardSkeletonCard` | Wire collections Phase 3 |
| PixiEmptyState | `BoardEmptyState` | Wire collections Phase 3 |
| PixiErrorState | `BoardErrorState` | Human copy + Retry |
| PixiImage | `BoardHeroImage` / `BoardArtImage` | |
| PixiAvatar | Flow9 ad-hoc | |
| PixiProgress | `BoardProgressTrack` / `BoardSoftPulse` | |
| AI sparkle | `BoardAiSparkleIcon` | Talk / credits / AI tools only |
| Soft edges | `BoardSoftEdgeGate` | Parity Phase 3 |

**Button states (Bible §40):** default / pressed (`BoardPressable`) / disabled (alpha + muted) / loading (caller shows `BoardLoadingIndicator` or disables CTA). Focused/selected for chips in later phases.
