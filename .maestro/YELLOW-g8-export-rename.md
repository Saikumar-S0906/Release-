# YELLOW — export rename deferred

`export_rename` (“Name this project”) is composed only after a successful export (`successUri != null` in `ExportSheet`). `.maestro/export/success-actions.yaml` stops at `export_sheet_confirm` and leaves the confirm/success steps commented because that path waits on the export worker and can open a system viewer. A Maestro flow that must finish an export before it can tap `export_rename` is not added here.

There is no `project_rename_sheet` tag. Library rename uses `library_rename_field` and `library_rename_confirm` (`.maestro/library/rename-project.yaml`). The overflow row is the string Rename.

`session_save_status` is the session autosave chip. `rename-project.yaml` asserts it once the sample session is open. `.maestro/chrome/compare-hold.yaml` already asserts the same chip. No extra flow.

`empty_library` needs a project list with nothing in it. `launch-to-home.yaml` uses `clearState`. Skipping clear-data on a device that already has projects will not show that plate.

`empty_packs` is the Missing plate in PackChrome. `.maestro/packs/empty-packs.yaml` opens Fill after `clearState` (inpaint Missing). Inpaint is sideload-only, so the plate shows Install from file and does not show Download. It does not claim a bound neural engine. Installing the pack hides the plate.
