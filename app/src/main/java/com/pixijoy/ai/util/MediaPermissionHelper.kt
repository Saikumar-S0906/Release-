package com.pixijoy.ai.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object MediaPermissionHelper {

    fun galleryReadPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasGalleryReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, galleryReadPermission()) ==
            PackageManager.PERMISSION_GRANTED

    fun speechPermission(): String = Manifest.permission.RECORD_AUDIO

    fun hasSpeechPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, speechPermission()) ==
            PackageManager.PERMISSION_GRANTED

    fun cameraPermission(): String = Manifest.permission.CAMERA

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, cameraPermission()) ==
            PackageManager.PERMISSION_GRANTED
}
