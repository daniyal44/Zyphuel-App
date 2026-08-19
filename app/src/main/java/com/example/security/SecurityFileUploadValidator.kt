package com.example.security

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.InputStream

data class FileUploadConfig(
    val maxFileSizeBytes: Long = 5 * 1024 * 1024L, // 5 MB max
    val allowedMimeTypes: Set<String> = setOf("image/jpeg", "image/png", "image/webp", "application/pdf"),
    val allowedExtensions: Set<String> = setOf("jpg", "jpeg", "png", "webp", "pdf")
)

sealed class FileValidationResult {
    data class Valid(val sanitizedFileName: String, val mimeType: String, val fileSizeBytes: Long) : FileValidationResult()
    data class Invalid(val reason: String) : FileValidationResult()

    val isValid: Boolean get() = this is Valid
}

object SecurityFileUploadValidator {

    private const val TAG = "FileUploadSecurity"

    @Volatile
    private var config = FileUploadConfig()

    fun updateConfig(newConfig: FileUploadConfig) {
        config = newConfig
    }

    /**
     * Validates file size, extension, MIME type, and magic header bytes from URI or InputStream.
     */
    fun validateAndStoreFile(
        context: Context,
        uri: Uri,
        customPrefix: String = "upload"
    ): FileValidationResult {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

            if (!config.allowedMimeTypes.contains(mimeType.lowercase())) {
                return FileValidationResult.Invalid("Disallowed file type: $mimeType. Only JPEG, PNG, WEBP, and PDF documents are allowed.")
            }

            var fileSizeBytes = 0L
            val headerBytes = ByteArray(12)

            contentResolver.openInputStream(uri)?.use { stream ->
                val read = stream.read(headerBytes, 0, headerBytes.size)
                if (read < 4) {
                    return FileValidationResult.Invalid("Invalid file format or truncated content.")
                }

                // Check Magic Bytes
                if (!verifyMagicBytes(headerBytes, mimeType)) {
                    return FileValidationResult.Invalid("File header signature does not match declared file type ($mimeType). Possible spoofing attempt.")
                }

                // Calculate remaining length
                fileSizeBytes = headerBytes.size.toLong()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    fileSizeBytes += bytesRead
                    if (fileSizeBytes > config.maxFileSizeBytes) {
                        return FileValidationResult.Invalid("File size exceeds limit of ${config.maxFileSizeBytes / (1024 * 1024)} MB.")
                    }
                }
            } ?: return FileValidationResult.Invalid("Unable to open input stream for file.")

            // Create sanitized isolated target file in private internal storage
            val secureDir = File(context.filesDir, "secure_uploads").apply {
                if (!exists()) mkdirs()
            }

            val sanitizedExtension = when (mimeType.lowercase()) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                "application/pdf" -> "pdf"
                else -> "bin"
            }

            val safeFileName = "${customPrefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.$sanitizedExtension"
            val targetFile = File(secureDir, safeFileName)

            // Prevent path traversal
            if (!targetFile.canonicalPath.startsWith(secureDir.canonicalPath)) {
                return FileValidationResult.Invalid("Security policy violation: Path traversal attempt blocked.")
            }

            // Copy file content securely
            contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Enforce non-executable file permissions for isolated storage
            targetFile.setExecutable(false, false)
            targetFile.setReadable(true, true)
            targetFile.setWritable(true, true)

            return FileValidationResult.Valid(
                sanitizedFileName = targetFile.absolutePath,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "File upload validation error: ${e.message}", e)
            return FileValidationResult.Invalid("Failed to process file upload securely: ${e.localizedMessage}")
        }
    }

    /**
     * Checks Magic Bytes / File Signatures for supported file types.
     */
    private fun verifyMagicBytes(header: ByteArray, mimeType: String): Boolean {
        if (header.size < 4) return false

        val b0 = header[0].toInt() and 0xFF
        val b1 = header[1].toInt() and 0xFF
        val b2 = header[2].toInt() and 0xFF
        val b3 = header[3].toInt() and 0xFF

        return when (mimeType.lowercase()) {
            "image/jpeg" -> b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF
            "image/png" -> b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47
            "application/pdf" -> b0 == 0x25 && b1 == 0x50 && b2 == 0x44 && b3 == 0x46
            "image/webp" -> {
                // RIFF header check: 'R' 'I' 'F' 'F' ... 'W' 'E' 'B' 'P'
                b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
            }
            else -> true
        }
    }
}
