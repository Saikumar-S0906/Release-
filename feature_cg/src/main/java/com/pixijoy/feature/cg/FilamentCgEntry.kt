package com.pixijoy.feature.cg

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.SwapChainFlags
import com.google.android.filament.Texture
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.pixijoy.movie.CgHost
import com.pixijoy.movie.CgPlateRequest
import com.pixijoy.movie.CgPreset
import com.pixijoy.movie.ClassicalCgRenderer
import com.pixijoy.movie.FilamentPropCgRenderer
import com.pixijoy.movie.GLTF_MAX_BYTES
import com.pixijoy.movie.cgFilamentCameraDistance
import com.pixijoy.movie.cgFilamentPropTransform
import com.pixijoy.movie.cgPlateOffset
import com.pixijoy.movie.cgPropResourceFile
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * On-demand Filament entry for PropGltf. Filament JNI is [compileOnly] here and
 * ships in on-demand `:feature_titles3d` — never in the base APK. [install] binds
 * [CgHost] only after a headless clear can be read back. Otherwise the classical
 * raster stays.
 */
object FilamentCgEntry {
    @JvmStatic
    fun install() {
        if (CgHost.filamentAvailable()) return
        if (FilamentPropSession.probe()) {
            CgHost.renderer = FilamentPropRenderer
        }
    }
}

/**
 * PropGltf plate. Directional key + procedural IBL (SH + tiny reflections cubemap).
 * No HDRI asset in this split — Release- HDRI is a later cut. Any miss returns the
 * classical plate.
 */
object FilamentPropRenderer : FilamentPropCgRenderer {
    override fun renderPlate(request: CgPlateRequest): Bitmap {
        if (request.preset != CgPreset.PropGltf || request.propFile == null) {
            return ClassicalCgRenderer.renderPlate(request)
        }
        return FilamentPropSession.renderProp(request) ?: ClassicalCgRenderer.renderPlate(request)
    }
}

private const val CLEAR_SIZE = 4
private const val READBACK_TIMEOUT_SEC = 2L
private const val MIN_VISIBLE_ALPHA = 8
private const val MAX_EDGE = 4096
private const val DIRECTIONAL_INTENSITY = 3.2f
/** Soft studio IBL intensity, matched to the low directional key on this plate. */
private const val IBL_INTENSITY = 2.4f
private const val NEAR_PLANE = 0.05
private const val FAR_PLANE = 50.0

/**
 * 3-band radiance SH for a soft studio fill (9 float3 = 27 floats).
 * Procedural — no KTX/HDRI in the feature APK. HDRI from Release- comes later.
 */
private val STUDIO_IBL_RADIANCE_SH = floatArrayOf(
    // L00
    1.00f, 0.98f, 0.95f,
    // L1-1, L10, L11
    0.02f, 0.02f, 0.03f,
    0.35f, 0.38f, 0.42f,
    0.05f, 0.04f, 0.03f,
    // L2-2 .. L22
    0.02f, 0.02f, 0.02f,
    -0.04f, -0.04f, -0.03f,
    0.08f, 0.08f, 0.09f,
    -0.04f, -0.03f, -0.03f,
    0.01f, 0.01f, 0.01f
)

private val filamentPropGpu = FilamentPropGpu()

internal object FilamentPropSession {
    fun probe(): Boolean = filamentPropGpu.probe()

    fun renderProp(request: CgPlateRequest): Bitmap? = filamentPropGpu.renderProp(request)
}

private class FilamentPropGpu {
    private val lock = Any()
    private var ready = false
    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var view: View? = null
    private var camera: Camera? = null
    private var swapChain: SwapChain? = null
    private var light: Int = 0
    private var indirectLight: IndirectLight? = null
    private var iblReflections: Texture? = null
    private var targetW = 0
    private var targetH = 0
    private var materials: UbershaderProvider? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null
    private var asset: FilamentAsset? = null
    private var loadedKey: String? = null

    fun probe(): Boolean = synchronized(lock) {
        if (ready) return true
        val ok = runCatching {
            boot()
            smoke()
        }.getOrDefault(false)
        if (!ok) shutdown()
        ready = ok
        ok
    }

    fun renderProp(request: CgPlateRequest): Bitmap? = synchronized(lock) {
        val file = request.propFile ?: return null
        if (!ready && !probe()) return null
        runCatching { draw(request, file) }.getOrNull()
    }

