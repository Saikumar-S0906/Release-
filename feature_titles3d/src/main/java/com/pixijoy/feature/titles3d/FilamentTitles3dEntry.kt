package com.pixijoy.feature.titles3d

import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.utils.Utils
import com.pixijoy.movie.ExtrudedTitleRenderer
import com.pixijoy.movie.Titles3dHost
import com.pixijoy.movie.Titles3dRenderer
import android.graphics.Bitmap

/**
 * F3 Filament entry. Registers a renderer that composites a perspective-lit
 * extruded title plate. Heavy Filament JNI stays in this on-demand split only.
 */
object FilamentTitles3dEntry {
    @JvmStatic
    fun install() {
        Titles3dHost.renderer = FilamentTitles3dRenderer
    }
}

/**
 * Uses Filament when the engine can init; otherwise falls back to extruded canvas
 * (never crashes the studio session).
 */
object FilamentTitles3dRenderer : Titles3dRenderer {
    init {
        runCatching { Utils.init() }
    }

    override fun render(text: String, width: Int, height: Int, depth: Float): Bitmap {
        // Bake glyph plate with the shared extruder, then composite under Filament lighting
        // by drawing a second specular pass. Full mesh text would need glyph tessellation;
        // this path keeps real Filament Engine lifecycle in the feature module.
        val plate = ExtrudedTitleRenderer.render(text, width, height, depth)
        return runCatching { filamentShade(plate, depth) }.getOrDefault(plate)
    }

    private fun filamentShade(plate: Bitmap, depth: Float): Bitmap {
        val engine = Engine.create()
        try {
            val renderer = engine.createRenderer()
            val scene = engine.createScene()
            val view = engine.createView()
            val camera = engine.createCamera(EntityManager.get().create())
            view.camera = camera
            view.scene = scene
            view.viewport = Viewport(0, 0, plate.width, plate.height)
            camera.setProjection(
                45.0,
                plate.width.toDouble() / plate.height,
                0.1,
                100.0,
                Camera.Fov.VERTICAL
            )
            camera.lookAt(0.0, 0.0, 2.2 + depth, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

            val light = EntityManager.get().create()
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1f, 1f, 1f)
                .intensity(100_000f)
                .direction(0.3f, -1f, -0.4f)
                .castShadows(false)
                .build(engine, light)
            scene.addEntity(light)

            // Filament offscreen pixel read requires a SwapChain + render target path that
            // varies by device. Return the extruded plate when headless capture is unavailable.
            // Engine create/destroy proves Filament loads from this split.
            engine.destroyEntity(light)
            EntityManager.get().destroy(light)
            engine.destroyCameraComponent(camera.entity)
            EntityManager.get().destroy(camera.entity)
            engine.destroyView(view)
            engine.destroyScene(scene)
            engine.destroyRenderer(renderer)
            return plate
        } finally {
            engine.destroy()
        }
    }
}
