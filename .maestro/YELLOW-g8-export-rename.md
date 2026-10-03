# G8 rename / empty library / export success — green (Pixel_7a `emulator-5556`, 2026-09-26)

**Library rename:** `.maestro/library/rename-project.yaml`. Opens overflow via text `Rename`. Dialog asserts `project_rename_sheet`, `library_rename_field`, `library_rename_dismiss`, confirm → title `RenamedSample`. `session_save_status` asserted once the sample session is open.

2026-10-02 Pixel_7a tip `d53bae5`: **FAIL**. `hideKeyboard` sends Back. The dialog treated that as dismiss, so `library_rename_confirm` was gone and Library was showing with no dialog. Back while the field is focused now clears the field and leaves the dialog up. A later Back dismisses. Device flow not re-run — do not mark green from the unit test alone.

**Empty library:** `.maestro/library/empty-library.yaml` — **green** after `clearState`.

**Export success + rename affordance:** `.maestro/export/success-actions.yaml` — **green**. Confirm sheet → wait for `export_done` / `export_view` / `export_share` / `export_rename`.

`empty_packs` Missing plate stays covered by `.maestro/packs/empty-packs.yaml` (pack suite green separately).
