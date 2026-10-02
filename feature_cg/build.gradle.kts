plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.pixijoy.feature.cg"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
    implementation(project(":app"))
    implementation(project(":movie"))
    // Filament JNI is already packaged by on-demand :feature_titles3d.
    // A second split cannot embed the same AAR. compileOnly keeps it out of
    // this APK and out of the base APK. Runtime load uses the titles split.
    compileOnly(libs.filament.android)
    compileOnly(libs.filament.gltfio)
}
