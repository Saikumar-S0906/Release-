# Maestro / Create sheet

`CreateSourceSheet` is a Compose `Dialog`. Hierarchy shows button **text** (Photo / Video / Camera / Try a sample) but **not** `testTag` resource-ids (`create_source_sheet`, `source_sample`, …).

Gate uses text `Try a sample` until Dialog tags are merged into a11y (`testTagsAsResourceId = true` on dialog content or semantics merge).
