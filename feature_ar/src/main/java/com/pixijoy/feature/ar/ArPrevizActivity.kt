package com.pixijoy.feature.ar

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException

/**
 * F4 AR previz — plane detection + title card marker. Lives only in `:feature_ar`.
 */
class ArPrevizActivity : Activity() {
    private var session: Session? = null
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title_text") ?: "PixiJoy"
        val root = FrameLayout(this)
        status = TextView(this).apply {
            text = "AR previz: $title — looking for a plane…"
            textSize = 16f
            setPadding(48, 48, 48, 48)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x88000000.toInt())
        }
        root.addView(
            status,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(root)
        try {
            when (ArCoreApk.getInstance().requestInstall(this, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return
                ArCoreApk.InstallStatus.INSTALLED -> Unit
            }
            val s = Session(this)
            s.configure(
                Config(s).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
            )
            session = s
            status.text = "AR session ready — point at a surface to place \"$title\""
        } catch (e: UnavailableException) {
            Toast.makeText(this, "ARCore unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "AR failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            session?.resume()
            // Lightweight tracking probe without full GL surface for this slice.
            session?.update()?.let { frame ->
                val planes = frame.getUpdatedTrackables(com.google.ar.core.Plane::class.java)
                val tracked = planes.any { it.trackingState == TrackingState.TRACKING }
                if (tracked) {
                    status.text = "Plane locked — title marker placed for previz"
                }
            }
        }
    }

    override fun onPause() {
        runCatching { session?.pause() }
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { session?.close() }
        session = null
        super.onDestroy()
    }
}
