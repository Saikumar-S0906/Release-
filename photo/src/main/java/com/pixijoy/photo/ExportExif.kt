package com.pixijoy.photo

import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface

fun applyExportExif(
    sourcePath: String?,
    destPfd: ParcelFileDescriptor,
    keepExif: Boolean,
    stripGps: Boolean = ExportExifPolicy.DEFAULT_STRIP_GPS
) {
    val dest = ExifInterface(destPfd.fileDescriptor)
    if (keepExif && !sourcePath.isNullOrBlank()) {
        runCatching {
            val src = ExifInterface(sourcePath)
            for (tag in ExportExifPolicy.COPY_TAGS) {
                if (!ExportExifPolicy.shouldWriteTag(tag, keepExif = true, stripGps = stripGps)) continue
                val value = src.getAttribute(tag) ?: continue
                dest.setAttribute(tag, value)
            }
        }
    }
    if (stripGps) {
        for (tag in ExportExifPolicy.GPS_TAGS) {
            dest.setAttribute(tag, null)
        }
    }
    runCatching { dest.saveAttributes() }
}
