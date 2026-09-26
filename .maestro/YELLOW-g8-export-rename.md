# G8 rename / empty library — green (Pixel_7a `emulator-5556`, 2026-09-25)

**Library rename:** `.maestro/library/rename-project.yaml` — **green**. Opens overflow via text `Rename` (`DropdownMenu` is a separate popup). Dialog asserts `project_rename_sheet`, `library_rename_field`, `library_rename_dismiss`, confirm → title `RenamedSample`. `session_save_status` asserted once the sample session is open.

**Empty library:** `.maestro/library/empty-library.yaml` — **green** after `clearState` (no projects → empty plate).

**Still deferred / manual-ish:** `export_rename` (“Name this project”) only after a successful export (`successUri != null`). `.maestro/export/success-actions.yaml` still stops before finishing export + system viewer. No Maestro pass claims that path.

`empty_packs` Missing plate stays covered by `.maestro/packs/empty-packs.yaml` (pack suite green separately).
