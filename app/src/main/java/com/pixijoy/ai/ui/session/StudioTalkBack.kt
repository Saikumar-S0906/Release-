package com.pixijoy.ai.ui.session

/**
 * One TalkBack name for Studio chrome.
 * Unselected controls speak the short label. A selected rail tool speaks
 * "Looks, selected". Pack file vs engine is "Downloaded" or "Ready to apply".
 */
internal fun talkBackName(label: String, selected: Boolean, selectedFormat: String): String =
    if (selected) String.format(selectedFormat, label) else label

internal enum class PackTalkBackKind {
    ReadyToApply,
    Downloaded,
    ComingWithPack,
    Other
}

/**
 * A verified hosted file is "Downloaded". "Ready to apply" only when the engine is bound.
 * E4 / F1 stay "Coming with pack" until that bind. They have no hosted file to call downloaded.
 */
internal fun packTalkBackKind(
    fileReady: Boolean,
    engineReady: Boolean,
    holdPack: Boolean = false
): PackTalkBackKind = when {
    fileReady && engineReady -> PackTalkBackKind.ReadyToApply
    holdPack && !engineReady -> PackTalkBackKind.ComingWithPack
    fileReady -> PackTalkBackKind.Downloaded
    else -> PackTalkBackKind.Other
}

internal fun packTalkBackPhrase(
    kind: PackTalkBackKind,
    downloaded: String,
    readyToApply: String,
    comingWithPack: String
): String? = when (kind) {
    PackTalkBackKind.ReadyToApply -> readyToApply
    PackTalkBackKind.Downloaded -> downloaded
    PackTalkBackKind.ComingWithPack -> comingWithPack
    PackTalkBackKind.Other -> null
}

internal fun cropEdgeTag(bound: CropBound): String = when (bound) {
    CropBound.Left -> "session_crop_edge_left"
    CropBound.Right -> "session_crop_edge_right"
    CropBound.Top -> "session_crop_edge_top"
    CropBound.Bottom -> "session_crop_edge_bottom"
}
