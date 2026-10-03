package com.pixijoy.studio.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.DeliverablePreset
import com.pixijoy.movie.DeliveryTc
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.ReviewNote
import java.io.File

/** R9 / P6 — deliverable presets, review notes, TC burn-in, handoff honesty. */
@Composable
fun MovieDeliverPanel(
    multi: MultiTrackTimeline,
    playheadMs: Long,
    projectDir: File,
    onMulti: (MultiTrackTimeline) -> Unit,
    onWriteHandoff: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteDraft by remember { mutableStateOf("") }
    val delivery = multi.delivery.normalized()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StudioHint(stringResource(R.string.studio_deliver_hint), "movie_deliver_hint")
        Text(
            stringResource(R.string.studio_deliver_no_watermark),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_deliver_no_watermark")
        )

        Text(stringResource(R.string.studio_deliver_preset), color = PixiText)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_deliver_presets")
        ) {
            listOf(
                DeliverablePreset.Social to R.string.studio_deliver_social,
                DeliverablePreset.Review to R.string.studio_deliver_review,
                DeliverablePreset.Master to R.string.studio_deliver_master
            ).forEach { (preset, labelRes) ->
                val selected = delivery.preset == preset
                TextButton(
                    onClick = { onMulti(preset.apply(multi)) },
                    modifier = Modifier.testTag("movie_deliver_${preset.name.lowercase()}")
                ) {
                    Text(
                        stringResource(labelRes) + if (selected) " ✓" else "",
                        color = PixiText
                    )
                }
            }
        }
        Text(
            stringResource(
                R.string.studio_deliver_preset_summary,
                com.pixijoy.movie.MovieExportSize.label(
                    com.pixijoy.movie.MovieExportSize.normalize(multi.outputHeight)
                ),
                multi.hdrMode.name
            ),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_deliver_preset_summary")
        )

        TextButton(
            onClick = {
                onMulti(
                    multi.copy(
                        delivery = delivery.copy(burnInTc = !delivery.burnInTc)
                    )
                )
            },
            modifier = Modifier.testTag("movie_deliver_burn_tc")
        ) {
            Text(
                if (delivery.burnInTc) {
                    stringResource(R.string.studio_deliver_tc_on)
                } else {
                    stringResource(R.string.studio_deliver_tc_off)
                },
                color = PixiText
            )
        }

        Text(stringResource(R.string.studio_deliver_notes), color = PixiText)
        OutlinedTextField(
            value = noteDraft,
            onValueChange = { noteDraft = it.take(400) },
            label = { Text(stringResource(R.string.studio_deliver_note_hint)) },
            singleLine = false,
            modifier = Modifier.testTag("movie_deliver_note_field")
        )
        TextButton(
            onClick = {
                val text = noteDraft.trim()
                if (text.isEmpty()) return@TextButton
                val note = ReviewNote(
                    id = "rn${delivery.notes.size}",
                    ms = playheadMs,
                    text = text
                )
                onMulti(
                    multi.copy(delivery = delivery.copy(notes = delivery.notes + note))
                )
                noteDraft = ""
            },
            modifier = Modifier.testTag("movie_deliver_note_add")
        ) {
            Text(stringResource(R.string.studio_deliver_note_add), color = PixiText)
        }
        delivery.notes.forEach { note ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("movie_deliver_note_${note.id}")
            ) {
                Text(
                    "${DeliveryTc.format(note.ms)} · ${note.text}",
                    color = PixiText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        onMulti(
                            multi.copy(
                                delivery = delivery.copy(
                                    notes = delivery.notes.filterNot { it.id == note.id }
                                )
                            )
                        )
                    },
                    modifier = Modifier.testTag("movie_deliver_note_del_${note.id}")
                ) {
                    Text(stringResource(R.string.studio_deliver_note_remove), color = PixiTextMuted)
                }
            }
        }

        TextButton(
            onClick = onWriteHandoff,
            modifier = Modifier.testTag("movie_deliver_write_handoff")
        ) {
            Text(stringResource(R.string.studio_deliver_write_handoff), color = PixiText)
        }
        Text(
            stringResource(
                R.string.studio_deliver_handoff_path,
                File(projectDir, "handoff").absolutePath
            ),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_deliver_handoff_path")
        )
        Text(
            stringResource(R.string.studio_deliver_gaps),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_deliver_gaps")
        )
    }
}
