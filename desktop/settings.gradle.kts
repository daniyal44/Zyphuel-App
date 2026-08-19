// Standalone Gradle build for the Zyphuel Desktop Ops Console.
//
// Deliberately NOT included in the root settings.gradle.kts: keeping this build
// isolated means a Compose-Desktop plugin/version problem here can never break the
// Android app build, and vice versa. The two share a backend (Firestore), not a build.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "zyphuel-desktop"
