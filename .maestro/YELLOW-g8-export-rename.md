# YELLOW — export rename deferred

`export_rename` (“Name this project”) is composed only after a successful export (`successUri != null` in `ExportSheet`). `.maestro/export/success-actions.yaml` stops at `export_sheet_confirm` and leaves the confirm/success steps commented because that path waits on the export worker and can open a system viewer. A Maestro flow that must finish an export before it can tap `export_rename` is not added here.

`ProjectRenameDialog` is an `AlertDialog` (its own window). `MainActivity`'s flag does not cross into that window. The dialog modifier sets `testTagsAsResourceId = true` (same pattern as `ExportSheet` and `CreateSourceSheet`) and the root tag `project_rename_sheet`. TalkBack labels stay the visible strings; the flag is not an accessibility announcement.

Tags that should surface as resource-ids once a device dump confirms them:

- `project_rename_sheet`
- `library_rename_field` (title field)
- `library_rename_confirm`
- `library_rename_dismiss`

No hierarchy dump was taken for this change. `.maestro/library/rename-project.yaml` still opens the dialog from the overflow string Rename (`DropdownMenu` is a separate popup; `library_action_rename` is not asserted as an id) and still asserts only `library_rename_field` and `library_rename_confirm`. `project_rename_sheet` and `library_rename_dismiss` stay commented until a dump shows those resource-ids. Soft-emu stays yellow.

`session_save_status` is the session autosave chip. `rename-project.yaml` asserts it once the sample session is open. `.maestro/chrome/compare-hold.yaml` already asserts the same chip. No extra flow.

`empty_library` needs a project list with nothing in it. `launch-to-home.yaml` uses `clearState`. Skipping clear-data on a device that already has projects will not show that plate.

`empty_packs` is the Missing plate in PackChrome. `.maestro/packs/empty-packs.yaml` opens Fill after `clearState` (inpaint Missing). Inpaint is sideload-only, so the plate shows Install from file and does not show Download. It does not claim a bound neural engine. Installing the pack hides the plate.