    private fun boot() {
        Gltfio.init()
        val created = Engine.create(Engine.Backend.OPENGL)
        engine = created
        renderer = created.createRenderer()
        scene = created.createScene()
        view = created.createView()
        val cameraEntity = EntityManager.get().create()
        val createdCamera = created.createCamera(cameraEntity)
        camera = createdCamera
        val createdView = view ?: error("view")
        val createdScene = scene ?: error("scene")
        createdView.setCamera(createdCamera)
        createdView.setScene(createdScene)
        createdView.setBlendMode(View.BlendMode.TRANSLUCENT)
        createdView.setPostProcessingEnabled(false)
        val clear = Renderer.ClearOptions()
        clear.clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        clear.clear = true
        renderer?.setClearOptions(clear)
        val distance = cgFilamentCameraDistance()
        createdCamera.lookAt(0.0, distance * 0.08, distance, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        light = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.97f, 0.92f)
            .intensity(DIRECTIONAL_INTENSITY)
            .direction(0.35f, -1f, -0.25f)
            .castShadows(false)
            .build(created, light)
        createdScene.addEntity(light)
        installIbl(created, createdScene)
        val provider = UbershaderProvider(created)
        materials = provider
        assetLoader = AssetLoader(created, provider, EntityManager.get())
        resourceLoader = ResourceLoader(created, false)
    }

    private fun smoke(): Boolean {
        ensureTarget(CLEAR_SIZE, CLEAR_SIZE)
        val buffer = readFrame() ?: return false
        return buffer.capacity() >= CLEAR_SIZE * CLEAR_SIZE * 4
    }

    private fun draw(request: CgPlateRequest, file: File): Bitmap? {
        val width = request.width.coerceIn(2, MAX_EDGE)
        val height = request.height.coerceIn(2, MAX_EDGE)
        ensureTarget(width, height)
        ensureAsset(file)
        applyTransform(request)
        val buffer = readFrame() ?: return null
        val decoded = rgbaToBitmap(buffer, width, height)
        if (decoded.second < MIN_VISIBLE_ALPHA) {
            decoded.first.recycle()
            return null
        }
        return placePlate(decoded.first, request)
    }

    private fun ensureTarget(width: Int, height: Int) {
        val created = engine ?: error("engine")
        val createdView = view ?: error("view")
        val createdCamera = camera ?: error("camera")
        if (swapChain != null && targetW == width && targetH == height) return
        swapChain?.let { created.destroySwapChain(it) }
        val flags = SwapChainFlags.CONFIG_READABLE or SwapChainFlags.CONFIG_TRANSPARENT
        swapChain = created.createSwapChain(width, height, flags)
        targetW = width
        targetH = height
        createdView.setViewport(Viewport(0, 0, width, height))
        val aspect = width.toDouble() / height.toDouble()
        createdCamera.setProjection(45.0, aspect, NEAR_PLANE, FAR_PLANE, Camera.Fov.VERTICAL)
    }

    private fun ensureAsset(file: File) {
        val key = file.absolutePath + ":" + file.length() + ":" + file.lastModified()
        if (key == loadedKey && asset != null) return
        releaseAsset()
        val created = loadAsset(file)
        scene?.addEntities(created.entities)
        asset = created
        loadedKey = key
    }

    private fun loadAsset(file: File): FilamentAsset {
        if (file.length() > GLTF_MAX_BYTES) error("Prop file is too large.")
        val loader = assetLoader ?: error("asset loader")
        val resources = resourceLoader ?: error("resource loader")
        val created = loader.createAsset(directCopy(file.readBytes())) ?: error("glTF was not accepted.")
        val loaded = runCatching {
            attachResources(file, created, resources)
            resources.loadResources(created)
            created.releaseSourceData()
            created
        }
        if (loaded.isFailure) loader.destroyAsset(created)
        return loaded.getOrThrow()
    }

    private fun attachResources(file: File, created: FilamentAsset, resources: ResourceLoader) {
        for (uri in created.resourceUris) {
            val sibling = cgPropResourceFile(file, uri) ?: error("Missing glTF resource.")
            if (sibling.length() > GLTF_MAX_BYTES) error("glTF resource is too large.")
            resources.addResourceData(uri, directCopy(sibling.readBytes()))
        }
    }

    private fun applyTransform(request: CgPlateRequest) {
        val current = asset ?: error("asset")
        val created = engine ?: error("engine")
        val box = current.boundingBox
        val tm = created.transformManager
        val instance = tm.getInstance(current.root)
        if (instance == 0) error("prop root has no transform")
        tm.setTransform(
            instance,
            cgFilamentPropTransform(
                yawDeg = request.yaw,
                pitchDeg = request.pitch,
                userScale = request.scale,
                center = box.center,
                halfExtent = box.halfExtent
            )
        )
    }

    private fun readFrame(): ByteBuffer? {
        val created = engine
        val createdRenderer = renderer
        val createdView = view
        val chain = swapChain
        if (created == null || createdRenderer == null || createdView == null || chain == null) {
            return null
        }
        if (!createdRenderer.beginFrame(chain, 0L)) return null
        return readPixels(created, createdRenderer, createdView)
    }

