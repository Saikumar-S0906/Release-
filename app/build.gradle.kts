import java.util.zip.ZipFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.pixijoy.ai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pixijoy.ai"
        minSdk = 31
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.0-studio"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Phone ABIs only. MediaPipe JNI is stored uncompressed; x86/x86_64
            // copies exist for emulators and are the bulk of the fat APK.
            // Debug stays multi-ABI. LiteRT's arm64/armeabi-v7a client stays.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Signed like debug so :benchmark can install a non-debuggable release twin.
        // Keep x86_64 so Macrobenchmark can run on lab AVDs; release stays phone ABIs.
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            ndk {
                abiFilters.clear()
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    dynamicFeatures += setOf(":feature_titles3d", ":feature_ar", ":feature_ffmpeg", ":feature_cg")
}

dependencies {
    implementation(project(":designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":photo"))
    implementation(project(":movie"))
    implementation(project(":ml"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.window)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.video)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.leakcanary.android.instrumentation)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}

tasks.register("reportApkSize") {
    group = "verification"
    description = "Print minified release APK size for the E5 ~40 MB gate."
    dependsOn("packageRelease")
    doLast {
        val apk = file("build/outputs/apk/release").listFiles()
            ?.filter { it.extension == "apk" }
            ?.maxByOrNull { it.length() }
            ?: error("No release APK. Run :app:assembleRelease first.")
        val mb = apk.length() / (1024.0 * 1024.0)
        logger.lifecycle("E5 APK ${apk.name}: ${"%.2f".format(mb)} MB (${apk.length()} bytes)")
        ZipFile(apk).use { zip ->
            val sos = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".so") }
                .sortedByDescending { it.size }
                .toList()
            val soBytes = sos.sumOf { it.size }
            logger.lifecycle(
                "E5 native libs: ${"%.2f".format(soBytes / (1024.0 * 1024.0))} MB across ${sos.size} .so files"
            )
            sos.forEach { entry ->
                logger.lifecycle(
                    "E5 so ${"%.2f".format(entry.size / (1024.0 * 1024.0))} MB ${entry.name}"
                )
            }
        }
        if (mb > 40.0) {
            logger.warn("E5 size gate: APK is above ~40 MB. Keep models out of the base APK.")
        }
    }
}

