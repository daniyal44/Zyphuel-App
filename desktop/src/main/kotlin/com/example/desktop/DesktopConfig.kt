package com.example.desktop

import com.google.gson.JsonParser
import java.io.File

/**
 * Firebase coordinates for the desktop console.
 *
 * Resolution order:
 *  1. Environment variables `ZYPHUEL_FIREBASE_PROJECT_ID` / `ZYPHUEL_FIREBASE_API_KEY`.
 *  2. The Android app's `google-services.json`, searched from the working directory upwards.
 *
 * The key is deliberately not hard-coded here, so the repository keeps exactly one copy of it.
 */
object DesktopConfig {

    data class Firebase(val projectId: String, val apiKey: String)

    fun load(): Result<Firebase> {
        val envProject = System.getenv("ZYPHUEL_FIREBASE_PROJECT_ID")?.trim()
        val envKey = System.getenv("ZYPHUEL_FIREBASE_API_KEY")?.trim()
        if (!envProject.isNullOrBlank() && !envKey.isNullOrBlank()) {
            return Result.success(Firebase(envProject, envKey))
        }

        val file = findGoogleServicesJson()
            ?: return Result.failure(
                IllegalStateException(
                    "google-services.json not found.\n\n" +
                        "Start the console from the Zyphuel project folder, or set the " +
                        "ZYPHUEL_FIREBASE_PROJECT_ID and ZYPHUEL_FIREBASE_API_KEY environment variables."
                )
            )

        return runCatching {
            val root = JsonParser.parseString(file.readText()).asJsonObject
            val projectId = root.getAsJsonObject("project_info").get("project_id").asString
            val clients = root.getAsJsonArray("client")
            require(clients.size() > 0) { "google-services.json has no client entries" }
            val keys = clients.get(0).asJsonObject.getAsJsonArray("api_key")
            require(keys.size() > 0) { "google-services.json has no api_key entries" }
            val apiKey = keys.get(0).asJsonObject.get("current_key").asString
            Firebase(projectId = projectId, apiKey = apiKey)
        }
    }

    /**
     * Walks up from the working directory looking for the Android app's config file.
     * The depth is generous because the packaged .exe lives several folders deep inside
     * `desktop/build/compose/binaries/...`.
     */
    private fun findGoogleServicesJson(): File? {
        var dir: File? = File("").absoluteFile
        repeat(12) {
            val current = dir ?: return null
            File(current, "app/google-services.json").let { if (it.isFile) return it }
            File(current, "google-services.json").let { if (it.isFile) return it }
            dir = current.parentFile
        }
        return null
    }
}
