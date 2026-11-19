import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Supabase project (optional): without a URL and key the app runs fully offline on its SQLite database.
// Put them in local.properties (not committed) or supabase.properties, or in the SUPABASE_URL / SUPABASE_ANON_KEY
// environment variables:  supabase.url=https://xxxx.supabase.co   supabase.anonKey=eyJ...
val supabaseProps = Properties().apply {
    listOf("supabase.properties", "local.properties").forEach { name ->
        rootProject.file(name).takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
}
fun supabaseSetting(key: String, env: String): String = (supabaseProps.getProperty(key) ?: System.getenv(env) ?: "").trim()

// Which later features are switched on. Demo scope = login + Feature 1 + Feature 2 only (nothing listed).
// To bring features out, put e.g.  features=vip  or  features=vip,map  or  features=all  in local.properties,
// or pass -Pfeatures=all on the command line (or set FF_FEATURES). vip = VIP pass, escrow and points; map = campus map.
val enabledFeatures: Set<String> = ((project.findProperty("features") as String?) ?: supabaseSetting("features", "FF_FEATURES"))
    .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
fun featureOn(name: String) = "all" in enabledFeatures || name in enabledFeatures

android {
    namespace = "com.chiforest.forestflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "forest.flow"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Each instrumented test runs in its own process with app data cleared (Test Orchestrator).
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        // -PfakeCloud: let the app talk plain HTTP (to tools/fake_supabase.py on the host). Never set for a shipped build.
        manifestPlaceholders["usesCleartext"] = if (project.hasProperty("fakeCloud")) "true" else "false"
        buildConfigField("boolean", "FEATURE_VIP_CLUB", featureOn("vip").toString())
        buildConfigField("boolean", "FEATURE_CAMPUS_MAP", featureOn("map").toString())
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseSetting("supabase.url", "SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseSetting("supabase.anonKey", "SUPABASE_ANON_KEY")}\"")
        // Drink assistant: any OpenAI-style chat endpoint. Leave ai.key empty and the app uses its offline matcher only.
        buildConfigField("String", "AI_KEY", "\"${supabaseSetting("ai.key", "FF_AI_KEY")}\"")
        buildConfigField("String", "AI_ENDPOINT", "\"${supabaseSetting("ai.endpoint", "FF_AI_ENDPOINT").ifBlank { "https://api.deepseek.com/chat/completions" }}\"")
        buildConfigField("String", "AI_AUTH_HEADER", "\"${supabaseSetting("ai.authHeader", "FF_AI_AUTH_HEADER").ifBlank { "Authorization" }}\"")
        buildConfigField("String", "AI_MODEL", "\"${supabaseSetting("ai.model", "FF_AI_MODEL").ifBlank { "deepseek-chat" }}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (project.hasProperty("testRelease")) proguardFiles("proguard-test-rules.pro")
            // Signed with the debug key so the release APK installs on a phone for demos.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // `./gradlew connectedReleaseAndroidTest -PtestRelease` runs the instrumented suite on the R8-minified release build.
    if (project.hasProperty("testRelease")) testBuildType = "release"
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "ObsoleteLintCustomCheck", "ObsoleteSdkInt")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller) // installs the baseline profile so the first launch is already precompiled
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.haze)
    implementation(libs.zxing.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
