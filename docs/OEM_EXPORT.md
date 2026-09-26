# OEM notes — background export

PixiJoy encodes with **WorkManager** + a low-importance **Export** notification so the user can leave the editor. Strings: `studio_export_background`, `export_leave_safe` (shown on photo + movie export sheets).

## Why OEMs matter

Aggressive battery savers can pause or kill background work. Users may see a stuck “Exporting…” notification or a missing file in Pictures/Movies/PixiJoy.

## Guidance (support / FAQ)

### Samsung (One UI)

1. Settings → Apps → PixiJoy → Battery → **Unrestricted** (or allow background activity).
2. Avoid “Sleeping apps” / “Deep sleeping apps” for PixiJoy during long movie encodes.
3. Keep the export notification visible until it clears.

### Xiaomi / Redmi / POCO (MIUI / HyperOS)

1. Settings → Apps → Manage apps → PixiJoy → Battery saver → **No restrictions**.
2. Enable **Autostart** if offered.
3. Lock PixiJoy in Recents (drag down / padlock) during long exports so the process is less likely to be swept.
4. Disable MIUI “Battery” → “App battery saver” optimizations for PixiJoy.

### General

- Prefer exporting while charging for 4K / long movies.
- If export fails, retry at a lower resolution; keep the screen on until the notification finishes (`studio_export_failed_help`).
- Pack downloads need network once; editing never uploads media.

## Engineering

- Do **not** replace WorkManager with an unbounded foreground-only path without a user-visible notification.
- Channel id: `pixijoy_export` (localized name = Export).
- Request `POST_NOTIFICATIONS` only when the user starts an export (API 33+).
