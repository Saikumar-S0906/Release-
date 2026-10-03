# PixiJoy - Hands-off operating guide

Saikumar acts as **client**. Day-to-day ownership sits with **ProjectManager** and the team. Do not wait on him for routine decisions. Ask only when a true client approval or product call is required.

Last updated: 2026-10-03 ~19:20 IST

## North star

- World-class Android photo + movie studio; local-first (edits stay on device).
- Quality is non-negotiable. Prefer fewer surfaces done right over rushed checklists.
- Continuous delivery: after a stage quality-exits, roll to the next without approval wait.

## Roles

| Role | Owns |
|------|------|
| ProjectManager | Status cadence, slice exits, lane assignment, swarm on blockers |
| Architect | Op shapes, sequencing, docs (`NORTH_STAR_ROADMAP.md`, `R3_LAYER_OP_SHAPES.md`) |
| UX/UI designer | Self-explain copy/tags, studio chrome rules, R3.1 / polish specs |
| developer (+ peers) | Lands on `origin/next`; feature + UX tracks in parallel |
| Reviwer | In-room clear / blocked; must-clear bugs |
| test_automator | Maestro + units on Pixel_7a; honest green / yellow-manual |

## Landing rule

Path on GitHub: **draft PR → both CI green (`static-and-unit` + `instrumented-smoke`) → undraft → Reviwer CLEAR in-room → developer squash-lands to `origin/next`**. Soft Maestro **YELLOW** (documented) does not block that land when the locked bar is met. No idle once CI is green.

Branch: `next` (remote integration). Local checkout: `C:\Users\andro\PixiJoy\PixiJoy` on DESKTOP-CB6KOBT (prefer local over cloud agents).

## Studio chrome rule (all tools)

- Canvas always visible (full-bleed).
- Controls on frosted / scrimmed sheet (`session_tool_scrim`); live preview while adjusting.
- **Apply** bakes the op; sticky **Export** stays opaque outside the glass.
- No opaque per-tool panels.
- Medium+ two-pane: `tablet_preview_pane` | `tablet_tools_pane` when `isStudioTwoPane`.
- Pack honesty: Download / Installing… / Ready / Failed; never "AI/neural" until `engineReady`; chips gated on `engineReady`.
- Unbound Slow-mo / Track chrome: disabled **"Coming with pack"** (not "Downloaded").

## Quality exit (slice)

A slice exits when **all** hold:

1. Review-clear (Reviwer)
2. Units / CI green as required for that PR
3. Honest Maestro green on Pixel_7a — or **yellow/manual** with a short doc (no fake greens)

Then ProjectManager **calls the exit** in the group and the team rolls forward.

## Cadence

- ProjectManager posts a **fresh** ~15 min pulse in the project group (what flipped, in progress, who, blockers).
- Never paste the previous pulse. If cadence slips, apologize once and resume.
- Blockers, progress, and concerns stay **in the group** — help each other; swarm, don't serial-wait.

## Agile rules

- Parallelize: feature track + UX polish can move together when DESKTOP is free.
- Timebox Maestro flakes; yellow/manual with docs beats freezing the whole team.
- Soft nits do not block exits.
- ViewModel / Hilt / BuildConfig hygiene are **exited** (G2/G3/G9); do not re-open as feature gates.

## Status snapshot (2026-10-03 ~19:20 IST)

### Tip / live train

- **Tip `origin/next`:** `c5ec074` (Merge #33). Landed since the 10-02 snapshot: [#31](https://github.com/Saikumar-S0906/PixiJoy/pull/31) H10 DEX/ZIP honesty `150c959`; [#32](https://github.com/Saikumar-S0906/PixiJoy/pull/32) editor stays open when a slider or rename field closes the keyboard `ec1272a`; [#33](https://github.com/Saikumar-S0906/PixiJoy/pull/33) depth + inpaint sideload bind `c5ec074`. #30 Track-ML stays in history (`d53bae5`).
- **Live gate:** none named. DESKTOP is on `next` @ `c5ec074`. Soft Maestro / P7 / HDRI still not device-green.
- **Soft Maestro:** last Pixel_7a dump was tip `d53bae5` (YELLOW). #32 is the code fix for slider, curves, and rename leaving the editor on IME Back. Device flows were not re-run after #32, so those four stay not-green until a dump off `c5ec074`. Create-sheet label is Movie (`source_video` id unchanged). PASS on the old dump: layers, fill-pack, talkback. YELLOW: glTF + dust. **P7 HOLD**.

### UX locks (still true)

- Glass studio: preview under frosted scrim; Apply bakes; sticky Export outside glass.
- G6/G7 empty + skeleton plates; G8 quiet save / rename tags as shipped.
- Export success: View / Share / Done.
- TalkBack: short verbs; "Looks, selected"; Downloaded vs Ready to apply; crop edges by side. Device TalkBack remains yellow (`.maestro/YELLOW-talkback-studio.md`).
- Pack Ready ≠ neural until `engineReady`.

### Soft polish queue

1. **Compare** — already gated in `PhotoSessionPreview.canCompare` (live delta, or a baked layer plus a snapshot). Not a new slice.
2. **Gallery / create picker** — still yellow-manual; first-session impression. System picker. Do not fake a green.
3. Device TalkBack pass on tip `c5ec074` when DESKTOP is free.
4. Slow-mo / Track pack-sheet final strings only after real pack bind (E4 FILM needs User **go FILM**; F1 Track stays honesty-chrome until engine).

### MET / exited (high level)

- Waves B–D (R3.1 UX, Wave C photo, R4 D1–D11 cinema) exited 2026-09-23+.
- R5 pack runtime + E1–E3 / E5 size bar; E4/F1 engines still HOLD until licensed `.tflite` + URL/SHA (F1 **contract** + #30 Track-ML fixlet MET; seed-only unbound unchanged).
- G1–G9 platform/UX chrome MET (device TalkBack / some Maestro soft yellow).
- H10 size gate MET; honesty (DEX class scan) landed in #31.

### Follow-up (not a feature gate)

- Gallery picker stays yellow-manual (system sheet).
- Re-run slider, curves, rename, and create-sheet on Pixel_7a off tip `c5ec074` before calling those four green.
- B4 downscaled live preview on Color drag only with jank evidence.

## Docs on `next`

- `docs/NORTH_STAR_ROADMAP.md`
- `docs/R3_LAYER_OP_SHAPES.md`
- `docs/TOTAL_REMAINING.txt`
- `docs/REMAINING_MASTER_PLAN.md`
- `docs/HANDS_OFF.md` (this file)
- `docs/R5_ML_PACKS.md`
- `docs/H10_PERF_SIZE_GATE.md`
- `docs/R4_STUDIO_UX.md`
- `docs/FUNCTIONALITY_MATRIX.md`

## When Saikumar returns

ProjectManager gives one consolidated: **done / left / next / any true approval needed**. No need for him to re-steer day-to-day.
