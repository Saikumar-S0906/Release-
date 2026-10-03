# Maestro / Create sheet — green (Pixel_7a `emulator-5556`, 2026-09-25)

`CreateSourceSheet` is a Material3 `ModalBottomSheet` (its own dialog window). The sheet modifier sets `testTagsAsResourceId = true` (same pattern as `ExportSheet` and `ProjectRenameDialog`).

**Maestro:** `.maestro/home/create-sheet.yaml` — **green**. Asserts:

- `create_source_sheet`
- `create_source_sheet_body`
- `source_photo`
- `source_video`
- `source_camera`
- `source_sample`

plus visible labels Photo / Movie / Camera / Try a sample.

The movie button id stays `source_video`. Its label is `studio_movie` ("Movie"), the same word as the Studio segment. A flow that asserts "Video" fails even though the button is on screen.
