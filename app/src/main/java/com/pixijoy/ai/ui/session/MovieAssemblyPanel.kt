package com.pixijoy.ai.ui.session

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
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.AssemblyVersion
import com.pixijoy.movie.MarkerKind
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.ProjectBin
import com.pixijoy.movie.TimelineMarker

/** R8 / P2 — markers, bins, named cuts, snap hint. */
@Composable
fun MovieAssemblyPanel(
    multi: MultiTrackTimeline,
    playheadMs: Long,
    mediaCount: Int,
    onMulti: (MultiTrackTimeline) -> Unit,
    onJumpMs: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var versionName by remember { mutableStateOf("") }
    var markerNote by remember { mutableStateOf("") }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StudioHint(stringResource(R.string.studio_assembly_hint), "movie_assembly_hint")

        Text(stringResource(R.string.studio_markers), color = PixiText)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_marker_kinds")
        ) {
            listOf(
                MarkerKind.Beat to R.string.studio_marker_beat,
                MarkerKind.Vfx to R.string.studio_marker_vfx,
                MarkerKind.Music to R.string.studio_marker_music,
                MarkerKind.Hard to R.string.studio_marker_hard
            ).forEach { (kind, labelRes) ->
                val label = stringResource(labelRes)
                PixiPrimaryButton(
                    label,
                    {
                        val mk = TimelineMarker(
                            id = "mk${multi.markers.size}",
                            ms = playheadMs,
                            kind = kind,
                            note = markerNote
                        )
                        onMulti(multi.copy(markers = multi.markers + mk))
                        markerNote = ""
                    },
                    Modifier.testTag("movie_marker_add_${kind.name.lowercase()}"),
                    contentDescription = label
                )
            }
        }
        OutlinedTextField(
            value = markerNote,
            onValueChange = { markerNote = it.take(120) },
            label = { Text(stringResource(R.string.studio_marker_note)) },
            singleLine = true,
            modifier = Modifier.testTag("movie_marker_note")
        )
        multi.markers.sortedBy { it.ms }.forEach { marker ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("movie_marker_${marker.id}")
            ) {
                TextButton(
                    onClick = { onJumpMs(marker.ms) },
                    modifier = Modifier.testTag("movie_marker_jump_${marker.id}")
                ) {
                    Text(
                        "${marker.kind.name} @ ${marker.ms}ms" +
                            if (marker.note.isNotBlank()) " — ${marker.note}" else "",
                        color = PixiText
                    )
                }
                TextButton(
                    onClick = {
                        onMulti(multi.copy(markers = multi.markers.filterNot { it.id == marker.id }))
                    },
                    modifier = Modifier.testTag("movie_marker_del_${marker.id}")
                ) {
                    Text(stringResource(R.string.studio_marker_remove), color = PixiTextMuted)
                }
            }
        }

        Text(stringResource(R.string.studio_bins), color = PixiText)
        val bins = if (multi.bins.isEmpty()) ProjectBin.defaults() else multi.bins
        bins.forEach { bin ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("movie_bin_${bin.id}")
            ) {
                Text(
                    "${bin.name} (${bin.mediaIndices.size})",
                    color = PixiText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val ensured = multi.ensureDefaultBins()
                        val target = ensured.bins.firstOrNull { it.id == bin.id } ?: bin
                        // Add first unused media index as a select (session media list).
                        val nextIdx = (0 until mediaCount).firstOrNull { it !in target.mediaIndices }
                            ?: return@TextButton
                        onMulti(
                            ensured.copy(
                                bins = ensured.bins.map {
                                    if (it.id == bin.id) {
                                        it.copy(mediaIndices = it.mediaIndices + nextIdx)
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    },
                    enabled = mediaCount > 0,
                    modifier = Modifier.testTag("movie_bin_add_${bin.id}")
                ) {
                    Text(stringResource(R.string.studio_bin_add_media), color = PixiText)
                }
            }
        }

        Text(stringResource(R.string.studio_versions), color = PixiText)
        OutlinedTextField(
            value = versionName,
            onValueChange = { versionName = it.take(40) },
            label = { Text(stringResource(R.string.studio_version_name)) },
            singleLine = true,
            modifier = Modifier.testTag("movie_version_name")
        )
        PixiPrimaryButton(
            stringResource(R.string.studio_version_save),
            {
                val name = versionName.ifBlank { "v${multi.assemblies.size + 1}_assembly" }
                onMulti(multi.saveAssembly(name))
                versionName = ""
            },
            Modifier.testTag("movie_version_save")
        )
        multi.assemblies.forEach { ver: AssemblyVersion ->
            val active = ver.id == multi.activeAssemblyId
            TextButton(
                onClick = { onMulti(multi.switchAssembly(ver.id)) },
                modifier = Modifier.testTag("movie_version_${ver.id}")
            ) {
                Text(
                    if (active) "✓ ${ver.name}" else ver.name,
                    color = PixiText
                )
            }
        }

        StudioHint(stringResource(R.string.studio_snap_hint), "movie_snap_hint")
    }
}
