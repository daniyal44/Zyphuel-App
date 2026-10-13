package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(private val context: Context) {

    private val TAG = "FirestoreUserRepository"

    private val db: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FirebaseFirestore instance", e)
                null
            }
        }

    /**
     * Saves or updates a user profile document in Firestore at path `users/{uid}`.
     */
    suspend fun saveOrUpdateUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        role: String = "customer",
        phoneNumber: String? = null,
        passwordHash: String? = null
    ): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                val docId = if (uid.isNotBlank()) uid else email.replace(".", "_")
                val docRef = firestore.collection("users").document(docId)

                val userMap = mutableMapOf<String, Any?>(
                    "uid" to docId,
                    "email" to email,
                    "displayName" to displayName,
                    "photoURL" to photoUrl,
                    "role" to role,
                    "lastLogin" to System.currentTimeMillis()
                )
                if (!phoneNumber.isNullOrBlank()) {
                    userMap["phoneNumber"] = phoneNumber
                }
                if (!passwordHash.isNullOrBlank()) {
                    userMap["passwordHash"] = passwordHash
                }

                docRef.set(
                    mapOf("createdAt" to System.currentTimeMillis()) + userMap,
                    SetOptions.merge()
                ).await()
            }

            Log.d(TAG, "User document upserted successfully in Firestore at users/$uid")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error upserting user in Firestore: ${e.message}")
            false
        }
    }

    /**
     * Real-time listener for a user's profile document in Firestore (`users/{uid}`).
     */
    fun observeUserProfile(
        email: String,
        onUpdate: (phoneNumber: String?, passwordHash: String?, photoUrl: String?) -> Unit
    ) {
        val firestore = db ?: return
        try {
            val docId = email.trim().replace(".", "_")
            firestore.collection("users").document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val phone = snapshot.getString("phoneNumber")
                    val pwdHash = snapshot.getString("passwordHash")
                    val photo = snapshot.getString("photoURL")
                    onUpdate(phone, pwdHash, photo)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error observing user profile in Firestore: ${e.message}")
        }
    }

    /**
     * Atomically increments the app download/install counter in Firestore at `app_stats/downloads`.
     * Called once per unique device install (guarded by SharedPreferences in MainViewModel).
     */
    suspend fun incrementAppDownloadCount(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000L) {
                val docRef = firestore.collection("app_stats").document("downloads")
                docRef.set(
                    mapOf("count" to FieldValue.increment(1), "lastUpdated" to System.currentTimeMillis()),
                    SetOptions.merge()
                ).await()
            }
            Log.d(TAG, "App download counter incremented in Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error incrementing app download counter: ${e.message}")
            false
        }
    }

    /**
     * Reads the current app download/install count from Firestore at `app_stats/downloads`.
     * Returns 0 if the document doesn't exist yet.
     */
    suspend fun getAppDownloadCount(): Long = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext 0L
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                firestore.collection("app_stats").document("downloads").get().await()
            }
            val count = snapshot?.getLong("count") ?: 0L
            Log.d(TAG, "App download count fetched from Firestore: $count")
            count
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching app download count: ${e.message}")
            0L
        }
    }

    /**
     * Saves the SMTP & Email Gateway settings to Firestore at `system_config/email_gateway`
     * so all devices (User, Rider, Admin) can seamlessly access active email dispatch configuration.
     */
    suspend fun saveEmailGatewayConfig(config: com.example.security.SmtpConfig): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                val docRef = firestore.collection("system_config").document("email_gateway")
                // SECURITY: appPassword is synced here so all devices share one gateway config, but
                // Firestore stores it in PLAINTEXT. Until email dispatch moves server-side (Cloud
                // Function / Firebase Trigger-Email extension), this document MUST be locked so only an
                // authenticated admin can read/write `system_config` — see /firestore.rules at repo root.
                val data = mapOf(
                    "host" to config.host,
                    "port" to config.port,
                    "senderEmail" to config.senderEmail,
                    "appPassword" to config.appPassword,
                    "senderName" to config.senderName,
                    "webhookUrl" to config.webhookUrl,
                    "isEnabled" to config.isEnabled,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
            }
            Log.d(TAG, "Email gateway config synced to Firestore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error saving email gateway config to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches current Email Gateway settings from Firestore.
     */
    suspend fun getEmailGatewayConfig(): com.example.security.SmtpConfig? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext null
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                firestore.collection("system_config").document("email_gateway").get().await()
            }
            if (snapshot != null && snapshot.exists()) {
                val host = snapshot.getString("host") ?: "smtp.gmail.com"
                val port = snapshot.getLong("port")?.toInt() ?: 465
                val senderEmail = snapshot.getString("senderEmail") ?: "m.daniyalkhan490@gmail.com"
                val appPassword = snapshot.getString("appPassword")?.takeIf { it.isNotBlank() } ?: ""
                val senderName = snapshot.getString("senderName") ?: "Zyphuel Delivery Operations"
                val webhookUrl = snapshot.getString("webhookUrl") ?: ""
                val isEnabled = snapshot.getBoolean("isEnabled") ?: true
                com.example.security.SmtpConfig(
                    host = host,
                    port = port,
                    senderEmail = senderEmail,
                    appPassword = appPassword,
                    senderName = senderName,
                    webhookUrl = webhookUrl,
                    isEnabled = isEnabled
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching email gateway config from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Listens in real-time to Email Gateway configuration updates from Firestore.
     */
    fun observeEmailGatewayConfig(
        onConfigChanged: (com.example.security.SmtpConfig) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            firestore.collection("system_config").document("email_gateway")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for email gateway config: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val host = snapshot.getString("host") ?: "smtp.gmail.com"
                        val port = snapshot.getLong("port")?.toInt() ?: 465
                        val senderEmail = snapshot.getString("senderEmail") ?: "m.daniyalkhan490@gmail.com"
                        val appPassword = snapshot.getString("appPassword")?.takeIf { it.isNotBlank() } ?: ""
                        val senderName = snapshot.getString("senderName") ?: "Zyphuel Delivery Operations"
                        val webhookUrl = snapshot.getString("webhookUrl") ?: ""
                        val isEnabled = snapshot.getBoolean("isEnabled") ?: true
                        onConfigChanged(
                            com.example.security.SmtpConfig(
                                host = host,
                                port = port,
                                senderEmail = senderEmail,
                                appPassword = appPassword,
                                senderName = senderName,
                                webhookUrl = webhookUrl,
                                isEnabled = isEnabled
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up email gateway listener: ${e.message}")
            null
        }
    }

    /**
     * Saves or updates the latest official application version metadata in Firestore at `system_config/app_version`.
     * Devices running an older versionCode / versionName than this document automatically display the
     * "New Version Available — Download Now" message bar, while devices on the latest version do not.
     */
    suspend fun saveAppVersionConfig(config: AppVersionConfig): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                val docRef = firestore.collection("system_config").document("app_version")
                val data = mapOf(
                    "latestVersionCode" to config.latestVersionCode,
                    "latestVersionName" to config.latestVersionName,
                    "downloadUrl" to config.downloadUrl,
                    "releaseNotes" to config.releaseNotes,
                    "forceUpdate" to config.forceUpdate,
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
            }
            Log.d(TAG, "App version config synced to Firestore: ${config.latestVersionName} (${config.latestVersionCode})")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error saving app version config to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Fetches the current latest application version metadata from Firestore at `system_config/app_version`.
     */
    suspend fun getAppVersionConfig(): AppVersionConfig? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext null
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                firestore.collection("system_config").document("app_version").get().await()
            }
            if (snapshot != null && snapshot.exists()) {
                val code = snapshot.getLong("latestVersionCode")?.toInt() ?: com.example.BuildConfig.VERSION_CODE
                val name = snapshot.getString("latestVersionName")?.takeIf { it.isNotBlank() } ?: com.example.BuildConfig.VERSION_NAME
                val url = snapshot.getString("downloadUrl")?.takeIf { it.isNotBlank() }
                    ?: "https://play.google.com/store/apps/details?id=com.aistudio.zyphuel.appv2"
                val notes = snapshot.getString("releaseNotes")?.takeIf { it.isNotBlank() }
                    ?: "New version available with real-time profile sync, 68+ world languages, and faster live tracking!"
                val force = snapshot.getBoolean("forceUpdate") ?: false
                val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                AppVersionConfig(
                    latestVersionCode = code,
                    latestVersionName = name,
                    downloadUrl = url,
                    releaseNotes = notes,
                    forceUpdate = force,
                    updatedAt = updatedAt
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching app version config from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Real-time listener for `system_config/app_version` so all connected devices immediately detect
     * when a newer version is released and show/hide the update message bar accordingly.
     */
    fun observeAppVersionConfig(
        onVersionChanged: (AppVersionConfig) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            firestore.collection("system_config").document("app_version")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val code = snapshot.getLong("latestVersionCode")?.toInt() ?: return@addSnapshotListener
                    val name = snapshot.getString("latestVersionName")?.takeIf { it.isNotBlank() } ?: com.example.BuildConfig.VERSION_NAME
                    val url = snapshot.getString("downloadUrl")?.takeIf { it.isNotBlank() }
                        ?: "https://play.google.com/store/apps/details?id=com.aistudio.zyphuel.appv2"
                    val notes = snapshot.getString("releaseNotes")?.takeIf { it.isNotBlank() }
                        ?: "New version available with real-time profile sync, 68+ world languages, and faster live tracking!"
                    val force = snapshot.getBoolean("forceUpdate") ?: false
                    val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    onVersionChanged(
                        AppVersionConfig(
                            latestVersionCode = code,
                            latestVersionName = name,
                            downloadUrl = url,
                            releaseNotes = notes,
                            forceUpdate = force,
                            updatedAt = updatedAt
                        )
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error observing app version config in Firestore: ${e.message}")
            null
        }
    }

    /**
     * Saves live OGRA base fuel rates to Firestore at `system_config/fuel_rates` for real-time cross-device sync.
     */
    suspend fun saveFuelRatesConfig(petrol: Float, diesel: Float, highOctane: Float): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val firestore = db ?: return@withContext false
        try {
            kotlinx.coroutines.withTimeoutOrNull(2000L) {
                firestore.collection("system_config").document("fuel_rates").set(
                    mapOf(
                        "petrol" to petrol.toDouble(),
                        "diesel" to diesel.toDouble(),
                        "highOctane" to highOctane.toDouble(),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error saving fuel rates to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Real-time listener for `system_config/fuel_rates` so fuel prices stay synced across all devices.
     */
    fun observeFuelRatesConfig(
        onRatesChanged: (petrol: Float, diesel: Float, highOctane: Float) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            firestore.collection("system_config").document("fuel_rates")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val petrol = snapshot.getDouble("petrol")?.toFloat() ?: return@addSnapshotListener
                    val diesel = snapshot.getDouble("diesel")?.toFloat() ?: return@addSnapshotListener
                    val highOctane = snapshot.getDouble("highOctane")?.toFloat() ?: 325.00f
                    onRatesChanged(petrol, diesel, highOctane)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error observing fuel rates in Firestore: ${e.message}")
            null
        }
    }
}

data class AppVersionConfig(
    val latestVersionCode: Int = com.example.BuildConfig.VERSION_CODE,
    val latestVersionName: String = com.example.BuildConfig.VERSION_NAME,
    val downloadUrl: String = "https://play.google.com/store/apps/details?id=com.aistudio.zyphuel.appv2",
    val releaseNotes: String = "New version available with real-time profile sync, 68+ world languages & faster tracking!",
    val forceUpdate: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

