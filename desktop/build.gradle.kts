import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("org.jetbrains.compose") version "1.9.0"
}

group = "com.example.desktop"
version = "1.0.0"

// Target 17 rather than a toolchain: Gradle here runs on the same JDK 25 that builds the
// Android app, and targeting 17 avoids provisioning a second JDK just for this module.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Dispatchers.Main on desktop is backed by Swing's event queue.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Firestore is reached over its REST API (the Firebase Android SDK is Android-only),
    // so all we need is a JSON reader — HTTP comes from the JDK's own HttpClient.
    implementation("com.google.code.gson:gson:2.11.0")
}

compose.desktop {
    application {
        mainClass = "com.example.desktop.MainKt"

        nativeDistributions {
            // The packaged app ships a jlink-trimmed JRE that contains only the modules named
            // here plus Compose's own. Anything missing shows up as "failed to launch" with no
            // further detail, so every JDK module the console actually touches is listed:
            //   java.net.http   - Firestore REST + OpenStreetMap tile fetches
            //   jdk.crypto.ec   - EC cipher suites, which Google's TLS certificates use
            //   java.sql        - Gson's lazily-loaded SQL date adapters
            //   java.naming     - DNS resolution used by HttpClient
            //   jdk.unsupported - sun.misc.Unsafe, pulled in by Gson and coroutines
            //   java.management - JMX hooks touched by the coroutines debug agent
            // `gradle -p desktop suggestRuntimeModules` re-derives this list if deps change.
            modules(
                "java.net.http",
                "jdk.crypto.ec",
                "java.sql",
                "java.naming",
                "jdk.unsupported",
                "java.management"
            )

            // Msi needs the WiX toolset installed. For a plain .exe you can run
            // `createDistributable`, which needs nothing extra — see RUN-DESKTOP.bat.
            targetFormats(TargetFormat.Msi)
            packageName = "ZyphuelOpsConsole"
            packageVersion = "1.0.0"
            description = "Zyphuel Operations Console - live rider tracking and order dispatch"
            vendor = "Zyphuel"

            windows {
                menu = true
                shortcut = true
                // Stable UUID so upgrades replace the previous install instead of stacking.
                upgradeUuid = "6F1B7A2C-3D4E-4F50-9A61-72B8C9D0E1F2"
            }
        }
    }
}
