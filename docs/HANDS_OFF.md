# PixiJoy — Hands-off operating guide

Saikumar acts as **client**. Day-to-day ownership sits with **ProjectManager** and the team. Do not wait on him for routine decisions. Ask only when a true client approval or product call is required.

Last updated: 2026-09-23 (IST)

## North star

- World-class Android photo + movie studio; local-first (edits stay on device).
- Quality is non-negotiable. Prefer fewer surfaces done right over rushed checklists.
- Continuous delivery: after a stage quality-exits, roll to the next without approval wait.

## Roles

| Role | Owns |
|------|------|
| ProjectManager | Status cadence, slice exits, lane assignment, swarm on blockers |
| Architect | Op shapes, sequencing, docs (`NORTH_STAR_ROADMAP.md`, `R3_LAYER_OP_SHAPES.md`) |
| UX/UI designer | Self-explain copy/tags, studio chrome rules, R3.1 specs |
| developer (+ peers) | Lands on `origin/next`; feature + UX tracks in parallel |
| Reviwer | In-room clear / blocked; must-clear bugs |
| test_automator | Maestro + units on Pixel_7a; honest green / yellow-manual |

## Landing rule

After **Reviwer** clears in-room → **push straight to `origin/next`**. No PR gate.

Branch: `next` (remote integration). Work lives under `C:\Users\andro\PixiJoy\PixiJoy`.

## Studio chrome rule (all tools)

- Canvas always visible (full-bleed).
- Controls on frosted / scrimmed sheet (`session_tool_scrim`); live preview while adjusting.
- **Apply** bakes the op; sticky **Export** stays opaque outside the glass.
- No opaque per-tool panels.

## Quality exit (slice)

A slice exits when **all** hold:

1. Review-clear (Reviwer)
2. Units green (`:photo` / `:app` as needed)
3. Honest Maestro green on Pixel_7a — or **yellow/manual** with a short doc (no fake greens)

Then ProjectManager **calls the exit** in the group and the team rolls forward.

## Cadence

- ProjectManager posts a **fresh** ~15 min pulse in the project group (what flipped, in progress, who, blockers).
- Never paste the previous pulse. If cadence slips, apologize once and resume.
- Blockers, progress, and concerns stay **in the group** — help each other; swarm, don’t serial-wait.

## Agile rules

- Parallelize: feature track + R3.1 UX can move together.
- Timebox Maestro flakes; yellow/manual with docs beats freezing the whole team.
- Soft nits do not block exits.
- ViewModel / Hilt split and BuildConfig hygiene = **named follow-up epic**, not a gate on features.

## Status snapshot (2026-09-23 IST)

### Group pulse — Phase 0 land / Wave B exit (2026-09-23)

**Flipped:** Wave **B exited**. Landed B2+B3+C1–C3 foundations + B5 docs on `next`.

- **B1 Selective HSL** — Maestro **green** on Pixel_7a (upgraded from yellow/manual).
- **B2 Compare** — live peel + pre-bake; `compare-hold.yaml` **green** Pixel_7a.
- **B3 R3.1 UX** — Library deferred delete+Undo, Export View/Share/Done, Recommended/Best-for, Create sheet, movie glass; Maestro green (create / best-for / library-delete-undo / export sheet).
- **B4** — measure-deferred (`PHASE3_LIVE_PREVIEW_MEASURE.md`); no downscale without jank evidence.
- **C1 LUT import** — wired (`CubeLutParser`, `lut:` keys, Looks Import); picker yellow/manual; chip Maestro green.
- **C2 RAW/DNG** — open + Exif + `preview.jpg` sidecar + RAW badge.
- **C3–C8 Wave C** — Fill undo · Warp · Presets/batch · ColorIntent · Soften (light). See exit note.
- **D1** — `MultiTrackTimeline` composition E2E (preview+export from MT; track mute).
- **D2** — ripple/roll + Soft transition fades + speed ramps; Maestro `d2-transition-speed` **green** Pixel_7a.
- **D3** — keyframed ClipMotion (zoom/pan/drift) via MatrixTransformation; Maestro `d3-keyframed-motion` **green** Pixel_7a.
- **D4** — TimelineTitle layers + OverlayEffect; Maestro `d4-titles-captions` **green** Pixel_7a.
- **D5** — TimelineGrade + bundled looks + scopes meters; Maestro `d5-grade-scopes` **green** Pixel_7a.
**In progress:** D6 audio finish.

**Blockers:** None.

---

### MET / exited

- **R2** — Heal/Stamp, HD–4K export, collage, export %, rail, brush, sticky Export, Ken Burns encode fix; Maestro launch/heal/collage/export (+ progress); Gallery picker **yellow/manual**
- **Layers first-slice** — seed, Fade/vis/Protect–Erase/Blend, Maestro green
- **Curves** — polish + Maestro green
- **Looks / Lut** — `PhotoOp.Lut` bundled Clean/Warm/Cool/Film/Night/Original; Maestro green
- **Glass scrim** — `session_tool_scrim`
- **Export hotfixes** — abandon pending MediaStore URIs; no `runBlocking` in movie progress
- **Selective HSL** — product + units + Maestro **green** Pixel_7a (`PhotoOp.SelectiveHsl` graph `A`)
- **R3.1 Create** — `home_create` → `create_source_sheet` (Maestro green)
- **R3.1 Compare** — live peel + pre-bake; identity gating; Maestro `compare-hold` green
- **R3.1 Library / Export / Recommended** — B3 MET
- **Wave B** — quality gates + R3.1 UX **exited** 2026-09-23
- **C4 Sky / background replace** — `PhotoOp.SkyReplace` graph `H`; heuristic sky/backdrop matte; Maestro green
- **Wave C** — C3 Fill undo/clear · C5 Perspective/Liquify · C6 presets + batch export · C7 export color intent · C8 light Soften (model-heavy → R5)
- **D1 multi-track composition** — MT preview/export parity; track mute; Maestro `multitrack-lanes`
- **D2 ripple/roll + transitions + speed ramps** — `rollCut`, Soft fade pair, `MovieSpeed.expandRamp`; Maestro `d2-transition-speed` green Pixel_7a
- **D3 keyframed transform / motion** — `ClipMotion` keyframes + Media3 `MatrixTransformation`; Maestro `d3-keyframed-motion` green Pixel_7a
- **D4 titles + captions** — `TimelineTitle` layers + `OverlayEffect`; Maestro `d4-titles-captions` green Pixel_7a
- **D5 grade + LUT + scopes** — `TimelineGrade` + `SingleColorLut` + scope meters; Maestro `d5-grade-scopes` green Pixel_7a

### Live gate

**D6** audio finish.

### Follow-up epic (not a feature gate)

Split Photo/Movie session → ViewModels + tool panels · consistent Hilt in StudioApp · BuildConfig deprecation when Gradle is touched · downscaled live preview on Color drag if janky (B4).

## Docs on `next`

- `docs/NORTH_STAR_ROADMAP.md`
- `docs/R3_LAYER_OP_SHAPES.md`
- `docs/R3_REMAINING.txt`
- `docs/TOTAL_REMAINING.txt`
- `docs/REMAINING_MASTER_PLAN.md`
- `docs/HANDS_OFF.md` (this file)

## When Saikumar returns

ProjectManager gives one consolidated: **done / left / next / any true approval needed**. No need for him to re-steer day-to-day.