tasks.register("reportH10SizeGate") {
    group = "verification"
    description =
        "H10: measure base APK MiB; DEX+ZIP+mapping scan keeps Filament/feature_cg out of base; assert on-demand delivery."
    dependsOn("packageRelease")
    doLast {
        val releaseDir = file("build/outputs/apk/release")
        val baseApk = releaseDir.listFiles()
            ?.filter { it.isFile && it.extension == "apk" }
            ?.maxByOrNull { it.length() }
            ?: error("H10: no release APK under $releaseDir. Run :app:assembleRelease first.")
        val baseMiB = baseApk.length() / (1024.0 * 1024.0)
        logger.lifecycle(
            "H10 base APK ${baseApk.name}: ${"%.2f".format(baseMiB)} MiB (${baseApk.length()} bytes)"
        )

        // --- helpers: DEX class descriptors (honesty vs ZIP-path-only) ---
        fun uleb128(buf: ByteArray, start: Int): Pair<Int, Int> {
            var result = 0
            var shift = 0
            var offset = start
            while (true) {
                val b = buf[offset].toInt() and 0xff
                offset++
                result = result or ((b and 0x7f) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            return result to offset
        }

        fun readMutf8(buf: ByteArray, start: Int): String {
            val (_, payloadStart) = uleb128(buf, start)
            // MUTF-8 length is in modified UTF-8 units; read until NUL.
            var end = payloadStart
            while (end < buf.size && buf[end] != 0.toByte()) end++
            return String(buf, payloadStart, end - payloadStart, Charsets.UTF_8)
        }

        fun leInt(buf: ByteArray, offset: Int): Int =
            ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

        fun dexClassBinaryNames(dex: ByteArray): List<String> {
            require(dex.size >= 0x70) { "H10: DEX too small (${dex.size})" }
            require(dex.copyOfRange(0, 4).contentEquals("dex\n".toByteArray())) {
                "H10: not a DEX file"
            }
            val stringIdsSize = leInt(dex, 56)
            val stringIdsOff = leInt(dex, 60)
            val typeIdsOff = leInt(dex, 68)
            val classDefsSize = leInt(dex, 96)
            val classDefsOff = leInt(dex, 100)
            fun stringAt(index: Int): String {
                require(index in 0 until stringIdsSize)
                val strDataOff = leInt(dex, stringIdsOff + index * 4)
                return readMutf8(dex, strDataOff)
            }
            fun typeDescriptor(typeIdx: Int): String {
                val stringIdx = leInt(dex, typeIdsOff + typeIdx * 4)
                return stringAt(stringIdx)
            }
            return (0 until classDefsSize).map { i ->
                val classIdx = leInt(dex, classDefsOff + i * 32)
                val desc = typeDescriptor(classIdx)
                if (desc.startsWith("L") && desc.endsWith(";")) desc.substring(1, desc.length - 1)
                else desc
            }
        }

        val forbiddenNativeHints = listOf(
            "libfilament",
            "libgltfio",
            "libfilament-jni"
        )
        val forbiddenClassPackages = listOf(
            "com/pixijoy/feature/cg/",
            "com/google/android/filament/",
            "com/google/android/gltfio/"
        )
        val forbiddenOriginalPrefixes = listOf(
            "com.pixijoy.feature.cg.",
            "com.google.android.filament.",
            "com.google.android.gltfio."
        )

        val hits = mutableListOf<String>()
        val dexClassSet = linkedSetOf<String>()

        ZipFile(baseApk).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val lower = entry.name.lowercase()
                // ZIP path scan: natives only (classes live inside classes*.dex).
                if (forbiddenNativeHints.any { hint -> lower.contains(hint) }) {
                    hits += "ZIP native ${"%.2f".format(entry.size / (1024.0 * 1024.0))} MiB  ${entry.name}"
                }
                if (entry.name.matches(Regex("""classes\d*\.dex"""))) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val names = dexClassBinaryNames(bytes)
                    dexClassSet.addAll(names)
                    names.forEach { bin ->
                        val slash = if (bin.contains('.')) bin.replace('.', '/') else bin
                        if (forbiddenClassPackages.any { pkg ->
                                slash == pkg.removeSuffix("/") || slash.startsWith(pkg)
                            }
                        ) {
                            hits += "DEX class $bin  (in ${entry.name})"
                        }
                    }
                    logger.lifecycle("H10 DEX ${entry.name}: ${names.size} classes scanned")
                }
            }
        }

        // Release shrinking: map forbidden originals -> residuals and match residuals in base DEX.
        val mappingFile = file("build/outputs/mapping/release/mapping.txt")
        if (mappingFile.isFile) {
            var mappedHits = 0
            mappingFile.forEachLine { line ->
                if (!line.contains(" -> ") || line.startsWith(" ")) return@forEachLine
                val arrow = line.indexOf(" -> ")
                if (arrow <= 0) return@forEachLine
                val original = line.substring(0, arrow).trim()
                var residual = line.substring(arrow + 4).trim()
                if (residual.endsWith(":")) residual = residual.dropLast(1)
                if (residual.startsWith("R8\$\$REMOVED\$\$CLASS\$\$")) return@forEachLine
                if (forbiddenOriginalPrefixes.none { original.startsWith(it) }) return@forEachLine
                // Method / field lines are indented; class lines have no leading space (already filtered).
                if (original.contains(" ")) return@forEachLine
                val residualBin = residual.replace('.', '/')
                if (dexClassSet.contains(residualBin) || dexClassSet.contains(residual)) {
                    hits += "DEX via mapping $original -> $residual"
                    mappedHits++
                }
            }
            logger.lifecycle(
                "H10 mapping: scanned ${mappingFile.name} for Filament/feature_cg residuals in base DEX" +
                    if (mappedHits == 0) " (none)" else " ($mappedHits hit(s))"
            )
        } else {
            logger.lifecycle(
                "H10 mapping: ${mappingFile.path} missing — DEX package scan only " +
                    "(re-run after minify release for shrinking residuals)."
            )
        }

        // P2: on-demand delivery must live in feature manifests (not install-time).
        fun assertOnDemandManifest(moduleDirName: String) {
            val manifest = rootProject.project(":$moduleDirName").file("src/main/AndroidManifest.xml")
            require(manifest.isFile) { "H10: missing $manifest" }
            val text = manifest.readText()
            if (!text.contains("<dist:on-demand")) {
                hits += "delivery $moduleDirName missing <dist:on-demand> in ${manifest.path}"
            }
            if (text.contains("<dist:install-time")) {
                hits += "delivery $moduleDirName has <dist:install-time> (must be on-demand only)"
            }
        }
        assertOnDemandManifest("feature_cg")
        assertOnDemandManifest("feature_titles3d")
        logger.lifecycle("H10 delivery: feature_cg + feature_titles3d declare dist:on-demand")

        if (hits.isNotEmpty()) {
            hits.forEach { logger.error("H10 FORBIDDEN in base: $it") }
            error(
                "H10 FAIL: Filament / feature_cg content found in base APK " +
                    "(DEX classes, natives, mapping residuals, or delivery). " +
                    "Keep CG engines in on-demand :feature_cg / :feature_titles3d."
            )
        }
        logger.lifecycle(
            "H10 base scan: no Filament / feature_cg natives or DEX classes in base " +
                "(${dexClassSet.size} classes across DEX); delivery on-demand OK."
        )

        fun reportSplit(modulePath: String, label: String) {
            val apk = rootProject.project(modulePath).projectDir
                .resolve("build/outputs/apk/release")
                .listFiles()
                ?.filter { it.isFile && it.extension == "apk" }
                ?.maxByOrNull { it.length() }
            if (apk == null) {
                logger.lifecycle("H10 $label: (no release APK yet — assemble that module)")
                return
            }
            val miB = apk.length() / (1024.0 * 1024.0)
            logger.lifecycle(
                "H10 $label ${apk.name}: ${"%.2f".format(miB)} MiB (${apk.length()} bytes)"
            )
        }
        reportSplit(":feature_cg", ":feature_cg")
        reportSplit(":feature_titles3d", ":feature_titles3d")

        if (baseMiB > 40.0) {
            logger.warn(
                "H10 note: base APK is above the E5 ~40 MiB aspiration (${"%.2f".format(baseMiB)}). " +
                    "H10 still passes if Filament/CG stay out of base; remeasure after ABI-filtered release."
            )
        } else {
            logger.lifecycle("H10 size: base under ~40 MiB aspiration.")
        }
        logger.lifecycle("H10 PASS: :feature_cg remains on-demand; base has no Filament engine.")
    }
}
