plugins {
    id("com.android.dynamic-feature")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pixijoy.feature.ffmpeg"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":movie"))
    // ffmpeg-kit-min yanked from Maven Central — restore when AAR is on Release-
    // implementation(libs.ffmpeg.kit.min)
}
