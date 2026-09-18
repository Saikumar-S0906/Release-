# PixiJoy — Hands-off operating guide

Saikumar acts as **client**. Day-to-day ownership sits with **ProjectManager** and the team. Do not wait on him for routine decisions. Ask only when a true client approval or product call is required.

Last updated: 2026-09-18 (IST)

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

## Status snapshot (2026-09-18 afternoon IST)

### MET / exited

- **R2** — Heal/Stamp, HD–4K export, collage, export %, rail, brush, sticky Export, Ken Burns encode fix; Maestro launch/heal/collage/export (+ progress); Gallery picker **yellow/manual**
- **Layers first-slice** — seed, Fade/vis/Protect–Erase/Blend, Maestro green
- **Curves** — polish + Maestro green
- **Looks / Lut** — `PhotoOp.Lut` bundled Clean/Warm/Cool/Film/Night/Original; Maestro green
- **Glass scrim** — `session_tool_scrim`
- **Export hotfixes** — abandon pending MediaStore URIs; no `runBlocking` in movie progress
- **Selective HSL** — product + units + review-clear (`PhotoOp.SelectiveHsl` graph `A`); **Maestro exit still open** (Create → Try a sample flake)
- **R3.1 Create** — `home_create` → `create_source_sheet`
- **R3.1 Compare** — press-hold `session_compare` + Saved/Saving (soft: enable only when compare delta exists)

### Live gate

Selective HSL Maestro: Create → `source_sample` / “Try a sample” → Color tool → Apply → Export.

### Parallel open (R3.1)

Best-for hub subtitles / Recommended · Library overflow + delete Snackbar Undo · Export success View/Share/Done + human quality labels.

### Follow-up epic (not a feature gate)

Split Photo/Movie session → ViewModels + tool panels · consistent Hilt in StudioApp · BuildConfig deprecation when Gradle is touched · downscaled live preview on Color drag if janky.

## Docs on `next`

- `docs/NORTH_STAR_ROADMAP.md`
- `docs/R3_LAYER_OP_SHAPES.md`
- `docs/R3_REMAINING.txt`
- `docs/TOTAL_REMAINING.txt`
- `docs/HANDS_OFF.md` (this file)

## When Saikumar returns

ProjectManager gives one consolidated: **done / left / next / any true approval needed**. No need for him to re-steer day-to-day.