    private fun readPixels(created: Engine, createdRenderer: Renderer, createdView: View): ByteBuffer? {
        val bytes = targetW * targetH * 4
        val buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
        val latch = CountDownLatch(1)
        val descriptor = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RGBA,
            Texture.Type.UBYTE
        )
        descriptor.setCallback(Executor { command -> command.run() }, Runnable { latch.countDown() })
        val read = runCatching {
            createdRenderer.render(createdView)
            createdRenderer.readPixels(0, 0, targetW, targetH, descriptor)
            createdRenderer.endFrame()
            created.flushAndWait()
            if (latch.await(READBACK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                buffer.rewind()
                buffer
            } else {
                null
            }
        }
        if (read.isFailure) runCatching { createdRenderer.endFrame() }
        return read.getOrThrow()
    }

    private fun rgbaToBitmap(buffer: ByteBuffer, width: Int, height: Int): Pair<Bitmap, Int> {
        val pixels = IntArray(width * height)
        var maxAlpha = 0
        buffer.rewind()
        for (y in 0 until height) {
            val row = (height - 1 - y) * width
            for (x in 0 until width) {
                val red = buffer.get().toInt() and 0xff
                val green = buffer.get().toInt() and 0xff
                val blue = buffer.get().toInt() and 0xff
                val alpha = buffer.get().toInt() and 0xff
                if (alpha > maxAlpha) maxAlpha = alpha
                pixels[row + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap to maxAlpha
    }

    private fun placePlate(centered: Bitmap, request: CgPlateRequest): Bitmap {
        if (request.ox == 0f && request.oy == 0f) return centered
        val out = Bitmap.createBitmap(centered.width, centered.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val (dx, dy) = cgPlateOffset(request.ox, request.oy, centered.width, centered.height)
        canvas.translate(dx, dy)
        canvas.drawBitmap(centered, 0f, 0f, null)
        centered.recycle()
        return out
    }


    private fun installIbl(created: Engine, createdScene: Scene) {
        val reflections = createStudioReflections(created)
        iblReflections = reflections
        val ibl = IndirectLight.Builder()
            .reflections(reflections)
            .radiance(3, STUDIO_IBL_RADIANCE_SH)
            .intensity(IBL_INTENSITY)
            .build(created)
        indirectLight = ibl
        createdScene.indirectLight = ibl
    }

    /**
     * 1×1 RGB cubemap so specular IBL has a bound reflections texture (avoids
     * stale GL cubemap state). Constant soft studio tint — not an HDRI asset.
     */
    private fun createStudioReflections(created: Engine): Texture {
        val texture = Texture.Builder()
            .width(1)
            .height(1)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
            .format(Texture.InternalFormat.RGB8)
            .build(created)
        val faceBytes = 3
        val buffer = ByteBuffer.allocateDirect(6 * faceBytes).order(ByteOrder.nativeOrder())
        // Soft warm gray on every face.
        repeat(6) {
            buffer.put(0xD8.toByte())
            buffer.put(0xD4.toByte())
            buffer.put(0xCC.toByte())
        }
        buffer.flip()
        val offsets = IntArray(6) { it * faceBytes }
        val descriptor = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RGB,
            Texture.Type.UBYTE
        )
        @Suppress("DEPRECATION")
        texture.setImage(created, 0, descriptor, offsets)
        return texture
    }

    private fun releaseAsset() {
        val current = asset ?: return
        scene?.removeEntities(current.entities)
        assetLoader?.destroyAsset(current)
        asset = null
        loadedKey = null
    }

    private fun directCopy(bytes: ByteArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }

    private fun shutdown() {
        runCatching { releaseAsset() }
        runCatching { resourceLoader?.destroy() }
        runCatching { assetLoader?.destroy() }
        runCatching { materials?.destroy() }
        val created = engine
        if (created != null) {
            destroyIbl(created)
            destroyLight(created)
            destroyCamera(created)
            runCatching { view?.let { created.destroyView(it) } }
            runCatching { scene?.let { created.destroyScene(it) } }
            runCatching { renderer?.let { created.destroyRenderer(it) } }
            runCatching { swapChain?.let { created.destroySwapChain(it) } }
            runCatching { created.destroy() }
        }
        engine = null
        renderer = null
        scene = null
        view = null
        camera = null
        swapChain = null
        light = 0
        indirectLight = null
        iblReflections = null
        targetW = 0
        targetH = 0
        materials = null
        assetLoader = null
        resourceLoader = null
        ready = false
    }

    private fun destroyIbl(created: Engine) {
        runCatching { scene?.indirectLight = null }
        runCatching { indirectLight?.let { created.destroyIndirectLight(it) } }
        runCatching { iblReflections?.let { created.destroyTexture(it) } }
        indirectLight = null
        iblReflections = null
    }

    private fun destroyLight(created: Engine) {
        if (light == 0) return
        runCatching {
            scene?.removeEntity(light)
            created.destroyEntity(light)
            EntityManager.get().destroy(light)
        }
        light = 0
    }

    private fun destroyCamera(created: Engine) {
        val current = camera ?: return
        runCatching {
            created.destroyCameraComponent(current.entity)
            EntityManager.get().destroy(current.entity)
        }
        camera = null
    }
}
