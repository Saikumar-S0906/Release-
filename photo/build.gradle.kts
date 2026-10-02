plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.pixijoy.photo"
    compileSdk = 35
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:media"))
    implementation(project(":ml"))
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.heifwriter)
    testImplementation(libs.junit)
    // SmartObjectsTest needs Bitmap; JVM stubs throw "not mocked".
    testImplementation("org.robolectric:robolectric:4.14.1")
}
