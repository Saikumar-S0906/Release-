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

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
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
