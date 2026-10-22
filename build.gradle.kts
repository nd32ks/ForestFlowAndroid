plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// This project lives in iCloud Drive. Folders ending in ".nosync" are never synced by iCloud,
// so build outputs are not uploaded, evicted or renamed underneath Gradle.
allprojects {
    layout.buildDirectory.set(layout.projectDirectory.dir("build.nosync"))
}
