import java.util.zip.ZipFile

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
    description = "H10: measure base APK MiB and assert Filament/feature_cg stay out of base."
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

        val forbiddenHints = listOf(
            "libfilament",
            "libgltfio",
            "libfilament-jni",
            "com/pixijoy/feature/cg/",
            "com/google/android/filament/"
        )
        val hits = mutableListOf<String>()
        ZipFile(baseApk).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val lower = entry.name.lowercase()
                if (forbiddenHints.any { hint -> lower.contains(hint) }) {
                    hits += "${"%.2f".format(entry.size / (1024.0 * 1024.0))} MiB  ${entry.name}"
                }
            }
        }
        if (hits.isNotEmpty()) {
            hits.forEach { logger.error("H10 FORBIDDEN in base: $it") }
            error(
                "H10 FAIL: Filament / feature_cg content found in base APK. " +
                    "Keep CG engines in on-demand :feature_cg / :feature_titles3d."
            )
        }
        logger.lifecycle("H10 base scan: no Filament / feature_cg JNI or classes in base APK.")

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
