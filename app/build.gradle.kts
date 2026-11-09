import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Single source of truth shared with iOS: version.properties at the repository root.
// Android reads it here; iOS (ios/project.yml, ZyphuelConfig.swift), README and the feature
// docs are kept identical by scripts/sync_versions.py, so both apps always ship the same
// versionCode / versionName. Never hardcode either value in this file.
val versionFile = file("${rootDir}/version.properties")
if (!versionFile.exists()) {
  throw GradleException(
    "version.properties is missing. It is the shared Android/iOS version source; " +
      "restore it or run: python scripts/sync_versions.py --bump"
  )
}
val sharedVersionProps = Properties()
FileInputStream(versionFile).use { stream -> sharedVersionProps.load(stream) }
val appVersionCode = sharedVersionProps.getProperty("versionCode")?.trim()?.toIntOrNull()
  ?: throw GradleException("version.properties is missing a numeric versionCode")
val appVersionName = sharedVersionProps.getProperty("versionName")?.trim()
  ?.takeIf { it.isNotEmpty() }
  ?: throw GradleException("version.properties is missing versionName")
if (!appVersionName.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
  throw GradleException(
    "versionName '$appVersionName' must use the 2.6.4.0.0.XX sub-version format."
  )
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.zyphuel.appv2"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

    manifestPlaceholders["MAPS_API_KEY"] = "AIzaZyphuelPlaceholderKey"

    // Load admin credential hash securely from git-ignored local configuration
    val adminProps = Properties()
    val adminCredsFile = file("${rootDir}/app/admin-credentials.properties")
    if (adminCredsFile.exists()) {
      FileInputStream(adminCredsFile).use { stream ->
        adminProps.load(stream)
      }
    }
    val localPropsFile = file("${rootDir}/local.properties")
    if (localPropsFile.exists()) {
      FileInputStream(localPropsFile).use { stream ->
        adminProps.load(stream)
      }
    }
    // FAIL-CLOSED (2026-11-03): no hardcoded fallback hash. A committed master-admin hash
    // is a public offline brute-force target, so the value must come only from the
    // environment or a git-ignored local file. When it is absent the master-admin path is
    // disabled rather than silently falling back to a repository value.
    val loadedAdminHash = (System.getenv("ADMIN_PASSWORD_HASH")
      ?: adminProps.getProperty("ADMIN_PASSWORD_HASH")
      ?: "").trim().lowercase()
    if (loadedAdminHash.isNotEmpty() && !loadedAdminHash.matches(Regex("^[0-9a-f]{64}$"))) {
      throw GradleException(
        "ADMIN_PASSWORD_HASH is malformed: expected 64 lowercase hex characters " +
          "(SHA-256 of '<password>' + PEPPER). Refusing to build with an invalid admin hash."
      )
    }
    if (loadedAdminHash.isEmpty()) {
      logger.warn(
        "WARNING: ADMIN_PASSWORD_HASH is not configured. The master-admin credential path " +
          "is DISABLED for this build. Set ADMIN_PASSWORD_HASH in the environment or in the " +
          "git-ignored app/admin-credentials.properties / local.properties to restore it."
      )
    }

    buildConfigField("String", "MASTER_ADMIN_HASH", "\"$loadedAdminHash\"")

    // Secure SMTP gateway password injection (2026-10-23). Never hardcode this in source:
    // provide SMTP_APP_PASSWORD via environment or the git-ignored local.properties /
    // app/admin-credentials.properties files. Empty by default; the admin-managed
    // Firestore email gateway config is the primary source at runtime.
    // This value is interpolated into generated Java source, so a secret carrying a line
    // break, quote or backslash used to leave an unterminated literal and break
    // `compileDebugJavaWithJavac` ("illegal start of expression"). It is therefore encoded
    // as lowercase hex — always a safe Java string — and decoded at runtime by
    // `com.example.security.BuildSecrets`. Spaces are stripped on both sides because Gmail
    // app passwords are commonly pasted as "xxxx xxxx xxxx xxxx".
    val rawSmtpPassword = System.getenv("SMTP_APP_PASSWORD")
      ?: adminProps.getProperty("SMTP_APP_PASSWORD")
      ?: ""
    val smtpPasswordHex = rawSmtpPassword.trim().toByteArray(Charsets.UTF_8)
      .joinToString("") { byte -> "%02x".format(byte) }
    buildConfigField("String", "SMTP_APP_PASSWORD_HEX", "\"$smtpPasswordHex\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
      // Without an upload key nothing is set here on purpose: `release` and `debug` fall back
      // to AGP's built-in `debug` signing config, which uses (and generates) the SDK debug
      // keystore in ~/.android. A committed or git-ignored keystore at the repo root is not
      // present on a CI runner, and pointing at a missing file fails `validateSigning`.
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
        signingConfigs.getByName("release")
      } else {
        signingConfigs.getByName("debug")
      }
    }
    debug { signingConfig = signingConfigs.getByName("debug") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.play.services.location)
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.firestore)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
