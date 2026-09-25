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
    Other
}

/** A verified file is "Downloaded". "Ready to apply" only when the engine is bound. */
internal fun packTalkBackKind(fileReady: Boolean, engineReady: Boolean): PackTalkBackKind = when {
    fileReady && engineReady -> PackTalkBackKind.ReadyToApply
    fileReady -> PackTalkBackKind.Downloaded
    else -> PackTalkBackKind.Other
}

internal fun packTalkBackPhrase(
    kind: PackTalkBackKind,
    downloaded: String,
    readyToApply: String
): String? = when (kind) {
    PackTalkBackKind.ReadyToApply -> readyToApply
    PackTalkBackKind.Downloaded -> downloaded
    PackTalkBackKind.Other -> null
}

internal fun cropEdgeTag(bound: CropBound): String = when (bound) {
    CropBound.Left -> "session_crop_edge_left"
    CropBound.Right -> "session_crop_edge_right"
    CropBound.Top -> "session_crop_edge_top"
    CropBound.Bottom -> "session_crop_edge_bottom"
}
