import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// The Twitch Client ID lives in local.properties (not in git) and reaches the code via BuildConfig.
// The app is a public client: no secret anywhere.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(name: String): String = localProperties.getProperty(name, "")

// CI passes -PversionCode=<run number> -PversionName=0.1.<run number>; local builds stay 1 / 0.1.0.
val ciVersionCode = providers.gradleProperty("versionCode").orNull?.toInt()
val ciVersionName = providers.gradleProperty("versionName").orNull

/**
 * Release signing: env vars in CI (keystore decoded from a GitHub secret), or a keystore.properties file kept
 * outside the repo (path in local.properties: release.keystoreProperties). Updates install over the previous
 * version only when every release is signed with this same key.
 */
val releaseSigning: Map<String, String>? = run {
    val fromEnv = listOf("TWICAST_KEYSTORE_FILE", "TWICAST_KEYSTORE_PASSWORD", "TWICAST_KEY_ALIAS", "TWICAST_KEY_PASSWORD")
        .associateWith { System.getenv(it).orEmpty() }
    if (fromEnv.values.all { it.isNotEmpty() }) {
        mapOf(
            "storeFile" to fromEnv.getValue("TWICAST_KEYSTORE_FILE"),
            "storePassword" to fromEnv.getValue("TWICAST_KEYSTORE_PASSWORD"),
            "keyAlias" to fromEnv.getValue("TWICAST_KEY_ALIAS"),
            "keyPassword" to fromEnv.getValue("TWICAST_KEY_PASSWORD"),
        )
    } else {
        localProperty("release.keystoreProperties").takeIf { it.isNotEmpty() }?.let { path ->
            Properties().apply { file(path).inputStream().use(::load) }.let { props ->
                props.stringPropertyNames().associateWith { props.getProperty(it) }
            }
        }
    }
}

android {
    namespace = "app.twicast.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.twicast.tv"
        minSdk = 26
        targetSdk = 37
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "0.1.0"

        buildConfigField("String", "TWITCH_CLIENT_ID", "\"${localProperty("twitch.clientId")}\"")
        // Releases (APK + notes only) go to a public repo, the source stays private; the app checks them on start.
        buildConfigField("String", "UPDATE_REPOSITORY", "\"RozumS321/TwiCast-TV-releases\"")
        // Only CI builds have a real version to compare; a local build would always "find" an update.
        buildConfigField("boolean", "UPDATES_ENABLED", "${ciVersionCode != null}")
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without the release key (e.g. a fresh clone) the build still installs, signed with the debug key.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.material)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.webkit)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
