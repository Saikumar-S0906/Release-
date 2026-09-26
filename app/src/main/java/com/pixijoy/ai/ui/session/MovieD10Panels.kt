package com.pixijoy.ai.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiAccentSoft
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.movie.ClipSlowMo
import com.pixijoy.ml.pack.PackId
import com.pixijoy.movie.SlowMoEngine
import com.pixijoy.movie.SlowMoPreset
import com.pixijoy.movie.TimelineClip

/** D10 slow-mo / frame-interp contract — outside [MovieSessionScreen] for DEX size. */
@Composable
fun MovieSlowMoPanel(
    selected: TimelineClip?,
    onSlowMo: (ClipSlowMo) -> Unit
) {
    val slowMo = selected?.slowMo ?: ClipSlowMo.None
    val resolved = slowMo.resolvedEngine()
    val mlReady = rememberPackEngineReady(PackId.FrameInterp)
    val neural = frameInterpNeuralChip(mlReady)
    val frameHold = frameHoldChip()
    val coming = stringResource(R.string.studio_hold_coming)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(
                if (mlReady) R.string.studio_movie_d10_hint_ready
                else R.string.studio_movie_d10_hint
            ),
            color = PixiTextMuted,
            modifier = Modifier.testTag("movie_d10_hint")
        )
        Text(stringResource(R.string.studio_slow_mo), color = PixiTextMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .testTag("movie_slowmo_presets")
        ) {
            SlowChip(
                label = stringResource(R.string.studio_slowmo_off),
                selected = slowMo.matchesPreset(SlowMoPreset.Off),
                tag = "movie_slowmo_off"
            ) { onSlowMo(SlowMoPreset.Off.toSlowMo()) }
            SlowChip(
                label = stringResource(R.string.studio_slowmo_half),
                selected = slowMo.matchesPreset(SlowMoPreset.Half),
                tag = "movie_slowmo_half"
            ) { onSlowMo(SlowMoPreset.Half.toSlowMo()) }
            SlowChip(
                label = stringResource(R.string.studio_slowmo_quarter),
                selected = slowMo.matchesPreset(SlowMoPreset.Quarter),
                tag = "movie_slowmo_quarter"
            ) { onSlowMo(SlowMoPreset.Quarter.toSlowMo()) }
            SlowChip(
                label = stringResource(R.string.studio_slowmo_eighth),
                selected = slowMo.matchesPreset(SlowMoPreset.Eighth),
                tag = "movie_slowmo_eighth"
            ) { onSlowMo(SlowMoPreset.Eighth.toSlowMo()) }
        }
        if (slowMo.isActive) {
            Text(stringResource(R.string.studio_slowmo_engine), color = PixiTextMuted)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .testTag("movie_slowmo_engines")
            ) {
                SlowChip(
                    label = stringResource(R.string.studio_slowmo_heuristic),
                    selected = resolved == SlowMoEngine.Heuristic,
                    tag = "movie_slowmo_heuristic",
                    enabled = frameHold.enabled
                ) { onSlowMo(slowMo.withEngine(SlowMoEngine.Heuristic)) }
                SlowChip(
                    label = if (neural is HoldControl.NeuralFrames) {
                        stringResource(R.string.studio_slowmo_ml)
                    } else {
                        coming
                    },
                    selected = resolved == SlowMoEngine.Ml && neural is HoldControl.NeuralFrames,
                    tag = "movie_slowmo_ml",
                    enabled = neural.enabled,
                    lockedPhrase = if (neural is HoldControl.ComingWithPack) coming else null
                ) { onSlowMo(slowMo.withEngine(SlowMoEngine.Ml)) }
            }
            Text(
                stringResource(
                    R.string.studio_slowmo_resolved,
                    when (
                        slowMoExportChip(
                            neural,
                            resolvedMl = resolved == SlowMoEngine.Ml
                        )
                    ) {
                        HoldControl.NeuralFrames -> stringResource(R.string.studio_slowmo_ml)
                        else -> stringResource(R.string.studio_slowmo_heuristic)
                    }
                ),
                color = PixiTextMuted,
                modifier = Modifier.testTag("movie_slowmo_resolved")
            )
            if (!mlReady) {
                PackStatusRow(
                    packId = PackId.FrameInterp,
                    statusTag = "movie_slowmo_pack_status",
                    downloadTag = "movie_slowmo_pack_download"
                )
            }
        }
    }
}

@Composable
private fun SlowChip(
    label: String,
    selected: Boolean,
    tag: String,
    enabled: Boolean = true,
    lockedPhrase: String? = null,
    onClick: () -> Unit
) {
    Text(
        label,
        color = if (enabled) PixiText else PixiTextMuted,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccentSoft else Color.White.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                if (lockedPhrase != null) contentDescription = lockedPhrase
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}
