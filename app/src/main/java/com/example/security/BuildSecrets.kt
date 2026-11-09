package com.example.security

import com.example.BuildConfig

/**
 * Credentials compiled into this build and decoded at runtime.
 *
 * The build script carries them as lowercase hex so no secret value is ever spliced into
 * generated Java source: quotes, backslashes and line breaks in a pasted password cannot
 * break `compileDebugJavaWithJavac`. Nothing here is hardcoded — see
 * `app/build.gradle.kts` (`SMTP_APP_PASSWORD` env var, `app/admin-credentials.properties`
 * or GitHub Actions secret).
 */
object BuildSecrets {

    /**
     * Email Gateway app password shipped with this build, or `""` when the build was made
     * without one (the admin-managed Firestore config then provides it at runtime).
     */
    val smtpAppPassword: String by lazy { decode(BuildConfig.SMTP_APP_PASSWORD_HEX) }

    /** Lowercase hex → UTF-8 text. Malformed input yields `""` instead of throwing. */
    internal fun decode(hex: String): String {
        if (hex.isEmpty() || hex.length % 2 != 0) return ""
        val bytes = ByteArray(hex.length / 2)
        for (index in bytes.indices) {
            val high = hex[index * 2].digitToIntOrNull(16) ?: return ""
            val low = hex[index * 2 + 1].digitToIntOrNull(16) ?: return ""
            bytes[index] = ((high shl 4) or low).toByte()
        }
        // Gmail app passwords are frequently pasted with spaces; the SMTP client wants none.
        return String(bytes, Charsets.UTF_8).replace(" ", "").trim()
    }
}
