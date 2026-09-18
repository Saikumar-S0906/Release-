package com.pixijoy.photo

/**
 * Export EXIF policy for photo saves (pure Kotlin — unit-testable on JVM).
 *
 * - [keepExif] controls non-GPS tags (orientation, datetime, camera, etc.).
 * - [stripGps] defaults to true and is the product default: GPS is never written
 *   even when the user chooses to keep EXIF. v1 does not expose a GPS toggle.
 *
 * Tag names match androidx.exifinterface.media.ExifInterface constants.
 */
object ExportExifPolicy {
    const val DEFAULT_STRIP_GPS: Boolean = true

    /** Tags we may copy when keepExif is true (GPS excluded by design). */
    val COPY_TAGS: List<String> = listOf(
        "DateTime",
        "DateTimeOriginal",
        "DateTimeDigitized",
        "OffsetTime",
        "OffsetTimeOriginal",
        "OffsetTimeDigitized",
        "Orientation",
        "Make",
        "Model",
        "Software",
        "ImageWidth",
        "ImageLength",
        "XResolution",
        "YResolution",
        "ResolutionUnit",
        "FNumber",
        "ExposureTime",
        "ISOSpeedRatings",
        "FocalLength",
        "WhiteBalance",
        "Flash",
        "ApertureValue",
        "BrightnessValue",
        "ExposureBiasValue",
        "MeteringMode",
        "PhotographicSensitivity"
    )

    val GPS_TAGS: List<String> = listOf(
        "GPSLatitude",
        "GPSLatitudeRef",
        "GPSLongitude",
        "GPSLongitudeRef",
        "GPSAltitude",
        "GPSAltitudeRef",
        "GPSTimeStamp",
        "GPSDateStamp",
        "GPSProcessingMethod",
        "GPSSpeed",
        "GPSSpeedRef",
        "GPSDestBearing",
        "GPSDestBearingRef",
        "GPSDestDistance",
        "GPSDestDistanceRef",
        "GPSDestLatitude",
        "GPSDestLatitudeRef",
        "GPSDestLongitude",
        "GPSDestLongitudeRef",
        "GPSDifferential",
        "GPSDOP",
        "GPSImgDirection",
        "GPSImgDirectionRef",
        "GPSMapDatum",
        "GPSMeasureMode",
        "GPSSatellites",
        "GPSStatus",
        "GPSTrack",
        "GPSTrackRef",
        "GPSVersionID",
        "GPSAreaInformation",
        "GPSHPositioningError"
    )

    fun isGpsTag(tag: String): Boolean =
        tag.startsWith("GPS") || tag in GPS_TAGS

    /**
     * Whether [tag] should be written under the given keep/strip policy.
     */
    fun shouldWriteTag(tag: String, keepExif: Boolean, stripGps: Boolean = DEFAULT_STRIP_GPS): Boolean {
        if (!keepExif) return false
        if (stripGps && isGpsTag(tag)) return false
        return tag in COPY_TAGS
    }
}
