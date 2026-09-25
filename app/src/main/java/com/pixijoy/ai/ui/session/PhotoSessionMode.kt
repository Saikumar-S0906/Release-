package com.pixijoy.ai.ui.session

internal enum class SessionMode {
    Crop,
    Adjust,
    Heal,
    Fill,
    Erase,
    Text,
    Expand,
    Stickers,
    Collage,
    Curves,
    SelectiveHsl,
    Looks,
    Layers,
    Mask,
    Sky,
    Warp,
    Soften,
    SuperRes,
    Denoise,
    Presets
}

internal fun defaultMode(toolKey: String): SessionMode = when (toolKey) {
    "layers" -> SessionMode.Layers
    "restore" -> SessionMode.Heal
    "expand" -> SessionMode.Expand
    "cutout" -> SessionMode.Erase
    "stickers" -> SessionMode.Stickers
    "collage" -> SessionMode.Collage
    else -> SessionMode.Crop
}

internal fun groupForTool(toolKey: String): PhotoGroup = when (toolKey) {
    "restore", "cutout" -> PhotoGroup.Retouch
    "stickers" -> PhotoGroup.Add
    "collage", "layers" -> PhotoGroup.Compose
    "expand" -> PhotoGroup.Crop
    else -> PhotoGroup.Crop
}

internal fun defaultModeFor(group: PhotoGroup, toolKey: String): SessionMode = when (group) {
    PhotoGroup.Crop -> if (toolKey == "expand") SessionMode.Expand else SessionMode.Crop
    PhotoGroup.Light -> SessionMode.Adjust
    PhotoGroup.Color -> SessionMode.Adjust
    PhotoGroup.Retouch -> when (toolKey) {
        "cutout" -> SessionMode.Erase
        else -> SessionMode.Heal
    }
    PhotoGroup.Compose -> when (toolKey) {
        "collage" -> SessionMode.Collage
        else -> SessionMode.Layers
    }
    PhotoGroup.Add -> if (toolKey == "stickers") SessionMode.Stickers else SessionMode.Text
}

internal fun modeBelongs(group: PhotoGroup, mode: SessionMode): Boolean = when (group) {
    PhotoGroup.Crop -> mode == SessionMode.Crop || mode == SessionMode.Expand
    PhotoGroup.Light -> mode == SessionMode.Adjust
    PhotoGroup.Color -> mode == SessionMode.Adjust || mode == SessionMode.Curves ||
        mode == SessionMode.SelectiveHsl || mode == SessionMode.Looks || mode == SessionMode.Presets
    PhotoGroup.Retouch -> mode == SessionMode.Heal || mode == SessionMode.Fill ||
        mode == SessionMode.Erase || mode == SessionMode.Sky || mode == SessionMode.Soften ||
        mode == SessionMode.SuperRes || mode == SessionMode.Denoise
    PhotoGroup.Compose -> mode == SessionMode.Layers || mode == SessionMode.Mask ||
        mode == SessionMode.Warp || mode == SessionMode.Collage
    PhotoGroup.Add -> mode == SessionMode.Text || mode == SessionMode.Stickers
}

internal fun modeUsesSingleFingerToolInput(mode: SessionMode): Boolean = when (mode) {
    SessionMode.Crop,
    SessionMode.Heal,
    SessionMode.Fill,
    SessionMode.Erase,
    SessionMode.Text,
    SessionMode.Stickers,
    SessionMode.Mask -> true
    else -> false
}
