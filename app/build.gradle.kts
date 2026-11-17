import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Public build-time configuration. CLI -P overrides local and tracked files.
val appConfig = Properties().apply {
    rootProject.file("config/app.properties").reader(Charsets.UTF_8).use { load(it) }
    rootProject.file("config/local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { load(it) }
}
fun configured(name: String, fallback: String = ""): String =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: appConfig.getProperty(name)?.takeIf { it.isNotBlank() } ?: fallback
fun httpsUrl(name: String, value: String): String {
    val parsed = URI(value)
    require(parsed.scheme == "https" && parsed.host != null && parsed.userInfo == null) { "$name must be an HTTPS URL without credentials" }
    return value
}
val configuredApi = httpsUrl("apiBaseUrl", configured("apiBaseUrl"))
require(configuredApi.endsWith("/") && URI(configuredApi).rawQuery == null && URI(configuredApi).rawFragment == null) { "apiBaseUrl must end with / and have no query or fragment" }

android {
    namespace = "com.example.gibdd_ochevidec"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        manifestPlaceholders["appLabel"] = "@string/app_name"
        buildConfigField("String", "API_BASE_URL", "\"$configuredApi\"")

        applicationId = "com.example.gibdd_ochevidec"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("ciVersionCode").map { it.toInt() }.orElse(1).get()
        versionName = providers.gradleProperty("ciVersionName").orElse("1.0").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["appLabel"] = "Трезвый водитель 44 — сотрудники (тест)"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.compose.material:material-icons-core")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
}

// Reproducible third-party inventory for the distributed Android application.
tasks.register("thirdPartyInventory") {
    doLast {
        val dependencies = configurations.getByName(providers.gradleProperty("inventoryConfiguration").orElse("releaseRuntimeClasspath").get()).incoming.resolutionResult.allComponents
            .mapNotNull { component -> (component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier)?.let {
                "${it.group}:${it.module}:${it.version}"
            } }.distinct().sorted()
        val output = layout.buildDirectory.file("reports/licenses/dependencies.txt").get().asFile
        output.parentFile.mkdirs()
        output.writeText(dependencies.joinToString("\n") + "\n")
    }
}
