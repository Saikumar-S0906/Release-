# Maestro / Create sheet — still yellow

`CreateSourceSheet` is a Material3 `ModalBottomSheet` (its own dialog window). Hierarchy used to show button **text** (Photo / Video / Camera / Try a sample) and not `testTag` resource-ids.

The sheet modifier now sets `testTagsAsResourceId = true` (same pattern as `ExportSheet` and `ProjectRenameDialog`). `MainActivity`'s flag does not cross into this window. TalkBack labels stay the visible strings; the flag is not an accessibility announcement.

Tags that should surface as resource-ids once a device dump confirms them:

- `create_source_sheet`
- `create_source_sheet_body`
- `source_photo`
- `source_files`
- `source_video`
- `source_camera`
- `source_sample`

No hierarchy dump was taken for this change. `.maestro/home/create-sheet.yaml` still gates on visible text (`Try a sample`, Photo, Video, Camera). Leave the `id:` asserts commented until a dump shows those resource-ids. Soft-emu stays yellow.
